package com.stockcommunity.stocks.finmind;

import com.stockcommunity.stocks.stockprice.StockPrice;
import com.stockcommunity.stocks.stockprice.StockPriceRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinMindStockPriceSyncService {

    private final StockPriceRepository stockPriceRepository;

    @Value("${finmind.base-url:https://api.finmindtrade.com/api/v4}")
    private String baseUrl;

    // 有去 FinMind 官網註冊帳號拿到 token 的話填進 application.properties，額度會比較高；
    // 留空也能動，只是免費未登入的額度比較小
    @Value("${finmind.token:}")
    private String token;

    // 批次補資料時，每打完一檔股票要停頓幾毫秒，避免短時間內連續呼叫被限流
    // OOVE 之前抓資料抓到爆掉、後來延遲 5 秒左右才穩定，這裡先保守設 2 秒，
    // 之後測試發現還是被擋（log 會印出 status 402），再拉高這個數字即可，不用改程式碼
    @Value("${finmind.backfill.delay-ms:2000}")
    private long delayMs;

    private RestClient restClient;

    @PostConstruct
    private void init() {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    // 補齊「單一檔股票」在指定區間內、資料庫還沒有的歷史行情
    // 回傳這次實際新增了幾筆
    public int backfillHistory(String stockCode, LocalDate startDate, LocalDate endDate) {

        FinMindResponse<FinMindStockPriceDTO> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/data")
                        .queryParam("dataset", "TaiwanStockPrice")
                        .queryParam("data_id", stockCode)
                        .queryParam("start_date", startDate)
                        .queryParam("end_date", endDate)
                        .build())
                .headers(headers -> {
                    if (token != null && !token.isBlank()) {
                        headers.setBearerAuth(token);
                    }
                })
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response == null || response.status() != 200
                || response.data() == null || response.data().isEmpty()) {
            log.info("FinMind 沒有回傳 {} 在 {}~{} 的資料，狀態碼: {}，訊息: {}",
                    stockCode, startDate, endDate,
                    response != null ? response.status() : null,
                    response != null ? response.msg() : null);
            return 0;
        }

        // 查資料庫裡這檔股票這段區間已經有的日期，等一下要跳過，避免撞 unique constraint
        Set<LocalDate> existingDates = new HashSet<>(
                stockPriceRepository.findExistingTradeDatesByStockCodeAndRange(stockCode, startDate, endDate));

        List<StockPrice> toSave = new ArrayList<>();
        int skippedNullCount = 0;

        for (FinMindStockPriceDTO dto : response.data()) {
            if (dto.date() == null || existingDates.contains(dto.date())) {
                continue;
            }

            // 開高低收只要有任何一個是 null，這筆資料就不完整，直接跳過，
            // 避免寫入時撞到 StockPrice 四個價格欄位的 NOT NULL 限制（跟 orders.quantity 那次同一種錯）
            if (dto.open() == null || dto.max() == null
                    || dto.min() == null || dto.close() == null) {
                skippedNullCount++;
                continue;
            }
            // 收盤價是 0 代表當天沒有實際成交（停牌/剛上市前），這種也不是真正的行情資料
            if (dto.close().compareTo(BigDecimal.ZERO) <= 0) {
                skippedNullCount++;
                continue;
            }

            StockPrice sp = new StockPrice();
            sp.setStockCode(stockCode);
            sp.setTradeDate(dto.date());
            sp.setOpenPrice(dto.open());
            sp.setHighPrice(dto.max());
            sp.setLowPrice(dto.min());
            sp.setClosePrice(dto.close());
            sp.setVolume(dto.tradingVolume() != null ? dto.tradingVolume() : 0L);
            toSave.add(sp);
        }

        if (!toSave.isEmpty()) {
            stockPriceRepository.saveAll(toSave);
        }

        log.info("{} 補歷史資料完成，新增 {} 筆，跳過不完整資料 {} 筆",
                stockCode, toSave.size(), skippedNullCount);
        return toSave.size();
    }

      // 已在下面修改方法，新增查詢資料庫最新交易日的邏輯，避免重複打FinMind API
//    // 批次補多檔股票，中間刻意間隔 delayMs 毫秒，避免短時間內打太多次 FinMind 被限流
//    public void backfillHistoryForStocks(List<String> stockCodes, LocalDate startDate, LocalDate endDate) {
//        for (String stockCode : stockCodes) {
//            try {
//                backfillHistory(stockCode, startDate, endDate);
//                Thread.sleep(delayMs);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//                log.warn("補歷史資料被中斷", e);
//                return;
//            } catch (Exception e) {
//                log.error("{} 補歷史資料失敗: {}", stockCode, e.getMessage());
//            }
//        }
//    }

    // 批次補多檔股票，中間刻意間隔 delayMs 毫秒，避免短時間內打太多次 FinMind 被限流
    public void backfillHistoryForStocks(List<String> stockCodes, LocalDate startDate, LocalDate endDate) {
        for (String stockCode : stockCodes) {
            try {
                // 新增：先查這檔股票資料庫裡最新的交易日期，如果已經接近 endDate（3天內，
                // 抓寬一點是為了跨週末），代表資料已經是最新的，不需要再重打一次FinMind，
                // 直接跳過這一檔，讓重複開機時能快速跳過已同步的股票
                Optional<LocalDate> latestDate = stockPriceRepository.findLatestTradeDateByStockCode(stockCode);
                if (latestDate.isPresent() && !latestDate.get().isBefore(endDate.minusDays(3))) {
                    log.info("{} 資料已是最新（最後交易日 {}），跳過本次補資料", stockCode, latestDate.get());
                    continue;
                }

                backfillHistory(stockCode, startDate, endDate);
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("補歷史資料被中斷", e);
                return;
            } catch (Exception e) {
                log.error("{} 補歷史資料失敗: {}", stockCode, e.getMessage());
            }
        }
    }
}