package com.stockcommunity.init;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.stockcommunity.holdings.Holding;
import com.stockcommunity.holdings.HoldingRepository;
import com.stockcommunity.marketmaker.MarketMakerProperties;
import com.stockcommunity.users.UserRepository;
import com.stockcommunity.users.UserService;
import com.stockcommunity.users.Users;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemAccountInitService {

    private final UserRepository userRepo; 
    
    private final UserService userService;

	private final HoldingRepository holdingRepo;

	private final MarketMakerProperties properties;


    @Transactional // 確保寫入資料庫的事務安全
    public void initSystemAccount() {
        log.info("正在檢查系統造市商帳號是否存在...");
        
        // 檢查是否已經存在於資料庫中
        Optional<Users> systemAccount = userRepo.findByUsername("SYSTEM_MARKET_MAKER");
        
        if (systemAccount.isEmpty()) {
            log.info("偵測到系統造市商帳號不存在，正在自動建立系統帳號：{}", "SYSTEM_MARKET_MAKER");
             
            // 存入資料庫
			// 此為測試帳號,正式環境需帶入正式帳號資訊
            userService.register("SYSTEM_MARKET_MAKER", "test1234", "0000@xxxx@smail.com");
            log.info("系統造市商帳號 {} 建立成功！", "SYSTEM_MARKET_MAKER");
            
        } else {
            log.info("系統造市商帳號已存在，跳過建立流程。");
        }
            
    }

    @Transactional // 確保寫入資料庫的事務安全
	public void initSystemHolding() {
		log.info("正在檢查系統造市商是否有相應持股...");

		List<String> stocks = properties.getStocks();
		boolean skippedLogged = false;

		// 檢查系統帳號是否存在
		Users systemAccount = userRepo.findByUsername("SYSTEM_MARKET_MAKER")
				.orElseThrow(() -> new IllegalStateException("找不到系統造市商帳號: " + "SYSTEM_MARKET_MAKER"));

		for (String stock : stocks) {
			Optional<Holding> systemHolding = holdingRepo.findByUserIdAndStockCode(systemAccount.getId(), stock);

			if (systemHolding.isEmpty()) {
				log.info("偵測到系統造市商 {} 未持有 {} 股票", "SYSTEM_MARKET_MAKER", stock);
				Holding holding = new Holding();
				
				holding.setUserId(systemAccount.getId());
				holding.setStockCode(stock);
				holding.setUpdatedAt(LocalDateTime.now());
				holding.setQuantity(properties.getInitialHolding());
				holding.setAveragePrice(BigDecimal.ZERO);

				holdingRepo.saveAndFlush(holding);
				log.info("系統造市商 {} 的 Holding 建立完成", stock);
			} else if (!skippedLogged) {
				log.info("部分系統造市商持股已存在，將自動跳過已建立的項目。");
				skippedLogged = true; // 避免重複印出日誌
			}
		}

	}
    
}
