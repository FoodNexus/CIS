package com.civicplatform.mapper;

import com.civicplatform.dto.request.ProjectRequest;
import com.civicplatform.dto.response.ProjectResponse;
import com.civicplatform.entity.Project;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {ProjectFundingMapper.class})
public interface ProjectMapper {

    @Mapping(target = "fundings", source = "fundings")
    @Mapping(target = "createdById", source = "createdBy.id")
    ProjectResponse toResponse(Project project);
    
    List<ProjectResponse> toResponseList(List<Project> projects);

    default List<ProjectResponse> toResponseList(List<Project> projects, boolean isRecommended) {
        if (projects == null) {
            return null;
        }
        return projects.stream()
                .map(p -> {
                    ProjectResponse r = toResponse(p);
                    r.setIsRecommended(isRecommended);
                    return r;
                })
                .collect(Collectors.toList());
    }
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "currentAmount", ignore = true)
    @Mapping(target = "voteCount", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "startDate", ignore = true)
    @Mapping(target = "completionDate", ignore = true)
    @Mapping(target = "finalReport", ignore = true)
    @Mapping(target = "fundings", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    Project toEntity(ProjectRequest projectRequest);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "currentAmount", ignore = true)
    @Mapping(target = "voteCount", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "startDate", ignore = true)
    @Mapping(target = "completionDate", ignore = true)
    @Mapping(target = "finalReport", ignore = true)
    @Mapping(target = "fundings", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    void updateEntity(ProjectRequest projectRequest, @MappingTarget Project project);

    @AfterMapping
    default void setProjectDefaults(@MappingTarget Project project) {
        if (project.getCurrentAmount() == null) {
            project.setCurrentAmount(java.math.BigDecimal.ZERO);
        }
        if (project.getVoteCount() == null) {
            project.setVoteCount(0);
        }
        if (project.getStatus() == null) {
            project.setStatus("SUBMITTED");
        }
    }
}
