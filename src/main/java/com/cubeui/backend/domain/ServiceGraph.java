package com.cubeui.backend.domain;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import javax.validation.constraints.NotEmpty;

@Entity
@Table(name="service_graph",
        indexes = {@Index(columnList = "app_id", name = "service_graph_index")})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TypeDef(
        name = "jsonb",
        typeClass = JsonBinaryType.class
)
public class ServiceGraph {

    //Need id to save
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @OneToOne
    @JoinColumn(name = "app_id")
    App appId;

    @NotEmpty
    @Type(type = "jsonb")
    @Column(nullable = false, columnDefinition = "jsonb")
    String serviceGraph;

}
