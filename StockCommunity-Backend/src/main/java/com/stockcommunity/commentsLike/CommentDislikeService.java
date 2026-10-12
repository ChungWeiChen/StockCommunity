package com.stockcommunity.commentsLike;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.stockcommunity.comments.Comment;
import com.stockcommunity.comments.CommentRepository;
import com.stockcommunity.commentsLike.dto.CommentLikeResponseDTO;
import com.stockcommunity.security.CurrentUserProvider;
import com.stockcommunity.users.UserRepository;
import com.stockcommunity.users.Users;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentDislikeService {

    private final CommentDislikeRepository commentDislikeRepo;
    private final CommentLikeRepository commentLikeRepo;

    private final CommentRepository commentRepo;
    private final UserRepository userRepo;

    private final CurrentUserProvider currentUserProvider;

    @Transactional
    public CommentLikeResponseDTO addDislike(Long commentId) {

        Long userId = currentUserProvider.getCurrentUserId();

        Users user = userRepo.findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND));

        Comment comment = commentRepo.findById(commentId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND));

        boolean alreadyDisliked =commentDislikeRepo.existsByUser_IdAndComment_Id(userId,commentId);

        // 原本有倒讚 → 取消倒讚
        if (alreadyDisliked) {

            commentDislikeRepo.deleteByUser_IdAndComment_Id(userId,commentId);

        } else {

            // 檢查原本有沒有按讚
            boolean alreadyLiked =commentLikeRepo.existsByUser_IdAndComment_Id(userId,commentId);

            // 原本有讚 → 取消讚
            if (alreadyLiked) {

                commentLikeRepo.deleteByUser_IdAndComment_Id(userId,commentId);
            }

            //新增讚
            CommentDislike dislike = new CommentDislike();

            dislike.setUser(user);
            dislike.setComment(comment);

            commentDislikeRepo.save(dislike);
        }

        long commentLikeNum = commentLikeRepo.countByComment_Id(commentId);

        long commentDislikeNum = commentDislikeRepo.countByComment_Id(commentId);


        boolean liked = commentLikeRepo.existsByUser_IdAndComment_Id( userId,commentId);

        boolean disliked = commentDislikeRepo.existsByUser_IdAndComment_Id(userId,commentId);

        return new CommentLikeResponseDTO(
                commentLikeNum,
                commentDislikeNum,
                liked,
                disliked
        );
    }
}