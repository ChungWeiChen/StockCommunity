package com.stockcommunity.posts;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.stockcommunity.common.SystemAccounts;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.stockcommunity.comments.Comment;
import com.stockcommunity.comments.CommentRepository;
import com.stockcommunity.comments.dto.CommentResponseDTO;
import com.stockcommunity.commentsLike.CommentDislikeRepository;
import com.stockcommunity.commentsLike.CommentLikeRepository;
import com.stockcommunity.postLikes.PostDislikeRepository;
import com.stockcommunity.postLikes.PostLikeRepository;
import com.stockcommunity.posts.dto.MySummaryDTO;
import com.stockcommunity.posts.dto.PopularStockDTO;
import com.stockcommunity.posts.dto.PostPageResponseDTO;
import com.stockcommunity.posts.dto.PostResponseDTO;
import com.stockcommunity.posts.dto.StockSummaryDTO;
import com.stockcommunity.security.CurrentUserProvider;
import com.stockcommunity.stocks.stock.Stock;
import com.stockcommunity.stocks.stock.StockRepository;
import com.stockcommunity.users.UserRepository;
import com.stockcommunity.users.UserService;
import com.stockcommunity.users.Users;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class PostService {

	private final PostRepository postRepo;
	private final PostLikeRepository postLikeRepo;
	private final PostDislikeRepository postDislikeRepo;

	private final CommentRepository commentRepo;
	private final CommentLikeRepository commentLikeRepo;
	private final CommentDislikeRepository commentDislikeRepo;
	
	private final StockRepository stockRepo;
	
	private final UserRepository userRepo;
	private final UserService userService;
	private final CurrentUserProvider currentUserProvider;
	
	// 新增貼文
	public PostResponseDTO insertPost(
	        String postTitle,
	        String postText,
	        MultipartFile photoFile,
	        Set<String> stockCodes)
	        throws IOException {

	    Long userId = currentUserProvider.getCurrentUserId();

	    Users user = userService.getProfile(userId);

	    Post addPost = new Post();

	    addPost.setPostTitle(postTitle);
	    addPost.setPostText(postText);
	    addPost.setUser(user);

	    if (stockCodes != null && !stockCodes.isEmpty()) {

	        Set<Stock> stocks = new HashSet<>();

	        for (String stockCode : stockCodes) {
	            Stock stock = stockRepo.findById(stockCode)
	                    .orElseThrow(() ->
	                        new ResponseStatusException(HttpStatus.NOT_FOUND));
	            
	            stocks.add(stock);
	        }
	        
	        addPost.setStocks(stocks);
	    }

	    if (photoFile != null && !photoFile.isEmpty()) {
	        addPost.setPhotoFile(photoFile.getBytes());
	    }

	    Post savedPost = postRepo.save(addPost);

	    return PostResponseDTO.fromEntity(savedPost);
	}
	
	// 查詢貼文加留言
	public PostPageResponseDTO findAllPosts(int page){

	    long totalPosts = Math.min(postRepo.count(), 50);

	    int totalPages = (int) Math.ceil(totalPosts / 5.0);

	    if (page < 0 || page >= 10) {
	        return new PostPageResponseDTO(
	                List.of(),
	                page,
	                totalPages
	        );
	    }

	    List<Post> posts = postRepo.findAllByOrderByCreatedAtDesc(PageRequest.of(page, 5));

	    Long userId = null;

	    try {
	        userId = currentUserProvider.getCurrentUserId();
	    } catch (Exception e) {
	    }

	    final Long currentUserId = userId;

	    List<PostResponseDTO> postDTOs =
	            posts.stream()
	            .map(post -> {

	                PostResponseDTO dto =PostResponseDTO.fromEntity(post);

	                long postLikeTotal =postLikeRepo.countByPost_Id(post.getId());

	                dto.setPostLikeTotal(postLikeTotal);

	                long postDislikeTotal =postDislikeRepo.countByPost_Id(post.getId());

	                dto.setPostDislikeTotal(postDislikeTotal);

	                List<Comment> comments =commentRepo.findByPostIdOrderByCreatedAtDesc(post.getId());

	                dto.setCommentTotal((long) comments.size());

	                List<CommentResponseDTO> commentDTOs =
	                        comments.stream()
	                        .map(comment -> {

	                            CommentResponseDTO commentDTO =CommentResponseDTO.fromEntity(comment);

	                            long commentLikeNum =commentLikeRepo.countByComment_Id(comment.getId());

	                            commentDTO.setCommentLikeNum(commentLikeNum);

	                            long commentDislikeNum =commentDislikeRepo.countByComment_Id(comment.getId());

	                            commentDTO.setCommentDislikeNum(commentDislikeNum);

	                            if (currentUserId != null) {

	                                boolean commentLiked =
	                                        commentLikeRepo
	                                        .existsByUser_IdAndComment_Id(
	                                                currentUserId,
	                                                comment.getId()
	                                        );

	                                commentDTO.setLiked(commentLiked);

	                                boolean commentDisliked =
	                                        commentDislikeRepo
	                                        .existsByUser_IdAndComment_Id(
	                                                currentUserId,
	                                                comment.getId()
	                                        );

	                                commentDTO.setDisliked(commentDisliked);

	                            } else {

	                                commentDTO.setLiked(false);
	                                commentDTO.setDisliked(false);
	                            }

	                            return commentDTO;
	                        })
	                        .toList();

	                dto.setComments(commentDTOs);

	                if (currentUserId != null) {

	                    boolean liked =
	                            postLikeRepo.existsByUser_IdAndPost_Id(
	                                    currentUserId,
	                                    post.getId()
	                            );

	                    dto.setLiked(liked);

	                    boolean disliked =
	                            postDislikeRepo.existsByUser_IdAndPost_Id(
	                                    currentUserId,
	                                    post.getId()
	                            );

	                    dto.setDisliked(disliked);

	                } else {

	                    dto.setLiked(false);
	                    dto.setDisliked(false);
	                }

	                return dto;
	            })
	            .toList();

	    return new PostPageResponseDTO(
	            postDTOs,
	            page,
	            totalPages
	    );
	}
	
	// 查詢我的貼文
	public PostPageResponseDTO findMyPosts(int page) {

	    Long userId = currentUserProvider.getCurrentUserId();

	    // 計算我的貼文總數，最多只計算 50 篇
	    long totalPosts =Math.min( postRepo.countByUser_Id(userId),50);

	    // 每頁 5 篇，計算總頁數
	    int totalPages =(int) Math.ceil(totalPosts / 5.0);

	    // 最多只能查 10 頁
	    if (page < 0 || page >= 10) {
	        return new PostPageResponseDTO(
	                List.of(),
	                page,
	                totalPages
	        );
	    }

	    // 查詢這一頁的 5 篇貼文
	    List<Post> posts =postRepo.findAllByUserIdOrderByCreatedAtDesc(userId,PageRequest.of(page, 5));

	    List<PostResponseDTO> postDTOs =
	            posts.stream()
	            .map(post -> {

	                PostResponseDTO dto =
	                        PostResponseDTO.fromEntity(post);

	                long postLikeTotal =
	                        postLikeRepo.countByPost_Id(post.getId());

	                dto.setPostLikeTotal(postLikeTotal);

	                long postDislikeTotal =
	                        postDislikeRepo.countByPost_Id(post.getId());

	                dto.setPostDislikeTotal(postDislikeTotal);

	                List<Comment> comments =
	                        commentRepo.findByPostIdOrderByCreatedAtDesc(
	                                post.getId()
	                        );

	                dto.setCommentTotal((long) comments.size());

	                List<CommentResponseDTO> commentDTOs =
	                        comments.stream()
	                        .map(comment -> {

	                            CommentResponseDTO commentDTO =
	                                    CommentResponseDTO.fromEntity(comment);

	                            long commentLikeNum =
	                                    commentLikeRepo.countByComment_Id(
	                                            comment.getId()
	                                    );

	                            commentDTO.setCommentLikeNum(commentLikeNum);

	                            long commentDislikeNum =
	                                    commentDislikeRepo.countByComment_Id(
	                                            comment.getId()
	                                    );

	                            commentDTO.setCommentDislikeNum(
	                                    commentDislikeNum
	                            );

	                            boolean commentLiked =
	                                    commentLikeRepo
	                                    .existsByUser_IdAndComment_Id(
	                                            userId,
	                                            comment.getId()
	                                    );

	                            commentDTO.setLiked(commentLiked);

	                            boolean commentDisliked =
	                                    commentDislikeRepo
	                                    .existsByUser_IdAndComment_Id(
	                                            userId,
	                                            comment.getId()
	                                    );

	                            commentDTO.setDisliked(commentDisliked);

	                            return commentDTO;
	                        })
	                        .toList();

	                dto.setComments(commentDTOs);

	                boolean liked =
	                        postLikeRepo.existsByUser_IdAndPost_Id(
	                                userId,
	                                post.getId()
	                        );

	                dto.setLiked(liked);

	                boolean disliked =
	                        postDislikeRepo.existsByUser_IdAndPost_Id(
	                                userId,
	                                post.getId()
	                        );

	                dto.setDisliked(disliked);

	                return dto;
	            })
	            .toList();

	    return new PostPageResponseDTO(
	            postDTOs,
	            page,
	            totalPages
	    );
	}
	
	
	//查詢貼文(根據username)
	//先找username
	public List<Users> searchUsers(String username) {
		
		return userRepo.findAllByUsername(username, SystemAccounts.RESERVED_USERNAMES);
	}
	
	//再從username找post
	public PostPageResponseDTO findPostsByUsername(String targetUsername, int page){
		long totalPosts =Math.min(postRepo.countByUser_Username(targetUsername),50);

		int totalPages = (int) Math.ceil(totalPosts / 5.0);
		
		if (page < 0 || page >= 10) {
		    return new PostPageResponseDTO(
		            List.of(),
		            page,
		            totalPages
		    );
		}
		
		
		List<Post> posts =postRepo.findAllByUser_UsernameOrderByCreatedAtDesc(targetUsername,PageRequest.of(page, 5));
		
		Long userId = null;
		
		try {
		      userId = currentUserProvider.getCurrentUserId();
		   } catch (Exception e) {
		    	
		   }
		final Long currentUserId = userId;
		
		
		List<PostResponseDTO> postDTOs = posts.stream()
	            .map(post -> {
	                
	                PostResponseDTO dto = PostResponseDTO.fromEntity(post);
	                
	                long postLikeTotal = postLikeRepo.countByPost_Id(post.getId());
	                
	                dto.setPostLikeTotal(postLikeTotal);
	                
	                long postDislikeTotal = postDislikeRepo.countByPost_Id(post.getId());
	                
	                dto.setPostDislikeTotal(postDislikeTotal);

	                List<Comment> comments = commentRepo.findByPostIdOrderByCreatedAtDesc(post.getId());
	                
	                dto.setCommentTotal((long) comments.size());
	               
	                List<CommentResponseDTO> commentDTOs =
		                		comments.stream()
		                        .map(comment -> {
		                        CommentResponseDTO commentDTO = CommentResponseDTO.fromEntity(comment);                 
		         
		                        long commentLikeNum = commentLikeRepo.countByComment_Id(comment.getId());

		                        commentDTO.setCommentLikeNum(commentLikeNum);
		                        
		                        long commentDislikeNum = commentDislikeRepo.countByComment_Id(comment.getId());

		                        commentDTO.setCommentDislikeNum(commentDislikeNum);
		                        
			                    if(currentUserId != null) {
			                    		boolean commentLiked = commentLikeRepo.existsByUser_IdAndComment_Id(currentUserId, comment.getId());
			                    		commentDTO.setLiked(commentLiked);
			                    		
			                    		boolean commentDisliked = commentDislikeRepo.existsByUser_IdAndComment_Id(currentUserId,comment.getId());
	                        		    commentDTO.setDisliked(commentDisliked);
	                        		}else{
	                        			commentDTO.setLiked(false);
	                        			commentDTO.setDisliked(false);
	                        		}
	                        		return commentDTO;
	        					})
	                        .toList();

	                dto.setComments(commentDTOs);	                
	             
	                if(currentUserId != null) {

	                    boolean liked =postLikeRepo.existsByUser_IdAndPost_Id(currentUserId,post.getId());

	                    dto.setLiked(liked);
	                    
	                    boolean disliked = postDislikeRepo.existsByUser_IdAndPost_Id(currentUserId,post.getId());

                	    dto.setDisliked(disliked);

	                } else {

	                    dto.setLiked(false);
	                    dto.setDisliked(false);
	                }
	                	
	                return dto;
	            })
	                
	            .toList();
		
		return new PostPageResponseDTO(
		        postDTOs,
		        page,
		        totalPages
		);
}
	
	// 查詢熱門貼文
	public List<PostResponseDTO> findHotPosts() {

	    // 1. 只取得近 7 天的貼文
	    LocalDateTime sevenDaysAgo =LocalDateTime.now().minusDays(7);

	    List<Post> posts =postRepo.findByCreatedAtAfter(sevenDaysAgo);

	    // 儲存每篇貼文的 HotScore
	    Map<Long, Double> hotScoreMap = new HashMap<>();

	    // 2. 計算每篇貼文的 HotScore
	    for (Post post : posts) {

	        // 按讚數
	        long postLikeTotal =postLikeRepo.countByPost_Id(post.getId());

	        // 倒讚數
	        long postDislikeTotal =postDislikeRepo.countByPost_Id(post.getId());

	        // 留言總數
	        long commentCount =commentRepo.countByPost_Id(post.getId());

	        // 不重複留言者人數
	        long uniqueCommenters =commentRepo.countUniqueCommentersByPostId(post.getId());

	        // 互動分數
	        double engagement =
	                postLikeTotal
	                + (0.5 * postDislikeTotal)
	                + (0.5 * commentCount)
	                + uniqueCommenters;

	        // 貼文發布到現在經過幾小時
	        long hoursSincePost =
	                Duration.between(
	                        post.getCreatedAt(),
	                        LocalDateTime.now()
	                ).toHours();

	        // HotScore
	        double hotScore =
	                engagement /
	                Math.pow(hoursSincePost + 12, 0.8);

	        // 記住這篇貼文的 HotScore
	        hotScoreMap.put(
	                post.getId(),
	                hotScore
	        );

	    }

	    // 3. 按照 HotScore 由高到低排序
	    posts.sort(
	            Comparator.comparingDouble(
	                    (Post post) ->
	                            hotScoreMap.get(post.getId())
	            ).reversed()
	    );


	    // 4. 排序完成後，只取前 25 篇
	    posts = posts.stream()
	            .limit(25)
	            .toList();


	    // 5. 取得目前登入的使用者
	    Long userId = null;

	    try {
	        userId =
	                currentUserProvider.getCurrentUserId();
	    } catch (Exception e) {
	    }

	    final Long currentUserId = userId;


	    // 6. 將排好順序的 Post 轉成 PostResponseDTO
	    return posts.stream()
	            .map(post -> {

	                PostResponseDTO dto =PostResponseDTO.fromEntity(post);


	                // 貼文按讚數
	                long postLikeTotal =postLikeRepo.countByPost_Id(post.getId());

	                dto.setPostLikeTotal(postLikeTotal);


	                // 貼文倒讚數
	                long postDislikeTotal =postDislikeRepo.countByPost_Id(post.getId());

	                dto.setPostDislikeTotal(postDislikeTotal);


	                // 取得這篇貼文的留言
	                List<Comment> comments =
	                        commentRepo
	                                .findByPostIdOrderByCreatedAtDesc(
	                                        post.getId()
	                                );

	                dto.setCommentTotal((long) comments.size());

	                // 7. Comment → CommentResponseDTO
	                List<CommentResponseDTO> commentDTOs =
	                        comments.stream()
	                                .map(comment -> {

	                                    CommentResponseDTO commentDTO =
	                                            CommentResponseDTO
	                                                    .fromEntity(comment);


	                                    // 留言按讚數
	                                    long commentLikeNum =
	                                            commentLikeRepo
	                                                    .countByComment_Id(
	                                                            comment.getId()
	                                                    );

	                                    commentDTO.setCommentLikeNum(
	                                            commentLikeNum
	                                    );


	                                    // 留言倒讚數
	                                    long commentDislikeNum =
	                                            commentDislikeRepo
	                                                    .countByComment_Id(
	                                                            comment.getId()
	                                                    );

	                                    commentDTO.setCommentDislikeNum(
	                                            commentDislikeNum
	                                    );


	                                    // 判斷目前使用者是否有按讚 / 倒讚
	                                    if (currentUserId != null) {

	                                        boolean commentLiked =
	                                                commentLikeRepo
	                                                        .existsByUser_IdAndComment_Id(
	                                                                currentUserId,
	                                                                comment.getId()
	                                                        );

	                                        commentDTO.setLiked(
	                                                commentLiked
	                                        );


	                                        boolean commentDisliked =
	                                                commentDislikeRepo
	                                                        .existsByUser_IdAndComment_Id(
	                                                                currentUserId,
	                                                                comment.getId()
	                                                        );

	                                        commentDTO.setDisliked(
	                                                commentDisliked
	                                        );

	                                    } else {

	                                        commentDTO.setLiked(false);
	                                        commentDTO.setDisliked(false);
	                                    }

	                                    return commentDTO;
	                                })
	                                .toList();


	                dto.setComments(commentDTOs);


	                // 8. 判斷目前使用者是否有對貼文按讚 / 倒讚
	                if (currentUserId != null) {

	                    boolean liked =
	                            postLikeRepo
	                                    .existsByUser_IdAndPost_Id(
	                                            currentUserId,
	                                            post.getId()
	                                    );

	                    dto.setLiked(liked);


	                    boolean disliked =
	                            postDislikeRepo
	                                    .existsByUser_IdAndPost_Id(
	                                            currentUserId,
	                                            post.getId()
	                                    );

	                    dto.setDisliked(disliked);

	                } else {

	                    dto.setLiked(false);
	                    dto.setDisliked(false);
	                }


	                return dto;
	            })
	            .toList();
	}
	
	// 查詢貼文（根據股票標籤）
	public PostPageResponseDTO findPostsByStockCode(String stockCode, int page) {

	    long totalPosts = Math.min(
	            postRepo.countByStocks_StockCode(stockCode),
	            50
	    );

	    int totalPages = (int) Math.ceil(totalPosts / 5.0);

	    if (page < 0 || page >= 10) {
	        return new PostPageResponseDTO(
	                List.of(),
	                page,
	                totalPages
	        );
	    }

	    List<Post> posts =
	            postRepo.findByStocks_StockCodeOrderByCreatedAtDesc(
	                    stockCode,
	                    PageRequest.of(page, 5)
	            );

	    // 取得目前登入使用者
	    Long userId = null;

	    try {
	        userId = currentUserProvider.getCurrentUserId();
	    } catch (Exception e) {
	    }

	    final Long currentUserId = userId;

	    List<PostResponseDTO> postDTOs =
	            posts.stream()
	                    .map(post -> {

	                        PostResponseDTO dto =
	                                PostResponseDTO.fromEntity(post);

	                        // 貼文按讚數
	                        long postLikeTotal =
	                                postLikeRepo.countByPost_Id(post.getId());

	                        dto.setPostLikeTotal(postLikeTotal);

	                        // 貼文倒讚數
	                        long postDislikeTotal =
	                                postDislikeRepo.countByPost_Id(post.getId());

	                        dto.setPostDislikeTotal(postDislikeTotal);

	                        // 留言
	                        List<Comment> comments =
	                                commentRepo.findByPostIdOrderByCreatedAtDesc(
	                                        post.getId()
	                                );

	                        dto.setCommentTotal((long) comments.size());

	                        List<CommentResponseDTO> commentDTOs =
	                                comments.stream()
	                                        .map(comment -> {

	                                            CommentResponseDTO commentDTO =
	                                                    CommentResponseDTO.fromEntity(comment);

	                                            // 留言按讚數
	                                            long commentLikeNum =
	                                                    commentLikeRepo.countByComment_Id(
	                                                            comment.getId()
	                                                    );

	                                            commentDTO.setCommentLikeNum(
	                                                    commentLikeNum
	                                            );

	                                            // 留言倒讚數
	                                            long commentDislikeNum =
	                                                    commentDislikeRepo.countByComment_Id(
	                                                            comment.getId()
	                                                    );

	                                            commentDTO.setCommentDislikeNum(
	                                                    commentDislikeNum
	                                            );

	                                            // 目前使用者對留言的狀態
	                                            if (currentUserId != null) {

	                                                boolean commentLiked =
	                                                        commentLikeRepo
	                                                                .existsByUser_IdAndComment_Id(
	                                                                        currentUserId,
	                                                                        comment.getId()
	                                                                );

	                                                commentDTO.setLiked(
	                                                        commentLiked
	                                                );

	                                                boolean commentDisliked =
	                                                        commentDislikeRepo
	                                                                .existsByUser_IdAndComment_Id(
	                                                                        currentUserId,
	                                                                        comment.getId()
	                                                                );

	                                                commentDTO.setDisliked(
	                                                        commentDisliked
	                                                );

	                                            } else {

	                                                commentDTO.setLiked(false);
	                                                commentDTO.setDisliked(false);
	                                            }

	                                            return commentDTO;
	                                        })
	                                        .toList();

	                        dto.setComments(commentDTOs);

	                        // 目前使用者對貼文的狀態
	                        if (currentUserId != null) {

	                            boolean liked =
	                                    postLikeRepo.existsByUser_IdAndPost_Id(
	                                            currentUserId,
	                                            post.getId()
	                                    );

	                            dto.setLiked(liked);

	                            boolean disliked =
	                                    postDislikeRepo.existsByUser_IdAndPost_Id(
	                                            currentUserId,
	                                            post.getId()
	                                    );

	                            dto.setDisliked(disliked);

	                        } else {

	                            dto.setLiked(false);
	                            dto.setDisliked(false);
	                        }

	                        return dto;
	                    })
	                    .toList();

	    return new PostPageResponseDTO(
	            postDTOs,
	            page,
	            totalPages
	    );
	}

	//編輯貼文
	public PostResponseDTO updatePost(Long postId, String postTitle, String postText,
			MultipartFile photoFile, Set<String> stockCodes, boolean removePhoto, boolean updateStocks)
			throws IOException {

		Long userId = currentUserProvider.getCurrentUserId();
		
		Post post = postRepo.findById(postId)
				.orElseThrow(() ->new ResponseStatusException(HttpStatus.NOT_FOUND));
		
		
		if(!post.getUser().getId().equals(userId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		
		if(postTitle != null && !postTitle.isBlank()) {
			post.setPostTitle(postTitle);
		}
		
		if(postText != null && !postText.isBlank()) {
			post.setPostText(postText);
		}

		if (removePhoto) {
		    post.setPhotoFile(null);
		} else if (photoFile != null && !photoFile.isEmpty()) {
		    post.setPhotoFile(photoFile.getBytes());
		}
		
		if (updateStocks) {
		    Set<Stock> stocks = new HashSet<>();
		    if (stockCodes != null) {
		        for (String stockCode : stockCodes) {
		            Stock stock = stockRepo.findById(stockCode)
		                    .orElseThrow(() ->
		                        new ResponseStatusException(
		                            HttpStatus.NOT_FOUND));
		            stocks.add(stock);
		        }
		    }
		    post.setStocks(stocks);
		}
		
		post.setUpdatedAt(LocalDateTime.now());
		
		Post editPost = postRepo.save(post);
		
		
		return PostResponseDTO.fromEntity(editPost);
	}
	
	//刪除貼文
	@Transactional
	public void deletePost(Long id) {

		Long userId = currentUserProvider.getCurrentUserId();

		Post post = postRepo.findById(id)
				.orElseThrow(() ->
						new ResponseStatusException(HttpStatus.NOT_FOUND));

		if (!post.getUser().getId().equals(userId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		
		//先刪貼文底下留言的讚
		commentLikeRepo.deleteByComment_Post_Id(id);
		// 先刪貼文底下留言的倒讚
		commentDislikeRepo.deleteByComment_Post_Id(id);
		//再刪貼文底下留言
		commentRepo.deleteByPost_Id(id);
		//刪貼文的讚
		postLikeRepo.deleteByPost_Id(id);
		//刪貼文的倒讚
		postDislikeRepo.deleteByPost_Id(id);
		//刪貼文
		postRepo.delete(post);
	}

	//查最熱門的五筆股票標籤
	public List<PopularStockDTO> getPopularStocks() {

	    List<Object[]> results =postRepo.findPopularStocks(PageRequest.of(0, 5));

	    return results.stream()
	            .map(row -> {

	                PopularStockDTO dto = new PopularStockDTO();

	                dto.setStockCode((String) row[0]);
	                dto.setSimpleName((String) row[1]);
	                dto.setPostCount((Long) row[2]);

	                return dto;
	            })
	            .toList();
	}
	
	// 查詢我的社群摘要
	public MySummaryDTO getMySummary() {

	    Long userId = currentUserProvider.getCurrentUserId();

	    Users user = userRepo.findById(userId)
	            .orElseThrow(() ->
	                    new ResponseStatusException(HttpStatus.NOT_FOUND));

	    long postCount = postRepo.countByUser_Id(userId);

	    long commentCount = commentRepo.countByUser_Id(userId);

	    long receivedLikeCount = postLikeRepo.countReceivedLikesByUserId(userId);

	    List<Object[]> results =
	            postRepo.findTopStocksByUserId(
	                    userId,
	                    PageRequest.of(0, 3)
	            );

	    List<StockSummaryDTO> topStocks =
	            results.stream()
	                    .map(row -> new StockSummaryDTO(
	                            (String) row[0],
	                            (String) row[1],
	                            (Long) row[2]
	                    ))
	                    .toList();

	    return new MySummaryDTO(
	            user.getId(),
	            user.getUsername(),
	            postCount,
	            commentCount,
	            receivedLikeCount,
	            topStocks
	    );
	}
	
	
}