package com.cubeui.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name="jira_issue_details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraIssueDetails {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  Long id;

  @JsonIgnore
  @ManyToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "user_id", nullable = false)
  User user;

  @Column
  @NotNull
  String replayId;

  @Column
  @NotNull
  String apiPath;

  @Column
  @NotNull
  String requestId;

  @Column
  @NotNull
  String jsonPath;

  @Column
  @NotNull
  String issueId;

  @Column
  @NotNull
  String issueKey;

  @Column
  @NotNull
  String issueUrl;

}
