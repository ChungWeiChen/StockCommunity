package com.stockcommunity.matchengine.dto;

import java.math.BigDecimal;


public record HoldingUpdate(
	Long userId,  // 對應 users.id

	String stockCode,  // 股票代號

	Integer quantity,  // 新購入的股數

	BigDecimal price   // 本次成交價（後面要用來重新計算平均成本）
){
	// 工廠方法
	public static HoldingUpdate create(
			Long userId,  
			String stockCode, 
			Integer quantity,  
			BigDecimal price) {
        return new HoldingUpdate(userId, stockCode, quantity, price);
    }
	
}
