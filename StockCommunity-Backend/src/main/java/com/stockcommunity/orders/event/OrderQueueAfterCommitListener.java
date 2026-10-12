package com.stockcommunity.orders.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.stockcommunity.orderqueue.OrderQueueManager;
import com.stockcommunity.orders.Order;
import com.stockcommunity.orders.OrderRepository;
import com.stockcommunity.orders.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderQueueAfterCommitListener {

    private final OrderRepository orderRepository;
    private final OrderQueueManager orderQueueManager;
    private final OrderService orderService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enqueue(OrderCreatedEvent event) {
        Order order = orderRepository.findById(event.orderId())
                .orElseThrow(() -> new IllegalStateException("找不到已提交的訂單：" + event.orderId()));
        try {
            orderQueueManager.addOrder(order);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("訂單 {} 加入撮合佇列時被中斷，將取消並解除凍結", event.orderId(), e);
            orderService.cancelUnqueuedOrder(event.orderId());
        }
    }
}
