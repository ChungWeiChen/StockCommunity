package com.stockcommunity.commentsLike;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentDislikeRepository
        extends JpaRepository<CommentDislike, Long> {

    boolean existsByUser_IdAndComment_Id(Long userId,Long commentId);

    void deleteByUser_IdAndComment_Id(Long userId,Long commentId);

    long countByComment_Id(Long commentId);

    // 刪除單一留言時使用
    void deleteByComment_Id(Long commentId);

    // 刪除整篇貼文時使用
    void deleteByComment_Post_Id(Long postId);
    // HotScore 之後會使用
    long countByComment_Post_Id(Long postId);
}
