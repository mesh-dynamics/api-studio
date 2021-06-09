package com.cubeui.backend.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name="jira_user_credentials")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JiraUserCredentials {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  Long id;

  @OneToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "user_id", nullable = false)
  User user;

  @Column(nullable = false)
  String userName;

  @Column(nullable = false)
  String APIKey;

  @Column(nullable = false)
  String jiraBaseURL;
}
