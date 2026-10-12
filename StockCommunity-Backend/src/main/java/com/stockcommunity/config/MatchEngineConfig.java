package com.stockcommunity.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

// 搓合引擎專用的執行緒池設定

@Configuration
public class MatchEngineConfig {
	@Bean(name = "matchEngineExecutor")
    Executor matchEngineExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setThreadNamePrefix("Match-Engine-Worker-");
        
        // 啟用優雅停機：關閉時等待任務完成，最多等 30 秒
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        return executor;
    }
}
