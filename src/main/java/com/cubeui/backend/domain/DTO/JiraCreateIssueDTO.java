package com.cubeui.backend.domain.DTO;

import javax.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class JiraCreateIssueDTO {
  @NotEmpty
  private String summary;

  @NotEmpty
  private String description;

  @NotEmpty
  private int issueTypeId;

  @NotEmpty
  private int projectId;

  private String replayId;

  private String requestId;

  private String apiPath;

  private String jsonPath;
}
