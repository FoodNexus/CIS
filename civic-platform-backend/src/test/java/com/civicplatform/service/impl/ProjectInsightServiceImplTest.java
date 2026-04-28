package com.civicplatform.service.impl;

import com.civicplatform.dto.response.AmbassadorInfluenceResponse;
import com.civicplatform.dto.response.EventResponse;
import com.civicplatform.dto.response.PostResponse;
import com.civicplatform.dto.response.ProjectInsightResponse;
import com.civicplatform.entity.Project;
import com.civicplatform.entity.User;
import com.civicplatform.repository.ProjectRepository;
import com.civicplatform.service.AmbassadorService;
import com.civicplatform.service.EventService;
import com.civicplatform.service.PostService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectInsightServiceImplTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private PostService postService;
    @Mock private EventService eventService;
    @Mock private AmbassadorService ambassadorService;

    @InjectMocks
    private ProjectInsightServiceImpl service;

    @Test
    void getProjectInsight_prefersCreatorRelatedSignalsAndResolvesAction() {
        User creator = User.builder().id(10L).userName("creatorA").build();
        Project project = Project.builder()
                .id(77L)
                .title("Green Rooftop")
                .goalAmount(BigDecimal.valueOf(1000))
                .currentAmount(BigDecimal.valueOf(250))
                .voteCount(2)
                .createdBy(creator)
                .build();
        when(projectRepository.findById(77L)).thenReturn(Optional.of(project));

        PostResponse relatedPost = PostResponse.builder().id(1L).creator("creatorA").engagementScore(2.5d).build();
        PostResponse unrelatedPost = PostResponse.builder().id(2L).creator("other").engagementScore(99.0d).build();
        when(postService.getFeedByPopularity()).thenReturn(List.of(unrelatedPost, relatedPost));

        EventResponse relatedEvent = EventResponse.builder().id(11L).organizerId(10L).popularityScore(1.2d).build();
        EventResponse unrelatedEvent = EventResponse.builder().id(12L).organizerId(99L).popularityScore(9.9d).build();
        when(eventService.getFeedByPopularity()).thenReturn(List.of(unrelatedEvent, relatedEvent));

        AmbassadorInfluenceResponse ownerInfluence = AmbassadorInfluenceResponse.builder()
                .userId(10L)
                .influenceScore(12.0d)
                .build();
        when(ambassadorService.getRankingByInfluence()).thenReturn(List.of(ownerInfluence));

        ProjectInsightResponse out = service.getProjectInsight(77L);

        assertEquals(77L, out.getProjectId());
        assertEquals(1L, out.getTopPostId());
        assertEquals(11L, out.getTopEventId());
        assertEquals(10L, out.getTopAmbassadorUserId());
        assertEquals("RUN_PROJECT_VOTING_CAMPAIGN", out.getRecommendedAction());
        assertEquals("INCUBATE", out.getWorkflowStage());
        assertNotNull(out.getWorkflowScore());
    }

    @Test
    void getProjectInsight_highSignalsMovesProjectToScale() {
        User creator = User.builder().id(20L).userName("creatorB").build();
        Project project = Project.builder()
                .id(88L)
                .title("City Food Lab")
                .goalAmount(BigDecimal.valueOf(1000))
                .currentAmount(BigDecimal.valueOf(1000))
                .voteCount(500)
                .createdBy(creator)
                .build();
        when(projectRepository.findById(88L)).thenReturn(Optional.of(project));
        when(postService.getFeedByPopularity()).thenReturn(List.of(
                PostResponse.builder().id(21L).creator("creatorB").engagementScore(30.0d).build()
        ));
        when(eventService.getFeedByPopularity()).thenReturn(List.of(
                EventResponse.builder().id(22L).organizerId(20L).popularityScore(20.0d).build()
        ));
        when(ambassadorService.getRankingByInfluence()).thenReturn(List.of(
                AmbassadorInfluenceResponse.builder().userId(20L).influenceScore(200.0d).build()
        ));

        ProjectInsightResponse out = service.getProjectInsight(88L);

        assertEquals("SCALE", out.getWorkflowStage());
    }

    @Test
    void getWorkflowInsights_sortsByWorkflowScoreDescending() {
        User creatorA = User.builder().id(30L).userName("creatorA").build();
        User creatorB = User.builder().id(31L).userName("creatorB").build();

        Project projectLow = Project.builder()
                .id(100L)
                .title("Low")
                .goalAmount(BigDecimal.valueOf(1000))
                .currentAmount(BigDecimal.valueOf(50))
                .voteCount(1)
                .createdBy(creatorA)
                .build();
        Project projectHigh = Project.builder()
                .id(101L)
                .title("High")
                .goalAmount(BigDecimal.valueOf(1000))
                .currentAmount(BigDecimal.valueOf(1000))
                .voteCount(400)
                .createdBy(creatorB)
                .build();

        when(projectRepository.findAll()).thenReturn(List.of(projectLow, projectHigh));
        when(projectRepository.findById(100L)).thenReturn(Optional.of(projectLow));
        when(projectRepository.findById(101L)).thenReturn(Optional.of(projectHigh));

        when(postService.getFeedByPopularity()).thenReturn(List.of(
                PostResponse.builder().id(31L).creator("creatorA").engagementScore(1.0d).build(),
                PostResponse.builder().id(32L).creator("creatorB").engagementScore(20.0d).build()
        ));
        when(eventService.getFeedByPopularity()).thenReturn(List.of(
                EventResponse.builder().id(41L).organizerId(30L).popularityScore(0.5d).build(),
                EventResponse.builder().id(42L).organizerId(31L).popularityScore(15.0d).build()
        ));
        when(ambassadorService.getRankingByInfluence()).thenReturn(List.of(
                AmbassadorInfluenceResponse.builder().userId(30L).influenceScore(1.0d).build(),
                AmbassadorInfluenceResponse.builder().userId(31L).influenceScore(120.0d).build()
        ));

        List<ProjectInsightResponse> out = service.getWorkflowInsights();

        assertEquals(2, out.size());
        assertEquals(101L, out.get(0).getProjectId());
        assertEquals(100L, out.get(1).getProjectId());
    }
}
