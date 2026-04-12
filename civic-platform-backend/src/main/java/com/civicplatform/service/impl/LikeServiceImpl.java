package com.civicplatform.service.impl;

import com.civicplatform.entity.Like;
import com.civicplatform.entity.Post;
import com.civicplatform.entity.User;
import com.civicplatform.repository.LikeRepository;
import com.civicplatform.repository.PostRepository;
import com.civicplatform.repository.UserRepository;
import com.civicplatform.enums.InteractionAction;
import com.civicplatform.enums.InteractionEntityType;
import com.civicplatform.service.LikeService;
import com.civicplatform.service.UserInteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final LikeRepository likeRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final UserInteractionService userInteractionService;

    @Override
    @Transactional
    public void likePost(Long postId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        // Check if already liked
        if (likeRepository.findByOwnerIdAndPostId(userId, postId).isPresent()) {
            throw new RuntimeException("User has already liked this post");
        }

        Like like = Like.builder()
                .owner(user)
                .post(post)
                .build();

        likeRepository.save(like);

        // Update post likes count
        post.setLikesCount(post.getLikesCount() + 1);
        postRepository.save(post);

        userInteractionService.record(userId, InteractionEntityType.POST, postId, InteractionAction.LIKE);
    }

    @Override
    @Transactional
    public void unlikePost(Long postId, Long userId) {
        Like like = likeRepository.findByOwnerIdAndPostId(userId, postId)
                .orElseThrow(() -> new RuntimeException("User has not liked this post"));

        likeRepository.delete(like);

        // Update post likes count
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));
        
        if (post.getLikesCount() > 0) {
            post.setLikesCount(post.getLikesCount() - 1);
            postRepository.save(post);
        }
    }

    @Override
    public boolean isPostLikedByUser(Long postId, Long userId) {
        return likeRepository.findByOwnerIdAndPostId(userId, postId).isPresent();
    }

    @Override
    public long getLikesCount(Long postId) {
        return likeRepository.countByPostId(postId);
    }
}
