package com.stockcommunity.config;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.stockcommunity.matchengine.dto.FinalOutcome;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


//1. 宣告一個專門存放成交結果的佇列 Bean
@Configuration
public class DbQueueConfig {
	@Bean
	BlockingQueue<FinalOutcome> dbWriteQueue() {
		// 設定一個有界佇列（例如最大允許堆積 5 萬筆），避免記憶體爆掉
		return new LinkedBlockingQueue<>(50000);
	}
}
