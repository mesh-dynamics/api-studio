package com.cubeui.backend.web.rest;

import com.cubeui.backend.config.ResetPasswordConfiguration;
import com.cubeui.backend.domain.Customer;
import com.cubeui.backend.domain.DTO.ResetPasswordConfigDTO;
import com.cubeui.backend.domain.ResetPasswordConfig;
import com.cubeui.backend.domain.User;
import com.cubeui.backend.repository.ResetPasswordConfigRepository;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/api/reset_config")
public class ResetPasswordConfigController {
  @Autowired
  private ResetPasswordConfigRepository resetPasswordConfigRepository;
  @Autowired
  private ResetPasswordConfiguration resetPasswordConfiguration;

  @PostMapping("")
  public ResponseEntity save(@RequestBody ResetPasswordConfigDTO resetPasswordConfigDTO,
      Authentication authentication) {
    User user = (User) authentication.getPrincipal();
    Customer customer = user.getCustomer();
    resetPasswordConfigRepository.save(ResetPasswordConfig.builder()
        .customer(customer)
        .oldPasswordsMatchSize(
            Optional.ofNullable(resetPasswordConfigDTO.getOldPasswordsMatchSize())
                .orElse(resetPasswordConfiguration.getOldPasswordsMatchSize()))
        .passwordLength(
            Optional.ofNullable(resetPasswordConfigDTO.getPasswordLength())
                .orElse(resetPasswordConfiguration.getPasswordLength()))
        .passwordResetDaysMin(
            Optional.ofNullable(resetPasswordConfigDTO.getPasswordResetDaysMin())
                .orElse(resetPasswordConfiguration.getPasswordResetDaysMin()))
        .passwordResetDaysMax(
            Optional.ofNullable(resetPasswordConfigDTO.getPasswordResetDaysMax())
                .orElse(resetPasswordConfiguration.getPasswordResetDaysMax()))
        .passwordResetRequestDays(
            Optional.ofNullable(resetPasswordConfigDTO.getPasswordResetRequestDays())
                .orElse(resetPasswordConfiguration.getPasswordResetRequestDays()))
        .build());
    return ResponseEntity.ok().build();
  }

}
