package io.cube.agent;

import java.time.Instant;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-17
 * @author Prasad M D
 */
public class FnResponse {
    @JsonProperty("returnVal")
    public String retVal;
    @JsonProperty("recordTimestamp")
    Optional<Instant> timeStamp;

    // for jackson deserialization
    public FnResponse() {
    }

    public FnResponse(String retVal, Optional<Instant> timeStamp) {
        this.retVal = retVal;
        this.timeStamp = timeStamp;
    }
}
