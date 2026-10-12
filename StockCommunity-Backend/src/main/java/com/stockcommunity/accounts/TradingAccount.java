package com.stockcommunity.accounts;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 與user 綁定的交易帳戶
@Entity
@Table(
        name = "trading_accounts",
        uniqueConstraints = @UniqueConstraint(columnNames = "user_id")
)
@Getter
@Setter
@NoArgsConstructor
public class TradingAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;
    // 最初發放的模擬資金
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal initialCash;
    // 目前可以拿來下單的金額
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal availableCash;
    // 已經掛買單、暫時不能重複使用的金額
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal frozenCash;
    // 防止兩個請求同時修改餘額
    @Version
    private Long version;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.initialCash == null) {
            this.initialCash = BigDecimal.ZERO;
        }
        if (this.availableCash == null) {
            this.availableCash = BigDecimal.ZERO;
        }
        if (this.frozenCash == null) {
            this.frozenCash = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 驗證方法寫在entity , 不放在service 因為這邊沒有setter
    public void reserveCash(BigDecimal amount) {
        // 驗證數量要合理
        requirePositive(amount, "凍結金額必須大於 0");
        // 可用資金不足
        if (this.availableCash.compareTo(amount) < 0) {
            throw new IllegalStateException("可用資金不足，目前可用：" + this.availableCash);
        }
        // 驗證過, 可用資金減少 , 凍結資金增加
        this.availableCash = this.availableCash.subtract(amount);
        this.frozenCash = this.frozenCash.add(amount);
    }

    // 驗證方法寫在entity , 不放在service 因為這邊沒有setter
    public BigDecimal settleBuy(BigDecimal reservedAmount, BigDecimal tradeAmount) {
        requirePositive(reservedAmount, "成交對應的凍結金額必須大於 0");
        requirePositive(tradeAmount, "成交金額必須大於 0");

        if (reservedAmount.compareTo(tradeAmount) < 0) {
            throw new IllegalStateException("實際成交金額超過該筆交易的凍結金額");
        }
        if (this.frozenCash.compareTo(reservedAmount) < 0) {
            throw new IllegalStateException("帳戶凍結資金不足，無法完成成交結算");
        }
        // 退款金額 = 總凍結資金 - 此單資金
        BigDecimal refund = reservedAmount.subtract(tradeAmount);

        this.frozenCash = this.frozenCash.subtract(reservedAmount);
        this.availableCash = this.availableCash.add(refund);
        return refund;
    }
    // 釋放凍結的資金
    public void releaseCash(BigDecimal amount) {
        requirePositive(amount, "解凍金額必須大於 0");
        if (this.frozenCash.compareTo(amount) < 0) {
            throw new IllegalStateException("帳戶凍結資金不足，無法解凍");
        }
        this.frozenCash = this.frozenCash.subtract(amount);
        this.availableCash = this.availableCash.add(amount);
    }

    public void creditCash(BigDecimal amount) {
        requirePositive(amount, "入帳金額必須大於 0");
        this.availableCash = this.availableCash.add(amount);
    }

    // 數量檢查 , null 或 <= 0 報錯
    private void requirePositive(BigDecimal amount, String message) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(message);
        }
    }
}
