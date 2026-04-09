package com.civicplatform.service.impl;

import com.civicplatform.dto.request.CampaignRequest;
import com.civicplatform.dto.response.CampaignResponse;
import com.civicplatform.entity.Campaign;
import com.civicplatform.entity.CampaignVote;
import com.civicplatform.entity.User;
import com.civicplatform.enums.CampaignStatus;
import com.civicplatform.mapper.CampaignMapper;
import com.civicplatform.repository.CampaignRepository;
import com.civicplatform.repository.CampaignVoteRepository;
import com.civicplatform.repository.UserRepository;
import com.civicplatform.service.CampaignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignServiceImpl implements CampaignService {

    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final CampaignVoteRepository campaignVoteRepository;
    private final CampaignMapper campaignMapper;

    @Override
    @Transactional
    public CampaignResponse createCampaign(CampaignRequest campaignRequest, Long createdById) {
        User createdBy = userRepository.findById(createdById)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + createdById));

        Campaign campaign = campaignMapper.toEntity(campaignRequest);
        campaign.setCreatedBy(createdBy);

        campaign = campaignRepository.save(campaign);
        return campaignMapper.toResponse(campaign);
    }

    @Override
    public CampaignResponse getCampaignById(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found with id: " + id));
        
        CampaignResponse response = campaignMapper.toResponse(campaign);
        response.setVoteCount((int) campaignVoteRepository.countByCampaignId(id));
        return response;
    }

    @Override
    public List<CampaignResponse> getAllCampaigns() {
        List<Campaign> campaigns = campaignRepository.findAll();
        return campaigns.stream()
                .map(campaign -> {
                    CampaignResponse response = campaignMapper.toResponse(campaign);
                    response.setVoteCount((int) campaignVoteRepository.countByCampaignId(campaign.getId()));
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<CampaignResponse> getCampaignsByStatus(CampaignStatus status) {
        List<Campaign> campaigns = campaignRepository.findByStatus(status);
        return campaigns.stream()
                .map(campaign -> {
                    CampaignResponse response = campaignMapper.toResponse(campaign);
                    response.setVoteCount((int) campaignVoteRepository.countByCampaignId(campaign.getId()));
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CampaignResponse updateCampaign(Long id, CampaignRequest campaignRequest) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found with id: " + id));

        campaignMapper.updateEntity(campaignRequest, campaign);
        campaign = campaignRepository.save(campaign);
        
        CampaignResponse response = campaignMapper.toResponse(campaign);
        response.setVoteCount((int) campaignVoteRepository.countByCampaignId(id));
        return response;
    }

    @Override
    @Transactional
    public void deleteCampaign(Long id) {
        if (!campaignRepository.existsById(id)) {
            throw new RuntimeException("Campaign not found with id: " + id);
        }
        campaignRepository.deleteById(id);
    }

    @Override
    @Transactional
    public CampaignResponse launchCampaign(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found with id: " + id));
        
        campaign.launch();
        campaign = campaignRepository.save(campaign);
        return campaignMapper.toResponse(campaign);
    }

    @Override
    @Transactional
    public CampaignResponse closeCampaign(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found with id: " + id));
        
        campaign.close();
        campaign = campaignRepository.save(campaign);
        return campaignMapper.toResponse(campaign);
    }

    @Override
    @Transactional
    public CampaignResponse cancelCampaign(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found with id: " + id));
        
        campaign.cancel();
        campaign = campaignRepository.save(campaign);
        return campaignMapper.toResponse(campaign);
    }

    @Override
    @Transactional
    public void voteForCampaign(Long campaignId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new RuntimeException("Campaign not found with id: " + campaignId));

        if (campaign.getStatus() != CampaignStatus.DRAFT) {
            throw new RuntimeException("Can only vote for DRAFT campaigns");
        }

        if (campaign.getCreatedBy() != null && campaign.getCreatedBy().getId().equals(userId)) {
            throw new RuntimeException("Campaign creators cannot vote on their own campaign");
        }

        // Check if already voted
        if (campaignVoteRepository.findByUserIdAndCampaignId(userId, campaignId).isPresent()) {
            throw new RuntimeException("User has already voted for this campaign");
        }

        CampaignVote vote = CampaignVote.builder()
                .user(user)
                .campaign(campaign)
                .build();

        campaignVoteRepository.save(vote);

        // Check if campaign reached 100 votes
        long voteCount = campaignVoteRepository.countByCampaignId(campaignId);
        if (voteCount >= 100) {
            campaign.launch();
            campaignRepository.save(campaign);
            log.info("Campaign {} has been automatically activated after reaching 100 votes", campaignId);
        }
    }

    @Override
    public boolean hasUserVoted(Long campaignId, Long userId) {
        return campaignVoteRepository.findByUserIdAndCampaignId(userId, campaignId).isPresent();
    }

    @Override
    @Transactional
    public void activateCampaignsReadyForActivation() {
        List<Campaign> campaignsReady = campaignRepository.findCampaignsReadyForActivation();
        
        for (Campaign campaign : campaignsReady) {
            campaign.launch();
            campaignRepository.save(campaign);
            log.info("Campaign {} has been automatically activated", campaign.getId());
        }
    }
}
