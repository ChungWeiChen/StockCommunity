package com.stockcommunity.matchengine;

import java.util.List;
import java.util.concurrent.BlockingQueue;

import com.stockcommunity.matchengine.dto.FinalOutcome;
import com.stockcommunity.accounts.TradingAccountService;
import com.stockcommunity.holdings.HoldingService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.stockcommunity.matchengine.dto.CancelOrderResult;
import com.stockcommunity.matchengine.dto.CancelOrderStatus;
import com.stockcommunity.orderbook.OrderBook;
import com.stockcommunity.orderbook.OrderBookManager;
import com.stockcommunity.orderqueue.OrderQueueManager;
import com.stockcommunity.orders.Order;
import com.stockcommunity.orders.OrderRepository;
import com.stockcommunity.orders.OrderStatus;
import com.stockcommunity.orders.OrderType;

import jakarta.annotation.PostConstruct;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

//只負責管理整個撮合流程，具體實作交由orderbook實現
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingEngine {

    private final OrderQueueManager orderQueueManager;

    private final OrderBookManager orderBookManager;

    private final BlockingQueue<FinalOutcome> dbWriteQueue;

    private final OrderRepository orderRepository;

    private final TradingAccountService tradingAccountService;

    private final HoldingService holdingService;

    private volatile Thread workerThread;

    // 從佇列中獲取訂單
    // 將訂單丟給該支股票的orderbook
    // 當orderbook回傳結果後將結果寫入資料庫(holding & trade)

    /**
     * Spring Boot 啟動時會自動呼叫此 init 方法
     */
    @PostConstruct
    public void init() {

        // 1. 撈出所有卡在 PENDING 跟 PARTIALLY_FILLED 的訂單
        List<Order> ghostPendingOrders = orderRepository
                .findByStatus(OrderStatus.PENDING);
        List<Order> ghostPartiallyFilledOrders = orderRepository
                .findByStatus(OrderStatus.PARTIALLY_FILLED);

        if (ghostPendingOrders.isEmpty() &&
                ghostPartiallyFilledOrders.isEmpty()) {
            return;
        }

        // 2. 逐筆解除資金／持股凍結，再把殘留訂單改為取消
        List<Order> ghostOrders = new java.util.ArrayList<>();
        ghostOrders.addAll(ghostPendingOrders);
        ghostOrders.addAll(ghostPartiallyFilledOrders);

        for (Order order : ghostOrders) {
            if (order.getOrderType() == OrderType.BUY) {
                tradingAccountService.releaseReservedCash(order, "系統重啟，退回殘留委託凍結資金");
            } else {
                holdingService.releaseReservedShares(order);
            }

            order.setStatus(order.getStatus() == OrderStatus.PARTIALLY_FILLED
                    ? OrderStatus.PARTIALLY_FILLED_CANCELLED
                    : OrderStatus.CANCELLED);

        }
        orderRepository.saveAll(ghostOrders);

        log.info("已完成修改不在引擎中的訂單狀態");
        // 備註：此處 init 執行完畢且無異常後，Spring 才會完成 Bean 初始化，
        // 隨後才會啟動 API Gateway（如 Controller）或 Kafka/RabbitMQ 監聽器。
    }

    // 當 Spring 容器啟動完成後，會使用專屬的執行緒池來執行這個行為
    @Async("matchEngineExecutor")
    @EventListener(ApplicationReadyEvent.class)
    public void matchOrder() {
        log.info("撮合引擎已成功在背景啟動...");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // 1. 從 Order Queue 取得訂單
                Order order = orderQueueManager.takeOrder();

                // 2. 找到該股票的 OrderBook
                OrderBook orderBook = orderBookManager.getOrCreateOrderBook(order.getStockCode());

                // 3. 將訂單丟給 OrderBook 撮合
                FinalOutcome finalOutcome = orderBook.handleOrder(order);

                // 4. 將撮合結果丟到 DB Queue
                dbWriteQueue.put(finalOutcome);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {

                // 實際專案這裡應該使用 logger
                // 需要另外設計失敗訂單 / retry 機制

                e.printStackTrace();
            }
        }
        log.info("Matching-Engine 執行緒已安全退出");

    }

    public CancelOrderResult cancelOrder(Order order) {

        if (order == null || order.getId() == null) {
            return CancelOrderResult.create(CancelOrderStatus.ORDER_NOT_FOUND);
        }

        // 第一階段：訂單還在等待撮合
        if (orderQueueManager.removeOrder(order)) {
            return CancelOrderResult.create(CancelOrderStatus.CANCELLED);
        }

        // 第二階段: 已經進入到訂單簿中
        return orderBookManager.cancelOrder(order);

    }

    // 理論上停止時應該要將訂單全部取消

    // @PreDestroy
    // public void stopWorker() {
    //
    // log.info("正在停止 Matching-Engine...");

    // if (workerThread != null) {
    // workerThread.interrupt();
    // }
    // }

}