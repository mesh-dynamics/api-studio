package com.cubeui.backend.domain;

import com.cubeui.backend.domain.enums.RecordingStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
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
@Table(name="recording",
        uniqueConstraints=@UniqueConstraint(columnNames={"app_id", "instance_id", "collection_name"}),
        indexes = {
                @Index(columnList = "app_id", name = "recording_index"),
                @Index(columnList = "instance_id", name = "recording_index"),
                @Index(columnList = "status", name = "recording_index")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recording {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "app_id")
    App app;

    //VNT: Many to many, not sure right now
    @OneToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "instance_id")
    Instance instance;

    @NotEmpty
    @Column(name = "collection_name", nullable = false, length = 200)
    String collectionName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    RecordingStatus status;

    @CreationTimestamp
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    LocalDateTime createdAt;

    @UpdateTimestamp
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    LocalDateTime updatedAt;

    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
    LocalDateTime completedAt;
}
