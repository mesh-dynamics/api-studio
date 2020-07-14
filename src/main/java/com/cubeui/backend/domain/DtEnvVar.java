package com.cubeui.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name="devtool_environment_variables")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtEnvVar {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  Long id;

  @JsonIgnore
  @ManyToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "environment", nullable = false)
  DtEnvironment environment;

  @Column
  @NotNull
  String key;

  @Column
  @NotNull
  String value;
}
