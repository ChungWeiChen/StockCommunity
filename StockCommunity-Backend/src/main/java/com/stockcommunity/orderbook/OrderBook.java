package com.stockcommunity.orderbook;

import com.stockcommunity.matchengine.dto.*;
import com.stockcommunity.orderbook.dto.BestPriceCache;
import com.stockcommunity.orders.*;
import com.stockcommunity.exception.MatchException;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Getter
@Setter
public class OrderBook {

	private String stockCode;

	// 使用 treemap(或類似的Map) 需要特別注意的「大小比較」盲點
	// 買單價高者得因此與原排序相反所以要 reverseOrder

	// 限價單存放佇列
	private final ConcurrentSkipListMap<BigDecimal, ConcurrentLinkedDeque<Order>> limitBuyOrders = new ConcurrentSkipListMap<>(
			Collections.reverseOrder());
	private final ConcurrentSkipListMap<BigDecimal, ConcurrentLinkedDeque<Order>> limitSellOrders = new ConcurrentSkipListMap<>();

	// 為每支股票建鎖
	private final ReentrantLock lock = new ReentrantLock();

	// 限制建構方式是傳入stockCode以建立
	public OrderBook(String stockCode) {
		this.stockCode = stockCode;
	}

	// 獲取最優價格
	// AI說為了效能最好分開而不用 enum 去做檢查
	// 還有說考慮到效能最好還是將價格會乘以100然後用long計算
	// 簡單說是:使用者送單的json用BigDecimal，內部運算轉成long

	// 進行訂單搓合
	public FinalOutcome handleOrder(Order incomingOrder) {

		lock.lock();

		try {

			List<MatchResult> results = new ArrayList<>();

			executeMatching(incomingOrder, results);

			OrderUpdate finalOrderUpdate = OrderUpdate.create(incomingOrder.getId(), incomingOrder.getUserId(),
					incomingOrder.getStockCode(), incomingOrder.getRemainingQuantity(), incomingOrder.getStatus());

			return FinalOutcome.create(results, finalOrderUpdate);

		} catch (Exception e) {

			log.error("股票撮合執行失敗，訂單ID: {}, 錯誤原因: {}", incomingOrder.getId(), e.getMessage(), e);

			throw new MatchException("撮合系統異常", e);

		} finally {

			lock.unlock();
		}
	}

	// 安排搓合的對象
	private void executeMatching(Order incomingOrder, List<MatchResult> results) {

		boolean isBuy = incomingOrder.getOrderType() == OrderType.BUY;

		// 例外處理沒有委託條件的情況
		if (incomingOrder.getTradingType() == null) {

		}
		;

//		if (incomingOrder.getPriceType() == PriceType.MARKET_ORDER) {
//			// 市價單處理

			switch (incomingOrder.getTradingType()) {
			case ROD -> executeRod(incomingOrder, isBuy, results);

			case IOC -> executeIoc(incomingOrder, isBuy, results);

			case FOK -> executeFok(incomingOrder, isBuy, results);
			}

			
//			matchMarketOrder(incomingOrder, isBuy, results);
//
//		} else {
//
//			matchLimitOrder(incomingOrder, isBuy, results);
//		}

		// 所有撮合結束後，只有這裡負責處理殘單
		// 目前 putOrderIntoQueue 裡面有寫確定只會掛限價單進orderbook中，但這樣可能不夠明確
		// 已改成只有限價單且是ROD條件
		if (incomingOrder.getRemainingQuantity() > 0) {
			putOrderIntoQueue(incomingOrder);
		}
	}

