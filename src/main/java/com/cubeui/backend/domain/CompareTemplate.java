package com.cubeui.backend.domain;

import com.cubeui.backend.domain.enums.TemplateType;
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
@Table(name="compare_template",
        uniqueConstraints=@UniqueConstraint(columnNames={"test_id", "type", "path"}),
        indexes = {
                @Index(columnList = "test_id", name = "compare_template_index"),
                @Index(columnList = "type", name = "compare_template_index"),
                @Index(columnList = "path", name = "compare_template_index")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompareTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @ManyToOne
    @JoinColumn(name = "test_id")
    Test testId;

    @NotEmpty
    @Column(nullable = false)
    String path;

    //VNT: unknown JSON
    @NotEmpty
    @Column(nullable = false)
    String template;

    @NotEmpty
    @Column(nullable = false)
    TemplateType type;

    @CreationTimestamp
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;
}
