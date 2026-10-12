package com.stockcommunity.orders;

import com.stockcommunity.accounts.TradingAccountService;
import com.stockcommunity.holdings.HoldingService;
import org.springframework.context.ApplicationEventPublisher;

import org.springframework.stereotype.Service;

import com.stockcommunity.matchengine.MatchingEngine;
import com.stockcommunity.matchengine.dto.CancelOrderResult;
import com.stockcommunity.matchengine.dto.CancelOrderStatus;
import com.stockcommunity.orderqueue.OrderQueueManager;
import com.stockcommunity.orders.event.OrderCreatedEvent;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import jakarta.transaction.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    private final MatchingEngine matchingEngine;
    private final TradingAccountService tradingAccountService;
    private final HoldingService holdingService;
    private final ApplicationEventPublisher eventPublisher;

    // 建立新訂單，歸屬於指定的會員
    @Transactional
    public Order createOrder(Long userId,
                             String stockCode,
                             OrderType orderType,
                             Integer quantity,
                             BigDecimal wantBuyPrice,
                             BigDecimal protectionPrice,
                             PriceType priceType,
                             TradingType tradingType,
                             TradingTime tradingTime) {

        if (stockCode == null || stockCode.isBlank()) {
            throw new IllegalArgumentException("股票代號不能為空");
        }
        if (orderType == null) {
            throw new IllegalArgumentException("必須指定買進或賣出");
        }

        // 股數必須大於 0，避免前端被繞過（直接打 API）送出負數/0股
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("股數必須大於 0");
        }

        // 防呆：一定要指定市價單或限價單
        if (priceType == null) {
            throw new IllegalArgumentException("必須指定價格類型（市價單或限價單）");
        }

        // 限價單才需要驗證價格；市價單不需要填價格，交由撮合當下的市價成交
        if (priceType == PriceType.LIMIT_ORDER) {
            if (wantBuyPrice == null || wantBuyPrice.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("限價單必須指定大於 0 的價格");
            }
        } else if (protectionPrice == null || protectionPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("市價單必須指定大於 0 的保護價格");
        }

        // 防呆：一定要指定下單條件
        if (tradingType == null) {
            throw new IllegalArgumentException("必須指定下單條件（ROD/IOC/FOK）");
        }

        // 防呆：一定要指定交易時段
        if (tradingTime == null) {
            throw new IllegalArgumentException("必須指定交易時段（盤中/盤後）");
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setStockCode(stockCode);
        order.setOrderType(orderType);
        // 初始股票登記時剩餘股票與使用者所下之股數相等
        order.setOriginalQuantity(quantity);
        order.setRemainingQuantity(quantity);
        // 市價單不存價格，避免跟限價單的真實委託價混淆
        order.setWantBuyPrice(priceType == PriceType.LIMIT_ORDER ? wantBuyPrice : null);
        order.setProtectionPrice(priceType == PriceType.MARKET_ORDER ? protectionPrice : null);
        order.setReservedCash(BigDecimal.ZERO);
        order.setReservedQuantity(0);
        order.setPriceType(priceType);
        order.setTradingType(tradingType);
        order.setTradingTime(tradingTime);

        Order savedOrder = orderRepository.save(order);
        if (orderType == OrderType.BUY) {
            BigDecimal reservationPrice = priceType == PriceType.LIMIT_ORDER ? wantBuyPrice : protectionPrice;
            BigDecimal requiredCash = reservationPrice
                    .multiply(BigDecimal.valueOf(quantity))
                    .setScale(2, RoundingMode.HALF_UP);
            tradingAccountService.reserveCash(savedOrder, requiredCash);
        } else {
            holdingService.reserveShares(savedOrder);
        }

        eventPublisher.publishEvent(new OrderCreatedEvent(savedOrder.getId()));
        return savedOrder;
    }

    // 保留舊呼叫介面，讓既有造市商限價單不必同步改簽章
    @Transactional
    public Order createOrder(Long userId,
                             String stockCode,
                             OrderType orderType,
                             Integer quantity,
                             BigDecimal wantBuyPrice,
                             PriceType priceType,
                             TradingType tradingType,
                             TradingTime tradingTime) throws InterruptedException {
        return createOrder(userId, stockCode, orderType, quantity, wantBuyPrice, null,
                priceType, tradingType, tradingTime);
    }

    // 查目前登入者自己的所有訂單
    public List<Order> findOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    // 依 id 找單一訂單，找不到就丟例外，統一在 Controller 層接
    public Order findOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("找不到訂單"));
    }

    // 取消訂單，將訂單狀態設為已取消
    @Transactional
    public Order cancelOrder(Long orderId, Long userId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("找不到訂單"));

        if (!order.getUserId().equals(userId)) {
            throw new SecurityException("無權限取消此訂單");
        }

        if (order.getStatus() == OrderStatus.FILLED
                || order.getStatus() == OrderStatus.CANCELLED
                || order.getStatus() == OrderStatus.PARTIALLY_FILLED_CANCELLED) {
            throw new IllegalStateException("只能取消待處理中或部分成交的訂單，目前狀態：" + order.getStatus());
        }

        CancelOrderResult result = matchingEngine.cancelOrder(order);

        if (result.status() == CancelOrderStatus.CANCELLED) {
            order.markCancelled();
        }

        if (result.status() == CancelOrderStatus.PARTIALLY_FILLED_CANCELLED) {
            order.markPartiallyFilledCancelled();
        }

        if (result.status() == CancelOrderStatus.ORDER_NOT_FOUND
                || result.status() == CancelOrderStatus.CANNOT_CANCEL) {
            throw new IllegalStateException("訂單目前無法取消");
        }

        releaseReservation(order, "取消委託退回剩餘凍結資源");

        return orderRepository.save(order);
    }

    @Transactional
    public void cancelUnqueuedOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("找不到訂單"));
        if (order.getStatus() == OrderStatus.PENDING) {
            order.markCancelled();
            releaseReservation(order, "訂單未能進入撮合佇列，退回凍結資源");
        }
    }

    private void releaseReservation(Order order, String description) {
        if (order.getOrderType() == OrderType.BUY) {
            tradingAccountService.releaseReservedCash(order, description);
        } else {
            holdingService.releaseReservedShares(order);
        }
    }

    // 找出時間區間內的order , byOrderId
    public List<Order> findOrdersByDateRange(
            Long userId,
            LocalDate startDate,
            LocalDate endDate) {

        ZoneId taipei = ZoneId.of("Asia/Taipei");

        Instant from = startDate.atStartOfDay(taipei).toInstant();
        Instant toExclusive = endDate.plusDays(1)
                .atStartOfDay(taipei)
                .toInstant();

        return orderRepository
                .findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                        userId, from, toExclusive);
    }
}