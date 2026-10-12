package com.stockcommunity.accounts;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface TradingAccountRepository extends JpaRepository<TradingAccount, Long> {

    Optional<TradingAccount> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    // 扣款、凍結、退款時使用 findByUserIdForUpdate()，有LOCK避免同時兩張訂單都認為餘額足夠。
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT account FROM TradingAccount account WHERE account.userId = :userId")
    Optional<TradingAccount> findByUserIdForUpdate(@Param("userId") Long userId);

    // 賽季重置用：撈出全部交易帳戶，但排除系統帳號
    @Query("SELECT a FROM TradingAccount a WHERE a.userId NOT IN " +
            "(SELECT u.id FROM Users u WHERE u.username IN :reservedUsernames)")
    List<TradingAccount> findAllExcludingUsernames(@Param("reservedUsernames") Set<String> reservedUsernames);
}
