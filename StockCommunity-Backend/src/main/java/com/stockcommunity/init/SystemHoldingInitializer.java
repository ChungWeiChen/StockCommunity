// package com.stockcommunity.init;

// import java.math.BigDecimal;
// import java.time.LocalDateTime;
// import java.util.Iterator;
// import java.util.List;
// import java.util.Optional;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.boot.context.event.ApplicationReadyEvent;
// import org.springframework.context.event.EventListener;
// import org.springframework.stereotype.Component;
// import org.springframework.transaction.event.TransactionPhase;
// import org.springframework.transaction.event.TransactionalEventListener;

// import com.stockcommunity.event.StockDataUpdatedEvent;
// import com.stockcommunity.event.SystemAccountCreateEvent;
// import com.stockcommunity.holdings.Holding;
// import com.stockcommunity.holdings.HoldingRepository;
// import com.stockcommunity.marketmaker.MarketMakerProperties;
// import com.stockcommunity.users.UserRepository;
// import com.stockcommunity.users.UserService;
// import com.stockcommunity.users.Users;

// import jakarta.transaction.Transactional;
// import lombok.RequiredArgsConstructor;

// @Component
// @RequiredArgsConstructor
// public class SystemHoldingInitializer {
// 	private static final Logger log = LoggerFactory.getLogger(SystemHoldingInitializer.class);

// 	private final UserRepository userRepo;

// 	private final HoldingRepository holdingRepo;

// 	private final MarketMakerProperties properties;

// 	private volatile boolean canStartCreateHolding = false;

// 	@Transactional // 確保寫入資料庫的事務安全
// 	@EventListener
// 	public void initSystemHolding(SystemAccountCreateEvent event) {
// 		log.info("正在檢查系統造市商是否有相應持股...");

// 		List<String> stocks = properties.getStocks();
// 		boolean skippedLogged = false;

// 		// 檢查系統帳號是否存在
// 		Users systemAccount = userRepo.findByUsername("SYSTEM_MARKET_MAKER")
// 				.orElseThrow(() -> new IllegalStateException("找不到系統造市商帳號: " + "SYSTEM_MARKET_MAKER"));

// 		for (String stock : stocks) {
// 			Optional<Holding> systemHolding = holdingRepo.findByUserIdAndStockCode(systemAccount.getId(), stock);

// 			if (systemHolding.isEmpty()) {
// 				log.info("偵測到系統造市商 {} 未持有 {} 股票", "SYSTEM_MARKET_MAKER", stock);
// 				Holding holding = new Holding();
				
// 				holding.setUserId(systemAccount.getId());
// 				holding.setStockCode(stock);
// 				holding.setUpdatedAt(LocalDateTime.now());
// 				holding.setQuantity(properties.getInitialHolding());
// 				holding.setAveragePrice(BigDecimal.ZERO);

// 				holdingRepo.saveAndFlush(holding);
// 				log.info("系統造市商 {} 的 Holding 建立完成", stock);
// 			} else if (!skippedLogged) {
// 				log.info("部分系統造市商持股已存在，將自動跳過已建立的項目。");
// 				skippedLogged = true; // 避免重複印出日誌
// 			}
// 		}

// 	}
// }
