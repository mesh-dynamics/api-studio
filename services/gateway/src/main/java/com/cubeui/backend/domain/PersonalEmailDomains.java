package com.cubeui.backend.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="personal_email_domains",
        uniqueConstraints=@UniqueConstraint(columnNames={"domain"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonalEmailDomains {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  Long id;

  @NotEmpty
  String domain;
}
