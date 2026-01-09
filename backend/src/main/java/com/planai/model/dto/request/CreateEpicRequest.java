package com.planai.model.dto.request;

import com.planai.model.enums.PriorityEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class CreateEpicRequest {

    @NotBlank(message = "Title must not be blank")
    @Size(max = 150)
    private String title;

    @Size(max = 1000)
    private String description;

    private PriorityEnum priority;
}
