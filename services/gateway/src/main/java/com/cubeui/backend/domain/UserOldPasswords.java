package com.cubeui.backend.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name="user_old_passwords")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOldPasswords {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  Long id;

  @ManyToOne
  @JoinColumn(name = "user_id")
  @OnDelete(action = OnDeleteAction.CASCADE)
  User user;

  @JsonIgnore
  @NotEmpty
  String password;

  @UpdateTimestamp
  @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")
  LocalDateTime updatedAt;

}
