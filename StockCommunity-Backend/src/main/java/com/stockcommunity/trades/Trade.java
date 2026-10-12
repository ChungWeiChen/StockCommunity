package com.stockcommunity.trades;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
@Getter
@Setter
@NoArgsConstructor
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long buyOrderId;  // 對應買方那筆 Order 的 id

    @Column(nullable = false)
    private Long sellOrderId;  // 對應賣方那筆 Order 的 id

    @Column(nullable = false)
    private String stockCode;  // 成交的股票代號

    @Column(nullable = false)
    private BigDecimal price;  // 成交價

    @Column(nullable = false)
    private Integer quantity;  // 成交量

    private LocalDateTime tradedAt;

    @PrePersist
    protected void onCreate() {
    	if (this.tradedAt == null) {
            this.tradedAt = LocalDateTime.now();
        }
    }
}
