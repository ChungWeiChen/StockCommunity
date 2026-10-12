package com.stockcommunity.matchengine.dto;

import java.util.List;

// DTO 最終打包 , 用於每張搓合訂單 , 不論搓合成功與否
public record FinalOutcome(
        List<MatchResult> matchResults,
        OrderUpdate incomingOrderUpdate
) {
    public FinalOutcome {
        matchResults = List.copyOf(matchResults);
    }

    public static FinalOutcome create(
            List<MatchResult> matchResults,
            OrderUpdate incomingOrderUpdate) {

        return new FinalOutcome(
                matchResults,
                incomingOrderUpdate
        );
    }
}
