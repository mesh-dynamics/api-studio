package com.cubeui.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;

@Entity
@Table(name="test_paths",
        uniqueConstraints=@UniqueConstraint(columnNames={"test_id", "path_id"}),
        indexes = {
                @Index(columnList = "test_id", name = "test_path_index")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestPath {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "test_id", nullable = false)
    TestConfig testConfig;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "path_id", nullable = false)
    Path path;
}
