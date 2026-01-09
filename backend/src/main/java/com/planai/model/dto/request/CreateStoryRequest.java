package com.planai.model.dto.request;

import com.planai.model.enums.PriorityEnum;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateStoryRequest {

    @NotBlank(message = "Title must not be blank")
    private String title;

    private String asA;

    private String iWant;

    private String soThat;

    private PriorityEnum priority;
}
