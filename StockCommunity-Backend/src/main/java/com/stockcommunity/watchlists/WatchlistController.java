package com.stockcommunity.watchlists;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.stockcommunity.security.CurrentUserProvider;
import com.stockcommunity.watchlists.dto.AddWatchlistItemRequest;
import com.stockcommunity.watchlists.dto.CreateWatchlistRequest;

import java.util.List;

@RestController
@RequestMapping("/api/watchlists")
public class WatchlistController {

    private final WatchlistService watchlistService;
    private final CurrentUserProvider currentUserProvider;

    public WatchlistController(WatchlistService watchlistService, CurrentUserProvider currentUserProvider) {
        this.watchlistService = watchlistService;
        this.currentUserProvider = currentUserProvider;
    }

    // 從目前的登入狀態（JWT Filter 已驗證過）解出使用者名稱，再查出對應的 userId
    // 不接受前端自己傳 userId，避免有人偽造別人的 id
    // 建立新清單，歸屬於目前登入的會員
    @PostMapping
    public ResponseEntity<?> createWatchlist(@RequestBody CreateWatchlistRequest request) {
        try {
            Long userId = currentUserProvider.getCurrentUserId();
            Watchlist watchlist = watchlistService.createWatchlist(userId, request.getName());
            return ResponseEntity.ok(watchlist);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 加股票進清單；擁有權檢查交給 Service 層處理
    @PostMapping("/{watchlistId}/items")
    public ResponseEntity<?> addWatchlistItem(
            @PathVariable Long watchlistId,
            @RequestBody AddWatchlistItemRequest request) {
        try {
            Long userId = currentUserProvider.getCurrentUserId();
            WatchlistItem item = watchlistService.addWatchlistItem(watchlistId, request.getStockCode(), userId);
            return ResponseEntity.ok(item);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 移除清單裡的股票項目，權限檢查邏輯寫在 Service 層
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<?> removeWatchlistItem(@PathVariable Long itemId) {
        try {
            Long userId = currentUserProvider.getCurrentUserId();
            watchlistService.removeWatchlistItem(itemId, userId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    // 查目前登入者自己的所有清單，不用也不能查別人的
    @GetMapping("/mine")
    public ResponseEntity<List<Watchlist>> getMyWatchlists() {
        Long userId = currentUserProvider.getCurrentUserId();
        List<Watchlist> watchlists = watchlistService.findWatchlistsByUserId(userId);
        return ResponseEntity.ok(watchlists);
    }

    // 查清單裡的股票；擁有權檢查交給 Service 層處理
    @GetMapping("/{watchlistId}/items")
    public ResponseEntity<?> getWatchlistItems(@PathVariable Long watchlistId) {
        try {
            Long userId = currentUserProvider.getCurrentUserId();
            List<WatchlistItem> items = watchlistService.findWatchlistItemsByWatchlistId(watchlistId, userId);
            return ResponseEntity.ok(items);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
