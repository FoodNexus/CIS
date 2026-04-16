package com.civicplatform.service;

import com.civicplatform.dto.rate.CitizenRateSnapshot;
import com.civicplatform.entity.Event;
import com.civicplatform.entity.User;
import com.civicplatform.enums.Badge;
import com.civicplatform.enums.EventType;
import com.civicplatform.enums.InteractionEntityType;
import com.civicplatform.enums.PostType;
import com.civicplatform.enums.UserType;
import com.civicplatform.repository.CommentRepository;
import com.civicplatform.repository.EventParticipantRepository;
import com.civicplatform.repository.PostRepository;
import com.civicplatform.repository.ProjectFundingRepository;
import com.civicplatform.repository.UserInteractionRepository;
import com.civicplatform.util.FoodCommunityContext;
import com.civicplatform.util.LocationProximityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Computes a multi-feature rate (0–100 composite) for invitation decisions.
 * Rule-based weighted sum: composite = min(100, rawTotal / RAW_SCALE_MAX * 100).
 */
@Service
@RequiredArgsConstructor
public class CitizenRateCalculationService {

    /** Sum of per-feature caps (see fields below); keep in sync when adding features. */
    private static final double RAW_SCALE_MAX = 260.0;

    private final EventParticipantRepository eventParticipantRepository;
    private final ProjectFundingRepository projectFundingRepository;
    private final UserInteractionRepository userInteractionRepository;
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    @Value("${app.invitation.food.solidarity-campaign-ids:97001,97002,97003}")
    private String solidarityCampaignIdsCsv;

