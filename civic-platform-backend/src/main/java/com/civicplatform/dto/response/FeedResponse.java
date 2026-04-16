package com.civicplatform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedResponse {

    @Builder.Default
    private List<CampaignResponse> campaigns = new ArrayList<>();
    @Builder.Default
    private List<ProjectResponse> projects = new ArrayList<>();
    @Builder.Default
    private List<PostResponse> posts = new ArrayList<>();
    @Builder.Default
    private List<EventResponse> events = new ArrayList<>();
    private String modelVersion;
    private Boolean coldStart;
}
