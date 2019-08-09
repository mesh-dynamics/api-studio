package com.cubeui.backend.domain;

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

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "gateway_service_id")
    Service gatewayService;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    String gatewayReqSelection;

    @Column
    int maxRunTimeMin;

    String emailId;

    String slackId;

//    @NotEmpty
//    @Column(nullable = false)
//    String endpoint;

    @CreationTimestamp
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;
}
