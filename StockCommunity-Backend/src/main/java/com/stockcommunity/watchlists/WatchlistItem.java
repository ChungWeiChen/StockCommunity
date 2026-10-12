package com.stockcommunity.watchlists;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "watchlist_items")
@Getter
@Setter
@NoArgsConstructor
public class WatchlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long watchlistId;   // 對應 watchlists.id

    @Column(nullable = false)
    private String stockCode;   // 股票代號，先用文字存，之後 Stocks 表建好再補正式關聯

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}