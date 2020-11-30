package com.cubeui.backend.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name="app_files")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppFile {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  Long id;

  @Column(nullable = false)
  String fileName;

  @Column(nullable = false)
  String fileType;

  @Lob
  @Column(name="image")
  private byte[] data;

  @OneToOne
  @JoinColumn(name = "app_id")
  @OnDelete(action = OnDeleteAction.CASCADE)
  App app;
}
