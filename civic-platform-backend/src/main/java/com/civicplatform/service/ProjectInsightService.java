package com.civicplatform.service;

import com.civicplatform.dto.response.ProjectInsightResponse;

import java.util.List;

public interface ProjectInsightService {
    ProjectInsightResponse getProjectInsight(Long projectId);
    List<ProjectInsightResponse> getWorkflowInsights();
}
