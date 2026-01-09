package com.planai.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {

    private Long id;

    private String name;

    private String description;

    private Long epicsCount;

    private Long storiesCount;

    private Long tasksCount;

    private Long conversationsCount;
}
