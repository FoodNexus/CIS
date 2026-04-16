package com.civicplatform.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MlRecommendRequest {

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("limit_campaigns")
    private int limitCampaigns = 5;

    @JsonProperty("limit_projects")
    private int limitProjects = 5;

    @JsonProperty("limit_posts")
    private int limitPosts = 10;

    @JsonProperty("limit_events")
    private int limitEvents = 9;
}
