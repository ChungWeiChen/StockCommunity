package com.stockcommunity.posts;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 查詢最新貼文
    List<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    

    
    // 查詢該使用者的貼文
    List<Post> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 計算該使用者的貼文總數
    long countByUser_Id(Long userId);


    

    // 查詢指定使用者的貼文
    List<Post> findAllByUser_UsernameOrderByCreatedAtDesc(String username, Pageable pageable);

    // 計算該使用者的貼文總數
    long countByUser_Username(String username);



    

    // 查詢指定股票的貼文
    List<Post> findByStocks_StockCodeOrderByCreatedAtDesc(String stockCode, Pageable pageable);

    // 計算這檔股票共有幾篇貼文
    long countByStocks_StockCode(String stockCode);



    

    // 查詢指定時間之後的貼文，供 HotScore 計算
    List<Post> findByCreatedAtAfter(LocalDateTime startTime);


    
    

    // 查詢全站熱門的五筆股票標籤
    @Query("""
        SELECT s.stockCode, s.simpleName, COUNT(p)
        FROM Post p
        JOIN p.stocks s
        GROUP BY s.stockCode, s.simpleName
        ORDER BY COUNT(p) DESC
    """)
    List<Object[]> findPopularStocks(Pageable pageable);


    // 查詢使用者最常發文的股票，依貼文數量由多至少排序
    @Query("""
        SELECT s.stockCode, s.simpleName, COUNT(p)
        FROM Post p
        JOIN p.stocks s
        WHERE p.user.id = :userId
        GROUP BY s.stockCode, s.simpleName
        ORDER BY COUNT(p) DESC
    """)
    List<Object[]> findTopStocksByUserId(
            @Param("userId") Long userId,
            Pageable pageable
    );
}