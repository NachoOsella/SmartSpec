package com.planai.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.planai.mapper.ProjectMapper;
import com.planai.model.dto.request.CreateProjectRequest;
import com.planai.model.dto.request.UpdateProjectRequest;
import com.planai.model.dto.response.ProjectDetailResponse;
import com.planai.model.dto.response.ProjectResponse;
import com.planai.model.entity.ProjectEntity;
import com.planai.repository.ProjectRepository;
import com.planai.service.ProjectService;

import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link ProjectService}.
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;

    public ProjectServiceImpl(ProjectRepository projectRepository, ProjectMapper projectMapper) {
        this.projectRepository = projectRepository;
        this.projectMapper = projectMapper;
    }

    @Override
    public List<ProjectResponse> getAllProjects() {
        List<ProjectEntity> projectEntities = projectRepository.findAll();
        return projectEntities.stream().map(projectMapper::toResponse).toList();
    }

    @Override
    public ProjectDetailResponse getProjectDetail(Long projectId) {
        ProjectEntity projectEntity = projectRepository
                .findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project with ID " + projectId + " does not exist."));
        return projectMapper.toDetailResponse(projectEntity);
    }

    @Override
    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        ProjectEntity projectEntity = projectMapper.toEntity(request);
        ProjectEntity savedEntity = projectRepository.save(projectEntity);
        return projectMapper.toResponse(savedEntity);
    }

    @Override
    @Transactional
    public ProjectResponse updateProject(Long projectId, UpdateProjectRequest request) {
        ProjectEntity projectEntity = projectRepository
                .findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project with ID " + projectId + " does not exist."));
        projectMapper.updateEntity(request, projectEntity);
        ProjectEntity updatedEntity = projectRepository.save(projectEntity);
        return projectMapper.toResponse(updatedEntity);
    }

    @Override
    @Transactional
    public void deleteProject(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new IllegalArgumentException("Project with ID " + projectId + " does not exist.");
        }
        projectRepository.deleteById(projectId);
    }
}
