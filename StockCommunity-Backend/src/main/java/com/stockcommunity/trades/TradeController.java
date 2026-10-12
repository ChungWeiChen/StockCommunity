package com.stockcommunity.trades;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.stockcommunity.security.CurrentUserProvider;

import java.util.List;

@RestController
@RequestMapping("/api/trades")
public class TradeController {

    private final TradeService tradeService;
    private final CurrentUserProvider currentUserProvider;

    public TradeController(TradeService tradeService, CurrentUserProvider currentUserProvider) {
        this.tradeService = tradeService;
        this.currentUserProvider = currentUserProvider;
    }

    // 注意：這裡故意沒有 POST 端點！
    // 成交紀錄不該讓前端直接新增，應該由撮合引擎內部呼叫 TradeService.createTrade()
    // 一旦開放 POST，使用者就能自己捏造假的成交紀錄，等於偽造交易歷史
    // 跟 OrderController、WatchlistController 同一套邏輯：從 SecurityContext 解出 username 再查 userId
    // 查詢目前登入者相關的所有成交紀錄（不管當初是買方還是賣方，任何一筆訂單只要有成交都會顯示）
    @GetMapping("/mine")
    public ResponseEntity<List<Trade>> getMyTrades() {
        Long userId = currentUserProvider.getCurrentUserId();
        List<Trade> trades = tradeService.findTradesByUserId(userId);
        return ResponseEntity.ok(trades);
    }


    
    // 查某檔股票的所有成交紀錄，不需要驗證身份（成交資訊屬於公開行情）
    @GetMapping("/stock/{stockCode}")
    public ResponseEntity<List<Trade>> getTradesByStock(@PathVariable String stockCode) {
        List<Trade> trades =tradeService.findTradesByStockCode(stockCode);
        return ResponseEntity.ok(trades);
    }
    
    
}
