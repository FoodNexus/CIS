package com.civicplatform.mapper;

import com.civicplatform.dto.request.CampaignRequest;
import com.civicplatform.dto.response.CampaignResponse;
import com.civicplatform.entity.Campaign;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {PostMapper.class})
public interface CampaignMapper {

    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "createdByName", expression = "java(campaign.getCreatedBy() != null ? campaign.getCreatedBy().getUserName() : null)")
    @Mapping(target = "voteCount", ignore = true)
    @Mapping(target = "posts", source = "posts", qualifiedByName = "mapPostListToSummary")
    CampaignResponse toResponse(Campaign campaign);
    
    List<CampaignResponse> toResponseList(List<Campaign> campaigns);

    default List<CampaignResponse> toResponseList(List<Campaign> campaigns, boolean isRecommended) {
        if (campaigns == null) {
            return null;
        }
        return campaigns.stream()
                .map(c -> {
                    CampaignResponse r = toResponse(c);
                    r.setIsRecommended(isRecommended);
                    return r;
                })
                .collect(Collectors.toList());
    }
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "currentKg", ignore = true)
    @Mapping(target = "currentMeals", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "posts", ignore = true)
    @Mapping(target = "votes", ignore = true)
    @Mapping(target = "startDate", source = "startDate", qualifiedByName = "stringToLocalDate")
    @Mapping(target = "endDate", source = "endDate", qualifiedByName = "stringToLocalDate")
    Campaign toEntity(CampaignRequest campaignRequest);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "currentKg", ignore = true)
    @Mapping(target = "currentMeals", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "posts", ignore = true)
    @Mapping(target = "votes", ignore = true)
    @Mapping(target = "startDate", source = "startDate", qualifiedByName = "stringToLocalDate")
    @Mapping(target = "endDate", source = "endDate", qualifiedByName = "stringToLocalDate")
    void updateEntity(CampaignRequest campaignRequest, @MappingTarget Campaign campaign);

    @AfterMapping
    default void setCampaignDefaults(@MappingTarget Campaign campaign) {
        if (campaign.getStatus() == null) {
            campaign.setStatus(com.civicplatform.enums.CampaignStatus.DRAFT);
        }
        if (campaign.getCurrentKg() == null) {
            campaign.setCurrentKg(0);
        }
        if (campaign.getCurrentMeals() == null) {
            campaign.setCurrentMeals(0);
        }
    }
    
    @Named("stringToLocalDate")
    default LocalDate stringToLocalDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