	// 市價單搓合細節
	private void matchMarketOrder(Order incomingOrder, boolean isBuy, List<MatchResult> matchResults) {

		var orderQueuesMap = getMatchOrderTypeMap(incomingOrder); // 透過進入的訂單決定要match的單子,ex:買單要match賣單
		var priceLevels = new ArrayList<>(orderQueuesMap.entrySet()); // 取出所有配對方的{key,value}

		for (var priceLevel : priceLevels) {
			if (incomingOrder.getRemainingQuantity() <= 0) {
				break;
			}

			BigDecimal price = priceLevel.getKey();
			var orderQueue = priceLevel.getValue();

			if (!marketProtectionMatches(incomingOrder, isBuy, price)) {
				break;
			}

			matchOrders(orderQueue, incomingOrder, isBuy, matchResults);

			if (orderQueue.isEmpty()) {
				orderQueuesMap.remove(price, orderQueue);
			}
		}
//		---保留修改前版本20260911---
//		while (
//				incomingOrder.getRemainingQuantity() > 0 // 訂單剩餘股數>0
//						&& !orderQueuesMap.isEmpty() // 訂單不為空
//		) {
//
//
//			var bestEntry = orderQueuesMap.firstEntry();
//
//			// 沒有配對單可配對,跳出迴圈
//			if(bestEntry == null){
//				break;
//			}
//
//			BigDecimal bestPrice = bestEntry.getKey();// 找出配對方的最接近的價格,key
//
//			var orderQueue = orderQueuesMap.get(bestPrice); // 最好價格的等待序列,value
//
//			// 最好價格的數量歸零,移除key
//			if (orderQueue.isEmpty()) {
//				orderQueuesMap.remove(bestPrice,orderQueue); // 雙參數刪除避免有建立時間差問題?
//				continue;
//			}
//
//			matchOrders(
//					orderQueue,
//					incomingOrder,
//					isBuy,
//					matchResults
//			);
//
//			// 該價格已經沒有訂單
//			if (orderQueue.isEmpty()) {
//				orderQueuesMap.remove(bestPrice,orderQueue);
//			}
//		}

	}

	// 限價單搓合細節
	private void matchLimitOrder(Order incomingOrder, boolean isBuy, // true -- 買單 / false -- 賣單 WEI
			List<MatchResult> matchResults) {
		log.info("開始訂單搓合流程...");

		var orderQueuesMap = getMatchOrderTypeMap(incomingOrder);
		var priceLevels = new ArrayList<>(orderQueuesMap.entrySet());

		for (var priceLevel : priceLevels) {
			if (incomingOrder.getRemainingQuantity() <= 0) {
				break;
			}

			BigDecimal price = priceLevel.getKey();
			var orderQueue = priceLevel.getValue();

			boolean pricesMatch = isBuy ? incomingOrder.getWantBuyPrice().compareTo(price) >= 0
					: incomingOrder.getWantBuyPrice().compareTo(price) <= 0;

			// 價格已不符合；後面的價格也不可能符合
			if (!pricesMatch) {
				break;
			}

			matchOrders(orderQueue, incomingOrder, isBuy, matchResults);

			log.info("matchLimitOrder:" + incomingOrder.getStatus());
			if (orderQueue.isEmpty()) {
				orderQueuesMap.remove(price, orderQueue);
			}
		}

//       ---保留修改前版本20260911---
//		 while (
//		            incomingOrder.getRemainingQuantity() > 0 // 訂單剩餘股數>0 WEI
//		            && !orderQueuesMap.isEmpty() // 訂單配對者不為空 WEI
//		    ) {
//
//
//		        BigDecimal bestPrice = orderQueuesMap.firstKey();// 找出配對方的最接近的價格 WEI
//
//
//		        boolean pricesMatch = isBuy
//		                ? incomingOrder.getWantBuyPrice() // 是買單 WEI
//		                        .compareTo(bestPrice) >= 0 // true -- 買單出價高於賣單掛單(成交) WEI
//		                : incomingOrder.getWantBuyPrice() // 是賣單 WEI
//		                        .compareTo(bestPrice) <= 0; // true -- 賣單出價低於買單低於買單(成交) WEI
//
//		        // 最優價都無法成交
//		        if (!pricesMatch) {
//
//		            // 到這裡代表對手方最優價格已經不符合
//		            // 後面的價格也不可能符合
//		            break;
//		        }
//
//		        var orderQueue = orderQueuesMap.get(bestPrice);
//
//		        if (orderQueue.isEmpty()) {
//				    orderQueuesMap.remove(bestPrice);
//				}
//
//		        matchOrders(
//		                orderQueue,
//		                incomingOrder,
//		                isBuy,
//		                matchResults
//		        );
//
//		        // 該價格已經沒有訂單
//		        if (orderQueue.isEmpty()) {
//		            orderQueuesMap.remove(bestPrice);
//		        }
//		    }

	}

