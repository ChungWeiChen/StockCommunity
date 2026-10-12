package com.stockcommunity.scheduler;

import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import com.stockcommunity.init.SystemAccountInitService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
@DependsOn("stockSyncScheduler") // 確保這個bean會在這個排程bean的初始化後才創建，才能確保一定拿的到股票資料
public class SystemAccountInitializer {
    private final SystemAccountInitService systemAccountInitService;

    @PostConstruct
    public void initMarketMaker() {
        log.info("正在執行造市者模組所用資料初始化作業...");
        try {
            // 呼叫你去外面獲取資料並存進資料庫的方法
            systemAccountInitService.initSystemAccount();
            systemAccountInitService.initSystemHolding();
            
            log.info(" 造市者初始化作業完成。");
        } catch (Exception e) {
            log.error(" 造市者初始化失敗，原因: {}", e.getMessage(), e);
        }
    }

    
}
