package com.cubeui.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import java.time.LocalDateTime;

@Entity
@Table(name="services",
        uniqueConstraints=@UniqueConstraint(columnNames={"name", "app_id"}),
        indexes = {
                @Index(columnList = "app_id", name = "service_index")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @NotEmpty
    @Column(nullable = false, length = 200)
    String name;

    @ManyToOne
    @JoinColumn(name = "app_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    App app;

    @CreationTimestamp
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;
}
