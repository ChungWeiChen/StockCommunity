package com.stockcommunity.stocks;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.stockcommunity.stocks.dto.StockDataDTO;
import com.stockcommunity.stocks.dto.StockPriceDataDTO;
import com.stockcommunity.stocks.stock.Stock;
import com.stockcommunity.stocks.stock.StockRepository;
import com.stockcommunity.stocks.stockprice.StockPrice;
import com.stockcommunity.stocks.stockprice.StockPriceRepository;

import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockDataSyncService {
	// 專門獲取資料的service
	private final StockRepository stockRepo;
	
	private final StockPriceRepository stockPriceRepo;
	
	@Value("${stock.api.base-url:https://openapi.twse.com.tw/v1}")
	private String baseUrl;

	
	
	private RestClient restClient;
	
	@PostConstruct // Spring 注入完畢後，會自動執行這個方法
	private void init() {
		// 建立 RestClient 並預設帶上瀏覽器的 User-Agent 標頭
		this.restClient = RestClient.builder()
				.baseUrl(baseUrl)
				.defaultHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
				.defaultHeader("Accept", "application/json, text/plain, */*") 
				.build();
	}

	/**
	 * 專門負責「獲取外來資料」並寫入資料庫的方法
	 */
	// 獲取股票資料的方法
	@Transactional
	public void fetchAndSyncExternalStockData() {
		try {
			// ParameterizedTypeReference 保留泛型資料
			List<StockDataDTO> cleanedList  = restClient.get()
					.uri("/opendata/t187ap03_L")
					.retrieve()
					.body(new ParameterizedTypeReference<List<StockDataDTO>>() {});

			if (cleanedList == null || cleanedList.isEmpty()) {
                log.warn("外部 API 未回傳任何個股資料。");
                return;
            }

            log.info("網路資料下載完成，總筆数: {}。開始進行差異比對...", cleanedList.size());

            // 一次性撈出資料庫現有的所有股票，並轉成 Map 提高比對速度 (Key: stockCode, Value: Stock實體)
            Map<String, Stock> existingStockMap = stockRepo.findAll().stream()
                    .filter(s -> s.getStockCode() != null)
                    .collect(Collectors.toMap(Stock::getStockCode, Function.identity()));

            // 準備用來存放「真正需要寫入/更新」的容器
            List<Stock> entitiesToSave = new ArrayList<>();
            int insertCount = 0;
            int updateCount = 0;
            
            // 開始逐筆比對
            for (StockDataDTO dto : cleanedList) {
                if (dto == null || dto.stockCode() == null) continue;

                String code = dto.stockCode();

                if (!existingStockMap.containsKey(code)) {
                    // 【情境 A】資料庫沒有這檔股票 -> 確定是全新品種，直接新增
                    Stock newStock = new Stock();
                    copyDtoToEntity(dto, newStock);
                    entitiesToSave.add(newStock);
                    insertCount++;
                } else {
                    // 【情境 B】資料庫已存在 -> 檢查欄位是否有任何變動
                    Stock dbStock = existingStockMap.get(code);
                    
                    if (isEntityChanged(dto, dbStock)) {
                        // 只要有任何一個欄位被改過，才更新這個 Entity
                        copyDtoToEntity(dto, dbStock);
                        entitiesToSave.add(dbStock); // JPA 會自動識別這是有 ID 的實體，進而執行 UPDATE
                        updateCount++;
                    }
                    // 如果沒變動，程式什麼都不做，直接跳過（達成您的需求！）
                }
            }
            
            // 批次寫入（包含新増與真正有變動的更新）
            if (!entitiesToSave.isEmpty()) {
                stockRepo.saveAll(entitiesToSave);
                log.info("Stock 資料表同步成功！本次新增 {} 筆，更新 {} 筆。", insertCount, updateCount);
            } else {
                log.info("個股基本資料完全無變動，無需更新資料庫。");
            }
			
		}catch (Exception e) {
			log.error("Stock 資料抓取失敗，錯誤原因: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	// 獲取股價
	@Transactional
	public void fetchAndSyncExternalStockPriceData() {
		try {
			log.info("開始從外部 API 抓取股票昨日行情資料...");
			// ParameterizedTypeReference 保留泛型資料
			List<StockPriceDataDTO> cleanedList  = restClient.get()
					.uri("/exchangeReport/STOCK_DAY_ALL")
					.retrieve()
					.body(new ParameterizedTypeReference<List<StockPriceDataDTO>>() {});
			
			
			if (cleanedList != null && !cleanedList.isEmpty()) {
				log.info("記憶體解析完成，準備寫入資料庫的總筆數: " + cleanedList.size());
	
				// 收集 API 資料中所有的交易日期（通常只有 1 個，但做防禦性處理）
	            Set<LocalDate> apiDates = cleanedList.stream()
	                    .map(StockPriceDataDTO::tradeDate)
	                    .filter(date -> date != null)
	                    .collect(Collectors.toSet());
	            
				// 一次性從 DB 撈出這些日期已存在的股票代碼，並轉換為 HashSet 提高查詢速度
	            // 這裡轉換為 Set 後，底層判斷 contains 速度為 O(1)
	            Set<String> existingStockCodes = stockPriceRepo
	            		.findExistingStockCodesByTradeDates(apiDates)
	                    .stream()
	                    .collect(Collectors.toSet());
			
				//預配記憶體空間，避免 ArrayList 頻繁擴容 (Resize)
	            List<StockPrice> entities = new ArrayList<>(cleanedList.size());

	            int skipCount = 0;
	            
	            for (StockPriceDataDTO dto : cleanedList) {
	            	if (dto == null || dto.stockCode() == null || dto.tradeDate() == null) continue;

	                // 如果資料庫在該日期已經有這檔股票，直接略過，不重複寫入
	                if (existingStockCodes.contains(dto.stockCode())) {
	                    skipCount++;
	                    continue; 
	                }
	                
	                StockPrice sp = new StockPrice();
	                sp.setStockCode(dto.stockCode());
	                sp.setTradeDate(dto.tradeDate());
	                
	                // 活用 Java 三元運算子優化效能，免去 Stream 建立銷毀的開銷
	                sp.setOpenPrice(dto.openPrice() != null ? dto.openPrice() : BigDecimal.ZERO);
	                sp.setHighPrice(dto.highPrice() != null ? dto.highPrice() : BigDecimal.ZERO);
	                sp.setLowPrice(dto.lowPrice() != null ? dto.lowPrice() : BigDecimal.ZERO);
	                sp.setClosePrice(dto.closePrice() != null ? dto.closePrice() : BigDecimal.ZERO);
	                sp.setVolume(dto.volume() != null ? dto.volume() : 0L);

	                entities.add(sp);
	            }
	            
				// 寫入資料庫
	            if (!entities.isEmpty()) {
	                stockPriceRepo.saveAll(entities);
	                log.info("StockPrice 同步完成！成功寫入 {} 筆，因重複而自動略過 {} 筆。", entities.size(), skipCount);
	            } else {
	                log.info("無新資料需要寫入（所有股票行情皆已存在於資料庫）。");
	            }
			}
		}catch (Exception e) {
			log.error("StockPrice 資料抓取失敗，錯誤原因: " + e.getMessage());
			throw new RuntimeException("同步股票資料失敗，交易已回滾", e);
		}
	}
	
	/**
     * 核心邏輯：比對 DTO 與 DB 現有資料是否不一致
     * 活用 Objects.equals 可以安全比對 null 值，避免 NullPointerException
     */
    private boolean isEntityChanged(StockDataDTO dto, Stock db) {
        return !Objects.equals(dto.companyName(), db.getCompanyName()) ||
               !Objects.equals(dto.simpleName(), db.getSimpleName()) ||
               !Objects.equals(dto.industry(), db.getIndustry()) ||
               !Objects.equals(dto.capital(), db.getCapital());
    }
    
    /**
     * 封裝欄位賦值邏輯，避免重複程式碼
     */
    private void copyDtoToEntity(StockDataDTO dto, Stock entity) {
        entity.setStockCode(dto.stockCode());
        entity.setCompanyName(dto.companyName());
        entity.setSimpleName(dto.simpleName());
        entity.setIndustry(dto.industry());
        entity.setCapital(dto.capital());
    }
	
}