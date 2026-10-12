package com.stockcommunity.posts.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class StockSummaryDTO {

    private String stockCode;
    
    private String simpleName;
    
    private Long postCount;
}
