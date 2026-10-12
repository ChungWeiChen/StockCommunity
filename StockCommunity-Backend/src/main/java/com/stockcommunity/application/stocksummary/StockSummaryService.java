package com.stockcommunity.application.stocksummary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.stockcommunity.application.stocksummary.dto.StockLastPriceDTO;
import com.stockcommunity.stocks.stock.Stock;
import com.stockcommunity.stocks.stock.StockRepository;
import com.stockcommunity.stocks.stockprice.StockPriceRepository;
import com.stockcommunity.trades.Trade;
import com.stockcommunity.trades.TradeRepository;

import lombok.RequiredArgsConstructor;

// 應用服務用的Controller
// 負責整合不同基礎模組中的資料
@Service
@RequiredArgsConstructor
public class StockSummaryService {
	private final StockRepository stockRepo;
	
	private final StockPriceRepository StockPriceRepo;
	
	private final TradeRepository tradeRepo;

	
	public StockLastPriceDTO LatestPriceByStockCode(String stockCode) {
    	Optional<Trade> tOp = tradeRepo.findLatestPriceByStockCodeAndDate(
    					stockCode, 
    					LocalDate.now());
    	
    	Optional<Stock> sOp = stockRepo.findById(stockCode);
    	Stock stock = sOp.get();
    	
    	Optional<BigDecimal> spOp = StockPriceRepo.findT1ClosePrice(
    					stockCode, 
    					LocalDate.now());
    	BigDecimal latestPrice = spOp.get();
    	
    	if(tOp.isEmpty()) {
    		
    		return StockLastPriceDTO.create(
    				stockCode,
    				stock.getSimpleName(),
    				latestPrice
    		);
    	}
    	Trade trade = tOp.get();
    	
    	return StockLastPriceDTO.create(
				stockCode,
				stock.getSimpleName(),
				trade.getPrice()
		);
    }
}
