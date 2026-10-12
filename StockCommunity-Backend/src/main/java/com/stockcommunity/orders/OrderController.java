package com.stockcommunity.orders;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.stockcommunity.orders.dto.CreateOrderRequest;
import com.stockcommunity.security.CurrentUserProvider;

import lombok.RequiredArgsConstructor;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CurrentUserProvider currentUserProvider;

    // 建立新訂單，歸屬於目前登入的會員
    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request) {
        try {
            Long userId = currentUserProvider.getCurrentUserId();
            Order order = orderService.createOrder(
                    userId,
                    request.getStockCode(),
                    request.getOrderType(),
                    request.getQuantity(),
                    request.getWantBuyPrice(),
                    request.getProtectionPrice(),
                    request.getPriceType(),
                    request.getTradingType(),
                    request.getTradingTime()
            );            
            
            return ResponseEntity.ok(order);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    // 跟 WatchlistController 完全同一套邏輯：從 SecurityContext 解出 username 再查 userId
    // 不接受前端自己傳 userId
    // 查目前登入者自己的所有訂單
    @GetMapping("/mine")
    public ResponseEntity<List<Order>> getMyOrders() {
        Long userId = currentUserProvider.getCurrentUserId();
        List<Order> orders = orderService.findOrdersByUserId(userId);
        return ResponseEntity.ok(orders);
    }

    // 查單一訂單前，先確認這張單是不是自己的
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable Long orderId) {
        try {
            Long userId = currentUserProvider.getCurrentUserId();
            Order order = orderService.findOrderById(orderId);
            if (!order.getUserId().equals(userId)) {
                return ResponseEntity.status(403).body("無權限查看此訂單");
            }
            return ResponseEntity.ok(order);
        } catch(IllegalArgumentException e) {
            // 查不到訂單，是資源不存在，不是請求格式有問題，用 404 比 400 更精確
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // 取消訂單，Service 層已經包含擁有權與狀態檢查
    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        try {
            Long userId = currentUserProvider.getCurrentUserId();
            Order order = orderService.cancelOrder(orderId, userId);
            return ResponseEntity.ok(order);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }

    // 查詢時間區間內order, byOrderId
    @GetMapping("/mine/range")
    public ResponseEntity<List<Order>> getMyOrdersByDateRange(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "開始日期不能晚於結束日期"
            );
        }

        Long userId = currentUserProvider.getCurrentUserId();
        List<Order> orders =
                orderService.findOrdersByDateRange(userId, startDate, endDate);

        return ResponseEntity.ok(orders);
    }
}
