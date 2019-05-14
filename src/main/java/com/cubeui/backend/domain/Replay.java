package com.cubeui.backend.domain;

import com.cubeui.backend.domain.enums.ReplayStatus;
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
@Table(name="replay",
        uniqueConstraints=@UniqueConstraint(columnNames={"test_id", "replay_name"}),
        indexes = {
                @Index(columnList = "test_id", name = "replay_index"),
                @Index(columnList = "status", name = "replay_index")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Replay {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @NotEmpty
    @Column(name = "replay_name", nullable = false, length = 200)
    String replayName;

    @ManyToOne
    @JoinColumn(name = "test_id")
    Test testId;

    @NotEmpty
    @Column(nullable = false)
    ReplayStatus status;

    @Column
    int reqCount;

    @Column
    int reqSent;

    @Column
    int reqFailed;

    //VNT: unknown JSON
    @Column
    String analysis;

    @CreationTimestamp
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;

    LocalDateTime completedAt;

    //VNT: unknown type REAL
    Double sampleRate;
}
