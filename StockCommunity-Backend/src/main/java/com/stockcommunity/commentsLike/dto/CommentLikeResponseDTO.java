package com.stockcommunity.commentsLike.dto;

import lombok.Data;

@Data
public class CommentLikeResponseDTO {

    private Long commentLikeNum;

    private boolean liked;
    
    private Long commentDislikeNum;
    
    private boolean disliked;
    
    public CommentLikeResponseDTO(
            Long commentLikeNum,
            Long commentDislikeNum,
            boolean liked,
            boolean disliked) {

        this.commentLikeNum = commentLikeNum;
        this.commentDislikeNum = commentDislikeNum;
        this.liked = liked;
        this.disliked = disliked;
    }
}