package com.stockcommunity.postLikes.dto;

import lombok.Data;

@Data
public class PostLikeResponseDTO {

    private Long postLikeTotal;

    private Long postDislikeTotal;

    private boolean liked;

    private boolean disliked;

    public PostLikeResponseDTO(
            Long postLikeTotal,
            Long postDislikeTotal,
            boolean liked,
            boolean disliked) {

        this.postLikeTotal = postLikeTotal;
        this.postDislikeTotal = postDislikeTotal;
        this.liked = liked;
        this.disliked = disliked;
    }
}