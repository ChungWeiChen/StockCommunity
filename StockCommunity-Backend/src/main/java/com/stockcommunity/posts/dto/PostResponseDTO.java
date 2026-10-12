package com.stockcommunity.posts.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.stockcommunity.comments.dto.CommentResponseDTO;
import com.stockcommunity.posts.Post;

import lombok.Data;

@Data
public class PostResponseDTO {
	
	private Long id;
	
    private String postTitle;
    
    private String postText;
    
    private byte[] photoFile;

    private Long postLikeTotal;
    
    private boolean liked;
    
    private Long postDislikeTotal;

    private boolean disliked;
    
    private Long commentTotal;

    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
	private Long userId;
	
	private String username;
	
	private byte[] userPhoto;

    private List<CommentResponseDTO> comments;
    
    private List<StockTag> stocks;
    
    @Data
    public static class StockTag {

        private String stockCode;
        private String simpleName;

        public StockTag(String stockCode, String simpleName) {
            this.stockCode = stockCode;
            this.simpleName = simpleName;
        }
    }
    
    
    
    public static PostResponseDTO fromEntity(Post post) {
    	
	    	PostResponseDTO dto = new PostResponseDTO();
	    	
	    	dto.setId(post.getId());
	    	dto.setPostTitle(post.getPostTitle());
	    	dto.setPostText(post.getPostText());
		dto.setPhotoFile(post.getPhotoFile());
		dto.setCreatedAt(post.getCreatedAt());
		dto.setUpdatedAt(post.getUpdatedAt());
		    
		dto.setUserId(post.getUser().getId());
		dto.setUsername(post.getUser().getUsername());
		dto.setUserPhoto(post.getUser().getUserPhoto());

		dto.setStocks(
	            post.getStocks()
	                .stream()
	                .map(stock -> new StockTag(
	                    stock.getStockCode(),
	                    stock.getSimpleName()
	                ))
	                .toList()
	        );
    
    return dto;
    }
}
