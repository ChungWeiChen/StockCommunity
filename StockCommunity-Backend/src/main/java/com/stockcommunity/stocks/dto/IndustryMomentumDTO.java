package com.stockcommunity.stocks.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 「今日領漲/領跌產業」單一產業的統計結果，給前端首頁小卡片用
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IndustryMomentumDTO {
    private String industryCode;         // 產業代號，對應 industry_category.id
    private String industryName;         // 產業中文名稱，對應 industry_category.name
    private BigDecimal avgChangePercent; // 該產業底下所有股票，當日漲跌幅的平均值(百分比)
    private int stockCount;              // 這次平均值是用幾檔股票算出來的，方便debug或前端顯示參考用
}