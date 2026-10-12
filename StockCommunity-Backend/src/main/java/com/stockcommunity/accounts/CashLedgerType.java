package com.stockcommunity.accounts;

// 資金流水帳的種類
public enum CashLedgerType {
    // 發放起始模擬資金
    INITIAL_DEPOSIT,
    // 下買單：可用資金轉成凍結資金
    ORDER_RESERVE,
    // 取消或未成交：凍結資金退回可用資金
    ORDER_RELEASE,
    // 買方成交付款
    BUY_SETTLEMENT,
    // 限價高於成交價時退回價差
    PRICE_DIFFERENCE_REFUND,
    // 賣方收到成交款
    SELL_PROCEEDS,
    // 管理用途，未來需要人工調整時使用
    MANUAL_ADJUSTMENT,
    // 每週賽季重置
    SEASON_RESET
}
