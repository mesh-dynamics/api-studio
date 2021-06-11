package com.cubeui.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Entity
@Table(name="test_intermediate_services",
        uniqueConstraints=@UniqueConstraint(columnNames={"test_id", "service_id"}),
        indexes = {
                @Index(columnList = "test_id", name = "intermediate_service_index")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestIntermediateService {

    //Need id to save
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "test_id")
    TestConfig testConfig;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "service_id")
    Service service;

}