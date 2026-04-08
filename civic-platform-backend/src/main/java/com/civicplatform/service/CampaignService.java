package com.civicplatform.service;

import com.civicplatform.dto.request.CampaignRequest;
import com.civicplatform.dto.response.CampaignResponse;
import com.civicplatform.enums.CampaignStatus;

import java.util.List;

public interface CampaignService {
    CampaignResponse createCampaign(CampaignRequest campaignRequest, Long createdById);
    CampaignResponse getCampaignById(Long id);
    List<CampaignResponse> getAllCampaigns();
    List<CampaignResponse> getCampaignsByStatus(CampaignStatus status);
    CampaignResponse updateCampaign(Long id, CampaignRequest campaignRequest);
    void deleteCampaign(Long id);
    CampaignResponse launchCampaign(Long id);
    CampaignResponse closeCampaign(Long id);
    CampaignResponse cancelCampaign(Long id);
    void voteForCampaign(Long campaignId, Long userId);

    boolean hasUserVoted(Long campaignId, Long userId);
    void activateCampaignsReadyForActivation();
}
