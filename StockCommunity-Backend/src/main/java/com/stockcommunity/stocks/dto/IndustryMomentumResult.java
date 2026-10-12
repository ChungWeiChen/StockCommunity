package com.stockcommunity.stocks.dto;

import java.util.List;

// 包裝「今日領漲/領跌產業」的完整回傳結果
// gainers：漲幅平均值最高的前幾名產業；losers：跌幅最深的前幾名產業
public record IndustryMomentumResult(
        List<IndustryMomentumDTO> gainers,
        List<IndustryMomentumDTO> losers) {
}