	private void matchOrders(ConcurrentLinkedDeque<Order> orderQueue, Order incomingOrder, boolean isBuy,
			List<MatchResult> matchResults) {

		if (orderQueue == null) {
			return;
		}

		log.info("orderbook不為空，開始撮合");

		Iterator<Order> iterator = orderQueue.iterator();

		while (iterator.hasNext() && incomingOrder.getRemainingQuantity() > 0) {

			Order makerOrder = iterator.next();

			// 已經成交完的訂單直接清掉
			if (makerOrder.getRemainingQuantity() <= 0) {
				iterator.remove();

				continue;
			}

			// 搓合時如果遇到自己的訂單 , 避開並繼續執行
			// 但需要處理 endless loop 問題
			if (Objects.equals(makerOrder.getUserId(), incomingOrder.getUserId())) {

				log.info("跳過自成交：incomingOrderId={}, makerOrderId={}, userId={}", incomingOrder.getId(),
						makerOrder.getId(), incomingOrder.getUserId());

				continue;
			}

			int quantity = Math.min(incomingOrder.getRemainingQuantity(), makerOrder.getRemainingQuantity());

			// 理論上經過前面的檢查後不應該發生
			if (quantity <= 0) {
				continue;
			}

			BigDecimal price = makerOrder.getWantBuyPrice();

			// 更新剩餘數量
			incomingOrder.setRemainingQuantity(incomingOrder.getRemainingQuantity() - quantity);

			makerOrder.setRemainingQuantity(makerOrder.getRemainingQuantity() - quantity);

			Order buyOrder = isBuy ? incomingOrder : makerOrder;

			Order sellOrder = isBuy ? makerOrder : incomingOrder;

			// 產生成交紀錄
			TradeRecord tradeResult = TradeRecord.create(
				buyOrder.getId(), 
				sellOrder.getId(), 
				stockCode, 
				price,
				quantity);

			HoldingUpdate buyerHoldingResult = HoldingUpdate.create(
				buyOrder.getUserId(), 
				stockCode, 
				quantity, 
				price);

			HoldingUpdate sellerHoldingResult = HoldingUpdate.create(
				sellOrder.getUserId(), 
				stockCode, 
				quantity, 
				price);

			OrderUpdate buyerOrderUpdate = OrderUpdate.create(
				buyOrder.getId(), 
				buyOrder.getUserId(),
				stockCode,
				buyOrder.getRemainingQuantity(),
				buyOrder.getRemainingQuantity() > 0 
					? OrderStatus.PARTIALLY_FILLED 
					: OrderStatus.FILLED);

			OrderUpdate sellerOrderUpdate = OrderUpdate.create(
				sellOrder.getId(), 
				sellOrder.getUserId(), 
				stockCode,
				sellOrder.getRemainingQuantity(),
				sellOrder.getRemainingQuantity() > 0 
					? OrderStatus.PARTIALLY_FILLED 
					: OrderStatus.FILLED);

			log.info(tradeResult.toString());
			log.info(buyerOrderUpdate.toString());
			log.info(sellerOrderUpdate.toString());
			log.info(buyerHoldingResult.toString());
			log.info(sellerHoldingResult.toString());

			matchResults.add(MatchResult.create(tradeResult, buyerOrderUpdate, sellerOrderUpdate, buyerHoldingResult,
					sellerHoldingResult));

			// Maker 已經完全成交
			if (makerOrder.getRemainingQuantity() == 0) {
				iterator.remove();
			}
		}
	}

