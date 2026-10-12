package com.stockcommunity.matchengine;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.stockcommunity.matchengine.dto.FinalOutcome;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

//專門負責處理DB寫入佇列，把資料寫進 DB
@Slf4j
@Component
@RequiredArgsConstructor
public class DbPersistenceWorker {

    private final BlockingQueue<FinalOutcome> dbWriteQueue;

    private final TradeResultSaveService tradeResultService;

    private volatile boolean running = true;

    @Async("dbPersistenceExecutor")
    @EventListener(ApplicationReadyEvent.class)
    public void startWorker() {
        log.info("DB Persistence Worker 已啟動");

        // 啟動一條獨立的背景執行緒來處理 DB 寫入
        try {
            while (running || !dbWriteQueue.isEmpty()) {

                FinalOutcome result =
                        dbWriteQueue.poll(1, TimeUnit.SECONDS);

                if (result == null) {
                    continue;
                }

                try {
                    tradeResultService.saveToDatabase(result);

                    log.info(
                        "DB 寫入完成，目前 Queue 剩餘 {} 筆",
                        dbWriteQueue.size()
                    );

                } catch (Exception e) {
                    log.error(
                        "DB 寫入失敗，跳過此筆資料",
                        e
                    );
                }
            }

        } catch (InterruptedException e) {

            /*
            * Executor shutdown 時可能中斷 Worker。
            *
            * 此時不立即退出，而是進入 shutdown drain。
            */
            log.info(
                "DB Worker 收到 shutdown interrupt，開始排空 Queue，目前剩餘 {} 筆",
                dbWriteQueue.size()
            );

            // 不再等待新資料，只處理目前已存在的資料
            FinalOutcome result;

            while ((result = dbWriteQueue.poll()) != null) {

                try {
                    tradeResultService.saveToDatabase(result);

                    log.info(
                        "Shutdown DB 寫入完成，目前 Queue 剩餘 {} 筆",
                        dbWriteQueue.size()
                    );

                } catch (Exception ex) {
                    log.error(
                        "Shutdown 階段 DB 寫入失敗",
                        ex
                    );
                }
            }
        }

        log.info(
            "DB Persistence Worker 已完成關閉，Queue 剩餘 {} 筆",
            dbWriteQueue.size()
        );
        

    }

    // 新增此方法：在 Spring 容器銷毀這個 Bean 時主動中斷執行緒
    @PreDestroy
    public void stopWorker() {

        int remaining = dbWriteQueue.size();

        log.info(
            "正在停止 DB Persistence Worker，目前 Queue 剩餘 {} 筆",
            remaining
        );

        running = false;

    }

}