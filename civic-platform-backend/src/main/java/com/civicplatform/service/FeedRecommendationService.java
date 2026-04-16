package com.civicplatform.service;

import com.civicplatform.dto.ml.MlRecommendResponse;
import com.civicplatform.dto.response.EventResponse;
import com.civicplatform.dto.response.FeedResponse;
import com.civicplatform.entity.Event;
import com.civicplatform.entity.User;
import com.civicplatform.mapper.EventMapper;
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

    private static final int FALLBACK_EVENTS = 9;

    private final MlServiceClient mlServiceClient;
    private final EventRepository eventRepository;
    private final EventMapper eventMapper;

    /**
     * Personalized feed for citizens: events only (ML + fallback). Campaigns, projects, and posts
     * are not used for the primary discovery experience.
     */
    public FeedResponse buildFeed(User user) {
        MlRecommendResponse ml = mlServiceClient.getRecommendations(user.getId());

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
                .campaigns(List.of())
                .projects(List.of())
                .posts(List.of())
                .events(eventResponses)
                .modelVersion(ml.getModelVersion())
                .coldStart(ml.isColdStart())
                .build();
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
