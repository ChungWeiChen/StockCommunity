package com.stockcommunity.event;

import lombok.Getter;

@Getter // 方便監聽方讀取裡面的資料
public class StockDataUpdatedEvent {
    
    private final String message;
    private final long timestamp;

    public StockDataUpdatedEvent(String message) {
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
}