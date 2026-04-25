package com.civicplatform.service.impl;

import com.civicplatform.dto.response.PostResponse;
import com.civicplatform.entity.Post;
import com.civicplatform.mapper.PostMapper;
import com.civicplatform.repository.CampaignRepository;
import com.civicplatform.repository.CommentRepository;
import com.civicplatform.repository.PostAttachmentRepository;
import com.civicplatform.repository.PostRepository;
import com.civicplatform.repository.UserRepository;
import com.civicplatform.service.NotificationService;
import com.civicplatform.service.PostMediaStorageService;
import com.civicplatform.service.ScoringProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock private PostRepository postRepository;
    @Mock private UserRepository userRepository;
    @Mock private CampaignRepository campaignRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private PostMapper postMapper;
    @Mock private PostAttachmentRepository postAttachmentRepository;
    @Mock private PostMediaStorageService postMediaStorageService;
    @Mock private NotificationService notificationService;
    @Mock private ScoringProperties scoringProperties;

    @InjectMocks
    private PostServiceImpl service;

    @Test
    void feedPopularity_prefersRecentPostWhenInteractionsAreEqual() {
        Post older = Post.builder().id(1L).likesCount(5).createdAt(LocalDateTime.now().minusHours(10)).build();
        Post newer = Post.builder().id(2L).likesCount(5).createdAt(LocalDateTime.now().minusHours(1)).build();
        when(postRepository.findAll()).thenReturn(List.of(older, newer));
        when(commentRepository.countByPostId(1L)).thenReturn(2L);
        when(commentRepository.countByPostId(2L)).thenReturn(2L);
        when(postMapper.toSummaryResponse(any(Post.class))).thenAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            return PostResponse.builder().id(post.getId()).build();
        });
        when(postAttachmentRepository.findByPost_IdOrderByIdAsc(any(Long.class))).thenReturn(List.of());
        ScoringProperties.PostEngagement cfg = new ScoringProperties.PostEngagement();
        cfg.setWeightLikes(2.0);
        cfg.setWeightComments(3.0);
        cfg.setWeightShares(4.0);
        when(scoringProperties.getPostEngagement()).thenReturn(cfg);

        List<PostResponse> out = service.getFeedByPopularity();

        assertEquals(2L, out.get(0).getId());
        assertEquals(1L, out.get(1).getId());
        org.junit.jupiter.api.Assertions.assertNotNull(out.get(0).getEngagementScore());
    }
}
