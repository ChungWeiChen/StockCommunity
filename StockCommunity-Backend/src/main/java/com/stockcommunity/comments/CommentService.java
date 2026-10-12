package com.stockcommunity.comments;

import java.io.IOException;

import java.util.List;
import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.stockcommunity.comments.dto.CommentResponseDTO;
import com.stockcommunity.commentsLike.CommentLikeRepository;
import com.stockcommunity.posts.Post;
import com.stockcommunity.posts.PostRepository;
import com.stockcommunity.security.CurrentUserProvider;
import com.stockcommunity.users.UserService;
import com.stockcommunity.users.Users;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class CommentService {

	private final CommentRepository commentRepo;
	private final CommentLikeRepository commentLikeRepo;
	private final PostRepository postRepo;

	private final UserService userService;
	private final CurrentUserProvider currentUserProvider;


	//新增留言
	public Comment insertCommentByPostId(Long postId, String commentText, MultipartFile commentPhoto)
			throws IOException{

		Long userId = currentUserProvider.getCurrentUserId();
		Users user = userService.getProfile(userId);
		Post post = postRepo.findById(postId)
				.orElseThrow(() ->new ResponseStatusException(HttpStatus.NOT_FOUND));

		Comment addComment = new Comment();
		addComment.setCommentText(commentText);
		addComment.setUser(user);
		addComment.setPost(post);

		if(commentPhoto != null && !commentPhoto.isEmpty()) {
			addComment.setCommentPhoto(commentPhoto.getBytes());
		}
		
		return commentRepo.save(addComment);

	}

	//查詢留言(依貼文postId)
	public List<CommentResponseDTO> getCommentsByPostId(Long postId) {

		postRepo.findById(postId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		List<Comment> comments = commentRepo.findByPostIdOrderByCreatedAtDesc(postId);

		return comments.stream()
				.map(CommentResponseDTO::fromEntity)
				.toList();
	}


	//編輯留言
	public CommentResponseDTO updateComment(Long commentId, String commentText,
			MultipartFile commentPhoto, boolean removePhoto)
			throws IOException{
		Long userId = currentUserProvider.getCurrentUserId();

		Comment comment = commentRepo.findById(commentId)
				.orElseThrow(() ->new ResponseStatusException(HttpStatus.NOT_FOUND));


		if(!comment.getUser().getId().equals(userId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}

		if(commentText != null && !commentText.isBlank()) {
			comment.setCommentText(commentText);
		}

		if (removePhoto) {
		    comment.setCommentPhoto(null);
		} else if (commentPhoto != null && !commentPhoto.isEmpty()) {
			comment.setCommentPhoto(commentPhoto.getBytes());
		}

		comment.setUpdatedAt(LocalDateTime.now());

		Comment editComment = commentRepo.save(comment);

		return CommentResponseDTO.fromEntity(editComment);
	}

	//刪除留言
	@Transactional
	public void deleteComment(Long commentId) {
		Long userId = currentUserProvider.getCurrentUserId();

		Comment comment = commentRepo.findById(commentId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		if(!comment.getUser().getId().equals(userId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		commentLikeRepo.deleteByComment_Id(commentId);

		commentRepo.delete(comment);
	}

}