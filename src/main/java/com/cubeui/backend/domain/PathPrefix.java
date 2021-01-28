package com.cubeui.backend.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name="path_prefixes", uniqueConstraints=@UniqueConstraint(columnNames={"prefix", "service_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PathPrefix {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  @Column
  @NotNull
  private String prefix;

  @ManyToOne
  @JoinColumn(name = "service_id")
  @OnDelete(action = OnDeleteAction.CASCADE)
  Service service;

}
