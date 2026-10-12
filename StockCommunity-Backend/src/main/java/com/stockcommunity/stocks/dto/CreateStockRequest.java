package com.stockcommunity.stocks.dto;

import lombok.Data;

@Data
public class CreateStockRequest {
    private String stockCode;
    private String companyName;
    private String simpleName;
    private String market;
    private String industry;
    private Long capital;   // 選填，股本；沒有資料來源時可以不填
}
