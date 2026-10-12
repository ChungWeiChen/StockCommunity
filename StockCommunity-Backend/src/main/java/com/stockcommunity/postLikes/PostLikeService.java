package com.stockcommunity.postLikes;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.stockcommunity.postLikes.dto.PostLikeResponseDTO;
import com.stockcommunity.posts.Post;
import com.stockcommunity.posts.PostRepository;
import com.stockcommunity.security.CurrentUserProvider;
import com.stockcommunity.users.UserRepository;
import com.stockcommunity.users.Users;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostLikeService {
	
	private final PostLikeRepository postLikeRepo;
	private final PostDislikeRepository postDislikeRepo;
	private final PostRepository postRepo;
	private final UserRepository userRepo;
	
	private final CurrentUserProvider currentUserProvider;
	
	@Transactional
	public PostLikeResponseDTO addLike(Long postId) {

	    Long userId = currentUserProvider.getCurrentUserId();

	    Users user = userRepo.findById(userId)
	            .orElseThrow(() ->
	                    new ResponseStatusException(HttpStatus.NOT_FOUND));

	    Post post = postRepo.findById(postId)
	            .orElseThrow(() ->
	                    new ResponseStatusException(HttpStatus.NOT_FOUND));

	    boolean alreadyLiked = postLikeRepo.existsByUser_IdAndPost_Id(userId, postId);

	    if (alreadyLiked) {

	        // 已經按讚，再按一次取消讚
	        postLikeRepo.deleteByUser_IdAndPost_Id(userId, postId);

	    } else {

	        // 如果原本有倒讚，先取消倒讚
	        boolean alreadyDisliked = postDislikeRepo.existsByUser_IdAndPost_Id(userId,postId);

	        if (alreadyDisliked) {
	            postDislikeRepo.deleteByUser_IdAndPost_Id(userId,postId);
	        }

	        // 新增按讚
	        PostLike like = new PostLike();

	        like.setUser(user);
	        like.setPost(post);

	        postLikeRepo.save(like);
	    }

	    // 重新查詢最新狀態
	    long postLikeTotal = postLikeRepo.countByPost_Id(postId);

	    long postDislikeTotal = postDislikeRepo.countByPost_Id(postId);

	    boolean liked = postLikeRepo.existsByUser_IdAndPost_Id(userId,postId);

	    boolean disliked = postDislikeRepo.existsByUser_IdAndPost_Id(userId,postId);

	    return new PostLikeResponseDTO(
	            postLikeTotal,
	            postDislikeTotal,
	            liked,
	            disliked
	    );
	}
}
