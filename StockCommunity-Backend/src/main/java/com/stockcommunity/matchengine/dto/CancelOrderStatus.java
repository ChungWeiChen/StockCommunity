package com.stockcommunity.matchengine.dto;

/**
 * CancelOrderStatus
 */
public enum CancelOrderStatus {
    // 取消
    CANCELLED,
    
    // 部分成交 , 剩餘委託取消
    PARTIALLY_FILLED_CANCELLED, 

    // 查無此訂單
    ORDER_NOT_FOUND,

    // 無法取消
    CANNOT_CANCEL

}
