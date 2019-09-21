package io.cube.agent;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.dongliu.gson.GsonJava8TypeAdapterFactory;


/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-06
 * @author Prasad M D
 */
public class SimpleHttpRecorder extends AbstractGsonSerializeRecorder {

    private CubeClient cubeClient;

    public SimpleHttpRecorder(Gson gson) {
        super(gson);
        this.cubeClient = new CubeClient(jsonMapper);
    }

    @Override
    public boolean record(Optional<Event> event) {
        Optional<String> cubeResponse = cubeClient.storeEvent(event);
        return true;
    }


}
