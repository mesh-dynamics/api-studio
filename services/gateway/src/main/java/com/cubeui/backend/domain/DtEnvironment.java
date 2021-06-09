package com.cubeui.backend.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name="devtool_environments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtEnvironment {

  public DtEnvironment(String name) {
    this.name = name;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column
  Long id;

  @JsonIgnore
  @ManyToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "user_id", nullable = false)
  User user;

  /**TODO
   * change nullable to false once deployed in each cluster
   */
  @JsonIgnore
  @ManyToOne
  @OnDelete(action = OnDeleteAction.CASCADE)
  @JoinColumn(name = "app_id", nullable = true)
  App app;

  @Column
  @NotNull
  String name;

  @Column(columnDefinition = "boolean default false")
  Boolean global;

  @OneToMany(mappedBy = "environment", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @OnDelete(action = OnDeleteAction.CASCADE)
  @LazyCollection(LazyCollectionOption.FALSE)
  List<DtEnvVar> vars;

  @OneToMany(mappedBy = "environment", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @OnDelete(action = OnDeleteAction.CASCADE)
  @LazyCollection(LazyCollectionOption.FALSE)
  List<DtEnvServiceHost> dtEnvServiceHosts;

  @OneToMany(mappedBy = "environment", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
  @OnDelete(action = OnDeleteAction.CASCADE)
  @LazyCollection(LazyCollectionOption.FALSE)
  List<DtEnvServiceCollection> dtEnvServiceCollections;
}