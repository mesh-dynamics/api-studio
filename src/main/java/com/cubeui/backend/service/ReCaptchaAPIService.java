package com.cubeui.backend.service;

import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;

import com.cubeui.backend.service.dto.GoogleRecaptchaResponseDTO;
import com.cubeui.backend.service.exception.InvalidReCaptchaException;
import com.cubeui.backend.service.exception.ReCaptchaInvalidException;
import java.net.URI;
import java.util.regex.Pattern;
import lombok.extern.java.Log;
import lombok.extern.log4j.Log4j;
import lombok.extern.slf4j.Slf4j;
import org.jboss.logging.Logger.Level;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service
@Transactional
@Slf4j
public class ReCaptchaAPIService {

    @Value("${external.recaptcha.secret-key}")
    private String reCaptchaSecret;

    @Value("${external.recaptcha.base-url}")
    private String reCaptchaBaseURL;

    private RestTemplate restTemplate;

    public ReCaptchaAPIService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    private static Pattern RESPONSE_PATTERN = Pattern.compile("[A-Za-z0-9_-]+");

    public void processResponse(String response, String clientIPAddress) {
        if(!responseSanityCheck(response)) {
            log.error("Response contains invalid characters");
            throw new InvalidReCaptchaException("Response contains invalid characters");
        }

        URI verifyUri = URI.create(String.format("%s?secret=%s&response=%s&remoteip=%s",
                reCaptchaBaseURL, reCaptchaSecret, response, clientIPAddress));

        GoogleRecaptchaResponseDTO googleResponse
            = restTemplate.getForObject(verifyUri, GoogleRecaptchaResponseDTO.class);
        if(!googleResponse.isSuccess()) {
            log.error("reCaptcha validation error occurred: " + googleResponse.getErrorCodes());
            throw new ReCaptchaInvalidException("reCaptcha was not successfully validated");
        }
        log.info("ReCaptcha validated successfully");
    }

    private boolean responseSanityCheck(String response) {
        return StringUtils.hasLength(response) && RESPONSE_PATTERN.matcher(response).matches();
    }

}
