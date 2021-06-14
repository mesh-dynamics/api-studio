package com.cubeio.logcollector.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

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

  @Column
  @NotNull
  private Long userId;
}
