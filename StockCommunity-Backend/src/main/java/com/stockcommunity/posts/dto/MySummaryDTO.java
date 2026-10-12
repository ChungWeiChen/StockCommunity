package com.stockcommunity.posts.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class MySummaryDTO {

    private Long userId;
    
    private String username;

    private Long postCount;

    private Long commentCount;

    // 我的貼文總共收到多少讚
    private Long receivedLikeCount;

    // 最常討論的前三檔股票
    private List<StockSummaryDTO> topStocks;
}