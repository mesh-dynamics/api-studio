package io.cube.agent;

import java.util.regex.Pattern;

import com.google.gson.GsonBuilder;

import net.dongliu.gson.GsonJava8TypeAdapterFactory;

public class GsonBuilderProvider {

    public static GsonBuilder getGsonBuilder() {
        return new GsonBuilder().registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory())
                .registerTypeAdapter(Pattern.class, new GsonPatternDeserializer());

    }
}
