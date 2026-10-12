package com.stockcommunity.holdings;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Set;

public interface HoldingRepository extends JpaRepository<Holding, Long> {

    // 排行榜用：撈出全部持股，但排除系統帳號(造市商/未來假AI帳號)
    @Query("SELECT h FROM Holding h WHERE h.userId NOT IN " +
            "(SELECT u.id FROM Users u WHERE u.username IN :reservedUsernames)")
    List<Holding> findAllExcludingUsernames(@Param("reservedUsernames") Set<String> reservedUsernames);

    // 查某個使用者的所有持股（顯示個人投資組合用）
    List<Holding> findByUserId(Long userId);

    // 查某個使用者對某檔股票的持股紀錄（成交後要更新這一筆，或判斷有沒有這筆持股）
    // 回傳 Optional，因為「還沒持有這檔股票」是正常情況，不算例外
    Optional<Holding> findByUserIdAndStockCode(Long userId, String stockCode);

    // lock防止同時被兩張單來查詢可賣數量
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM Holding h WHERE h.userId = :userId AND h.stockCode = :stockCode")
    Optional<Holding> findByUserIdAndStockCodeForUpdate(
            @Param("userId") Long userId,
            @Param("stockCode") String stockCode);
}
