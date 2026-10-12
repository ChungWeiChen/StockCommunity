package com.stockcommunity.matchengine.dto;

public record CancelOrderResult(
    CancelOrderStatus status
) {
    public static CancelOrderResult create(CancelOrderStatus status) {
        return new CancelOrderResult(status);
    }
}
