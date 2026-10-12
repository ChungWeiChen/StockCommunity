package com.stockcommunity.watchlists;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Long> {

    List<WatchlistItem> findByWatchlistId(Long watchlistId);

    // 新增：查某個清單裡有沒有已經加入過這檔股票，用來防止重複加入
    Optional<WatchlistItem> findByWatchlistIdAndStockCode(Long watchlistId, String stockCode);
}
