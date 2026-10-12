package com.stockcommunity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling  // 🌟 必須加上這個註解，排程才會生效
@EnableAsync // <-- 記得加上這行開啟非同步功能
@SpringBootApplication
public class StockCommunityApplication {

	public static void main(String[] args) {
		SpringApplication.run(StockCommunityApplication.class, args);
	}

}
