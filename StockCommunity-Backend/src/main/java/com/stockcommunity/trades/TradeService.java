package com.stockcommunity.trades;

import org.springframework.stereotype.Service;

import com.stockcommunity.orders.Order;
import com.stockcommunity.orders.OrderRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TradeService {

    private final TradeRepository tradeRepository;
    private final OrderRepository orderRepository;

    public TradeService(TradeRepository tradeRepository, OrderRepository orderRepository) {
        this.tradeRepository = tradeRepository;
        this.orderRepository = orderRepository;
    }

    // 撮合成功後呼叫，記錄一筆成交
    public Trade createTrade(
            Long buyOrderId,
            Long sellOrderId,
            String stockCode,
            BigDecimal price,
            Integer quantity) {

        // 防呆：買單、賣單 id 不能相同
        if (buyOrderId != null && buyOrderId.equals(sellOrderId)) {
            throw new IllegalArgumentException("買單與賣單不能是同一筆訂單");
        }

        // 防呆：買單、賣單都必須是真實存在的訂單
        if (!orderRepository.existsById(buyOrderId)) {
            throw new IllegalArgumentException("找不到買單：" + buyOrderId);
        }
        if (!orderRepository.existsById(sellOrderId)) {
            throw new IllegalArgumentException("找不到賣單：" + sellOrderId);
        }

        // 防呆：成交股數必須大於 0
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("成交股數必須大於 0");
        }

        // 防呆：成交價格必須大於 0
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("成交價格必須大於 0");
        }

        Trade trade = new Trade();
        trade.setBuyOrderId(buyOrderId);
        trade.setSellOrderId(sellOrderId);
        trade.setStockCode(stockCode);
        trade.setPrice(price);
        trade.setQuantity(quantity);
        return tradeRepository.save(trade);
    }

    // 查某筆訂單相關的所有成交紀錄，不管這筆單當初是買方還是賣方都查得到
    public List<Trade> findTradesByOrderId(Long orderId) {
        List<Trade> asBuyer = tradeRepository.findByBuyOrderId(orderId);
        List<Trade> asSeller = tradeRepository.findBySellOrderId(orderId);

        return Stream.concat(asBuyer.stream(), asSeller.stream())
                .collect(Collectors.toList());
    }

    // 查登入者名下所有訂單相關的成交紀錄：先查出這個人的所有 Order，再逐筆撈成交紀錄合併
    public List<Trade> findTradesByUserId(Long userId) {
        List<Order> myOrders = orderRepository.findByUserId(userId);

        return myOrders.stream()
                .flatMap(order -> findTradesByOrderId(order.getId()).stream())
                .collect(Collectors.toList());
    }

    // 查某檔股票的所有成交紀錄
    public List<Trade> findTradesByStockCode(String stockCode) {
        return tradeRepository.findByStockCode(stockCode);
    }

}
