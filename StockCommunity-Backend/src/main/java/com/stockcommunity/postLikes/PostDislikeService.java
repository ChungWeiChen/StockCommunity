package com.stockcommunity.postLikes;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.stockcommunity.postLikes.dto.PostLikeResponseDTO;
import com.stockcommunity.posts.Post;
import com.stockcommunity.posts.PostRepository;
import com.stockcommunity.security.CurrentUserProvider;
import com.stockcommunity.users.UserRepository;
import com.stockcommunity.users.Users;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostDislikeService {

    private final PostDislikeRepository postDislikeRepo;
    private final PostLikeRepository postLikeRepo;

    private final PostRepository postRepo;
    private final UserRepository userRepo;

    private final CurrentUserProvider currentUserProvider;

    @Transactional
    public PostLikeResponseDTO addDislike(Long postId) {

        Long userId = currentUserProvider.getCurrentUserId();

        Users user = userRepo.findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND));

        Post post = postRepo.findById(postId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND));

        boolean alreadyDisliked =postDislikeRepo.existsByUser_IdAndPost_Id(userId,postId);

        if (alreadyDisliked) {

            // 已經倒讚，再按一次取消倒讚
            postDislikeRepo.deleteByUser_IdAndPost_Id(userId, postId);

        } else {

            // 原本有按讚，先取消按讚
            boolean alreadyLiked =postLikeRepo.existsByUser_IdAndPost_Id(userId,postId);

            if (alreadyLiked) {
                postLikeRepo.deleteByUser_IdAndPost_Id(userId,postId);
            }

            // 新增倒讚
            PostDislike dislike = new PostDislike();

            dislike.setUser(user);
            dislike.setPost(post);

            postDislikeRepo.save(dislike);
        }

        long postLikeTotal = postLikeRepo.countByPost_Id(postId);

        long postDislikeTotal = postDislikeRepo.countByPost_Id(postId);

        boolean liked = postLikeRepo.existsByUser_IdAndPost_Id(userId,postId);

        boolean disliked = postDislikeRepo.existsByUser_IdAndPost_Id(userId,postId);

        return new PostLikeResponseDTO(
                postLikeTotal,
                postDislikeTotal,
                liked,
                disliked
        );
    }
}
