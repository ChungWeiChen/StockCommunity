package com.stockcommunity.leaderboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class LeaderboardEntryDTO {
    private Long userId;
    private String username;       // 暱稱欄位其實就是用username顯示，所以這裡直接放username
    private BigDecimal totalAssets; // 總資產（現金餘額＋凍結資金＋持股市值），排行榜依此欄位由大到小排序

//    private BigDecimal unrealizedPnl; // 未實現損益總額
//    // 原本是 totalAssets 用 unrealizedPnl(未實現損益總額)，9/29 改成總資產排名：
//    // 未實現損益沒有持股就是0、沒意義；總資產則不論有沒有交易都能反映「賺最多」的真實排名
}