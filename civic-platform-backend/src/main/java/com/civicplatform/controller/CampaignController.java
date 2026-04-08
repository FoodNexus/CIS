package com.civicplatform.controller;

import com.civicplatform.dto.request.CampaignRequest;
import com.civicplatform.dto.response.CampaignResponse;
import com.civicplatform.entity.User;
import com.civicplatform.enums.CampaignStatus;
import com.civicplatform.enums.UserType;
import com.civicplatform.repository.UserRepository;
import com.civicplatform.service.CampaignService;
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
@RequestMapping("/campaigns")
@RequiredArgsConstructor
@Tag(name = "Campaign Management", description = "Campaign management APIs")
public class CampaignController {

    private final CampaignService campaignService;
    private final UserRepository userRepository;

    @Operation(summary = "Create a new campaign")
    @PostMapping
    public ResponseEntity<CampaignResponse> createCampaign(@Valid @RequestBody CampaignRequest campaignRequest, Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        if (user.getUserType() != UserType.DONOR && user.getUserType() != UserType.AMBASSADOR) {
            throw new AccessDeniedException("Only DONOR and AMBASSADOR users can create campaigns");
        }
        Long userId = user.getId();
        CampaignResponse response = campaignService.createCampaign(campaignRequest, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Get campaign by ID")
    @GetMapping("/{id}")
    public ResponseEntity<CampaignResponse> getCampaignById(@PathVariable Long id) {
        CampaignResponse response = campaignService.getCampaignById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all campaigns")
    @GetMapping
    public ResponseEntity<List<CampaignResponse>> getAllCampaigns() {
        List<CampaignResponse> response = campaignService.getAllCampaigns();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get campaigns by status")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<CampaignResponse>> getCampaignsByStatus(@PathVariable CampaignStatus status) {
        List<CampaignResponse> response = campaignService.getCampaignsByStatus(status);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update campaign")
    @PutMapping("/{id}")
    public ResponseEntity<CampaignResponse> updateCampaign(@PathVariable Long id, @Valid @RequestBody CampaignRequest campaignRequest, Authentication authentication) {
        checkCampaignOwnership(id, authentication);
        CampaignResponse response = campaignService.updateCampaign(id, campaignRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete campaign")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCampaign(@PathVariable Long id, Authentication authentication) {
        checkCampaignOwnership(id, authentication);
        campaignService.deleteCampaign(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Launch campaign")
    @PostMapping("/{id}/launch")
    public ResponseEntity<CampaignResponse> launchCampaign(@PathVariable Long id, Authentication authentication) {
        checkCampaignOwnership(id, authentication);
        CampaignResponse response = campaignService.launchCampaign(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Close campaign")
    @PostMapping("/{id}/close")
    public ResponseEntity<CampaignResponse> closeCampaign(@PathVariable Long id, Authentication authentication) {
        checkCampaignOwnership(id, authentication);
        CampaignResponse response = campaignService.closeCampaign(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cancel campaign")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<CampaignResponse> cancelCampaign(@PathVariable Long id, Authentication authentication) {
        checkCampaignOwnership(id, authentication);
        CampaignResponse response = campaignService.cancelCampaign(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Vote for campaign")
    @PostMapping("/{id}/vote")
    public ResponseEntity<Void> voteForCampaign(@PathVariable Long id, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        campaignService.voteForCampaign(id, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Whether current user has voted for this campaign")
    @GetMapping("/{id}/has-voted")
    public ResponseEntity<Boolean> hasVoted(@PathVariable Long id, Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        return ResponseEntity.ok(campaignService.hasUserVoted(id, userId));
    }

    @Operation(summary = "Activate campaigns ready for activation")
    @PostMapping("/activate-ready")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activateCampaignsReadyForActivation() {
        campaignService.activateCampaignsReadyForActivation();
        return ResponseEntity.ok().build();
    }

    private void checkCampaignOwnership(Long campaignId, Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isAdmin) {
            CampaignResponse campaign = campaignService.getCampaignById(campaignId);
            if (campaign.getCreatedById() == null || !user.getId().equals(campaign.getCreatedById())) {
                throw new AccessDeniedException("You are not the owner of this campaign");
            }
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
