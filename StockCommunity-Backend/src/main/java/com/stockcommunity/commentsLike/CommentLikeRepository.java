package com.stockcommunity.commentsLike;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long>{

	boolean existsByUser_IdAndComment_Id(Long userId, Long commentId);
	
	void deleteByUser_IdAndComment_Id(Long userId, Long commentId);
	
	void deleteByComment_Id(Long commentId);
	
	void deleteByComment_Post_Id(Long postId);
	
	long countByComment_Id(Long commentId);
}
