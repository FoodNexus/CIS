package com.civicplatform.controller;

import com.civicplatform.dto.request.ProjectFundingRequest;
import com.civicplatform.dto.request.ProjectRequest;
import com.civicplatform.dto.response.ProjectFundingResponse;
import com.civicplatform.dto.response.ProjectResponse;
import com.civicplatform.entity.User;
import com.civicplatform.enums.UserType;
import com.civicplatform.repository.UserRepository;
import com.civicplatform.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@Tag(name = "Project Management", description = "Project management APIs")
public class ProjectController {

    private final ProjectService projectService;
    private final UserRepository userRepository;

    @Operation(summary = "Create a new project")
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody ProjectRequest projectRequest, Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        if (user.getUserType() != UserType.DONOR && user.getUserType() != UserType.AMBASSADOR) {
            throw new AccessDeniedException("Only DONOR and AMBASSADOR users can create projects");
        }
        Long userId = user.getId();
        ProjectResponse response = projectService.createProject(projectRequest, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /** Must be before GET /{id} or "my-fundings" is parsed as an id. */
    @Operation(summary = "Get my project donations (funding history)")
    @GetMapping("/my-fundings")
    public ResponseEntity<List<ProjectFundingResponse>> getMyFundings(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        List<ProjectFundingResponse> response = projectService.getFundingsByUser(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get projects by status")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ProjectResponse>> getProjectsByStatus(@PathVariable String status) {
        List<ProjectResponse> response = projectService.getProjectsByStatus(status);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all projects")
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        List<ProjectResponse> response = projectService.getAllProjects();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Whether current user has voted for this project")
    @GetMapping("/{id}/has-voted")
    public ResponseEntity<Boolean> hasVoted(@PathVariable Long id, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        return ResponseEntity.ok(projectService.hasUserVoted(id, userId));
    }

    @Operation(summary = "Get project by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long id) {
        ProjectResponse response = projectService.getProjectById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update project")
    @PutMapping("/{id}")
    public ResponseEntity<ProjectResponse> updateProject(@PathVariable Long id, @Valid @RequestBody ProjectRequest projectRequest, Authentication authentication) {
        checkAdminOrDonorAmbassador(authentication);
        ProjectResponse response = projectService.updateProject(id, projectRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete project")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id, Authentication authentication) {
        checkAdminOrDonorAmbassador(authentication);
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Vote for project")
    @PostMapping("/{id}/vote")
    public ResponseEntity<Void> voteForProject(@PathVariable Long id, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        projectService.voteForProject(id, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Fund project")
    @PostMapping("/{id}/fund")
    public ResponseEntity<Void> fundProject(@PathVariable Long id, @Valid @RequestBody ProjectFundingRequest fundingRequest, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        fundingRequest.setProjectId(id);
        projectService.fundProject(fundingRequest, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Complete project")
    @PostMapping("/{id}/complete")
    public ResponseEntity<ProjectResponse> completeProject(@PathVariable Long id, @RequestParam String finalReport, Authentication authentication) {
        checkAdminOrDonorAmbassador(authentication);
        ProjectResponse response = projectService.completeProject(id, finalReport);
        return ResponseEntity.ok(response);
    }

    private void checkAdminOrDonorAmbassador(Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isAdmin && user.getUserType() != UserType.DONOR && user.getUserType() != UserType.AMBASSADOR) {
            throw new AccessDeniedException("Only ADMIN, DONOR, or AMBASSADOR can perform this action");
        }
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        return getUserFromAuthentication(authentication).getId();
    }

    private User getUserFromAuthentication(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }
}
