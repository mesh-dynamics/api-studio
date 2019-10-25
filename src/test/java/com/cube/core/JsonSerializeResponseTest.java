package com.cube.core;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;


import com.cube.dao.ReqRespStore;
import com.cube.dao.Response;
import com.cube.ws.Config;

public class JsonSerializeResponseTest {

    public static void main(String[] args) {
        try {
            Config config = new Config();
            ReqRespStore reqRespStore = config.rrstore;
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new Jdk8Module());
            mapper.registerModule(new JavaTimeModule());
            Optional<Response> response = reqRespStore.getResponseOld("72471111-e096-4494-942e-5fa942c07e90");
            String responseAsJson = mapper.writeValueAsString(response);
            Response response1 = mapper.readValue(responseAsJson , Response.class);
            System.out.println(response1.reqId);
            System.out.println(responseAsJson);
        } catch (Exception e) {
             e.printStackTrace();
        }
    }


}
