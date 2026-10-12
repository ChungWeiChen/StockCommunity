package com.stockcommunity.commentsLike;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.stockcommunity.comments.Comment;
import com.stockcommunity.comments.CommentRepository;
import com.stockcommunity.commentsLike.dto.CommentLikeResponseDTO;
import com.stockcommunity.security.CurrentUserProvider;
import com.stockcommunity.users.UserRepository;
import com.stockcommunity.users.Users;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentLikeService {

	private final CommentLikeRepository commentLikeRepo;
	private final CommentDislikeRepository commentDislikeRepo;
	private final CommentRepository commentRepo;
	private final UserRepository userRepo;

	private final CurrentUserProvider currentUserProvider;
	
	@Transactional
	public CommentLikeResponseDTO addLike(Long commentId) {

	    Long userId = currentUserProvider.getCurrentUserId();

	    Users user = userRepo.findById(userId)
	            .orElseThrow(() ->
	                    new ResponseStatusException(HttpStatus.NOT_FOUND));

	    Comment comment = commentRepo.findById(commentId)
	            .orElseThrow(() ->
	                    new ResponseStatusException(HttpStatus.NOT_FOUND));

	    boolean alreadyLiked = commentLikeRepo.existsByUser_IdAndComment_Id(userId,commentId);

	    // 原本已經按讚，按了取消讚
	    if (alreadyLiked) {

	        commentLikeRepo.deleteByUser_IdAndComment_Id(userId,commentId);

	    } else {

	        // 檢查原本有沒有倒讚
	        boolean alreadyDisliked = commentDislikeRepo.existsByUser_IdAndComment_Id(userId,commentId);

	        // 有倒讚的話，先取消倒讚
	        if (alreadyDisliked) {

	            commentDislikeRepo.deleteByUser_IdAndComment_Id(userId,commentId);
	        }

	        // 再新增 Like
	        CommentLike like = new CommentLike();

	        like.setUser(user);
	        like.setComment(comment);

	        commentLikeRepo.save(like);
	    }

	    long commentLikeNum = commentLikeRepo.countByComment_Id(commentId);

	    long commentDislikeNum = commentDislikeRepo.countByComment_Id(commentId);

	    boolean liked = commentLikeRepo.existsByUser_IdAndComment_Id(userId,commentId);

	    boolean disliked =commentDislikeRepo.existsByUser_IdAndComment_Id(userId,commentId);

	    return new CommentLikeResponseDTO(
	            commentLikeNum,
	            commentDislikeNum,
	            liked,
	            disliked
	    );
	}
		

}
