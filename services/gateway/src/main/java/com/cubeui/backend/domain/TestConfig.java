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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.validation.constraints.NotEmpty;
import java.time.LocalDateTime;

@Entity
@Table(name="test_config",
        uniqueConstraints=@UniqueConstraint(columnNames={"app_id", "test_config_name"}),
        indexes = {
                @Index(columnList = "app_id", name = "test_index")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TypeDef(
        name = "jsonb",
        typeClass = JsonBinaryType.class
)
public class TestConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @NotEmpty
    @Column(name = "test_config_name", nullable = false)
    String testConfigName;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "app_id")
    App app;

    String description;

    String gatewayReqSelection;

    @Column
    int maxRunTimeMin;

    String emailId;

    String slackId;

//    @NotEmpty
//    @Column(nullable = false)
//    String endpoint;

    @CreationTimestamp
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    LocalDateTime createdAt;

    @UpdateTimestamp
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    LocalDateTime updatedAt;

    String tag;

    String dynamicInjectionConfigVersion;
}
