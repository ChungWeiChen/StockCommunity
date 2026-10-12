package com.stockcommunity.orders;

public enum TradingType {
    ROD,  // 當日有效 (Rest of Day)
    IOC,  // 立即成交否則取消 (Immediate or Cancel)
    FOK   // 全部成交否則取消 (Fill or Kill)
}
