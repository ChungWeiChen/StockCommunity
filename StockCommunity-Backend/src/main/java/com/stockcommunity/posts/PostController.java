package com.stockcommunity.posts;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.stockcommunity.posts.dto.MySummaryDTO;
import com.stockcommunity.posts.dto.PopularStockDTO;
import com.stockcommunity.posts.dto.PostPageResponseDTO;
import com.stockcommunity.posts.dto.PostResponseDTO;
import com.stockcommunity.users.Users;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
public class PostController {

	private final PostService postService;

	//新增貼文
	@PostMapping("/api/posts/add")
	public ResponseEntity<?> insertPost(
			@RequestParam(required = false) String postTitle,
			@RequestParam String postText,
			@RequestParam(required = false) MultipartFile photoFile,
			@RequestParam(required = false) Set<String> stockCodes)
			throws IOException {
		PostResponseDTO savedPost =
				postService.insertPost(postTitle, postText, photoFile,stockCodes);

		return ResponseEntity.ok(savedPost);
	}

	// 查詢全部貼文
	@GetMapping("/api/posts")
	public PostPageResponseDTO post(
	        @RequestParam(defaultValue = "0") int page) {

	    return postService.findAllPosts(page);
	}
	
	//查詢我的貼文
	@GetMapping("/api/posts/my")
	public PostPageResponseDTO myPost(
	        @RequestParam(defaultValue = "0") int page) {

	    return postService.findMyPosts(page);
	}
		
	//模糊搜尋使用者
	@GetMapping("/api/posts/search")
	public ResponseEntity<List<Users>> searchUsers(@RequestParam String username) {
		List<Users> users = postService.searchUsers(username);
		
		return ResponseEntity.ok(users);
	}
	

	//查詢貼文(根據username)
	@GetMapping("/api/posts/username")
	public ResponseEntity<PostPageResponseDTO> usernamePosts(
	        @RequestParam String username,
	        @RequestParam(defaultValue = "0") int page) {

	    PostPageResponseDTO posts =
	            postService.findPostsByUsername(username, page);

	    return ResponseEntity.ok(posts);
	}
	
	//查詢熱門貼文
	@GetMapping("/api/posts/hot")
	public List<PostResponseDTO> hotPosts() {

		return postService.findHotPosts();
	}
		
	// 查詢貼文(根據股票標籤)
	@GetMapping("/api/posts/stock/{stockCode}")
	public ResponseEntity<PostPageResponseDTO> stockPosts(
	        @PathVariable String stockCode,
	        @RequestParam(defaultValue = "0") int page) {

	    PostPageResponseDTO posts =
	            postService.findPostsByStockCode(stockCode, page);

	    return ResponseEntity.ok(posts);
	}

	
	//編輯貼文
	@PatchMapping("/api/posts/edit/{postId}")
	public ResponseEntity<?> editPost(@PathVariable Long postId,
						        @RequestParam(required = false) String postTitle,
						        @RequestParam(required = false) String postText,
						        @RequestParam(required = false) MultipartFile photoFile,
								@RequestParam(required = false) Set<String> stockCodes,
								@RequestParam(defaultValue = "false") boolean removePhoto,
								@RequestParam(defaultValue = "false") boolean updateStocks)
						        	throws IOException {
		
		PostResponseDTO editPost = postService.updatePost(postId, postTitle, postText,
				photoFile, stockCodes, removePhoto, updateStocks);
				
		return ResponseEntity.ok(editPost);
	}

	//刪除貼文
	@DeleteMapping("/api/posts/delete/{postId}")
	public ResponseEntity<Void> deletePost(@PathVariable Long postId) {
	    
	    postService.deletePost(postId);
	    return ResponseEntity.noContent().build();
	}

	//最熱門五筆股票標籤
	@GetMapping("/api/posts/popular-stocks")
	public ResponseEntity<List<PopularStockDTO>> getPopularStocks() {

	    List<PopularStockDTO> popularStocks =postService.getPopularStocks();

	    return ResponseEntity.ok(popularStocks);
	}
	
	//我的摘要
	@GetMapping("/api/posts/summary")
	public ResponseEntity<MySummaryDTO> getMySummary() {

	    MySummaryDTO summary = postService.getMySummary();

	    return ResponseEntity.ok(summary);
	}
	
}