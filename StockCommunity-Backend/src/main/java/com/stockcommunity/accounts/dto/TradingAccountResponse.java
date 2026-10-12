package com.stockcommunity.accounts.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.stockcommunity.accounts.TradingAccount;

// 交易帳戶查詢用的DTO
public record TradingAccountResponse(
        BigDecimal initialCash,
        BigDecimal availableCash,
        BigDecimal frozenCash,
        LocalDateTime updatedAt
) {
    public static TradingAccountResponse from(TradingAccount account) {
        return new TradingAccountResponse(
                account.getInitialCash(),
                account.getAvailableCash(),
                account.getFrozenCash(),
                account.getUpdatedAt()
        );
    }
}
