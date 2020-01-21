package com.cubeui.backend.service.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

// from https://www.baeldung.com/spring-security-registration-captcha
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class GoogleRecaptchaResponseDTO {

  @JsonProperty("success")
  private boolean success;

  @JsonProperty("challenge_ts")
  private String challengeTs;

  @JsonProperty("hostname")
  private String hostname;

  @JsonProperty("error-codes")
  private String[] errorCodes;
}