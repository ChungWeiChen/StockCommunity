package com.stockcommunity.stocks.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StockDataDTO(
		@JsonProperty("公司代號") 
		String stockCode, // 股票代號，例如 "2330"，自然鍵，不用流水號 id

		@JsonProperty("公司名稱") 
		String companyName, // 公司法定全名，例如 "台灣積體電路製造股份有限公司"

		@JsonProperty("公司簡稱") 
		String simpleName, // 股票簡稱，例如 "台積電"，畫面顯示用；取代原本語意模糊的 stockName

		@JsonProperty("") String 
		market, // 上市 / 上櫃

		@JsonProperty("產業別") 
		String industry, // 產業別，例如 "半導體業"

		@JsonProperty("實收資本額") 
		Long capital
) {}
