package com.stockcommunity.comments;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository  extends JpaRepository<Comment, Long>{


	List<Comment> findByPostIdOrderByCreatedAtDesc(Long postId);
	
	void deleteByPost_Id(Long postId);
	
	long countByUser_Id(Long userId);
	
	long countByPost_Id(Long postId);
	
	
	@Query("""
		    SELECT COUNT(DISTINCT c.user.id)
		    FROM Comment c
		    WHERE c.post.id = :postId
		""")
		long countUniqueCommentersByPostId(@Param("postId") Long postId);
}
