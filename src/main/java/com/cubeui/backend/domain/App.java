package com.cubeui.backend.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
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
@Table(name="apps",
        uniqueConstraints=@UniqueConstraint(columnNames={"customer_id", "name"}),
        indexes = {
                @Index(columnList = "customer_id", name = "app_index")
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
    @OnDelete(action = OnDeleteAction.CASCADE)
    Customer customer;

    @CreationTimestamp
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    LocalDateTime createdAt;

    @UpdateTimestamp
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    LocalDateTime updatedAt;

    @Column(length = 200)
    String displayName;

    @Column(length = 200)
    String userId;
}
