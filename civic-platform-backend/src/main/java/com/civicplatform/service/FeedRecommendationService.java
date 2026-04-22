package com.civicplatform.service;

import com.civicplatform.dto.ml.MlRecommendResponse;
import com.civicplatform.dto.response.CampaignResponse;
import com.civicplatform.dto.response.EventResponse;
import com.civicplatform.dto.response.FeedResponse;
import com.civicplatform.entity.Campaign;
import com.civicplatform.entity.Event;
import com.civicplatform.enums.CampaignStatus;
import com.civicplatform.entity.User;
import com.civicplatform.mapper.CampaignMapper;
import com.civicplatform.mapper.EventMapper;
import com.civicplatform.repository.CampaignRepository;
import com.civicplatform.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedRecommendationService {

    private static final int FALLBACK_CAMPAIGNS = 6;
    private static final int FALLBACK_EVENTS = 9;

    private final MlServiceClient mlServiceClient;
    private final CampaignRepository campaignRepository;
    private final CampaignMapper campaignMapper;
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;

    public FeedResponse buildFeed(User user) {
        MlRecommendResponse ml = mlServiceClient.getRecommendations(user.getId());

        List<Long> cIds = ml.getRecommendedCampaignIds() != null ? ml.getRecommendedCampaignIds() : List.of();
        List<Campaign> campaigns = orderByIds(cIds, campaignRepository.findAllById(cIds), Campaign::getId);
        List<CampaignResponse> campaignResponses = campaigns.stream()
                .map(c -> {
                    CampaignResponse r = campaignMapper.toResponse(c);
                    r.setIsRecommended(true);
                    return r;
                })
                .toList();
        if (campaignResponses.isEmpty()) {
            campaignResponses = fallbackCampaigns(FALLBACK_CAMPAIGNS);
        }

        List<Long> eIds = ml.getRecommendedEventIds() != null ? ml.getRecommendedEventIds() : List.of();
        List<Event> events = orderByIds(eIds, eventRepository.findAllById(eIds), Event::getId);

        List<EventResponse> eventResponses = events.stream()
                .map(e -> {
                    EventResponse r = eventMapper.toResponse(e);
                    r.setIsRecommended(true);
                    return r;
                })
                .toList();

        if (eventResponses.isEmpty()) {
            eventResponses = fallbackEvents(FALLBACK_EVENTS);
        }

        return FeedResponse.builder()
                .campaigns(campaignResponses)
                .projects(List.of())
                .posts(List.of())
                .events(eventResponses)
                .modelVersion(ml.getModelVersion())
                .coldStart(ml.isColdStart())
                .build();
    }

    private List<CampaignResponse> fallbackCampaigns(int limit) {
        return campaignRepository.findByStatus(CampaignStatus.ACTIVE).stream()
                .limit(limit)
                .map(c -> {
                    CampaignResponse r = campaignMapper.toResponse(c);
                    r.setIsRecommended(false);
                    return r;
                })
                .toList();
    }

    private List<EventResponse> fallbackEvents(int limit) {
        List<Event> upcoming = eventRepository.findUpcomingEvents(LocalDateTime.now());
        return upcoming.stream()
                .limit(limit)
                .map(e -> {
                    EventResponse r = eventMapper.toResponse(e);
                    r.setIsRecommended(false);
                    return r;
                })
                .toList();
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
