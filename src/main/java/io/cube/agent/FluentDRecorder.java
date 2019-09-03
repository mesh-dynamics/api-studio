package io.cube.agent;

import java.util.Optional;

import com.google.gson.Gson;

public class FluentDRecorder extends AbstractGsonSerializeRecorder {

    public FluentDRecorder(Gson gson) {
        super(gson);
    }


    @Override
    public boolean record(FnReqResponse fnReqResponse) {
        try {
            // TODO might wanna explore java fluent logger
            // https://github.com/fluent/fluent-logger-java
            String jsonSerialized = jsonMapper.writeValueAsString(fnReqResponse);
            // The prefix will be a part of the fluentd parse regex
            LOGGER.info("Cube FnReqResp Event:" + jsonSerialized);
            return true;
        } catch (Exception e) {
            LOGGER.error("Unable to serialize Function Req Response Object :: " + e.getMessage());
            return false;
        }

    }
}
