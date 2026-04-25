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
import com.civicplatform.service.AmbassadorService;
import com.civicplatform.service.ScoringProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AmbassadorServiceImpl implements AmbassadorService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ScoringProperties scoringProperties;

    /**
     * Computes influence ranking for ambassadors using deterministic weighted scoring.
     */
    @Override
    public List<AmbassadorInfluenceResponse> getRankingByInfluence() {
        ScoringProperties.AmbassadorInfluence cfg = scoringProperties.getAmbassadorInfluence();
        List<User> ambassadors = userRepository.findByUserType(UserType.AMBASSADOR);

        return ambassadors.stream()
                .map(user -> toInfluenceResponse(user, cfg))
                .sorted((left, right) -> {
                    int byScore = Double.compare(right.getInfluenceScore(), left.getInfluenceScore());
                    if (byScore != 0) {
                        return byScore;
                    }
                    int byActions = Long.compare(right.getActionsCount(), left.getActionsCount());
                    if (byActions != 0) {
                        return byActions;
                    }
                    return Long.compare(left.getUserId(), right.getUserId());
                })
                .collect(Collectors.toList());
    }

    private AmbassadorInfluenceResponse toInfluenceResponse(User ambassador, ScoringProperties.AmbassadorInfluence cfg) {
        long eventsCount = eventRepository.findByOrganizerId(ambassador.getId()).size();
        List<Post> ambassadorPosts = postRepository.findByCreator(ambassador.getUserName());
        long postsCount = ambassadorPosts.size();

        long totalEngagement = ambassadorPosts.stream()
                .mapToLong(p -> {
                    long likes = p.getLikesCount() == null ? 0L : p.getLikesCount();
                    long comments = p.getId() == null ? 0L : commentRepository.countByPostId(p.getId());
                    long shares = 0L;
                    return likes + comments + shares;
                })
                .sum();

        long referralsCount = eventParticipantRepository.countNonCancelledByOrganizerId(ambassador.getId());
        long actionsCount = eventsCount + postsCount + referralsCount;
        double badgeBonus = badgeBonus(ambassador.getBadge(), cfg);
        double influenceScore = (eventsCount * cfg.getWeightEvent())
                + (postsCount * cfg.getWeightPost())
                + (totalEngagement * cfg.getWeightEngagement())
                + (referralsCount * cfg.getWeightReferral())
                + badgeBonus;

        log.debug("ambassador={} events={} posts={} totalEngagement={} referrals={} bonus={} score={}",
                ambassador.getId(), eventsCount, postsCount, totalEngagement, referralsCount, badgeBonus, influenceScore);
        return AmbassadorInfluenceResponse.builder()
                .userId(ambassador.getId())
                .userName(ambassador.getUserName())
                .influenceScore(influenceScore)
                .actionsCount(actionsCount)
                .eventsCount(eventsCount)
                .postsCount(postsCount)
                .totalEngagement(totalEngagement)
                .referralsCount(referralsCount)
                .build();
    }

    private double badgeBonus(Badge badge, ScoringProperties.AmbassadorInfluence cfg) {
        if (badge == null) {
            return 0.0d;
        }
        return switch (badge) {
            case BRONZE -> cfg.getBadgeLocal();
            case SILVER -> cfg.getBadgeRegional();
            case GOLD, PLATINUM -> cfg.getBadgeNational();
            default -> 0.0d;
        };
    }
}