	// 新增限價單沉澱(ROD)
	private void putOrderIntoQueue(Order order) {
//		old version 20260914
//		if (order.getPriceType() == PriceType.LIMIT_ORDER) {
//			// 使用限價單的佇列
//			var bookSide = getSameOrderTypeMap(order);
//			bookSide.computeIfAbsent(order.getWantBuyPrice(), k -> new ConcurrentLinkedDeque<>()).add(order);
//		}

		// 非限價單不處理
		if (order.getPriceType() != PriceType.LIMIT_ORDER) {
			return;
		}

		// 非ROD條件不處理
		if (order.getTradingType() != TradingType.ROD) {
			return;
		}

		// 不合理數量不處理
		if (order.getRemainingQuantity() == null || order.getRemainingQuantity() <= 0) {
			return;
		}

		if (order.getWantBuyPrice() == null || order.getWantBuyPrice().compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("限價掛單必須有大於 0 的價格");
		}

		var bookSide = getSameOrderTypeMap(order);

		bookSide.computeIfAbsent(order.getWantBuyPrice(), price -> new ConcurrentLinkedDeque<>()).add(order);
	}

	public void removeOrder(Order incomingorder) {
//		// 市價單不會被放進限價佇列(putOrderIntoQueue 只收 LIMIT_ORDER)，
//		// 沒有 wantBuyPrice 可以查，代表本來就沒有東西可以移除，直接返回避免 Map.get(null) 炸掉
//		if (incomingorder.getWantBuyPrice() == null) {
//			return;
//		}

		lock.lock();
		try {
			var priceMap = getSameOrderTypeMap(incomingorder);
			if (incomingorder.getWantBuyPrice() == null) {
				throw new IllegalStateException("市價單已進入撮合流程，無法取消");
			}

			ConcurrentLinkedDeque<Order> orders = priceMap.get(incomingorder.getWantBuyPrice());
			boolean removed = orders != null
					&& orders.removeIf(order -> order.getId().equals(incomingorder.getId()));
			if (!removed) {
				throw new IllegalStateException("訂單已進入成交流程或不在委託簿中，無法取消");
			}
			if (orders.isEmpty()) {
				priceMap.remove(incomingorder.getWantBuyPrice(), orders);
			}
		} finally {
			lock.unlock();
		}
	}

	private ConcurrentSkipListMap<BigDecimal, ConcurrentLinkedDeque<Order>> getSameOrderTypeMap(Order order) {
		return (order.getOrderType() == OrderType.BUY) ? limitBuyOrders : limitSellOrders;
	}

	private ConcurrentSkipListMap<BigDecimal, ConcurrentLinkedDeque<Order>> getMatchOrderTypeMap(Order order) {
		return (order.getOrderType() == OrderType.BUY) ? limitSellOrders : limitBuyOrders;
	}

	// 獲取最好的買賣價
	public BestPriceCache getBestPrice() {
		return BestPriceCache.create(getBestBidPrice(), getBestAskPrice());
	}
	
	// 獲取最好的賣價
	public BigDecimal getBestAskPrice() {
		return limitSellOrders.isEmpty() ? null : limitSellOrders.firstKey();
	}

	// 獲取最好的買價
	public BigDecimal getBestBidPrice() {
		return limitBuyOrders.isEmpty() ? null : limitBuyOrders.firstKey();
	}

