package com.stockcommunity.holdings;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stockcommunity.security.CurrentUserProvider;

import java.util.List;

@RestController
@RequestMapping("/api/holdings")
public class HoldingController {

    private final HoldingService holdingService;
    private final CurrentUserProvider currentUserProvider;

    public HoldingController(HoldingService holdingService, CurrentUserProvider currentUserProvider) {
        this.holdingService = holdingService;
        this.currentUserProvider = currentUserProvider;
    }

    // 注意：這裡故意沒有 POST 端點！
    // 持股是成交後系統自動計算出來的結果，不是使用者自己填的
    // 應該由撮合引擎內部呼叫 HoldingService.applyBuyTrade() / applySellTrade()
    // 跟 OrderController、TradeController 同一套邏輯：從 SecurityContext 解出 username 再查 userId
    // 查詢目前登入者的所有持股（投資組合）
    @GetMapping("/mine")
    public ResponseEntity<List<Holding>> getMyHoldings() {
        Long userId = currentUserProvider.getCurrentUserId();
        List<Holding> holdings = holdingService.findHoldingsByUserId(userId);
        return ResponseEntity.ok(holdings);
    }
    
    @GetMapping("/mine/{stockCode}")
    public ResponseEntity<Holding> getMyHoldingByStockCode(@PathVariable String stockCode) {
        Long userId = currentUserProvider.getCurrentUserId();
        Holding holdings = holdingService
        		.findHoldingByUserIdAndStockCode(userId, stockCode);
        return ResponseEntity.ok(holdings);
    }
    
}
