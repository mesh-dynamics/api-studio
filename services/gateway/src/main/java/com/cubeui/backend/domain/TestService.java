package com.cubeui.backend.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name="test_services",
    uniqueConstraints=@UniqueConstraint(columnNames={"test_id", "service_id"}),
    indexes = {
        @Index(columnList = "test_id", name = "test_service_index")
    })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestService {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  Long id;

  @ManyToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "test_id", nullable = false)
  TestConfig testConfig;

  @ManyToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "service_id", nullable = false)
  Service service;
}