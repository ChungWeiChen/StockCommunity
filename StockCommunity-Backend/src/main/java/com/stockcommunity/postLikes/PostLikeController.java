package com.stockcommunity.postLikes;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stockcommunity.postLikes.dto.PostLikeResponseDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PostLikeController {

    private final PostLikeService postLikeService;

    @PostMapping("/api/posts/{postId}/likes")
    public ResponseEntity<PostLikeResponseDTO> addLike(
            @PathVariable Long postId) {

        PostLikeResponseDTO response =
                postLikeService.addLike(postId);

        return ResponseEntity.ok(response);
    }
}