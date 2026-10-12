package com.stockcommunity.commentsLike;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stockcommunity.commentsLike.dto.CommentLikeResponseDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class CommentDislikeController {

    private final CommentDislikeService commentDislikeService;

    @PostMapping("/api/comments/{commentId}/dislikes")
    public ResponseEntity<CommentLikeResponseDTO> addDislike(
            @PathVariable Long commentId) {

        CommentLikeResponseDTO response =
                commentDislikeService.addDislike(commentId);

        return ResponseEntity.ok(response);
    }
}