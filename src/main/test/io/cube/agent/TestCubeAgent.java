package io.cube.agent;

import java.time.Instant;
import java.util.Optional;

public class TestCubeAgent {


    public static void main(String[] args) {
        SimpleRecorder recorder = new SimpleRecorder();
        SimpleMocker mocker = new SimpleMocker();
        try {
            Class classzz = CubeClient.class;
            FnKey fnKey = new FnKey("ravivj", "movieinfo", "dev", "movieinfo"
                    , classzz.getMethod("getMockResponse" , FnReqResponse.class));
//            recorder.record(fnKey , Optional.of("trace_3"), Optional.of("span_3") , Optional.of("parentSpan_3")
//                    , "Simple Response 3" , new Object[] {"1" , "5"});

//            System.out.println(Instant.ofEpochSecond(1557489786L).toString());
            Object returnVal = mocker.mock(fnKey, Optional.of("trace_1") , Optional.empty() , Optional.empty() ,
                    Optional.of(Instant.ofEpochSecond(1557489555)), new Object[] {"1" , "5"});
            System.out.println(returnVal.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
