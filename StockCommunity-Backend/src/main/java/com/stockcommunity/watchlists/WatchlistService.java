package com.stockcommunity.watchlists;

import org.springframework.stereotype.Service;

import com.stockcommunity.stocks.stock.StockRepository;

import java.util.List;

@Service
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final WatchlistItemRepository watchlistItemRepository;
    private final StockRepository stockRepository;

    public WatchlistService(WatchlistRepository watchlistRepository,
                            WatchlistItemRepository watchlistItemRepository,
                            StockRepository stockRepository) {
        this.watchlistRepository = watchlistRepository;
        this.watchlistItemRepository = watchlistItemRepository;
        this.stockRepository = stockRepository;
    }

    // 建立新的自選股清單
    public Watchlist createWatchlist(Long userId, String name) {
        // 防呆：清單名稱不能是空字串或只有空白
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("清單名稱不能為空");
        }

        Watchlist watchlist = new Watchlist();
        watchlist.setUserId(userId);
        watchlist.setName(name.trim());
        return watchlistRepository.save(watchlist);
    }

    // 把一檔股票加進指定清單
    // userId 是目前登入者，用來確認這個清單真的是他自己的
    public WatchlistItem addWatchlistItem(Long watchlistId, String stockCode, Long userId) {
        // 擁有權檢查：清單必須存在，且必須屬於目前登入者
        Watchlist watchlist = findWatchlistById(watchlistId);
        if (!watchlist.getUserId().equals(userId)) {
            throw new SecurityException("無權限操作此清單");
        }

        // 防呆：股票代號不能是空字串
        if (stockCode == null || stockCode.trim().isEmpty()) {
            throw new IllegalArgumentException("股票代號不能為空");
        }

        // 防呆：股票代號必須是真實存在的股票，stockCode 本身就是 Stock 的主鍵
        boolean stockExists = stockRepository.findById(stockCode).isPresent();
        if (!stockExists) {
            throw new IllegalArgumentException("找不到股票代號：" + stockCode);
        }

        // 防呆：同一檔股票不能重複加入同一個清單
        boolean alreadyExists = watchlistItemRepository
                .findByWatchlistIdAndStockCode(watchlistId, stockCode)
                .isPresent();
        if (alreadyExists) {
            throw new IllegalArgumentException("這檔股票已經在清單裡了");
        }

        WatchlistItem watchlistItem = new WatchlistItem();
        watchlistItem.setWatchlistId(watchlistId);
        watchlistItem.setStockCode(stockCode);
        return watchlistItemRepository.save(watchlistItem);
    }

    // 移除清單裡的一檔股票
    // userId 是目前登入者，用來確認這個項目所屬的清單真的是他自己的，不能刪別人的
    public void removeWatchlistItem(Long itemId, Long userId) {
        WatchlistItem item = watchlistItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("找不到清單項目"));
        Watchlist watchlist = watchlistRepository.findById(item.getWatchlistId())
                .orElseThrow(() -> new IllegalArgumentException("找不到清單"));
        if (!watchlist.getUserId().equals(userId)) {
            throw new SecurityException("無權限刪除此項目");
        }
        watchlistItemRepository.deleteById(itemId);
    }

    // 查某個會員底下所有的自選股清單
    public List<Watchlist> findWatchlistsByUserId(Long userId) {
        return watchlistRepository.findByUserId(userId);
    }

    // 查某個清單底下所有的股票項目
    // userId 是目前登入者，用來確認這個清單真的是他自己的
    public List<WatchlistItem> findWatchlistItemsByWatchlistId(Long watchlistId, Long userId) {
        Watchlist watchlist = findWatchlistById(watchlistId);
        if (!watchlist.getUserId().equals(userId)) {
            throw new SecurityException("無權限查看此清單");
        }
        return watchlistItemRepository.findByWatchlistId(watchlistId);
    }

    // 用清單 id 查一筆清單，查不到就丟例外
    public Watchlist findWatchlistById(Long watchlistId) {
        return watchlistRepository.findById(watchlistId)
                .orElseThrow(() -> new IllegalArgumentException("找不到清單"));
    }

}