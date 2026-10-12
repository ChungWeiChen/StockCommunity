package com.stockcommunity.stocks.Industry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.stockcommunity.stocks.dto.IndustryMomentumDTO;
import com.stockcommunity.stocks.dto.IndustryMomentumResult;
import com.stockcommunity.stocks.stock.Stock;
import com.stockcommunity.stocks.stock.StockRepository;
import com.stockcommunity.stocks.stockprice.StockPrice;
import com.stockcommunity.stocks.stockprice.StockPriceRepository;

import lombok.RequiredArgsConstructor;

// 負責計算「今日領漲/領跌產業」，給社群首頁的市場焦點卡片用
@Service
@RequiredArgsConstructor
public class IndustryMomentumService {

    private final StockPriceRepository stockPriceRepository;
    private final StockRepository stockRepository;
    private final IndustryCategoryRepository industryCategoryRepository;

    // 【9/26 加快取】股價資料一天只會更新一次，切頁/重新整理卻會一直重算，
    // 除了浪費運算，還會跟造市者搶DB連線、容易逾時。
    // 這裡快取的是「全部產業」算好的結果，topN只是從裡面取前幾名，
    // 不用因為topN不同就重算，所以快取不用跟topN綁在一起。
    private volatile List<IndustryMomentumDTO> cachedAll = null;
    private volatile LocalDateTime lastComputeTime = null;

    // 【9/26 加快取】後端一啟動就先在背景把這份資料算好放進快取，
    // 使用者不用等第一次真的呼叫這支API時才臨時算，也不會卡在前端5秒逾時。
    // 丟到背景執行緒算，不擋住後端啟動流程。
    @EventListener(ApplicationReadyEvent.class)
    public void warmUpCacheOnStartup() {
        CompletableFuture.runAsync(() -> {
            System.out.println("=== 產業漲跌排行：啟動預熱開始 ===");
            computeAll();
            System.out.println("=== 產業漲跌排行：啟動預熱完成 ===");
        });
    }

    // topN：領漲、領跌各要回傳前幾名，由Controller那邊的參數決定
    public IndustryMomentumResult calculateIndustryMomentum(int topN) {

        List<IndustryMomentumDTO> all = getAllWithCache();

        // 漲幅平均值由高到低排序，取前topN名 = 領漲產業
        List<IndustryMomentumDTO> gainers = all.stream()
                .sorted(Comparator.comparing(IndustryMomentumDTO::getAvgChangePercent).reversed())
                .limit(topN)
                .toList();

        // 漲幅平均值由低到高排序，取前topN名 = 領跌產業
        List<IndustryMomentumDTO> losers = all.stream()
                .sorted(Comparator.comparing(IndustryMomentumDTO::getAvgChangePercent))
                .limit(topN)
                .toList();

        return new IndustryMomentumResult(gainers, losers);
    }

    // 有快取而且還沒過期(10分鐘內)就直接回傳，不然就重新計算
    private List<IndustryMomentumDTO> getAllWithCache() {
        LocalDateTime lastUpdate = lastComputeTime;
        List<IndustryMomentumDTO> cached = cachedAll;

        if (cached != null
                && lastUpdate != null
                && lastUpdate.plusHours(6).isAfter(LocalDateTime.now())) {

            System.out.println("=== 產業漲跌排行：使用快取 ===");
            return cached;
        }

        return computeAll();
    }

