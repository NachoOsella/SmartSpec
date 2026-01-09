package com.planai.mapper;

import java.util.Collections;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import com.planai.model.dto.response.ConversationResponse;
import com.planai.model.dto.response.MessageResponse;
import com.planai.model.entity.ConversationEntity;
import com.planai.model.entity.MessageEntity;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ConversationMapper {

    private final ModelMapper modelMapper;

    public MessageResponse toMessageResponse(MessageEntity entity) {
        return modelMapper.map(entity, MessageResponse.class);
    }

    public ConversationResponse toResponse(ConversationEntity entity) {
        ConversationResponse response = modelMapper.map(entity, ConversationResponse.class);
        
        if (entity.getMessages() != null) {
            response.setMessages(entity.getMessages().stream()
                    .map(this::toMessageResponse)
                    .collect(Collectors.toList()));
        } else {
            response.setMessages(Collections.emptyList());
        }
        
        return response;
    }
}
