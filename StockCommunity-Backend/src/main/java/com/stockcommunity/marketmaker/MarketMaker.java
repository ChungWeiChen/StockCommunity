package com.stockcommunity.marketmaker;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.DependsOn;

import org.springframework.scheduling.annotation.Scheduled;

import org.springframework.stereotype.Component;

import com.stockcommunity.holdings.Holding;
import com.stockcommunity.holdings.HoldingRepository;
import com.stockcommunity.marketmaker.dto.MakerOrderSpec;
import com.stockcommunity.orders.Order;
import com.stockcommunity.orders.OrderRepository;
import com.stockcommunity.orders.OrderService;
import com.stockcommunity.orders.OrderStatus;
import com.stockcommunity.orders.OrderType;
import com.stockcommunity.orders.PriceType;
import com.stockcommunity.orders.TradingTime;
import com.stockcommunity.orders.TradingType;
import com.stockcommunity.stocks.stockprice.StockPriceRepository;
import com.stockcommunity.trades.Trade;
import com.stockcommunity.trades.TradeRepository;
import com.stockcommunity.users.UserRepository;
import com.stockcommunity.users.Users;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 造市者
@Slf4j
@Component
@RequiredArgsConstructor
@DependsOn({"systemAccountInitializer"
	, "matchingEngine"
	, "dbPersistenceExecutor"}) // 確保這個bean會在這個排程bean的初始化後才創建，才能確保一定拿的到股票
public class MarketMaker {
	// 核心價值:提供流動性，使引擎中時時刻刻有單可以搓合
	// 時刻調整報價
	private final StockPriceRepository stockPriceRepo;

	private final TradeRepository tradeRepo;

	private final UserRepository userRepo;
	
	private final OrderRepository orderRepository;
	
	private final OrderService orderService;
	
	private final HoldingRepository holdingRepo;



	@Value("#{'${market-maker.stocks}'.split(',\\s*')}")
	private List<String> marketMakerStocks;
	
	private Long SYSTEM_ID;

	// 找不到就用預設值1000
	@Value("${market-maker.generateMMOrders.quantity:1000}")
    private Integer quantity;

	// 價差基準值
	private static final BigDecimal SPREAD = new BigDecimal("0.2");

	// 總共幾組訂單
	@Value("${market-maker.generateMMOrders.time:2}")
	private int time;

	// 紀錄上一次的發送的訂單價格是多少，方便確認是否要送新訂單
	private final Map<String, BigDecimal> lastOrderPriceMap = new HashMap<>();

	// 限制造市者允許持倉的最大上限與下限
	@Value("${market-maker.max.holding.quantity:20000}")
	private Integer maxHoldingQuantity;
	
	@Value("${market-maker.min.holding.quantity:0}")
	private Integer minHoldingQuantity;
	
	

	@PostConstruct
	private void init() {
		this.SYSTEM_ID = userRepo.findByUsername("SYSTEM_MARKET_MAKER").get().getId();
		cleanupPreviousMarketMakerOrders();
	}


	// fairpricemap在已經有過訂單時會提供有價格異動的股票(獲取特定股票最新成交價格)
	// 所以checkresult就可以移除
	// GPT提供的取消功能沒有用上 stockcode ，加上後使用就需要提供 stockcode


