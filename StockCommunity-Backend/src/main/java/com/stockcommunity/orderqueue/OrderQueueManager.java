package com.stockcommunity.orderqueue;

import java.util.concurrent.BlockingQueue;

import org.springframework.stereotype.Component;

import com.stockcommunity.orders.Order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderQueueManager {
	
	// 話說這個後面可能要改用DTO、畢竟不在entitymanger的範疇中直接用不知道會不會出問題

	// 配合 OrderQueueConfig 生成Bean，Manger這邊只需注入
//	// 關鍵：將 @Qualifier 寫在變數宣告上方(名稱有對上就先不用，出事再來改)
	private final BlockingQueue<Order> matchOrderQueue;

//    @Qualifier("limitOrderQueue")
// 	private final BlockingQueue<Order> limitOrderQueue;
    
// //    @Qualifier("marketOrderQueue")
//     private final BlockingQueue<> marketOrderQueue;
    
	// 訂單佇列(不去分到底是限價還是市價)
	// 
	public void addOrder(Order order) throws InterruptedException {
		log.info("MatchOrderQueue 新增一筆訂單");
		matchOrderQueue.put(order);
		
	}
	
	// 拿訂單去處理
	public Order takeOrder() throws InterruptedException {
		return matchOrderQueue.take();
	}

	// 刪除訂單
	public boolean removeOrder(Order order) {
		if (order == null || order.getId() == null) {
			return false;
		}
		
		return matchOrderQueue.removeIf(qOrder ->
			qOrder != null &&
			qOrder.getId() != null &&
			qOrder.getId().equals(order.getId())
		);
	}

	// // 限價單佇列
	// // 新增訂單
	// public boolean addLimitQueue(Order order) {
	// 	log.info("limitOrderQueue 新增一筆訂單");
	// 	return limitOrderQueue.offer(order);
	// }

	// // 拿訂單去處理
	// public Order takeLimitQueue() throws InterruptedException {
	// 	return limitOrderQueue.take();
	// }

	// // 刪除訂單


	// // 市價單佇列方法
	// // 新增訂單
	// public boolean addMarketQueue(Order order) {
	// 	return marketOrderQueue.offer(order);
	// }

	// // 拿訂單去處理
	// public Order putMarketQueue() throws InterruptedException {
	// 	return marketOrderQueue.take();
	// }

	

}
