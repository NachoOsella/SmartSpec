package com.planai.model.dto.response;

import com.planai.model.enums.StatusEnum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    private Long id;

    private String title;

    private String description;

    private StatusEnum status;

    private Integer estimatedHours;

    private Integer orderIndex;
}
