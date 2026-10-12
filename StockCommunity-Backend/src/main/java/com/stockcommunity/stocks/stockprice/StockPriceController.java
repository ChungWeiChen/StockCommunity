package com.stockcommunity.stocks.stockprice;

import com.stockcommunity.stocks.finmind.FinMindStockPriceSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/stock-prices")
public class StockPriceController {

    private final StockPriceService stockPriceService;
    private final FinMindStockPriceSyncService finMindStockPriceSyncService;

    public StockPriceController(StockPriceService stockPriceService,
                                FinMindStockPriceSyncService finMindStockPriceSyncService) {
        this.stockPriceService = stockPriceService;
        this.finMindStockPriceSyncService = finMindStockPriceSyncService;
    }

    // 目前不需要
    // 因為 DTO 移動到 dto 資料夾，需要用時要加上import com.stockcommunity.stocks.dto.CreateStockRequest;
    // // 新增一筆每日行情
    // @PostMapping
    // public ResponseEntity<?> createStockPrice(@RequestBody CreateStockPriceRequest request) {
    //     try {
    //         StockPrice stockPrice = stockPriceService.createStockPrice(
    //                 request.getStockCode(),
    //                 request.getTradeDate(),
    //                 request.getOpenPrice(),
    //                 request.getHighPrice(),
    //                 request.getLowPrice(),
    //                 request.getClosePrice(),
    //                 request.getVolume()
    //         );
    //         return ResponseEntity.ok(stockPrice);
    //     } catch (IllegalArgumentException e) {
    //         return ResponseEntity.badRequest().body(e.getMessage());
    //     }
    // }

    // 查某檔股票的所有歷史行情，不需要驗證身份（行情資訊屬於公開資料）
    @GetMapping("/{stockCode}")
    public ResponseEntity<List<StockPrice>> getPriceHistory(@PathVariable String stockCode) {
        List<StockPrice> history = stockPriceService.findPriceHistory(stockCode);
        return ResponseEntity.ok(history);
    }

    // 查某檔股票在某一天相對於前一天的漲跌幅
    // 範例：GET /api/stock-prices/2330/change-percent?tradeDate=2026-08-04&previousDate=2026-08-01
    @GetMapping("/{stockCode}/change-percent")
    public ResponseEntity<?> getChangePercent(
            @PathVariable String stockCode,
            @RequestParam LocalDate tradeDate,
            @RequestParam LocalDate previousDate) {
        try {
            BigDecimal changePercent = stockPriceService.calculateChangePercent(stockCode, tradeDate, previousDate);
            return ResponseEntity.ok(changePercent);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 查某檔股票在指定日期區間的歷史行情，給前端畫走勢圖用
    // 範例：GET /api/stock-prices/2330/range?startDate=2026-08-01&endDate=2026-09-01
    @GetMapping("/{stockCode}/range")
    public ResponseEntity<?> getPriceHistoryByRange(
            @PathVariable String stockCode,
            @RequestParam LocalDate startDate, @RequestParam LocalDate endDate) {
        try {
            List<StockPrice> history = stockPriceService.findPriceHistoryByRange(stockCode, startDate, endDate);
            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 【新增】手動觸發：向 FinMind 補齊「單一檔股票」在指定區間內、資料庫還沒有的歷史行情
    // 開發/測試用的一次性補資料端點，沒有被 SecurityConfig 放行，需要帶登入後的 JWT token 才能打
    // 範例：POST /api/stock-prices/2330/backfill?startDate=2026-06-01&endDate=2026-09-01
    @PostMapping("/{stockCode}/backfill")
    public ResponseEntity<?> backfillHistory(
            @PathVariable String stockCode,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        int saved = finMindStockPriceSyncService.backfillHistory(stockCode, startDate, endDate);
        return ResponseEntity.ok("補了 " + saved + " 筆歷史資料");
    }

    // 【新增】手動觸發：批次補齊多檔股票的歷史行情
    // 範例：POST /api/stock-prices/backfill-batch?stockCodes=2330,2454,2308,2317,2303&startDate=2026-06-01&endDate=2026-09-01
    @PostMapping("/backfill-batch")
    public ResponseEntity<?> backfillHistoryBatch(
            @RequestParam List<String> stockCodes,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        finMindStockPriceSyncService.backfillHistoryForStocks(stockCodes, startDate, endDate);
        return ResponseEntity.ok("批次補資料已執行完畢");
    }
}