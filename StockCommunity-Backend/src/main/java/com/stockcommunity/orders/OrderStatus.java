package com.stockcommunity.orders;

public enum OrderStatus {
    PENDING, 
    PARTIALLY_FILLED, // 部分成交
    PARTIALLY_FILLED_CANCELLED, // 部分成交 , 剩餘委託取消, 用於IOC
    FILLED, // 全部成交
    CANCELLED, 
}
