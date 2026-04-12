package com.civicplatform.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MlRecommendResponse {

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("recommended_campaign_ids")
    @JsonDeserialize(contentAs = Long.class)
    private List<Long> recommendedCampaignIds = new ArrayList<>();

    @JsonProperty("recommended_project_ids")
    @JsonDeserialize(contentAs = Long.class)
    private List<Long> recommendedProjectIds = new ArrayList<>();

    @JsonProperty("recommended_post_ids")
    @JsonDeserialize(contentAs = Long.class)
    private List<Long> recommendedPostIds = new ArrayList<>();

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("is_cold_start")
    private boolean coldStart;

    public static MlRecommendResponse empty(Long userId) {
        MlRecommendResponse r = new MlRecommendResponse();
        r.setUserId(userId);
        r.setRecommendedCampaignIds(new ArrayList<>());
        r.setRecommendedProjectIds(new ArrayList<>());
        r.setRecommendedPostIds(new ArrayList<>());
        r.setModelVersion("none");
        r.setColdStart(true);
        return r;
    }
}
