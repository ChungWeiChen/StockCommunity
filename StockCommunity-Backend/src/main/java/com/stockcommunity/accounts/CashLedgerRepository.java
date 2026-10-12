package com.stockcommunity.accounts;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashLedgerRepository extends JpaRepository<CashLedger, Long> {

    Page<CashLedger> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<CashLedger> findByOrderIdOrderByCreatedAtAsc(Long orderId);

    List<CashLedger> findByTradeIdOrderByCreatedAtAsc(Long tradeId);

    boolean existsByReferenceKey(String referenceKey);
}