	private void executeRod(Order incomingOrder, boolean isBuy, List<MatchResult> results) {
		// 對應市價單或限價單搓合
		executeByPriceType(incomingOrder, isBuy, results);

		// 市價單不會留在委託簿；未成交餘量必須直接取消，讓後續結算解除凍結
		if (incomingOrder.getPriceType() == PriceType.MARKET_ORDER
				&& incomingOrder.getRemainingQuantity() > 0) {
			incomingOrder.setStatus(
					incomingOrder.getRemainingQuantity() < incomingOrder.getOriginalQuantity()
							? OrderStatus.PARTIALLY_FILLED_CANCELLED
							: OrderStatus.CANCELLED);
			return;
		}

		// 訂單完全搓合 , 沒有剩餘數量
		if (incomingOrder.getRemainingQuantity() == 0) {
			incomingOrder.setStatus(OrderStatus.FILLED);
		} else if (incomingOrder.getRemainingQuantity() < incomingOrder.getOriginalQuantity()) {
	        // 部分成交，其餘數量留在委託簿中等待
	        incomingOrder.setStatus(OrderStatus.PARTIALLY_FILLED);
	        log.info("ROD: " + incomingOrder.getStatus() + "，已部分成交，剩餘數量保留在委託簿");
	    } else {
	        // 完全沒有成交，整張單留在委託簿中（通常維持 NEW 狀態）
	        incomingOrder.setStatus(OrderStatus.PENDING);
	        log.info("ROD: " + incomingOrder.getStatus() + "，未成交，進入委託簿等待");
	    }

	}

	private void executeIoc(Order incomingOrder, boolean isBuy, List<MatchResult> results) {

		// 對應市價單或限價單搓合
		executeByPriceType(incomingOrder, isBuy, results);

		// 訂單完全搓合 , 沒有剩餘數量
		if (incomingOrder.getRemainingQuantity() == 0) {
			incomingOrder.setStatus(OrderStatus.FILLED);
		} else if (incomingOrder.getRemainingQuantity() < incomingOrder.getOriginalQuantity()) {
			// 部分成交，未成交餘量取消
			incomingOrder.setStatus(OrderStatus.PARTIALLY_FILLED_CANCELLED);
			log.info("IOC:" + incomingOrder.getStatus() + "IOC 已經部分成交,並將剩餘數量取消");
		} else {
			// 直接取消
			incomingOrder.setStatus(OrderStatus.CANCELLED);
			log.info("IOC:" + incomingOrder.getStatus() + "直接取消");
		}
	}

	// 根據PriceType,選擇走市價單或限價單的搓合方法
	private void executeByPriceType(Order incomingOrder, boolean isBuy, List<MatchResult> results) {

		if (incomingOrder.getPriceType() == PriceType.MARKET_ORDER) {
			matchMarketOrder(incomingOrder, isBuy, results);
		} else {
			matchLimitOrder(incomingOrder, isBuy, results);
		}
	}

	// 立即全部成交否則取消
	private void executeFok(Order incomingOrder, boolean isBuy, List<MatchResult> results) {

		if (!hasEnoughQuantity(incomingOrder, isBuy)) {
			incomingOrder.setStatus(OrderStatus.CANCELLED);
			return;
		}

		executeByPriceType(incomingOrder, isBuy, results);
		incomingOrder.setStatus(OrderStatus.FILLED);

	}

	// 計算有沒有足夠數量配對 (FOK)
	private boolean hasEnoughQuantity(Order incomingOrder, boolean isBuy) {

		long availableQuantity = 0;
		var oppositeBook = getMatchOrderTypeMap(incomingOrder);

		for (var priceLevel : oppositeBook.entrySet()) {
			BigDecimal makerPrice = priceLevel.getKey();

			// 限價 FOK 只能計算符合價格的掛單
			if (incomingOrder.getPriceType() == PriceType.LIMIT_ORDER
					&& !pricesMatch(incomingOrder, isBuy, makerPrice)) {
				break;
			}
			if (incomingOrder.getPriceType() == PriceType.MARKET_ORDER
					&& !marketProtectionMatches(incomingOrder, isBuy, makerPrice)) {
				break;
			}

			for (Order makerOrder : priceLevel.getValue()) {
				if (makerOrder.getRemainingQuantity() <= 0) {
					continue;
				}

				// 自己的訂單不能計入可成交量
				if (Objects.equals(makerOrder.getUserId(), incomingOrder.getUserId())) {
					continue;
				}

				availableQuantity += makerOrder.getRemainingQuantity();

				// 可成交量足夠配對
				if (availableQuantity >= incomingOrder.getRemainingQuantity()) {
					return true;
				}
			}
		}
		log.error("配對單不足,FOK條件搓合失敗");

		return false;
	}

