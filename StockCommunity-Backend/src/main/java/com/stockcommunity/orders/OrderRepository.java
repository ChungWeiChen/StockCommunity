package com.stockcommunity.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

	List<Order> findByUserId(Long userId); // 查詢某使用者的所有訂單

	// 找當天是否有尚且存活的訂單(給造市者用)
	@Query("SELECT COUNT(o) > 0 FROM Order o " 
			+ "WHERE CAST(o.createdAt AS date) = CAST(:time AS date)"
			+ "AND o.status IN (OrderStatus.PENDING, OrderStatus.PARTIALLY_FILLED) " 
			+ "AND o.userId = :userId")
	boolean existsTodayAliveOrderByCreatedAtDay(@Param("time") Instant time, @Param("userId") Long userId);

	@Query("SELECT o.id FROM Order o " + "WHERE CAST(o.createdAt AS date) = CAST(:time AS date)"
			+ "AND o.stockCode = :stockCode " + "AND o.status IN (OrderStatus.PENDING, OrderStatus.PARTIALLY_FILLED) "
			+ "AND o.userId = :userId")
	List<Long> findTodayAliveOrderByOrderIdAndStockCode(@Param("time") Instant time, @Param("userId") Long userId,
			@Param("stockCode") String stockCode);

	@Query("""
			    SELECT o FROM Order o
			    WHERE o.userId = :userId
			    AND o.status IN (OrderStatus.PENDING, OrderStatus.PARTIALLY_FILLED)
			""")
	List<Order> findActiveOrdersByUserId(@Param("userId") Long userId);


	/*
	 * 給引擎刪除上一輪未成交訂單用的
	 */
	List<Order> findByStatus(OrderStatus status);
	
	
	// 直接把 PENDING 和 PARTIALLY_FILLED 一起改掉（最推薦，減少 DB 連線次數）
    @Modifying
    @Transactional
    @Query("UPDATE Order o SET o.status = :cancelledStatus WHERE o.status IN (:statuses)")
    int updateMultipleStatusToCancelled(@Param("cancelledStatus") OrderStatus cancelledStatus, @Param("statuses") OrderStatus statuses);


	List<Order>
	findByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
			Long userId,
			Instant from,
			Instant toExclusive
	);

}
