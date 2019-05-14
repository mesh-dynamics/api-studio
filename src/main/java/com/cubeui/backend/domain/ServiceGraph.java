package com.cubeui.backend.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name="service_graph",
        indexes = {@Index(columnList = "app_id", name = "service_graph_index")})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceGraph {

    //Need id to save
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    @ManyToOne
    @JoinColumn(name = "app_id")
    App appId;

    //unknown JSON type
    @Column(nullable = false)
    String serviceGraph;

}
