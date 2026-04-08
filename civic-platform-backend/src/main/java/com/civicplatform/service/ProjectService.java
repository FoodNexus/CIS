package com.civicplatform.service;

import com.civicplatform.dto.request.ProjectFundingRequest;
import com.civicplatform.dto.request.ProjectRequest;
import com.civicplatform.dto.response.ProjectFundingResponse;
import com.civicplatform.dto.response.ProjectResponse;

import java.util.List;

public interface ProjectService {
    ProjectResponse createProject(ProjectRequest projectRequest, Long organizerId);
    ProjectResponse getProjectById(Long id);
    List<ProjectResponse> getAllProjects();
    List<ProjectResponse> getProjectsByStatus(String status);
    ProjectResponse updateProject(Long id, ProjectRequest projectRequest);
    void deleteProject(Long id);
    void voteForProject(Long projectId, Long userId);

    boolean hasUserVoted(Long projectId, Long userId);
    void fundProject(ProjectFundingRequest fundingRequest, Long userId);
    ProjectResponse completeProject(Long id, String finalReport);
    List<ProjectFundingResponse> getFundingsByUser(Long userId);
}
