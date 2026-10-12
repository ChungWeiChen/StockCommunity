package com.stockcommunity.matchengine.dto;

import com.stockcommunity.orders.OrderStatus;

public record OrderUpdate(
		Long orderid,

		Long userId,

		String stockCode,

		// 剩餘委託股數
		Integer remainingQuantity,

		// PENDING 待處理/等待中, PARTIALLY_FILLED 部分成交,
		// FILLED 已成交, CANCELLED 已取消
		OrderStatus status
) {
	// 工廠方法
	public static OrderUpdate create(
			Long id,
			Long userId,
			String stockCode,
			Integer remainingQuantity,
			OrderStatus status) {
		return new OrderUpdate(
				id,
				userId,
				stockCode,
				remainingQuantity,
				status);
	}
}
