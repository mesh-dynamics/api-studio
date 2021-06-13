/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
