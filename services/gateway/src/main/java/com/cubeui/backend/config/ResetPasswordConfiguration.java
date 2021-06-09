package com.cubeui.backend.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class ResetPasswordConfiguration {
  //New Passwords shouln't match with x(oldPasswordsMatchSize) old passwords
  @Value("${password.old.match.size}")
  private Integer oldPasswordsMatchSize = 3;
  //Min day/s after which the password needs to be reset if the password length is <= passwordLength
  @Value("${password.reset.days.min}")
  private Integer passwordResetDaysMin = 180;
  //day/s after which the password needs to be reset if the password length is > passwordLength
  @Value("${password.reset.days.max}")
  private Integer passwordResetDaysMax = 365;
  //Password length which will define when to reset the password(min, max)
  @Value("${password.length}")
  private Integer passwordLength = 20;
  //The user can send only once the reset password request in the y(passwordResetRequestDays) number of day/s
  @Value("${password.reset.request.days}")
  private Integer passwordResetRequestDays = 1;

}
