package com.stockcommunity.config;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executors;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;


@Configuration
public class RestClientConfig {

	@Bean 
	RestClient restClient() {
        // 1. 使用 Java 原生 HttpClient 配置連線池與執行緒
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5)) // 連線超時 5 秒
                // 根據 CPU 核心數設定執行緒數量
                .executor(Executors.newFixedThreadPool(
                		Runtime.getRuntime().availableProcessors() * 2)) 
                .build();

        // 2. 包裝成 Spring 的 HttpRequestFactory
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10)); // 讀取超時 10 秒

        // 👈 使用 RestClient.builder() 靜態方法，不依賴 Spring 注入
        return RestClient.builder() 
                .baseUrl("https://example.com")
                .requestFactory(requestFactory)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}