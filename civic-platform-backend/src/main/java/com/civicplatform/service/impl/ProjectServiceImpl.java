package com.civicplatform.service.impl;

import com.civicplatform.dto.request.ProjectFundingRequest;
import com.civicplatform.dto.request.ProjectRequest;
import com.civicplatform.dto.response.ProjectFundingResponse;
import com.civicplatform.dto.response.ProjectResponse;
import com.civicplatform.entity.Project;
import com.civicplatform.entity.ProjectFunding;
import com.civicplatform.entity.User;
import com.civicplatform.mapper.ProjectFundingMapper;
import com.civicplatform.mapper.ProjectMapper;
import com.civicplatform.entity.ProjectVote;
import com.civicplatform.entity.ProjectVoteId;
import com.civicplatform.repository.ProjectFundingRepository;
import com.civicplatform.repository.ProjectRepository;
import com.civicplatform.repository.ProjectVoteRepository;
import com.civicplatform.repository.UserRepository;
import com.civicplatform.enums.InteractionAction;
import com.civicplatform.enums.InteractionEntityType;
import com.civicplatform.service.ProjectService;
import com.civicplatform.service.UserInteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectFundingRepository projectFundingRepository;
    private final ProjectVoteRepository projectVoteRepository;
    private final ProjectMapper projectMapper;
    private final ProjectFundingMapper projectFundingMapper;
    private final UserInteractionService userInteractionService;

    @Override
    @Transactional
    public ProjectResponse createProject(ProjectRequest projectRequest, Long organizerId) {
        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + organizerId));

        Project project = projectMapper.toEntity(projectRequest);
        project.setCreatedBy(organizer);
        project.submit();

        project = projectRepository.save(project);
        return projectMapper.toResponse(project);
    }

    @Override
    public List<ProjectFundingResponse> getFundingsByUser(Long userId) {
        List<ProjectFunding> rows = projectFundingRepository.findByUserIdWithDetails(userId);
        return projectFundingMapper.toResponseList(rows);
    }

    @Override
    public ProjectResponse getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));
        return projectMapper.toResponse(project);
    }

    @Override
    public List<ProjectResponse> getAllProjects() {
        List<Project> projects = projectRepository.findAll();
        return projects.stream()
                .map(projectMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProjectResponse> getProjectsByStatus(String status) {
        List<Project> projects = projectRepository.findByStatus(status);
        return projects.stream()
                .map(projectMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProjectResponse updateProject(Long id, ProjectRequest projectRequest) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));

        projectMapper.updateEntity(projectRequest, project);
        project = projectRepository.save(project);
        return projectMapper.toResponse(project);
    }

    @Override
    @Transactional
    public void deleteProject(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new RuntimeException("Project not found with id: " + id);
        }
        projectRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void voteForProject(Long projectId, Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + projectId));

        if (project.getCreatedBy() != null && project.getCreatedBy().getId().equals(userId)) {
            throw new RuntimeException("Project creators cannot vote on their own project");
        }

        if (projectVoteRepository.findByUserIdAndProjectId(userId, projectId).isPresent()) {
            throw new RuntimeException("User has already voted for this project");
        }

        ProjectVote vote = new ProjectVote();
        vote.setId(new ProjectVoteId(userId, projectId));
        projectVoteRepository.saveAndFlush(vote);

        project.vote();
        projectRepository.save(project);

        userInteractionService.record(userId, InteractionEntityType.PROJECT, projectId, InteractionAction.VOTE);
    }

    @Override
    public boolean hasUserVoted(Long projectId, Long userId) {
        return projectVoteRepository.findByUserIdAndProjectId(userId, projectId).isPresent();
    }

    @Override
    @Transactional
    public void fundProject(ProjectFundingRequest fundingRequest, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Project project = projectRepository.findById(fundingRequest.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + fundingRequest.getProjectId()));

        if (project.isFullyFunded()) {
            throw new RuntimeException("Project is already fully funded");
        }

        ProjectFunding funding = ProjectFunding.builder()
                .project(project)
                .user(user)
                .amount(fundingRequest.getAmount())
                .fundDate(LocalDateTime.now())
                .paymentMethod(fundingRequest.getPaymentMethod())
                .build();

        projectFundingRepository.save(funding);

        // Update project funding
        project.fund(fundingRequest.getAmount());
        projectRepository.save(project);

        // Check if project is now fully funded
        if (project.isFullyFunded()) {
            project.setStatus("FULLY_FUNDED");
            projectRepository.save(project);
        }

        userInteractionService.record(userId, InteractionEntityType.PROJECT, project.getId(), InteractionAction.FUND);
    }

    @Override
    @Transactional
    public ProjectResponse completeProject(Long id, String finalReport) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found with id: " + id));

        if (!project.isFullyFunded()) {
            throw new RuntimeException("Project must be fully funded before completion");
        }

        project.complete(finalReport);
        project = projectRepository.save(project);
        return projectMapper.toResponse(project);
    }
}
