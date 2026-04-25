package com.civicplatform.service.impl;

import com.civicplatform.dto.response.AmbassadorInfluenceResponse;
import com.civicplatform.entity.Post;
import com.civicplatform.entity.User;
import com.civicplatform.enums.Badge;
import com.civicplatform.enums.UserType;
import com.civicplatform.repository.CommentRepository;
import com.civicplatform.repository.EventParticipantRepository;
import com.civicplatform.repository.EventRepository;
import com.civicplatform.repository.PostRepository;
import com.civicplatform.repository.UserRepository;
import com.civicplatform.service.ScoringProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AmbassadorServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private EventRepository eventRepository;
    @Mock private EventParticipantRepository eventParticipantRepository;
    @Mock private PostRepository postRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private ScoringProperties scoringProperties;

    @InjectMocks
    private AmbassadorServiceImpl service;

    @Test
    void influenceRanking_handlesMissingBadgeAndNoHistory() {
        User ambassador = User.builder()
                .id(7L)
                .userName("ambassadorA")
                .userType(UserType.AMBASSADOR)
                .badge(null)
                .build();
        Post post = Post.builder().id(88L).likesCount(0).build();

        when(userRepository.findByUserType(UserType.AMBASSADOR)).thenReturn(List.of(ambassador));
        when(eventRepository.findByOrganizerId(7L)).thenReturn(List.of());
        when(postRepository.findByCreator("ambassadorA")).thenReturn(List.of(post));
        when(commentRepository.countByPostId(88L)).thenReturn(0L);
        when(eventParticipantRepository.countNonCancelledByOrganizerId(7L)).thenReturn(0L);
        ScoringProperties.AmbassadorInfluence cfg = new ScoringProperties.AmbassadorInfluence();
        when(scoringProperties.getAmbassadorInfluence()).thenReturn(cfg);

        List<AmbassadorInfluenceResponse> out = service.getRankingByInfluence();

        assertEquals(1, out.size());
        assertEquals(7L, out.get(0).getUserId());
        assertEquals(3.0d, out.get(0).getInfluenceScore());
    }

    @Test
    void influenceRanking_appliesBadgeBonusOrdering() {
        User ambassador1 = User.builder().id(1L).userName("a1").userType(UserType.AMBASSADOR).badge(Badge.BRONZE).build();
        User ambassador2 = User.builder().id(2L).userName("a2").userType(UserType.AMBASSADOR).badge(Badge.GOLD).build();
        when(userRepository.findByUserType(UserType.AMBASSADOR)).thenReturn(List.of(ambassador1, ambassador2));
        when(eventRepository.findByOrganizerId(1L)).thenReturn(List.of());
        when(eventRepository.findByOrganizerId(2L)).thenReturn(List.of());
        when(postRepository.findByCreator("a1")).thenReturn(List.of());
        when(postRepository.findByCreator("a2")).thenReturn(List.of());
        when(eventParticipantRepository.countNonCancelledByOrganizerId(1L)).thenReturn(0L);
        when(eventParticipantRepository.countNonCancelledByOrganizerId(2L)).thenReturn(0L);
        ScoringProperties.AmbassadorInfluence cfg = new ScoringProperties.AmbassadorInfluence();
        cfg.setBadgeLocal(10.0);
        cfg.setBadgeRegional(25.0);
        cfg.setBadgeNational(50.0);
        when(scoringProperties.getAmbassadorInfluence()).thenReturn(cfg);

        List<AmbassadorInfluenceResponse> out = service.getRankingByInfluence();

        assertEquals(2L, out.get(0).getUserId());
        assertEquals(1L, out.get(1).getUserId());
    }
}
