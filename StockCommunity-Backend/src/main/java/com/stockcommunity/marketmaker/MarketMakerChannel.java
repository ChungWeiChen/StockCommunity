package com.stockcommunity.marketmaker;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.stereotype.Component;

import com.stockcommunity.marketmaker.dto.ExecutionReport;
import com.stockcommunity.matchengine.dto.MatchResult;
import com.stockcommunity.orders.Order;
// 取消

//// 專門給造市者用的訂單通道佇列
//@Component
//public class MarketMakerChannel {
//	// 造市者 發送訂單給 引擎 的通道
//    private final BlockingQueue<Order> orderQueue = new LinkedBlockingQueue<>();
//
//    public BlockingQueue<Order> getOrderQueue() { return orderQueue; }
//    // // 引擎傳回結果的通道，目前不會用到
//    // private final BlockingQueue<List<MatchResult>> reportQueue = new LinkedBlockingQueue<>();
//
//    // public BlockingQueue<List<MatchResult>> getReportQueue() { return reportQueue; }
//
//}
