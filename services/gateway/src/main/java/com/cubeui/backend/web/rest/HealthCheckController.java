package com.cubeui.backend.web.rest;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthCheckController {

    private static final Supplier<Map<String,String>> trailersSup = ()->Map.of("Mesh" , "Dynamics" , "City" , "Bangalore");
    private static final String[] trailerHeaderFields = trailersSup.get().keySet().toArray(new String[0]);
    public HealthCheckController() {

    }

    @RequestMapping("")
    public ResponseEntity all(HttpServletRequest request , @RequestBody Optional<String> body , HttpServletResponse response) {
        //Adding protocol , body and trailers for debugging purpose. To see what protocol (Http1.1/Http2) incoming connection is received
        response.setTrailerFields(trailersSup);
        String resp = "All Is Well! "+request.getProtocol() + " body:"+body.orElse(null);
        return ResponseEntity.status(200).header("Trailer" , trailerHeaderFields).body(resp);
    }
}