	// 定時檢查一次外部行情跟訂單情況，並主動下單
	@Scheduled(fixedRateString = "${market-maker.scheduled.fixedRate}")
	public void onTimerQuote() {

		log.info("造市者開始進行提供訂單流程...");

		boolean existTodayOrder =
				orderRepository.existsTodayAliveOrderByCreatedAtDay(Instant.now(), SYSTEM_ID);

		// 1. 決定本輪要造市的股票，以及各自的基準價格
		Optional<Map<String, BigDecimal>> fairPriceOp =
				getFairPriceMap(existTodayOrder);

		
		if (fairPriceOp.isEmpty()) {
			log.info("本輪無新訂單產生、跳過產生新單環節");
			return;
		}

		Map<String, BigDecimal> fairPrice = fairPriceOp.get();

		
		// 2. 如果今天已經造過市
		// 就檢查上一次的成交價格跟今次抓到的是否一樣，一致就不添加新單
		// 看會不會出現應該更新但價格卻抓到一樣而不更新
		// 否則需要取消舊單產生新單
		if (existTodayOrder) {
			for(Entry<String, BigDecimal> stockPrice : fairPrice.entrySet()) {
				String stockPriceKey = stockPrice.getKey();
				BigDecimal stockPriceValue = stockPrice.getValue();

				BigDecimal lastTradePrice = lastOrderPriceMap.get(stockPriceKey);
				if(lastTradePrice.compareTo(stockPriceValue) == 0) {
					log.info("該價格區間的訂單已存在，跳過產生新單環節");
					return;
				}
			}
			cancelExistingOrders(fairPrice.keySet());
		}

		// 新增 檢查持倉是否達到已接近上限或歸零
		Map<String, MakerOrderSpec> checkResult = 
				checkHoldingQuantity(fairPrice.keySet());
		

		// 3. 根據基準價格產生新訂單
		Optional<List<Order>> ordersOp = generateMMOrders(fairPriceOp.get(), checkResult);

		// 4. 丟進撮合引擎
		if(ordersOp.isEmpty()) {
			log.info("本輪無新訂單產生、跳過送出訂單環節");
			return;
		}

		for (Order order : ordersOp.get()) {
			try {
				orderService.createOrder(
					order.getUserId(),
					order.getStockCode(),
                    order.getOrderType(),
                    order.getOriginalQuantity(),
                    order.getWantBuyPrice(),
                    order.getPriceType(),
                    order.getTradingType(),
                    order.getTradingTime()
				);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				log.warn("造市者送出訂單時被中斷", e);
				return;
			}
		}
	}

	// 挑選價位表
	private Optional<Map<String, BigDecimal>> getFairPriceMap(
			boolean existTodayOrder) {
		Optional<Map<String, BigDecimal>> fairPriceMap;
	
		// 先檢查今天有沒有已經發過訂單
		if (!existTodayOrder) {
			// 沒有的話使用closeingprice做起始點
			log.info("使用上個交易日的收盤價作為價格基準");
			fairPriceMap = getStocksClosePrice(marketMakerStocks);

		} else {
			// 已發過訂單，目前只有找該支股票最近成交價，並做出價位調整
			// 未來可新增確定持倉是否平衡
			log.info("使用股票最近的成交價作為價格基準");
			fairPriceMap = getTradePriceByStock(marketMakerStocks);
		}

		
		return fairPriceMap.isEmpty() ? Optional.empty() : fairPriceMap;

	}
	
	
	// 確認是否接近持倉上限或歸零並設定訂單規格
	private Map<String, MakerOrderSpec> 
		checkHoldingQuantity(Collection<String> stockCodes){
		
		Map<String, MakerOrderSpec> result = new HashMap<>();
			
		for (String stockCode : stockCodes) {
			Holding holding = holdingRepo
					.findByUserIdAndStockCode(SYSTEM_ID, stockCode).get();
			
			// 買賣股數必定在設定的quantity到0之間
			Integer buyQuantity = Math.max(
		            0,
		            Math.min(
		                    quantity,
		                    maxHoldingQuantity - holding.getQuantity()
		            )
		    );

			Integer sellQuantity = Math.max(
		            0,
		            Math.min(
		                    quantity,
		                    holding.getQuantity() - minHoldingQuantity
		            )
		    );
			
			boolean buy = buyQuantity > 0;
		    boolean sell = sellQuantity > 0;
		    // 如有需要可自行填入其他基準值
			result.put(stockCode, MakerOrderSpec.create(
					buy,
					sell,
					buyQuantity,
					sellQuantity,
					time,
					SPREAD));
			
		}
		return result;
		
	}

	
	

