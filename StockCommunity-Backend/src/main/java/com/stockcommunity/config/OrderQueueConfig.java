package com.stockcommunity.config;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.stockcommunity.orders.Order;


//用 Config 宣告 Bean
@Configuration
public class OrderQueueConfig {
    @Bean
    BlockingQueue<Order> matchOrderQueue() {
        return new LinkedBlockingQueue<>(10000);
    }

    // @Bean
    // BlockingQueue<Order> limitOrderQueue() {
    //     return new LinkedBlockingQueue<>(10000);
    // }

    // @Bean
    // BlockingQueue<Order> marketOrderQueue() {
    //     return new LinkedBlockingQueue<>(10000);
    // }
}
