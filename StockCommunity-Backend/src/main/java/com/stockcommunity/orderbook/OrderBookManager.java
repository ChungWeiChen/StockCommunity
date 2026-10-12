package com.stockcommunity.orderbook;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import com.stockcommunity.matchengine.dto.CancelOrderResult;
import com.stockcommunity.matchengine.dto.CancelOrderStatus;
import com.stockcommunity.matchengine.dto.DepthEntryDTO;
import com.stockcommunity.orderbook.dto.BestPriceCache;
import com.stockcommunity.orderbook.dto.MarketDepthCache;
import com.stockcommunity.orders.Order;
import com.stockcommunity.orders.OrderStatus;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

// 所有股票orderbook的管理者
@Component
@Slf4j 
@Getter
@Setter
public class OrderBookManager {
    private final Map<String, OrderBook> stockOrderbooks = new ConcurrentHashMap<>();

    private final Map<String, MarketDepthCache> depthCacheMap = new ConcurrentHashMap<>();


    
    // 宣告 Spring 的任務排程器與任務控制物件
    private ThreadPoolTaskScheduler depthScheduler;
    private ScheduledFuture<?> scheduledFuture;

    /**
     * 根據股票代碼獲取 OrderBook，如果不存在則創建一個新的
     * 
     * @param code 股票代碼 (對應 Stock 的 @Id)
     * @return 該股票的 OrderBook
     */
    public OrderBook getOrCreateOrderBook(String stockCode) {
        return stockOrderbooks.computeIfAbsent(stockCode, k -> new OrderBook(k));
    }

    /**
     * 移除特定股票的 OrderBook
     */
    public void removeOrderBook(String code) {
    	stockOrderbooks.remove(code);
    }

    public CancelOrderResult cancelOrder(Order order) {
        if (order == null || order.getId() == null) {
            return CancelOrderResult.create(CancelOrderStatus.ORDER_NOT_FOUND);
        }

        OrderBook orderBook = stockOrderbooks.get(order.getStockCode());
        if(orderBook == null) {
            throw new IllegalArgumentException("找不到訂單");
        }

        orderBook.removeOrder(order);
        
        if(order.getStatus() == OrderStatus.PARTIALLY_FILLED) {
            return CancelOrderResult
            		.create(CancelOrderStatus.PARTIALLY_FILLED_CANCELLED);
        }
        
        return CancelOrderResult.create(CancelOrderStatus.CANCELLED);
    }

    /**
     * 在 Spring Bean 初始化完成後，自動啟動專屬的定時執行緒
     */

    @PostConstruct
    public void init() {
        System.out.println("====== [市場深度獲取中心] 正在初始化排程器... ======");
        
        depthScheduler = new ThreadPoolTaskScheduler();
        depthScheduler.setPoolSize(1); 
        depthScheduler.setThreadNamePrefix("市場深度獲取中心-");
        depthScheduler.initialize(); 

        int periodInSeconds = 5;

        // 💡 優化：改用標準的 Runnable Lambda 寫法，並在內部補上防禦
        this.scheduledFuture = depthScheduler.scheduleAtFixedRate(
                () -> {
                    System.out.println("⏰ 定時任務觸發：開始計算五檔快取... 當前股票總數: " + stockOrderbooks.size());
                    refreshMarketDepthCache();
                }, 
                Duration.ofSeconds(periodInSeconds)
        );
        
        System.out.println("====== [市場深度獲取中心] 排程器啟動成功，每 5 秒執行一次 ======");
    }

    private void refreshMarketDepthCache() {
        if (stockOrderbooks.isEmpty()) {
            return;
        }

        try {
            for (Map.Entry<String, OrderBook> entry : stockOrderbooks.entrySet()) {
                String stockCode = entry.getKey();
                OrderBook orderBook = entry.getValue();

                List<DepthEntryDTO> top5Bids = orderBook.getTop5(true);
                List<DepthEntryDTO> top5Asks = orderBook.getTop5(false);

                depthCacheMap.put(stockCode, new MarketDepthCache(top5Bids, top5Asks, System.currentTimeMillis()));
            }
        } catch (Throwable e) { 
            // 💡 關鍵：改成捕捉 Throwable。
            // 萬一裡面發生 NullPointerException 或 NoClassDefFoundError，保證能印出錯誤且不弄死排程執行緒
            e.printStackTrace(); 
        }
    }

    /**
     * 🚪 供 Controller 或 WebSocket 呼叫的 API 接口
     * @param stockCode 股票代號 (例如 "2330")
     */
    public MarketDepthCache getLatestMarketDepth(String stockCode) {
        // 從快取 Map 中直接極速獲取，如果該股票還沒有快取，回傳空五檔避免 NullPointerException
        return depthCacheMap.getOrDefault(stockCode, 
                new MarketDepthCache(List.of(), List.of(), System.currentTimeMillis()));
    }
    
    
    
    


    /**
     * 🔴 當 Spring 容器關閉時，優雅地停止排程器
     */
    @PreDestroy
    public void shutdown() {
        if (this.scheduledFuture != null) {
            this.scheduledFuture.cancel(false);
        }
        if (this.depthScheduler != null) {
            this.depthScheduler.shutdown();
        }
    }
}