    // 真正去DB算一次，算完存進快取
    private synchronized List<IndustryMomentumDTO> computeAll() {

        // 雙重檢查：如果剛好有另一個請求已經算完存進快取了，直接用，不用重算一次
        LocalDateTime lastUpdate = lastComputeTime;
        List<IndustryMomentumDTO> cached = cachedAll;
        if (cached != null
                && lastUpdate != null
                && lastUpdate.plusHours(6).isAfter(LocalDateTime.now())) {
            return cached;
        }

        System.out.println("=== 產業漲跌排行：重新計算 ===");

        // 每檔股票最新一筆股價(今日收盤價)
        List<StockPrice> latestPrices = stockPriceRepository.findLatestPricePerStock();

        // 股票代號 -> 產業代號 的對照表，一次查完避免在迴圈裡重複打資料庫(N+1問題)
        Map<String, String> stockToIndustry = stockRepository.findAll().stream()
                .collect(Collectors.toMap(Stock::getStockCode, Stock::getIndustry));

        // 產業代號 -> 中文名稱 的對照表
        Map<String, String> industryNames = industryCategoryRepository.findAll().stream()
                .collect(Collectors.toMap(IndustryCategory::getId, IndustryCategory::getName));

        // 【9/26 效能優化】前一天收盤價原本是在下面迴圈裡「每檔股票各查一次」，
        // 股票一多、頁面被快速切換時就會變成幾百次查詢把DB塞滿。
        // 改法：先依「交易日期」分組(通常全部股票都是同一天)，同一天的股票一次批次查完，
        // 查完存進Map，下面迴圈只查記憶體裡的Map，不再逐檔打資料庫。
        Map<String, BigDecimal> t1CloseByStockCode = new HashMap<>();
        Map<LocalDate, List<String>> stockCodesByTradeDate = latestPrices.stream()
                .collect(Collectors.groupingBy(StockPrice::getTradeDate,
                        Collectors.mapping(StockPrice::getStockCode, Collectors.toList())));

        for (Map.Entry<LocalDate, List<String>> entry : stockCodesByTradeDate.entrySet()) {
            List<Object[]> rows = stockPriceRepository.findT1ClosePricesBatch(
                    entry.getValue(), entry.getKey());
            for (Object[] row : rows) {
                t1CloseByStockCode.put((String) row[0], (BigDecimal) row[1]);
            }
        }

        // 暫存「這檔股票屬於哪個產業、當日漲跌幅是多少」，只在這個方法裡用，不用另外開檔案
        record StockChange(String industryCode, double changePercent) {}

        List<StockChange> changes = latestPrices.stream()
                .map(sp -> {
                    // 今日收盤價是0，代表資料異常(常見於ETF/債券當天無成交、或FinMind匯入失敗)，
                    // 不是真的跌到0，採用會算出假的-100%，直接跳過不採計
                    if (sp.getClosePrice().compareTo(BigDecimal.ZERO) == 0) return null;

                    String industryCode = stockToIndustry.get(sp.getStockCode());
                    if (industryCode == null) return null; // 查不到產業別(股票主檔缺資料，或本身就不屬於26個產業分類)就跳過

                    // 前一個交易日收盤價，改成查上面已經批次查好的Map，不再逐檔打資料庫
                    // 查不到代表這檔股票只有今天一筆資料、沒得比較，跳過；是0同樣是資料異常，跳過避免除以零
                    BigDecimal t1ClosePrice = t1CloseByStockCode.get(sp.getStockCode());
                    if (t1ClosePrice == null || t1ClosePrice.compareTo(BigDecimal.ZERO) == 0) return null;

                    // 漲跌幅 = (今日收盤 - 前一天收盤) / 前一天收盤 * 100
                    double changePercent = sp.getClosePrice()
                            .subtract(t1ClosePrice)
                            .divide(t1ClosePrice, 6, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .doubleValue();

                    return new StockChange(industryCode, changePercent);
                })
                .filter(Objects::nonNull)
                .toList();

        // 依產業代號分組，DoubleSummaryStatistics可以一次拿到平均值跟股票數量，不用自己另外算
        Map<String, DoubleSummaryStatistics> grouped = changes.stream()
                .collect(Collectors.groupingBy(StockChange::industryCode,
                        Collectors.summarizingDouble(StockChange::changePercent)));

        // 組成前端要的DTO，平均值四捨五入到小數點後2位
        List<IndustryMomentumDTO> all = grouped.entrySet().stream()
                .map(e -> new IndustryMomentumDTO(
                        e.getKey(),
                        industryNames.getOrDefault(e.getKey(), e.getKey()),
                        BigDecimal.valueOf(e.getValue().getAverage()).setScale(2, RoundingMode.HALF_UP),
                        (int) e.getValue().getCount()))
                .toList();

        cachedAll = all;
        lastComputeTime = LocalDateTime.now();

        return all;
    }
}