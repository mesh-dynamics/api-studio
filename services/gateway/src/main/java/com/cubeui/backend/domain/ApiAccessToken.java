package com.cubeui.backend.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name="api_access_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiAccessToken {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  @Column
  @NotNull
  private String token;

  @OneToOne
  @JoinColumn(name = "user_Id")
  @OnDelete(action = OnDeleteAction.CASCADE)
  private User user;
}
