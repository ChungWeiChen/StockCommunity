package com.stockcommunity.application.stocksummary;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stockcommunity.application.stocksummary.dto.StockLastPriceDTO;

import lombok.RequiredArgsConstructor;

// 應用服務用的Controller
// 負責傳送整合型的資料
@RestController
@RequestMapping("/api/stock-summary")
@RequiredArgsConstructor
public class StockSummaryController {
	private final StockSummaryService stockSummaryService;
	
	@GetMapping("/{stockCode}")
	public ResponseEntity<StockLastPriceDTO> getStockAndNewestPrice (@PathVariable String stockCode) {
		StockLastPriceDTO dto = stockSummaryService.LatestPriceByStockCode(stockCode);
		return ResponseEntity.ok(dto);
	}
	
}
