package com.civicplatform.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodic heartbeat for deterministic scoring configuration visibility.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ScoringHeartbeatScheduler {

    private final ScoringProperties scoringProperties;

    /**
     * Logs active scoring coefficients on a fixed delay for operational transparency.
     */
    @Scheduled(fixedDelayString = "${app.scoring.audit-interval-ms:3600000}")
    public void emitScoringHeartbeat() {
        log.debug("Scoring heartbeat | post(likes={}, comments={}, shares={}) event(inscription={}, reliability={}, urgency={}, historyDays={}) ambassador(event={}, post={}, engagement={}, referral={})",
                scoringProperties.getPostEngagement().getWeightLikes(),
                scoringProperties.getPostEngagement().getWeightComments(),
                scoringProperties.getPostEngagement().getWeightShares(),
                scoringProperties.getEventPopularity().getWeightInscriptionRate(),
                scoringProperties.getEventPopularity().getWeightOrganizerReliability(),
                scoringProperties.getEventPopularity().getWeightUrgency(),
                scoringProperties.getEventPopularity().getHistoryDays(),
                scoringProperties.getAmbassadorInfluence().getWeightEvent(),
                scoringProperties.getAmbassadorInfluence().getWeightPost(),
                scoringProperties.getAmbassadorInfluence().getWeightEngagement(),
                scoringProperties.getAmbassadorInfluence().getWeightReferral());
    }
}
