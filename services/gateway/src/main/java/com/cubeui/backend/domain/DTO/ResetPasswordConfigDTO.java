package com.cubeui.backend.domain.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordConfigDTO {

  Integer passwordLength;
  Integer passwordResetDaysMin;
  Integer passwordResetDaysMax;
  Integer oldPasswordsMatchSize;
  Integer passwordResetRequestDays;
}
