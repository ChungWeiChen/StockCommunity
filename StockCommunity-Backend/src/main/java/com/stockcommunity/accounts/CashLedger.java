package com.stockcommunity.accounts;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 交易時交易帳戶的流水帳entity
// 不用setter 因為readonly
@Entity
@Table(
        name = "cash_ledgers",
        uniqueConstraints = @UniqueConstraint(columnNames = "reference_key"),
        // 因為流水帳數量大,加入index減輕SQL負擔
        indexes = {
                @Index(name = "idx_cash_ledger_user_created", columnList = "user_id, created_at"),
                @Index(name = "idx_cash_ledger_order", columnList = "order_id"),
                @Index(name = "idx_cash_ledger_trade", columnList = "trade_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CashLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;
    // 相關委託；起始資金沒有委託，所以可以是 null
    @Column(name = "order_id")
    private Long orderId;
    // 相關成交；非成交流水可以是 null
    @Column(name = "trade_id")
    private Long tradeId;

    // 交易的種類配合enum
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private CashLedgerType type;

    // 可用資金變化量
    @Column(name = "available_cash_delta", nullable = false, precision = 19, scale = 2)
    private BigDecimal availableCashDelta;

    // 凍結資金變化量
    @Column(name = "frozen_cash_delta", nullable = false, precision = 19, scale = 2)
    private BigDecimal frozenCashDelta;

    // 異動完成後的可用資金
    @Column(name = "available_cash_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal availableCashAfter;

    // 異動完成後的凍結資金
    @Column(name = "frozen_cash_after", nullable = false, precision = 19, scale = 2)
    private BigDecimal frozenCashAfter;

    // 比對用, 防止相同業務事件重複入帳
    @Column(name = "reference_key", nullable = false, unique = true, length = 120)
    private String referenceKey;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public CashLedger(
            Long userId,
            Long orderId,
            Long tradeId,
            CashLedgerType type,
            BigDecimal availableCashDelta,
            BigDecimal frozenCashDelta,
            BigDecimal availableCashAfter,
            BigDecimal frozenCashAfter,
            String referenceKey,
            String description) {
        this.userId = userId;
        this.orderId = orderId;
        this.tradeId = tradeId;
        this.type = type;
        this.availableCashDelta = availableCashDelta;
        this.frozenCashDelta = frozenCashDelta;
        this.availableCashAfter = availableCashAfter;
        this.frozenCashAfter = frozenCashAfter;
        this.referenceKey = referenceKey;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }
}
