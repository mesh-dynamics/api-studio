package com.cubeui.backend.domain;

import com.cubeui.backend.domain.enums.InstanceName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="instances")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Instance {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    InstanceName name;

    @Column(nullable = false)
    String gatewayEndpoint;

    @CreationTimestamp
    LocalDateTime createdAt;

}