	// 造市者的運算邏輯
	// 固定時間定期產生新的訂單
	private Optional<List<Order>> generateMMOrders(
			Map<String, BigDecimal> priceMap, 
			Map<String, MakerOrderSpec> orderSpec) {
		// 目前打算價格上下造各兩組價位，一個價位給1000股，共計四千股
		
		if (priceMap == null || priceMap.isEmpty()) {
	        log.info("不需要產生新訂單");
	        return Optional.empty();
	    }
		if(orderSpec == null || orderSpec.isEmpty()) {
			log.warn("訂單規劃書並未傳入，無法執行");
	        return Optional.empty();
		}
		
		List<Order> orders = new ArrayList<>();
	    
		for(Map.Entry<String, BigDecimal> stockPrice : priceMap.entrySet()) {

			String stockCode = stockPrice.getKey();
			BigDecimal price = stockPrice.getValue();
			BigDecimal orderPairSpread = orderSpec.get(stockCode).spread();
			Integer time = orderSpec.get(stockCode).time();

			// 💡 核心改動：從 MakerOrderSpec 拿出這一輪「總共能買/賣」的總量
	        MakerOrderSpec spec = orderSpec.get(stockCode);
	        int totalBuyQty = (spec != null) ? spec.buyQuantity() : 0;
	        int totalSellQty = (spec != null) ? spec.sellQuantity() : 0;
	        
	        // 💡 將總量平分到每一層（例如總共能買 600 股，分 2 層，每層就是 300 股）
	        int buyQtyPerLayer = totalBuyQty / time;
	        int sellQtyPerLayer = totalSellQty / time;

			for(int i = 1 ; i <= time ; i++) {
				// 偏移量
				BigDecimal offset = orderPairSpread.multiply(BigDecimal.valueOf(i));

				BigDecimal upperPrice = price.add(offset);
				BigDecimal lowerPrice = price.subtract(offset);			

				// 💡 呼叫乾淨的 createOrderPair，只給它這層應得的數量
	            List<Order> orderPair = createOrderPair(
	                    stockCode, 
	                    upperPrice, 
	                    lowerPrice, 
	                    buyQtyPerLayer, 
	                    sellQtyPerLayer
	            );

				orders.addAll(orderPair);
			}

			lastOrderPriceMap.put(stockCode, price);
		}


		log.info("已產生新訂單");
		return orders.isEmpty() ? Optional.empty() : Optional.of(orders);
	}


	private List<Order> createOrderPair(
	        String stockCode, 
	        BigDecimal upperPrice,
	        BigDecimal lowerPrice,
	        int buyQty,            // 直接傳入這「一層」該掛的數量
	        int sellQty) {         // 直接傳入這「一層」該掛的數量

	    List<Order> orders = new ArrayList<>();


	    if (buyQty > 0) {

	        Order buyOrder = new Order();

	        buyOrder.setUserId(SYSTEM_ID);
	        buyOrder.setStockCode(stockCode);
	        buyOrder.setOrderType(OrderType.BUY);
	        buyOrder.setOriginalQuantity(buyQty);
	        buyOrder.setRemainingQuantity(buyQty);
	        buyOrder.setWantBuyPrice(lowerPrice);
	        buyOrder.setPriceType(PriceType.LIMIT_ORDER);
	        buyOrder.setTradingType(TradingType.ROD);
	        buyOrder.setTradingTime(TradingTime.DURING_HOURS);

	        orders.add(buyOrder);
	    }

	    if (sellQty > 0) {

	        Order sellOrder = new Order();

	        sellOrder.setUserId(SYSTEM_ID);
	        sellOrder.setStockCode(stockCode);
	        sellOrder.setOrderType(OrderType.SELL);
	        sellOrder.setOriginalQuantity(sellQty);
	        sellOrder.setRemainingQuantity(sellQty);
	        sellOrder.setWantBuyPrice(upperPrice);
	        sellOrder.setPriceType(PriceType.LIMIT_ORDER);
	        sellOrder.setTradingType(TradingType.ROD);
	        sellOrder.setTradingTime(TradingTime.DURING_HOURS);

	        orders.add(sellOrder);
	    }

	    return orders;

		
	}
	
	private Optional<Map<String, BigDecimal>> getStocksClosePrice(
			Collection<String> stockCode) {
		Optional<BigDecimal> op;
		Map<String, BigDecimal> stockFairMap = new HashMap<>();

		for (String stock : stockCode) {
			op = stockPriceRepo.findT1ClosePrice(
					stock, 
					LocalDate.now());

			if (op.isEmpty()) {
				continue;
			} else {
				stockFairMap.put(stock, op.get());
			}
		}
		
		return stockFairMap.isEmpty() 
				? Optional.empty() 
				: Optional.of(stockFairMap);

	}

