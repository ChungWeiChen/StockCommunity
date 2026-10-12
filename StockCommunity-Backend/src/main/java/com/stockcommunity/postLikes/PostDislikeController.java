package com.stockcommunity.postLikes;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stockcommunity.postLikes.dto.PostLikeResponseDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PostDislikeController {

    private final PostDislikeService postDislikeService;

    @PostMapping("/api/posts/{postId}/dislikes")
    public ResponseEntity<PostLikeResponseDTO> addDislike(
            @PathVariable Long postId) {

        PostLikeResponseDTO response =
                postDislikeService.addDislike(postId);

        return ResponseEntity.ok(response);
    }
}