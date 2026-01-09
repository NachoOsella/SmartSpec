package com.planai.mapper;

import java.util.Collections;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import com.planai.model.dto.request.CreateProjectRequest;
import com.planai.model.dto.request.UpdateProjectRequest;
import com.planai.model.dto.response.ProjectDetailResponse;
import com.planai.model.dto.response.ProjectResponse;
import com.planai.model.entity.ProjectEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProjectMapper {

    private final ModelMapper modelMapper;
    private final EpicMapper epicMapper;

    public ProjectEntity toEntity(CreateProjectRequest request) {
        return modelMapper.map(request, ProjectEntity.class);
    }

    public void updateEntity(UpdateProjectRequest request, ProjectEntity entity) {
        modelMapper.map(request, entity);
    }

    public ProjectResponse toResponse(ProjectEntity entity) {
        // ProjectResponse usually contains counts which might need manual setting if not in entity
        // Assuming for now simple mapping, service layer might populate counts if they are calculated
        return modelMapper.map(entity, ProjectResponse.class);
    }

    public ProjectDetailResponse toDetailResponse(ProjectEntity entity) {
        ProjectDetailResponse response = modelMapper.map(entity, ProjectDetailResponse.class);
        
        if (entity.getEpics() != null) {
            response.setEpics(entity.getEpics().stream()
                    .map(epicMapper::toResponse)
                    .collect(Collectors.toList()));
        } else {
            response.setEpics(Collections.emptyList());
        }
        
        return response;
    }
}