	private Optional<Map<String, BigDecimal>> getTradePriceByStock(Collection<String> stockCode) {

		Map<String, BigDecimal> stockFairMap = new HashMap<>();

		for (String stock : stockCode) {
			Optional<Trade> trade = tradeRepo
					.findLatestPriceByStockCodeAndDate(stock, LocalDate.now());

			if (trade.isPresent()) {
				stockFairMap.put(stock, trade.get().getPrice());
			}
		}
		
		return stockFairMap.isEmpty() 
				? Optional.empty() 
				: Optional.of(stockFairMap);

	}

	private void cancelExistingOrders(Collection<String> stockCodes) {

		Optional<Users> userOp =
				userRepo.findByUsername("SYSTEM_MARKET_MAKER");

		if (userOp.isEmpty() || stockCodes.isEmpty()) {
			return;
		}

		Long systemId = userOp.get().getId();
		List<Long> allOrderIds = new ArrayList<>();

		for (String stockCode : stockCodes) {
			List<Long> orderIds = orderRepository.findTodayAliveOrderByOrderIdAndStockCode(
							Instant.now(),
							systemId,
							stockCode);

			allOrderIds.addAll(orderIds);
		}

		for (Long orderId : allOrderIds) {
			orderService.cancelOrder(orderId, systemId);
		}
	}

	// 初始化清理過期訂單方法
	private void cleanupPreviousMarketMakerOrders() {

		List<Order> existingOrders =
				orderRepository.findActiveOrdersByUserId(SYSTEM_ID);

		if (existingOrders.isEmpty()) {
			return;
		}

		log.info(
			"發現 {} 筆上一輪造市者未完成訂單，開始取消",
			existingOrders.size()
		);

		for (Order order : existingOrders) {
			if(order.getStatus() == OrderStatus.PARTIALLY_FILLED) {
				order.setStatus(OrderStatus.PARTIALLY_FILLED_CANCELLED);
			} else {
				order.setStatus(OrderStatus.CANCELLED);
			}
		}

		orderRepository.saveAll(existingOrders);
	}

	@PreDestroy
	public void stopWorker() {
		log.info("正在停止造市者模組...");
		// 不管 Worker 活不活著，都要清理現存訂單
		cancelExistingOrders(marketMakerStocks);

	}

	// 無用
		// 檢查那些股票買賣哪一方的訂單需要產生
		// private Optional<Map<String, MakerOrdersCheck>> 
		// 	checkNeedReplenishOrders(
		// 			boolean existTodayOrder,
		// 			Optional<Map<String, BigDecimal>> op) {
		
			
		// 	if(op == null || op.isEmpty()) {
		// 		return Optional.empty();
		// 	}
			
		// 	Map<String, BigDecimal> fairPriceMap = op.get();
			
		// 	Map<String, MakerOrdersCheck> result = new HashMap<>();

		// 	// 依照今日是否有已發出的訂單決定如何產生檢查結果
		// 	if (!existTodayOrder) {
		// 		// 沒發出過就是全產生

		// 		for (String stockCode : fairPriceMap.keySet()) {
		// 			result.put(stockCode, MakerOrdersCheck.create(true, true));
		// 		}
		// 	} else {
		// 		// 有發出過，且 fairPriceMap 有東西
		// 		// 目前用最簡單的方式: 價格有變動就將該股票所有還存在的訂單都砍掉
		// 		for (String stockCode : fairPriceMap.keySet()) {
		// 			result.put(stockCode, MakerOrdersCheck.create(true, true));
		// 		}
		// 		// 刪除舊有的訂單
		// 		Optional<Users> userOp = userRepo.findByUsername("SYSTEM_MARKET_MAKER");
		//         if(userOp.isPresent()) {
		//         	List<Long> orderIds = orderRepository.findTodayInAliveOrderId(
		//         			Instant.now(),
		//         			userOp.get().getId());
		//         	for(Long orderId : orderIds) {
		//         		orderService.cancelOrder(orderId, userOp.get().getId());
		//         	}
		//         }
		// 	}
		// 	return result.isEmpty() ? Optional.empty() : Optional.of(result);
		// }
}
