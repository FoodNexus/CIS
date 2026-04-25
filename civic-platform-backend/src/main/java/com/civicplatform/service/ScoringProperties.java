package com.civicplatform.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configurable deterministic coefficients for feed and ranking scores.
 */
@Component
@ConfigurationProperties(prefix = "app.scoring")
@Getter
@Setter
public class ScoringProperties {

    private PostEngagement postEngagement = new PostEngagement();
    private EventPopularity eventPopularity = new EventPopularity();
    private AmbassadorInfluence ambassadorInfluence = new AmbassadorInfluence();

    @Getter
    @Setter
    public static class PostEngagement {
        private double weightLikes = 2.0;
        private double weightComments = 3.0;
        private double weightShares = 4.0;
    }

    @Getter
    @Setter
    public static class EventPopularity {
        private double weightInscriptionRate = 0.4;
        private double weightOrganizerReliability = 0.3;
        private double weightUrgency = 0.3;
        private int historyDays = 90;
    }

    @Getter
    @Setter
    public static class AmbassadorInfluence {
        private double weightEvent = 2.0;
        private double weightPost = 3.0;
        private double weightEngagement = 0.5;
        private double weightReferral = 5.0;
        private double badgeLocal = 10.0;
        private double badgeRegional = 25.0;
        private double badgeNational = 50.0;
    }
}
