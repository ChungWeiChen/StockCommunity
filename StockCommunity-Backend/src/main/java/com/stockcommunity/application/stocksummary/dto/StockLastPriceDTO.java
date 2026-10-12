package com.stockcommunity.application.stocksummary.dto;

import java.math.BigDecimal;


public record StockLastPriceDTO(
		// 股票代號
		String stockCode,  
		// 股票簡稱
	    String simpleName,   
	    // 傳回去的價格(收盤價或本日上一筆成交價)
	    BigDecimal price) {
	public static StockLastPriceDTO create(
			String stockCode,  
		    String simpleName,  
		    BigDecimal price) {
		return new StockLastPriceDTO(stockCode, simpleName, price);
	}
	
}
