package com.civicplatform.service.impl;

import com.civicplatform.dto.response.AmbassadorInfluenceResponse;
import com.civicplatform.dto.response.EventResponse;
import com.civicplatform.dto.response.PostResponse;
import com.civicplatform.dto.response.ProjectInsightResponse;
import com.civicplatform.entity.Project;
import com.civicplatform.repository.ProjectRepository;
import com.civicplatform.service.AmbassadorService;
import com.civicplatform.service.EventService;
import com.civicplatform.service.PostService;
import com.civicplatform.service.ProjectInsightService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProjectInsightServiceImpl implements ProjectInsightService {

    private final ProjectRepository projectRepository;
    private final PostService postService;
    private final EventService eventService;
    private final AmbassadorService ambassadorService;

    @Override
    public ProjectInsightResponse getProjectInsight(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));

        String creatorUserName = project.getCreatedBy() == null ? null : project.getCreatedBy().getUserName();
        Long creatorUserId = project.getCreatedBy() == null ? null : project.getCreatedBy().getId();

        List<PostResponse> postFeed = postService.getFeedByPopularity();
        List<EventResponse> eventFeed = eventService.getFeedByPopularity();
        List<AmbassadorInfluenceResponse> ambassadorRanking = ambassadorService.getRankingByInfluence();

        Optional<PostResponse> topRelatedPost = postFeed.stream()
                .filter(p -> creatorUserName != null && creatorUserName.equalsIgnoreCase(p.getCreator()))
                .max(Comparator.comparing(this::safePostScore));
        Optional<EventResponse> topRelatedEvent = eventFeed.stream()
                .filter(e -> creatorUserId != null && Objects.equals(creatorUserId, e.getOrganizerId()))
                .max(Comparator.comparing(this::safeEventScore));
        Optional<AmbassadorInfluenceResponse> ownerInfluence = ambassadorRanking.stream()
                .filter(a -> creatorUserId != null && Objects.equals(creatorUserId, a.getUserId()))
                .findFirst();

        double fundingComponent = computeFundingComponent(project);
        double voteComponent = normalizePositive(project.getVoteCount() == null ? 0.0d : project.getVoteCount().doubleValue(), 20.0d);
        double postPopularityComponent = normalizePositive(
                topRelatedPost.map(this::safePostScore).orElse(0.0d), 5.0d
        );
        double eventPopularityComponent = normalizePositive(
                topRelatedEvent.map(this::safeEventScore).orElse(0.0d), 2.0d
        );
        double ambassadorInfluenceComponent = normalizePositive(
                ownerInfluence.map(this::safeInfluenceScore).orElse(0.0d), 25.0d
        );

        double workflowScore = (fundingComponent * 0.40d)
                + (voteComponent * 0.20d)
                + (postPopularityComponent * 0.15d)
                + (eventPopularityComponent * 0.15d)
                + (ambassadorInfluenceComponent * 0.10d);

        String workflowStage = resolveStage(workflowScore);
        String recommendedAction = resolveRecommendedAction(
                fundingComponent, voteComponent, postPopularityComponent, eventPopularityComponent, ambassadorInfluenceComponent
        );

        return ProjectInsightResponse.builder()
                .projectId(project.getId())
                .projectTitle(project.getTitle())
                .workflowScore(workflowScore)
                .workflowStage(workflowStage)
                .recommendedAction(recommendedAction)
                .fundingComponent(fundingComponent)
                .voteComponent(voteComponent)
                .postPopularityComponent(postPopularityComponent)
                .eventPopularityComponent(eventPopularityComponent)
                .ambassadorInfluenceComponent(ambassadorInfluenceComponent)
                .topPostId(topRelatedPost.map(PostResponse::getId).orElse(null))
                .topEventId(topRelatedEvent.map(EventResponse::getId).orElse(null))
                .topAmbassadorUserId(ownerInfluence.map(AmbassadorInfluenceResponse::getUserId).orElse(null))
                .build();
    }

    @Override
    public List<ProjectInsightResponse> getWorkflowInsights() {
        return projectRepository.findAll().stream()
                .map(Project::getId)
                .filter(Objects::nonNull)
                .map(this::getProjectInsight)
                .sorted((left, right) -> {
                    int byScore = Double.compare(
                            right.getWorkflowScore() == null ? 0.0d : right.getWorkflowScore(),
                            left.getWorkflowScore() == null ? 0.0d : left.getWorkflowScore()
                    );
                    if (byScore != 0) {
                        return byScore;
                    }
                    Long leftId = left.getProjectId() == null ? Long.MAX_VALUE : left.getProjectId();
                    Long rightId = right.getProjectId() == null ? Long.MAX_VALUE : right.getProjectId();
                    return leftId.compareTo(rightId);
                })
                .toList();
    }

    private double computeFundingComponent(Project project) {
        if (project.getGoalAmount() == null || project.getGoalAmount().signum() <= 0) {
            return 0.0d;
        }
        double goal = project.getGoalAmount().doubleValue();
        double current = project.getCurrentAmount() == null ? 0.0d : project.getCurrentAmount().doubleValue();
        double raw = current / goal;
        return clamp01(raw);
    }

    private double normalizePositive(double value, double scale) {
        if (value <= 0.0d) {
            return 0.0d;
        }
        double safeScale = scale <= 0.0d ? 1.0d : scale;
        return value / (value + safeScale);
    }

    private double clamp01(double value) {
        if (value < 0.0d) {
            return 0.0d;
        }
        return Math.min(value, 1.0d);
    }

    private String resolveStage(double workflowScore) {
        if (workflowScore >= 0.75d) {
            return "SCALE";
        }
        if (workflowScore >= 0.45d) {
            return "ACTIVATE";
        }
        return "INCUBATE";
    }

    private String resolveRecommendedAction(double funding, double votes, double posts, double events, double ambassador) {
        double min = Math.min(funding, Math.min(votes, Math.min(posts, Math.min(events, ambassador))));
        if (Double.compare(min, funding) == 0) {
            return "PRIORITIZE_PROJECT_FUNDING_DRIVE";
        }
        if (Double.compare(min, votes) == 0) {
            return "RUN_PROJECT_VOTING_CAMPAIGN";
        }
        if (Double.compare(min, posts) == 0) {
            return "BOOST_PROJECT_COMMUNICATION_POSTS";
        }
        if (Double.compare(min, events) == 0) {
            return "CREATE_EVENT_AROUND_PROJECT";
        }
        return "ASSIGN_AMBASSADOR_PROMOTION_SUPPORT";
    }

    private double safePostScore(PostResponse post) {
        return post == null || post.getEngagementScore() == null ? 0.0d : post.getEngagementScore();
    }

    private double safeEventScore(EventResponse event) {
        return event == null || event.getPopularityScore() == null ? 0.0d : event.getPopularityScore();
    }

    private double safeInfluenceScore(AmbassadorInfluenceResponse ambassador) {
        return ambassador == null || ambassador.getInfluenceScore() == null ? 0.0d : ambassador.getInfluenceScore();
    }
}
