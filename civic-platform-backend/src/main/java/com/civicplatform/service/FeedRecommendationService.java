package com.civicplatform.service;

import com.civicplatform.dto.ml.MlRecommendResponse;
import com.civicplatform.dto.response.CampaignResponse;
import com.civicplatform.dto.response.FeedResponse;
import com.civicplatform.dto.response.PostResponse;
import com.civicplatform.dto.response.ProjectResponse;
import com.civicplatform.entity.Campaign;
import com.civicplatform.entity.Post;
import com.civicplatform.entity.Project;
import com.civicplatform.entity.User;
import com.civicplatform.mapper.CampaignMapper;
import com.civicplatform.mapper.PostMapper;
import com.civicplatform.mapper.ProjectMapper;
import com.civicplatform.repository.CampaignRepository;
import com.civicplatform.repository.CampaignVoteRepository;
import com.civicplatform.repository.PostRepository;
import com.civicplatform.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedRecommendationService {

    private final MlServiceClient mlServiceClient;
    private final CampaignRepository campaignRepository;
    private final ProjectRepository projectRepository;
    private final PostRepository postRepository;
    private final CampaignVoteRepository campaignVoteRepository;
    private final CampaignMapper campaignMapper;
    private final ProjectMapper projectMapper;
    private final PostMapper postMapper;

    public FeedResponse buildFeed(User user) {
        MlRecommendResponse ml = mlServiceClient.getRecommendations(user.getId());

        List<Long> cIds = ml.getRecommendedCampaignIds() != null ? ml.getRecommendedCampaignIds() : List.of();
        List<Long> pIds = ml.getRecommendedProjectIds() != null ? ml.getRecommendedProjectIds() : List.of();
        List<Long> postIds = ml.getRecommendedPostIds() != null ? ml.getRecommendedPostIds() : List.of();

        List<Campaign> campaigns = orderByIds(cIds, campaignRepository.findAllById(cIds), Campaign::getId);
        List<Project> projects = orderByIds(pIds, projectRepository.findAllById(pIds), Project::getId);
        List<Post> posts = orderByIds(postIds, postRepository.findAllById(postIds), Post::getId);

        List<CampaignResponse> campaignResponses = campaigns.stream()
                .map(c -> {
                    CampaignResponse r = campaignMapper.toResponse(c);
                    r.setVoteCount((int) campaignVoteRepository.countByCampaignId(c.getId()));
                    r.setIsRecommended(true);
                    return r;
                })
                .toList();

        List<ProjectResponse> projectResponses = projects.stream()
                .map(p -> {
                    ProjectResponse r = projectMapper.toResponse(p);
                    r.setIsRecommended(true);
                    return r;
                })
                .toList();

        List<PostResponse> postResponses = posts.stream()
                .map(p -> {
                    PostResponse r = postMapper.toSummaryResponse(p);
                    r.setIsRecommended(true);
                    return r;
                })
                .toList();

        return FeedResponse.builder()
                .campaigns(campaignResponses)
                .projects(projectResponses)
                .posts(postResponses)
                .modelVersion(ml.getModelVersion())
                .coldStart(ml.isColdStart())
                .build();
    }

    private static <T> List<T> orderByIds(List<Long> ids, List<T> found, Function<T, Long> idGetter) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Map<Long, T> map = found.stream()
                .collect(Collectors.toMap(idGetter, Function.identity(), (a, b) -> a));
        return ids.stream().map(map::get).filter(Objects::nonNull).toList();
    }
}
