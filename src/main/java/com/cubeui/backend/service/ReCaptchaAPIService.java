package com.cubeui.backend.service;

import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.http.ResponseEntity.status;

import com.cubeui.backend.service.dto.GoogleRecaptchaResponseDTO;
import com.cubeui.backend.service.exception.InvalidReCaptchaException;
import com.cubeui.backend.service.exception.ReCaptchaInvalidException;
import java.net.URI;
import java.util.regex.Pattern;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service
@Transactional
@Log
public class ReCaptchaAPIService {

    @Value("${external.recaptcha.secret-key}")
    private String reCaptchaSecret;

    private RestTemplate restTemplate;

    public ReCaptchaAPIService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    private static Pattern RESPONSE_PATTERN = Pattern.compile("[A-Za-z0-9_-]+");

    public void processResponse(String response, String clientIPAddress) {
        if(!responseSanityCheck(response)) {
            throw new InvalidReCaptchaException("Response contains invalid characters");
        }

        URI verifyUri = URI.create(String.format(
            "https://www.google.com/recaptcha/api/siteverify?secret=%s&response=%s&remoteip=%s",
                reCaptchaSecret, response, clientIPAddress));

        GoogleRecaptchaResponseDTO googleResponse = restTemplate.getForObject(verifyUri, GoogleRecaptchaResponseDTO.class);

        if(!googleResponse.isSuccess()) {
            throw new ReCaptchaInvalidException("reCaptcha was not successfully validated");
        }
    }

    private boolean responseSanityCheck(String response) {
        return StringUtils.hasLength(response) && RESPONSE_PATTERN.matcher(response).matches();
    }

}
