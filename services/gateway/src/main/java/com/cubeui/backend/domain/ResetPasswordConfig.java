package com.cubeui.backend.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name="reset_password_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordConfig {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  Long id;

  @OneToOne
  @JoinColumn(name = "customer_id")
  @OnDelete(action = OnDeleteAction.CASCADE)
  Customer customer;

  Integer passwordLength;
  Integer passwordResetDaysMin;
  Integer passwordResetDaysMax;
  Integer oldPasswordsMatchSize;
  Integer passwordResetRequestDays;

}
