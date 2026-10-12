package com.stockcommunity.comments.dto;

import java.time.LocalDateTime;

import com.stockcommunity.comments.Comment;

import lombok.Data;

@Data
public class CommentResponseDTO {
	
	private Long id;
	
	private String commentText;
	
	private byte[] commentPhoto;
	
	private Long commentLikeNum;
	
	private boolean liked;
	
	private Long commentDislikeNum;
	
	private boolean disliked;
	
	private LocalDateTime createdAt;
	
	private LocalDateTime updatedAt;
	
	private Long postId;
	
	private Long userId;
	
	private String username;
	
	private byte[] userPhoto;
	

	
	public static CommentResponseDTO fromEntity(Comment comment) {
		
		CommentResponseDTO dto = new CommentResponseDTO();
		
		dto.setId(comment.getId());
		dto.setCommentText(comment.getCommentText());
		dto.setCommentPhoto(comment.getCommentPhoto());
		dto.setCreatedAt(comment.getCreatedAt());
		dto.setUpdatedAt(comment.getUpdatedAt());
		
		dto.setPostId(comment.getPost().getId());
		dto.setUserId(comment.getUser().getId());
		dto.setUsername(comment.getUser().getUsername());
		dto.setUserPhoto(comment.getUser().getUserPhoto());
		
		
		return dto;
	}
	
}
