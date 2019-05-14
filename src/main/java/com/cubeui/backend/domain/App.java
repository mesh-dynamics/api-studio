package com.cubeui.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;
import java.time.LocalDateTime;

@Entity
@Table(name="apps",
        uniqueConstraints=@UniqueConstraint(columnNames={"name", "customer_id", "instance_id"}),
        indexes = {
                @Index(columnList = "customer_id", name = "app_index"),
                @Index(columnList = "instance_id", name = "app_index")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class App {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @NotEmpty
    @Column(nullable = false, length = 200)
    String name;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    User customerId;

    @ManyToOne
    @JoinColumn(name = "instance_id")
    Instance instanceId;

    @CreationTimestamp
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;
}
