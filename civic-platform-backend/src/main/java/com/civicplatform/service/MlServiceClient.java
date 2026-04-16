package com.civicplatform.service;

import com.civicplatform.dto.ml.MlRecommendRequest;
import com.civicplatform.dto.ml.MlRecommendResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Service
@Slf4j
public class MlServiceClient {

    private final RestTemplate restTemplate;
    private final String mlServiceUrl;

    public MlServiceClient(
            RestTemplateBuilder builder,
            @Value("${ml.service.url}") String mlServiceUrl,
            @Value("${ml.service.timeout-seconds:10}") int timeoutSeconds) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(timeoutSeconds))
                .setReadTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
        this.mlServiceUrl = mlServiceUrl.replaceAll("/$", "");
    }

    public MlRecommendResponse getRecommendations(Long userId) {
        String url = mlServiceUrl + "/recommend";
        MlRecommendRequest request = new MlRecommendRequest();
        request.setUserId(userId);
        request.setLimitCampaigns(0);
        request.setLimitProjects(0);
        request.setLimitPosts(0);
        request.setLimitEvents(9);

        try {
            ResponseEntity<MlRecommendResponse> response =
                    restTemplate.postForEntity(url, request, MlRecommendResponse.class);
            MlRecommendResponse body = response.getBody();
            if (body != null && body.getRecommendedEventIds() == null) {
                body.setRecommendedEventIds(new java.util.ArrayList<>());
            }
            return body != null ? body : MlRecommendResponse.empty(userId);
        } catch (Exception e) {
            log.warn("ML service unavailable: {}. Using empty recommendations.", e.getMessage());
            return MlRecommendResponse.empty(userId);
        }
    }

    public void triggerRetrain() {
        try {
            restTemplate.postForEntity(mlServiceUrl + "/retrain", null, Object.class);
        } catch (Exception e) {
            log.warn("Could not trigger ML retrain: {}", e.getMessage());
        }
    }

    public boolean isHealthy() {
        try {
            ResponseEntity<Object> response =
                    restTemplate.getForEntity(mlServiceUrl + "/health", Object.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}
