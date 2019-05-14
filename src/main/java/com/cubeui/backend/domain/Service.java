package com.cubeui.backend.domain;

import com.cubeui.backend.domain.enums.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="services",
        uniqueConstraints=@UniqueConstraint(columnNames={"name", "app_id"}),
        indexes = {
                @Index(columnList = "app_id", name = "service_index"),
                @Index(columnList = "type", name = "service_index")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @Column(nullable = false)
    String name;

    @Column(nullable = false)
    ServiceType type;

    @ManyToOne
    @JoinColumn(name = "app_id")
    App appId;

    @CreationTimestamp
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;
}
