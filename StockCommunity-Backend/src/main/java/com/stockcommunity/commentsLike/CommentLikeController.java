package com.stockcommunity.commentsLike;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stockcommunity.commentsLike.dto.CommentLikeResponseDTO;

import lombok.RequiredArgsConstructor;


@RestController
@RequiredArgsConstructor
public class CommentLikeController {
	
	private final CommentLikeService commentLikeService;
	
	@PostMapping("/api/comments/{commentId}/likes")
	public ResponseEntity<CommentLikeResponseDTO> addLike(
            @PathVariable Long commentId) {

        CommentLikeResponseDTO response =
        		commentLikeService.addLike(commentId);

        return ResponseEntity.ok(response);
    }

}
