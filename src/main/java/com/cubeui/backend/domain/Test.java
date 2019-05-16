package com.cubeui.backend.domain;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import java.time.LocalDateTime;

@Entity
@Table(name="tests",
        uniqueConstraints=@UniqueConstraint(columnNames={"collection_id", "test_config_name"}),
        indexes = {
                @Index(columnList = "collection_id", name = "test_index")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TypeDef(
        name = "jsonb",
        typeClass = JsonBinaryType.class
)
public class Test {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @NotEmpty
    @Column(name = "test_config_name", nullable = false)
    String testConfigName;

    String description;

    @ManyToOne
    @JoinColumn(name = "collection_id")
    Recording collectionId;

    @ManyToOne
    @JoinColumn(name = "gateway_service_id")
    Service gatewayServiceId;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    String gatewayPathSelection;

    @NotEmpty
    @Column(nullable = false)
    String endpoint;

    @CreationTimestamp
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;
}
