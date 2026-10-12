package com.stockcommunity.stocks.stock;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockService stockService;
    

    public  StockController(StockService stockService) {
        this.stockService = stockService;
    }

    // 目前看來用不到
    // 因為 DTO 移動到 dto 資料夾，需要用時要加上import com.stockcommunity.stocks.dto.CreateStockRequest;
    // // 新增股票主檔資料
    // @PostMapping
    // public ResponseEntity<?> createStock(@RequestBody CreateStockRequest request) {
    //     try {
    //         Stock stock = stockService.createStock(
    //                 request.getStockCode(),
    //                 request.getCompanyName(),
    //                 request.getSimpleName(),
    //                 request.getMarket(),
    //                 request.getIndustry(),
    //                 request.getCapital()
    //         );
    //         return ResponseEntity.ok(stock);
    //     } catch (IllegalArgumentException e) {
    //         return ResponseEntity.badRequest().body(e.getMessage());
    //     }
    // }


    // 查詢全部股票，讓下單/自選股頁面可以下拉選單選代號
    @GetMapping
    public ResponseEntity<List<Stock>> getAllStocks(
            @RequestParam(required = false) String industry) {

        if (industry != null && !industry.isBlank()) {
            return ResponseEntity.ok(stockService.findStocksByIndustry(industry));
        }
        return ResponseEntity.ok(stockService.findAllStocks());
    }

    // 追加一個分頁搜索版
    @GetMapping("/search")
    public ResponseEntity<List<Stock>> searchStocks(@RequestParam String keyword) {
        return ResponseEntity.ok(stockService.searchStocks(keyword));
    }

    // 依代號查單一股票
    @GetMapping("/{stockCode}")
    public ResponseEntity<?> getStock(@PathVariable String stockCode) {
        try {
            Stock stock = stockService.findByStockCode(stockCode);
            return ResponseEntity.ok(stock);
        } catch (IllegalArgumentException e) {
            // 查不到股票代號，是資源不存在，不是請求格式有問題，用 404 比 400 更精確
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
    
    
}
