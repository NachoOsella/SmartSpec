package com.planai.mapper;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import com.planai.model.dto.request.CreateTaskRequest;
import com.planai.model.dto.response.TaskResponse;
import com.planai.model.entity.TaskEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TaskMapper {

    private final ModelMapper modelMapper;

    public TaskEntity toEntity(CreateTaskRequest request) {
        return modelMapper.map(request, TaskEntity.class);
    }

    public TaskResponse toResponse(TaskEntity entity) {
        return modelMapper.map(entity, TaskResponse.class);
    }
}
