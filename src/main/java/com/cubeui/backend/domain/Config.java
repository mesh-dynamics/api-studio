package com.cubeui.backend.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="configs", uniqueConstraints=
@UniqueConstraint(columnNames={"key","customer", "app", "configType", "userId"}))

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Config {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  Long id;

  @NotEmpty
  String customer;

  String app;
  String service;
  String configType;
  String key;
  @Column(length = 5000)
  String value;
  String userId;
  @Column(columnDefinition = "boolean default true")
  Boolean authenticate;
}