    /**
     * @param event required for location / context; must not be null when matching for that event.
     */
    public CitizenRateSnapshot compute(User citizen, Event event, Long donorId) {
        Map<String, Double> features = new LinkedHashMap<>();

        double badge = badgePoints(citizen.getBadge());
        features.put("badgeEngagement", badge);

        int eventsAttended = eventParticipantRepository.countCompletedByUserId(citizen.getId());
        double participation = participationPoints(eventsAttended);
        features.put("pastEventParticipation", participation);

        int fundingCount = projectFundingRepository.countByUserId(citizen.getId());
        double funding = fundingPoints(fundingCount);
        features.put("communityFunding", funding);

        double userType = userTypePoints(citizen.getUserType());
        features.put("roleEngagement", userType);

        Optional<LocalDateTime> lastInteraction = userInteractionRepository.findLastInteractionDate(citizen.getId());
        Optional<LocalDateTime> lastEventActivity = latestEventActivity(citizen.getId());
        Optional<LocalDateTime> lastActivity = maxDate(lastInteraction, lastEventActivity);
        double recency = recencyPoints(lastActivity);
        features.put("platformRecency", recency);

        double donorAffinity = 0.0;
        if (donorId != null && eventParticipantRepository.hasAttendedEventByOrganizer(citizen.getId(), donorId)) {
            donorAffinity = 15.0;
        }
        features.put("donorCompatibility", donorAffinity);

        double location = locationAlignmentPoints(citizen, event);
        features.put("locationAlignment", location);

        double topicAlignment = interestTopicAlignmentPoints(citizen, event);
        features.put("interestTopicAlignment", topicAlignment);

        long eventIx = userInteractionRepository.countByUserIdAndEntityType(citizen.getId(), InteractionEntityType.EVENT);
        double interest = Math.min(15.0, eventIx * 3.0);
        features.put("eventInterestEngagement", interest);

        long distCount = eventParticipantRepository.countCompletedByUserIdAndEventType(citizen.getId(), EventType.DISTRIBUTION);
        double distributionPts = Math.min(12.0, distCount * 3.0);
        features.put("distributionParticipation", distributionPts);

        double foodTopic = foodSolidarityTopicFit(citizen, event);
        features.put("foodSolidarityTopicFit", foodTopic);

        double ambassadorScope = ambassadorScopePoints(citizen);
        features.put("ambassadorScope", ambassadorScope);

        double reliability = checkInReliabilityPoints(citizen.getId());
        features.put("checkInReliability", reliability);

        long foodComments = commentRepository.countFoodRelatedCommentsByAuthor(citizen.getId());
        double recipePts = Math.min(10.0, foodComments * 2.0);
        features.put("recipeCommentEngagement", recipePts);

        double solidarity = solidarityCampaignPoints(citizen.getId());
        features.put("solidarityCampaignEngagement", solidarity);

        double testimonial = testimonialVoicePoints(citizen, event);
        features.put("testimonialVoice", testimonial);

        double weekend = weekendAvailabilityPoints(citizen, event);
        features.put("weekendAvailabilityFit", weekend);

        double rawTotal = features.values().stream().mapToDouble(Double::doubleValue).sum();
        double composite = Math.min(100.0, (rawTotal / RAW_SCALE_MAX) * 100.0);
        features.put("rawTotal", round2(rawTotal));
        features.put("compositeRate", round2(composite));

        return CitizenRateSnapshot.builder()
                .rawTotal(rawTotal)
                .compositeRate(composite)
                .features(features)
                .build();
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private Optional<LocalDateTime> latestEventActivity(Long userId) {
        Optional<LocalDateTime> r = eventParticipantRepository.findMaxRegisteredAtByUserId(userId);
        Optional<LocalDateTime> c = eventParticipantRepository.findMaxCompletedAtByUserId(userId);
        return maxDate(r, c);
    }

    private static Optional<LocalDateTime> maxDate(Optional<LocalDateTime> a, Optional<LocalDateTime> b) {
        if (a.isEmpty()) {
            return b;
        }
        if (b.isEmpty()) {
            return a;
        }
        return Optional.of(Comparator.<LocalDateTime>naturalOrder().compare(a.get(), b.get()) >= 0 ? a.get() : b.get());
    }

    private double checkInReliabilityPoints(Long userId) {
        long denom = eventParticipantRepository.countRegisteredNonCancelledByUserId(userId);
        if (denom == 0) {
            return 0.0;
        }
        long num = eventParticipantRepository.countReliableAttendanceByUserId(userId);
        double ratio = (double) num / (double) denom;
        return Math.min(12.0, ratio * 12.0);
    }

    private double solidarityCampaignPoints(Long userId) {
        List<Long> ids = parseSolidarityCampaignIds();
        if (ids.isEmpty()) {
            return 0.0;
        }
        long n = userInteractionRepository.countByUserIdAndEntityTypeAndEntityIdIn(
                userId, InteractionEntityType.CAMPAIGN, ids);
        return Math.min(12.0, n * 3.0);
    }

    private List<Long> parseSolidarityCampaignIds() {
        if (solidarityCampaignIdsCsv == null || solidarityCampaignIdsCsv.isBlank()) {
            return List.of();
        }
        List<Long> out = new ArrayList<>();
        for (String part : solidarityCampaignIdsCsv.split(",")) {
            String s = part.trim();
            if (s.isEmpty()) {
                continue;
            }
            try {
                out.add(Long.parseLong(s));
            } catch (NumberFormatException ignored) {
                // skip invalid token
            }
        }
        return out;
    }

    private double ambassadorScopePoints(User citizen) {
        String interests = citizen.getInterests() != null ? citizen.getInterests().toLowerCase() : "";
        if (interests.contains("ambassador:national")) {
            return 10.0;
        }
        if (interests.contains("ambassador:regional")) {
            return 7.0;
        }
        if (interests.contains("ambassador:local")) {
            return 5.0;
        }
        if (citizen.getUserType() == UserType.AMBASSADOR) {
            return 3.0;
        }
        return 0.0;
    }

    private double testimonialVoicePoints(User citizen, Event event) {
        if (event == null || citizen.getUserName() == null) {
            return 0.0;
        }
        String corpus = eventCorpus(event);
        String cl = corpus.toLowerCase();
        if (!FoodCommunityContext.matchesCorpus(corpus) && event.getType() != EventType.DISTRIBUTION) {
            return 0.0;
        }
        if (!cl.contains("ramadan") && !cl.contains("solidarity") && !cl.contains("iftar")) {
            return 0.0;
        }
        long n = postRepository.countByCreatorAndType(citizen.getUserName(), PostType.TESTIMONIAL);
        return Math.min(8.0, n * 4.0);
    }

    private double weekendAvailabilityPoints(User citizen, Event event) {
        if (event == null) {
            return 0.0;
        }
        String corpus = eventCorpus(event).toLowerCase();
        if (!corpus.contains("weekend") && !corpus.contains("saturday") && !corpus.contains("sunday")) {
            return 0.0;
        }
        String interests = citizen.getInterests() != null ? citizen.getInterests().toLowerCase() : "";
        if (interests.contains("weekend") || interests.contains("saturday") || interests.contains("sunday")) {
            return 8.0;
        }
        return 0.0;
    }

    private double foodSolidarityTopicFit(User citizen, Event event) {
        if (event == null) {
            return 0.0;
        }
        String corpus = eventCorpus(event);
        if (!FoodCommunityContext.matchesCorpus(corpus) && event.getType() != EventType.DISTRIBUTION) {
            return 0.0;
        }
        String c = corpus.toLowerCase();
        double score = 0.0;
        String tags = citizen.getInterests();
        if (tags != null && !tags.isBlank()) {
            String tl = tags.toLowerCase();
            String[] keys = {
                    "cooking", "sustainability", "food waste", "ramadan", "solidarity", "fridge",
                    "recipe", "muslim", "iftar", "volunteer", "weekend", "community", "hunger"
            };
            for (String k : keys) {
                if (tl.contains(k) && c.contains(k)) {
                    score += 3.0;
                }
            }
            if (tl.contains("sustainability") && (c.contains("food") || c.contains("waste") || c.contains("leftover"))) {
                score += 4.0;
            }
            if (tl.contains("muslim") && (c.contains("ramadan") || c.contains("iftar"))) {
                score += 5.0;
            }
        }
        return Math.min(15.0, score);
    }

    private static String eventCorpus(Event event) {
        StringBuilder sb = new StringBuilder();
        if (event.getTitle() != null) {
            sb.append(event.getTitle()).append(' ');
        }
        if (event.getDescription() != null) {
            sb.append(event.getDescription()).append(' ');
        }
        return sb.toString();
    }

    /**
     * Proximity between the citizen's {@link User#getAddress()} and the event holding place:
     * {@link Event#getLocation()}, plus title and description so neighborhoods named only in the title
     * still match. Uses token overlap, substring checks, and a Greater Tunis region bonus — no GPS.
     */
    private double locationAlignmentPoints(User citizen, Event event) {
        return LocationProximityUtil.scoreAddressToEventVenue(citizen.getAddress(), event, 15.0);
    }

    /**
     * Overlap between the citizen's stated interests (and light keyword buckets) and the event
     * title, description, type, and location (e.g. environment vs. technology workshops).
     */
    private double interestTopicAlignmentPoints(User citizen, Event event) {
        if (event == null) {
            return 0.0;
        }
        String tags = citizen.getInterests();
        String company = citizen.getCompanyName();
        StringBuilder corpus = new StringBuilder();
        if (event.getTitle() != null) {
            corpus.append(event.getTitle()).append(' ');
        }
        if (event.getDescription() != null) {
            corpus.append(event.getDescription()).append(' ');
        }
        if (event.getType() != null) {
            corpus.append(event.getType().name()).append(' ');
        }
        if (event.getLocation() != null) {
            corpus.append(event.getLocation()).append(' ');
        }
        String c = corpus.toString().toLowerCase();
        double score = 0.0;

        if (tags != null && !tags.isBlank()) {
            for (String raw : tags.split(",")) {
                String t = raw.trim().toLowerCase();
                if (t.length() < 3) {
                    continue;
                }
                if (c.contains(t)) {
                    score += 5.0;
                }
            }
        }

        if (tags != null) {
            String tl = tags.toLowerCase();
            if (environmentBucket(c) && environmentInterest(tl)) {
                score += 8.0;
            }
            if (technologyBucket(c) && technologyInterest(tl)) {
                score += 8.0;
            }
        }

        if (company != null && !company.isBlank()) {
            String cl = company.toLowerCase();
            if (technologyBucket(c) && (cl.contains("software") || cl.contains("developer") || cl.contains("tech"))) {
                score += 5.0;
            }
            if (environmentBucket(c) && (cl.contains("environment") || cl.contains("marine") || cl.contains("eco"))) {
                score += 5.0;
            }
        }

        return Math.min(18.0, score);
    }

    private static boolean environmentBucket(String corpus) {
        return corpus.contains("beach")
                || corpus.contains("cleanup")
                || corpus.contains("eco")
                || corpus.contains("shore")
                || corpus.contains("coastal")
                || corpus.contains("environment")
                || corpus.contains("green");
    }

    private static boolean technologyBucket(String corpus) {
        return corpus.contains("code")
                || corpus.contains("coding")
                || corpus.contains("software")
                || corpus.contains("developer")
                || corpus.contains("technology")
                || corpus.contains("workshop")
                || corpus.contains("python")
                || corpus.contains("tech")
                || corpus.contains("lab");
    }

    private static boolean environmentInterest(String tagsLower) {
        return tagsLower.contains("environment")
                || tagsLower.contains("sustainability")
                || tagsLower.contains("beach")
                || tagsLower.contains("eco")
                || tagsLower.contains("climate");
    }

    private static boolean technologyInterest(String tagsLower) {
        return tagsLower.contains("technology")
                || tagsLower.contains("software")
                || tagsLower.contains("coding")
                || tagsLower.contains("developer")
                || tagsLower.contains("python")
                || tagsLower.contains("workshop");
    }

    private double badgePoints(Badge badge) {
        if (badge == null) {
            return 0.0;
        }
        return switch (badge) {
            case PLATINUM -> 40.0;
            case GOLD -> 30.0;
            case SILVER -> 20.0;
            case BRONZE -> 10.0;
            default -> 0.0;
        };
    }

    private double participationPoints(int count) {
        if (count >= 10) {
            return 30.0;
        }
        if (count >= 7) {
            return 22.0;
        }
        if (count >= 5) {
            return 15.0;
        }
        if (count >= 3) {
            return 10.0;
        }
        if (count >= 1) {
            return 5.0;
        }
        return 0.0;
    }

    private double fundingPoints(int count) {
        if (count >= 5) {
            return 20.0;
        }
        if (count >= 3) {
            return 14.0;
        }
        if (count >= 1) {
            return 8.0;
        }
        return 0.0;
    }

    private double userTypePoints(UserType type) {
        if (type == UserType.AMBASSADOR) {
            return 10.0;
        }
        return 5.0;
    }

    private double recencyPoints(Optional<LocalDateTime> lastActivity) {
        if (lastActivity.isEmpty()) {
            return 0.0;
        }
        long days = ChronoUnit.DAYS.between(lastActivity.get(), LocalDateTime.now());
        if (days < 0) {
            return 10.0;
        }
        if (days <= 7) {
            return 10.0;
        }
        if (days <= 14) {
            return 7.0;
        }
        if (days <= 30) {
            return 4.0;
        }
        if (days <= 60) {
            return 1.0;
        }
        return 0.0;
    }
}
