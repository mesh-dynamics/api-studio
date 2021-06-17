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
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name="app_files")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppFile {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  Long id;

  @Column(nullable = false)
  String fileName;

  @Column(nullable = false)
  String fileType;

  @Lob
  @Column(name="image")
  private byte[] data;

  @OneToOne
  @JoinColumn(name = "app_id")
  @OnDelete(action = OnDeleteAction.CASCADE)
  App app;
}
