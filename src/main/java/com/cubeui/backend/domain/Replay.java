package com.cubeui.backend.domain;

import com.cubeui.backend.domain.enums.ReplayStatus;
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
@Table(name="replay",
        uniqueConstraints=@UniqueConstraint(columnNames={"test_id", "collection_id", "replay_name"}),
        indexes = {
                @Index(columnList = "test_id", name = "replay_index"),
                @Index(columnList = "collection_id", name = "replay_index"),
                @Index(columnList = "status", name = "replay_index")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TypeDef(
        name = "jsonb",
        typeClass = JsonBinaryType.class
)
public class Replay {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @NotEmpty
    @Column(name = "replay_name", nullable = false, length = 200)
    String replayName;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "test_id")
    TestConfig testConfig;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "collection_id")
    Recording recording;

    @NotEmpty
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ReplayStatus status;

    @Column
    int reqCount;

    @Column
    int reqSent;

    @Column
    int reqFailed;

    @Type(type = "jsonb")
    @Column(nullable = false, columnDefinition = "jsonb")
    String analysis;

    @CreationTimestamp
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;

    LocalDateTime completedAt;

    Double sampleRate;
}
