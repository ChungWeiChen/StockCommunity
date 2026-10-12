package com.stockcommunity.accounts.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.stockcommunity.accounts.CashLedger;
import com.stockcommunity.accounts.CashLedgerType;
// 交易流水帳DTO
public record CashLedgerResponse(
        Long id,
        Long orderId,
        Long tradeId,
        CashLedgerType type,
        BigDecimal availableCashDelta,
        BigDecimal frozenCashDelta,
        BigDecimal availableCashAfter,
        BigDecimal frozenCashAfter,
        String description,
        LocalDateTime createdAt
) {
    public static CashLedgerResponse from(CashLedger ledger) {
        return new CashLedgerResponse(
                ledger.getId(),
                ledger.getOrderId(),
                ledger.getTradeId(),
                ledger.getType(),
                ledger.getAvailableCashDelta(),
                ledger.getFrozenCashDelta(),
                ledger.getAvailableCashAfter(),
                ledger.getFrozenCashAfter(),
                ledger.getDescription(),
                ledger.getCreatedAt()
        );
    }
}
