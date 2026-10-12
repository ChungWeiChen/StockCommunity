package com.stockcommunity.orderbook.dto;

import java.math.BigDecimal;

// 最優買賣價 DTO
public record BestPriceCache(
		BigDecimal BestBidPrice, // 買
		BigDecimal BestAskPrice	// 賣
) {
	public static BestPriceCache create(
			BigDecimal BestBidPrice, 
			BigDecimal BestAskPrice) {
		return new BestPriceCache(
			BestBidPrice, 
			BestAskPrice);
	}
}
