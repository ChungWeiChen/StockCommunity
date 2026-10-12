package com.stockcommunity.matchengine;

import java.util.Objects;
import java.util.Optional;

import com.stockcommunity.accounts.TradingAccountService;
import com.stockcommunity.matchengine.dto.FinalOutcome;
import org.springframework.stereotype.Component;

import com.stockcommunity.holdings.Holding;
import com.stockcommunity.holdings.HoldingRepository;
import com.stockcommunity.matchengine.dto.MatchResult;
import com.stockcommunity.matchengine.dto.OrderUpdate;
import com.stockcommunity.orders.Order;
import com.stockcommunity.orders.OrderRepository;
import com.stockcommunity.orders.OrderStatus;
import com.stockcommunity.orders.OrderType;
import com.stockcommunity.trades.Trade;
import com.stockcommunity.trades.TradeRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class TradeResultSaveService {
	// 注入你的 Spring Data JPA Repository
	private final OrderRepository orderRepo;

	private final HoldingRepository holdingRepo;

	private final TradeRepository tradeRepo;

	private final TradingAccountService tradingAccountService;

	private final com.stockcommunity.holdings.HoldingService holdingService;

	@Transactional
	public void saveToDatabase(FinalOutcome finalOutcome) {

		// 先依序保存所有實際成交
		for (MatchResult result
				: finalOutcome.matchResults()) {

			saveMatchResult(result);
		}

		// 最後保存進場訂單的最終狀態
		updateOrder(
				finalOutcome.incomingOrderUpdate()
		);
	}

	// 處理訂單最後的狀態 , 並更改DB , 無論搓合成功與否都要與記憶體內狀態一致
	private void updateOrder(
			OrderUpdate update) {

		Order order = orderRepo
				.findById(update.orderid())
				.orElseThrow(() ->
						new IllegalArgumentException(
								"找不到訂單"
						)
				);

		if (!Objects.equals(
				order.getUserId(),
				update.userId())) {

			throw new IllegalStateException(
					"訂單使用者不一致"
			);
		}

		if (!Objects.equals(
				order.getStockCode(),
				update.stockCode())) {

			throw new IllegalStateException(
					"訂單股票不一致"
			);
		}

		order.setRemainingQuantity(
				update.remainingQuantity()
		);

		order.setStatus(
				update.status()
		);

		if (update.status() == OrderStatus.CANCELLED
				|| update.status() == OrderStatus.PARTIALLY_FILLED_CANCELLED) {
			releaseRemainingReservation(order, "未成交委託退回剩餘凍結資源");
		}
	}

	// 處理MathchResult的資料 , 將舊版儲存獨立出來作為其中的方法
	private void saveMatchResult(MatchResult result) {
		// 呼叫 Repository 寫入 holding & trade 資料表
		// 建議這裡可以使用 批次寫入（Batch Insert）來進一步榨出資料庫效能！
		Trade trade = handleTrade(result);

		Order buyOrder = orderRepo.findById(result.buyerOrderUpdate().orderid())
				.orElseThrow(() -> new IllegalArgumentException("找不到買單"));
		Order sellOrder = orderRepo.findById(result.sellerOrderUpdate().orderid())
				.orElseThrow(() -> new IllegalArgumentException("找不到賣單"));

		// 交易帳戶買方處理
		tradingAccountService.settleBuyTrade(buyOrder, trade);
		// 交易帳戶賣方處理
		tradingAccountService.creditSellTrade(sellOrder, trade);

		// find and save 可能會有 Lost Update（更新遺失）問題
		// holding 處理
		Optional<Holding> buyerHoldingOp = holdingRepo.findByUserIdAndStockCode(
				result.buyerholdingUpdate().userId(),
				result.buyerholdingUpdate().stockCode());

		handleHolding(buyerHoldingOp, result, true);
		holdingService.settleReservedSale(sellOrder, result.tradeRecord().quantity());

		// Order 更新處理
		handleOrder(Optional.of(buyOrder), result, true);
		handleOrder(Optional.of(sellOrder), result, false);
	}

	private Trade handleTrade(MatchResult result) {
		Trade trade = new Trade();

		trade.setBuyOrderId(result.tradeRecord().buyOrderId());
		trade.setSellOrderId(result.tradeRecord().sellOrderId());
		trade.setStockCode(result.tradeRecord().stockCode());
		trade.setPrice(result.tradeRecord().price());
		trade.setQuantity(result.tradeRecord().quantity());
		trade.setTradedAt(result.timestamp());

		return tradeRepo.save(trade);
	}

	private void handleOrder(
			Optional<Order> orderOptional,
			MatchResult result,
			boolean isBuy) {
		if (orderOptional.isEmpty()) {
			log.error("查無訂單無法更動");
			return;
		}

		Order order = orderOptional.get();

		OrderUpdate orderUpdate = isBuy
				? result.buyerOrderUpdate()
				: result.sellerOrderUpdate();

		if(!Objects.equals(order.getId(), orderUpdate.orderid()) ||
				!Objects.equals(order.getUserId(), orderUpdate.userId()) ||
				!order.getStockCode().equals(orderUpdate.stockCode())) {
			log.error("兩筆資料對不上，無法更新");
			return;
		}

		order.setRemainingQuantity(orderUpdate.remainingQuantity());
		order.setStatus(orderUpdate.status());

		orderRepo.save(order);
		log.info((isBuy ? "Buyer" : "Seller") + "Order已更新");

	}

	private void handleHolding(
    		Optional<Holding> HoldingOptional, 
    		MatchResult result,
    		boolean isBuy) {
    	
    	Holding holding;
    	if(HoldingOptional.isEmpty()) {
    		holding = new Holding();
    		
    		holding.setUserId(isBuy 
    				? result.buyerholdingUpdate().userId() 
    				: result.sellerholdingUpdate().userId());
    		holding.setStockCode(result.tradeRecord().stockCode());
    		
    		holding.RecalculateAveragePriceAndQuantity(result.tradeRecord().quantity(), 
    				result.tradeRecord().price(), isBuy);  
    		
    		holding.setUpdatedAt(result.timestamp());
    	} else {
    		holding = HoldingOptional.get();

    		holding.RecalculateAveragePriceAndQuantity(result.tradeRecord().quantity(), 
    				result.tradeRecord().price(), isBuy);  		
    		
    		holding.setUpdatedAt(result.timestamp());
    	}
    	
    	holdingRepo.save(holding);
    	log.info((isBuy ? "Buyer" : "Seller") + "holding已更新");

	}

	private void releaseRemainingReservation(Order order, String description) {
		if (order.getOrderType() == OrderType.BUY) {
			tradingAccountService.releaseReservedCash(order, description);
		} else {
			holdingService.releaseReservedShares(order);
		}

	}
}