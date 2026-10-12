package com.stockcommunity.orderbook;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stockcommunity.orderbook.dto.MarketDepthCache;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketDepthController {

    private final OrderBookManager orderBookManager;

    /**
     * 前端獲取特定股票五檔的 API
     * 範例：GET /api/v1/market/depth?stockCode=2330
     */
    @GetMapping("/depth")
    public ResponseEntity<MarketDepthCache> getMarketDepth(@RequestParam String stockCode) {
        // 直接從 Manager 的 ConcurrentHashMap 中拔出 volatile 快取，O(1) 速度極快
        MarketDepthCache depth = orderBookManager.getLatestMarketDepth(stockCode);
        return ResponseEntity.ok(depth);
    }
    
    
    
    
}
