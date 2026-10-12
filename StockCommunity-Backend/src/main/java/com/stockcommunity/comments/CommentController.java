package com.stockcommunity.comments;

import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.stockcommunity.comments.dto.CommentResponseDTO;

import lombok.RequiredArgsConstructor;



@RequiredArgsConstructor
@RestController
public class CommentController {


	private final CommentService commentService;


	//新增留言
	@PostMapping("/api/posts/{postId}/comments")
	public ResponseEntity<?> insertCommentByPostId(
			@PathVariable Long postId,
			@RequestParam String commentText,
			@RequestParam(required = false) MultipartFile commentPhoto)
					throws IOException{

		commentService.insertCommentByPostId(postId, commentText, commentPhoto);

		return ResponseEntity.ok().build();

	}


//	//查詢留言(依貼文postId)
//	@GetMapping("/api/posts/{postId}/comments")
//	public List<CommentResponseDTO> getCommentsByPostId(@PathVariable Long postId) {
//
//		return commentService.getCommentsByPostId(postId);
//
//	}

	
	//編輯留言
	@PatchMapping("/api/comments/edit/{commentId}")
	public ResponseEntity<?> editComment(@PathVariable Long commentId,
								@RequestParam String commentText,
								@RequestParam(required = false)MultipartFile commentPhoto,
								@RequestParam(defaultValue = "false") boolean removePhoto) 
										throws IOException {
		
		CommentResponseDTO editComment = commentService.updateComment(commentId, commentText,
				commentPhoto,removePhoto);
		
		
		return ResponseEntity.ok(editComment);
	}
	
	//刪除留言
	@DeleteMapping("/api/comments/delete/{commentId}")
	public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
		
		commentService.deleteComment(commentId);
		return ResponseEntity.noContent().build();
		
	}
	
}
