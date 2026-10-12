package com.stockcommunity.news.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NewsDTO {

    // 股票代號
    private String stockCode;

    // 新聞標題
    private String title;

    // 原始新聞網址
    private String url;

    // 發布時間
    private String publishedAt;

    // 新聞來源
    private String sourceName;
}