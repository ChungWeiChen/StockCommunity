package com.stockcommunity.stocks.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.stockcommunity.util.MinguoLocalDateDeserializer;

import tools.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StockPriceDataDTO(
		@JsonProperty("Code") 
		String stockCode, // 股票代號，對應 stocks.stockCode

		// 用自訂的反序列化器
		@JsonDeserialize(using = MinguoLocalDateDeserializer.class) @JsonProperty("Date") 
		LocalDate tradeDate, // 交易日期（只到日期，不含時間）

		@JsonProperty("OpeningPrice") 
		BigDecimal openPrice, // 開盤價

		@JsonProperty("HighestPrice") 
		BigDecimal highPrice, // 最高價

		@JsonProperty("LowestPrice") 
		BigDecimal lowPrice, // 最低價

		@JsonProperty("ClosingPrice") 
		BigDecimal closePrice, // 收盤價

		@JsonProperty("TradeVolume") 
		Long volume // 成交量

) {}
