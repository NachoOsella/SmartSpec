package com.planai.model.dto.response;

import com.planai.model.enums.MessageRoleEnum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    private Long id;

    private MessageRoleEnum role;

    private String content;
}
