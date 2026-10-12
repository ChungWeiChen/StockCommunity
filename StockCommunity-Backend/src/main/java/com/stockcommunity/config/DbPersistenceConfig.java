package com.stockcommunity.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class DbPersistenceConfig {

    @Bean(name = "dbPersistenceExecutor")
    Executor dbPersistenceExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(100);

        executor.setThreadNamePrefix("DB-Write-Worker-");

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        
        // 🔥 關鍵設定 1：拒絕新任務，但允許排隊中的任務執行完畢
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 🔥 關鍵設定 2：設定最長等待時間（例如 30 秒），避免無限期卡死
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        return executor;
    }
}
