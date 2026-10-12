package com.stockcommunity.posts.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PopularStockDTO {

    private String stockCode;
    
    private String simpleName;
    
    private Long postCount;

}
