package com.stockcommunity.postLikes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostLikeRepository extends JpaRepository<PostLike, Long>{

	boolean existsByUser_IdAndPost_Id(Long userId, Long postId);
	
	void deleteByUser_IdAndPost_Id(Long userId, Long postId);
	
	long countByPost_Id(Long postId);
	
	void deleteByPost_Id(Long postId);
	
	@Query("""
		    SELECT COUNT(pl)
		    FROM PostLike pl
		    WHERE pl.post.user.id = :userId
		""")
		long countReceivedLikesByUserId(@Param("userId") Long userId);
	
	
}
