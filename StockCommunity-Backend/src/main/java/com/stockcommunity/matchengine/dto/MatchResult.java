package com.stockcommunity.matchengine.dto;

import java.time.LocalDateTime;

// 搓合成功後的領域物件，存放要新增更新trade跟holding的DTO

public record MatchResult(
		TradeRecord tradeRecord, // 用於寫入 trade 表

		OrderUpdate buyerOrderUpdate,
		OrderUpdate sellerOrderUpdate,

		HoldingUpdate buyerholdingUpdate, // 用於更新買方的 holding 表
		HoldingUpdate sellerholdingUpdate, // 用於更新賣方的 holding 表

		LocalDateTime timestamp // 先用LocalDateTime
) {
	// 工廠方法，讓建立更直覺
	public static MatchResult create(
			TradeRecord trade,
			OrderUpdate buyerOrderUpdate,
			OrderUpdate sellerOrderUpdate,
			HoldingUpdate buyerholdingUpdate,
			HoldingUpdate sellerholdingUpdate) {
		return new MatchResult(trade,
				buyerOrderUpdate,
				sellerOrderUpdate,
				buyerholdingUpdate,
				sellerholdingUpdate,
				LocalDateTime.now());
	}
}