	// 限價單比價
	private boolean pricesMatch(Order incomingOrder, boolean isBuy, BigDecimal makerPrice) {

		return isBuy ? incomingOrder.getWantBuyPrice().compareTo(makerPrice) >= 0
				: incomingOrder.getWantBuyPrice().compareTo(makerPrice) <= 0;
	}

	private boolean marketProtectionMatches(Order incomingOrder, boolean isBuy, BigDecimal makerPrice) {
		BigDecimal protectionPrice = incomingOrder.getProtectionPrice();
		if (protectionPrice == null) {
			return false;
		}
		return isBuy
				? makerPrice.compareTo(protectionPrice) <= 0
				: makerPrice.compareTo(protectionPrice) >= 0;
	}

	/**
     * 核心方法：抓取五檔數據
     * @param isBid true 代表抓取買方(Bids)，false 代表賣方(Asks)
     */
    public List<DepthEntryDTO> getTop5(boolean isBuy) {
		// 先暫時設定為二
        List<DepthEntryDTO> top5List = new ArrayList<>(2);
        
        // 根據買賣方選擇對應的 Map
        ConcurrentSkipListMap<BigDecimal, ConcurrentLinkedDeque<Order>> book = isBuy 
						? limitBuyOrders 
						: limitSellOrders;

        // 使用 entrySet 的 iterator，這是 Weakly Consistent，不會鎖住整個 Map
        int count = 0;
        for (Map.Entry<BigDecimal, ConcurrentLinkedDeque<Order>> entry : book.entrySet()) {
            if (count >= 5) break;

            BigDecimal price = entry.getKey();
            ConcurrentLinkedDeque<Order> queue = entry.getValue();

            // 如果該價格的隊列為空，跳過它（可能剛好被撮合完還沒被 map.remove）
            if (queue == null || queue.isEmpty()) {
                continue; 
            }

            // 統計該價格層級的總數量
            long totalQty = 0;
            for (Order order : queue) {
                totalQty += order.getRemainingQuantity();
            }

            // 再次檢查，如果加總後數量大於 0 才放入五檔
            if (totalQty > 0) {
                top5List.add(new DepthEntryDTO(price, totalQty));
                count++;
            }
        }
        return top5List;
    }
	// 訂單獲取移除
	// // 獲取最佳價格的那筆訂單的API
	// public Order getBestAskOrder() {
	// if (asks.isEmpty())
	// return null;
	// return asks.firstEntry().getValue().peek();
	// }

	// public Order getBestBidOrder() {
	// if (bids.isEmpty())
	// return null;
	// return bids.firstEntry().getValue().peek();
	// }

	// 不記得這個是要幹嘛的
	// 當第一筆訂單完全成交(數量歸零)時就自orderbook中移除
	// public void popBestAskOrder() {
	// if (asks.isEmpty())
	// return;
	//
	// var entry = asks.firstEntry();
	// ConcurrentLinkedDeque<Order> queue = entry.getValue();
	// queue.poll(); // 真正移除已經被吃完的第一筆訂單
	//
	// // 如果這個價格的排隊單都被吃光了，就把這個價格層級整個拔掉
	// if (queue.isEmpty()) {
	// asks.remove(entry.getKey());
	// }
	// }

}
