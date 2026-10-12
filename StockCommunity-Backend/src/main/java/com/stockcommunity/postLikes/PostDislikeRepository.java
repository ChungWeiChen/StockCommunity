package com.stockcommunity.postLikes;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PostDislikeRepository
        extends JpaRepository<PostDislike, Long> {

    // 判斷某個使用者是否倒讚這篇貼文
    boolean existsByUser_IdAndPost_Id(Long userId,Long postId);

    // 取消倒讚
    void deleteByUser_IdAndPost_Id(Long userId,Long postId);

    // 計算這篇貼文的倒讚總數
    long countByPost_Id(Long postId);

    // 刪除貼文時，可以先清除該貼文所有倒讚
    void deleteByPost_Id(Long postId);
}
