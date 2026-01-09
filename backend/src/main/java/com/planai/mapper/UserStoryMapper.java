package com.planai.mapper;

import java.util.Collections;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import com.planai.model.dto.request.CreateStoryRequest;
import com.planai.model.dto.response.UserStoryResponse;
import com.planai.model.entity.UserStoryEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserStoryMapper {

    private final ModelMapper modelMapper;
    private final TaskMapper taskMapper;

    public UserStoryEntity toEntity(CreateStoryRequest request) {
        return modelMapper.map(request, UserStoryEntity.class);
    }

    public UserStoryResponse toResponse(UserStoryEntity entity) {
        UserStoryResponse response = modelMapper.map(entity, UserStoryResponse.class);

        if (entity.getTasks() != null) {
            response.setTasks(entity.getTasks().stream().map(taskMapper::toResponse).collect(Collectors.toList()));
        } else {
            response.setTasks(Collections.emptyList());
        }

        return response;
    }
}
