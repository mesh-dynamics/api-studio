package io.cube.agent;

import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.md.dao.Event;
import io.md.utils.FnKey;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;

public class TestCubeAgent {


    public static void main(String[] args) {
        Gson gson = new GsonBuilder().registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory())
                .registerTypeAdapter(Pattern.class, new GsonPatternDeserializer()).create();
        try {
            ConsoleRecorder recorder = ConsoleRecorder.getInstance();
            //SimpleMocker mocker = new SimpleMocker(gson);
            Class classzz = CubeClient.class;
            FnKey fnKey = new FnKey("ravivj", "movieinfo", "dev", "movieinfo"
                    , classzz.getMethod("getMockResponseEvent", Event.class));
//            recorder.record(fnKey , Optional.of("trace_3"), Optional.of("span_3") , Optional.of("parentSpan_3")
//                    , "Simple Response 3" , new Object[] {"1" , "5"});

//            System.out.println(Instant.ofEpochSecond(1557489786L).toString());

            /*Object returnVal = mocker.mock(fnKey, Optional.of("trace_1") , Optional.empty() , Optional.empty() ,
                    Optional.of(Instant.ofEpochSecond(1557489555)), Optional.empty(), new Object[] {"1" , "5"});
            System.out.println(returnVal.toString());*/
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}