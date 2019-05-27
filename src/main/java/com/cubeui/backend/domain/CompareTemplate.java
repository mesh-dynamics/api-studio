package com.cubeui.backend.domain;

import com.cubeui.backend.domain.enums.TemplateType;
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
@Table(name="compare_template",
        uniqueConstraints=@UniqueConstraint(columnNames={"app_id", "service_id", "type", "path"}),
        indexes = {
                @Index(columnList = "app_id", name = "compare_template_index"),
                @Index(columnList = "service_id", name = "compare_template_index"),
                @Index(columnList = "type", name = "compare_template_index"),
                @Index(columnList = "path", name = "compare_template_index")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TypeDef(
        name = "jsonb",
        typeClass = JsonBinaryType.class
)
public class CompareTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "app_id")
    App app;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "service_id")
    Service service;

    @NotEmpty
    @Column(nullable = false)
    String path;

    @NotEmpty
    @Type(type = "jsonb")
    @Column(nullable = false, columnDefinition = "jsonb")
    String template;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    TemplateType type;

    @CreationTimestamp
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;
}
