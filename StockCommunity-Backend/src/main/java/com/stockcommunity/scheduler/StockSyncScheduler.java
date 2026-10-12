package com.stockcommunity.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.stockcommunity.stocks.StockDataSyncService;
import com.stockcommunity.stocks.finmind.FinMindStockPriceSyncService;
import com.stockcommunity.event.StockDataUpdatedEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class StockSyncScheduler {

    private final StockDataSyncService stockDataSyncService;
    private final ApplicationEventPublisher eventPublisher;
    private final FinMindStockPriceSyncService finMindStockPriceSyncService;

    @Value("#{'${stock.backfill.codes}'.split(',')}")
    private List<String> backfillStockCodes;

    @PostConstruct
    public void initialize() {
        try {
            log.info("啟動初始化：開始獲取第一批股票資料...");
            stockDataSyncService.fetchAndSyncExternalStockData();

            // 關鍵：強迫停頓 5 秒，讓前一個連線完全釋放，也避免觸發證交所 WAF
            Thread.sleep(5000);

            log.info("啟動初始化：開始獲取第二批歷史價位資料...");
            stockDataSyncService.fetchAndSyncExternalStockPriceData();

            // 新增：每次開機都順便幫預設股票補齊近 5 年的歷史資料
            // 不用管展示當天用哪台電腦，只要程式有跑起來就會自動補，不用再手動打 API
            // 原本 syncHistoryPriceJob() 掛 cron 排程每天下午3點才跑，展示當天早上開機根本還沒到那個時間，
            // 圖表會是空的，所以改成開機時就直接補，不等 cron
            log.info("啟動初始化：開始補齊預設股票的長天期歷史資料(近5年)...");
            LocalDate backfillEndDate = LocalDate.now();
            LocalDate backfillStartDate = backfillEndDate.minusYears(5);
            finMindStockPriceSyncService.backfillHistoryForStocks(
                    backfillStockCodes, backfillStartDate, backfillEndDate);

            eventPublisher.publishEvent(new StockDataUpdatedEvent("股票資料更新完成"));
            log.info("股票與歷史價位資料獲取完成");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("初始化等待被中斷", e);
        } catch (Exception e) {
            log.error("初始化獲取失敗", e);
        }
    }

    @Scheduled(
            initialDelayString = "${stock.sync.initial-delay}",
            fixedRateString = "${stock.sync.fixed-rate:604800000}"
    )
    public void syncCompanyInfoJob() {
        log.info("正在執行公司資訊同步作業...");
        try {
            log.info("排程啟動:開始獲取第一批股票資料...");
            stockDataSyncService.fetchAndSyncExternalStockData();

            Thread.sleep(4000);

            log.info("排程啟動：開始獲取第二批歷史價位資料...");
            stockDataSyncService.fetchAndSyncExternalStockPriceData();

            log.info("資訊同步作業完成。");
        } catch (Exception e) {
            log.error("資訊同步失敗，原因: {}", e.getMessage(), e);
        }
    }

    @Scheduled(cron = "${stock.backfill.cron}")
    public void syncHistoryPriceJob() {
        log.info("▶ 正在執行歷史股價補資料排程...");
        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusMonths(3);

            finMindStockPriceSyncService.backfillHistoryForStocks(
                    backfillStockCodes, startDate, endDate);

            log.info("歷史股價補資料排程完成。");
        } catch (Exception e) {
            log.error("歷史股價補資料排程失敗，原因: {}", e.getMessage(), e);
        }
    }
}