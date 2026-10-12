package com.stockcommunity.matchengine.dto;

import java.math.BigDecimal;


public record TradeRecord(
	Long buyOrderId, // 對應買方那筆 Order 的 id
	Long sellOrderId, // 對應賣方那筆 Order 的 id
	String stockCode, // 成交的股票代號
	BigDecimal price, // 成交價
	Integer quantity // 成交量
) {
	// 工廠方法
	public static TradeRecord create(
			Long buyOrderId, // 對應買方那筆 Order 的 id
			Long sellOrderId, // 對應賣方那筆 Order 的 id
			String stockCode, // 成交的股票代號
			BigDecimal price, // 成交價
			Integer quantity) {
        return new TradeRecord(buyOrderId, sellOrderId, stockCode, price, quantity);
    }
}
