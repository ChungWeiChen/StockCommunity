package com.stockcommunity.orders;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// 下單的使用者 ID ，對應 users.id，先不做 JPA 關聯，用純欄位比較好懂
	@Column(nullable = false)
	private Long userId; 

	// 股票代號，尚未關聯至stock表
	@Column(nullable = false)
	private String stockCode; 
	
	// BUY 或 SELL
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private OrderType orderType; 
	
	// 原始委託股數
	@Column(nullable = false)
	private Integer originalQuantity; 
	
	// 剩餘委託股數
	private Integer remainingQuantity;
	
	// 委託價格:市價單(MARKET_ORDER)是 null，限價單(LIMIT_ORDER)才需要填
	private BigDecimal wantBuyPrice;

	// 市價單成交價格上限（買）或下限（賣）
	private BigDecimal protectionPrice;

	// 這張買單目前仍占用的凍結資金；舊資料的 null 視為 0
	@Column(precision = 19, scale = 2)
	private BigDecimal reservedCash;

	// 這張賣單目前仍占用的凍結股數；舊資料的 null 視為 0
	private Integer reservedQuantity;

	// 市價單／限價單
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PriceType priceType;

	// ROD ／ IOC ／ FOK，台股下單條件
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TradingType tradingType; 
	
	// DURING_HOURS:盤中 / AFTER_HOURS:盤後
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TradingTime tradingTime; 
	
	// PENDING 待處理/等待中, PARTIALLY_FILLED 部分成交, 
	// PARTIALLY_FILLED_CANCELLED 部分成交、剩餘委託取消, 
	// FILLED 已成交, CANCELLED 已取消
	@Enumerated(EnumType.STRING)
	private OrderStatus status; 
	
	// 型別從 LocalDateTime 改成 Instant(絕對時間戳，取代原本沒有時區資訊的 LocalDateTime)
	private Instant createdAt; 
	
	@PrePersist
	protected void onCreate() {
		this.createdAt = Instant.now();
		if (this.status == null) {
			this.status = OrderStatus.PENDING;
		}
		if (this.reservedCash == null) {
			this.reservedCash = BigDecimal.ZERO;
		}
		if (this.reservedQuantity == null) {
			this.reservedQuantity = 0;
		}

	}

	// order 修改狀態用的方法
	public void markNew() {
		status = OrderStatus.PENDING;
	}

	public void markPartiallyFilled() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException(
                "Order cannot become PARTIALLY_FILLED from " + status
            );
        }

        status = OrderStatus.PARTIALLY_FILLED;
    }
	
	public void markPartiallyFilledCancelled() {
        if (status != OrderStatus.PARTIALLY_FILLED) {
            throw new IllegalStateException(
                "Order cannot become PARTIALLY_FILLED_CANCELLED from " + status
            );
        }

        status = OrderStatus.PARTIALLY_FILLED_CANCELLED;
    }

    public void markFilled() {
        if (status != OrderStatus.PENDING
                && status != OrderStatus.PARTIALLY_FILLED) {
            throw new IllegalStateException(
                "Order cannot become FILLED from " + status
            );
        }

        status = OrderStatus.FILLED;
    }

    public void markCancelled() {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException(
                "Order cannot be cancelled from " + status
            );
        }

        status = OrderStatus.CANCELLED;
    }

}
