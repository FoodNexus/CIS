package com.civicplatform.mapper;

import com.civicplatform.dto.request.ProjectFundingRequest;
import com.civicplatform.dto.response.ProjectFundingResponse;
import com.civicplatform.entity.ProjectFunding;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProjectFundingMapper {

    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "projectTitle", source = "project.title")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "userName", source = "user.userName")
    @Mapping(target = "userEmail", source = "user.email")
    ProjectFundingResponse toResponse(ProjectFunding projectFunding);
    
    List<ProjectFundingResponse> toResponseList(List<ProjectFunding> projectFundings);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fundDate", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "user", ignore = true)
    ProjectFunding toEntity(ProjectFundingRequest projectFundingRequest);
    
    void updateEntity(ProjectFundingRequest projectFundingRequest, @MappingTarget ProjectFunding projectFunding);
}
