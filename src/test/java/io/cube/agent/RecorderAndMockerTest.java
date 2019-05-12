package io.cube.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-10
 * @author Prasad M D
 */
class RecorderAndMockerTest {

    enum Mode {
        Regular,
        Record,
        Mock
    }

    static final String CUSTID = "Test";
    static final String APPID = "Test";
    static final String INSTANCEID = "Test";
    static final String SERVICE = "ProdDiscount";
    static Mode mode;


    static Recorder recorder = new SimpleRecorder();
    static Mocker mocker = new SimpleMocker();
    static Map<String, Double> disc1 = new HashMap<String, Double>();
    static Map<String, Double> disc2 = new HashMap<String, Double>();
    static Map<String, Double> disc3 = new HashMap<String, Double>();
    static {
        disc1.put("Prod1", 0.50); disc1.put("Prod2", 0.50); disc1.put("Prod3", 0.5); disc1.put("Prod4", 0.5);
        disc2.put("Prod1", 0.25); disc2.put("Prod2", 0.25); disc2.put("Prod3", 0.25); disc2.put("Prod4", 0.25);
        disc3.put("Prod1", 0.10); disc3.put("Prod2", 0.20); disc3.put("Prod3", 0.30); disc3.put("Prod4", 0.4);
    }
    static ProdDiscount prodDiscount1 = new ProdDiscount(disc1, "Half");
    static ProdDiscount prodDiscount2 = new ProdDiscount(disc2, "Quarter");
    static ProdDiscount prodDiscount3 = new ProdDiscount(disc3, "Varied");

    static Optional<String> traceid = Optional.empty();
    static Optional<String> spanid = Optional.empty();
    static Optional<String> parentSpanid = Optional.empty();
    static Optional<Instant> timestamp = Optional.empty();

    static class ProdDiscount {

        public ProdDiscount(Map<String, Double> discountMap, String name) {
            this.discountMap = discountMap;
            this.name = name;
        }

        Map<String, Double> discountMap;
        String name;

        static class ProdPrice {
            String productId;
            float price;
        }


        double discountedPrice(String productId, float price) {

            if (dpk == null) {
                Method function = new Object(){}.getClass().getEnclosingMethod();
                dpk = new FnKey(CUSTID, APPID, INSTANCEID, SERVICE, function);
            }

            if (mode == Mode.Mock) {
                return (float) mocker.mock(dpk, traceid, spanid, parentSpanid,
                        timestamp, productId, price);
            }

            double discount = Optional.ofNullable(discountMap.get(productId)).orElse(Double.valueOf(0));

            double ret = price*(1-discount);
            if (mode == Mode.Record) {
                RecorderAndMockerTest.recorder.record(dpk, traceid, spanid,
                        parentSpanid,
                        Double.valueOf(ret), productId, price);
            }
            return ret;
        }

        double discountedPrice(ProdPrice pp) {
            if (dpk2 == null) {
                Method function = new Object(){}.getClass().getEnclosingMethod();
                dpk2 = new FnKey(CUSTID, APPID, INSTANCEID, SERVICE, function);
            }

            if (mode == Mode.Mock) {
                return (float) mocker.mock(dpk, traceid, spanid, parentSpanid,
                        timestamp, pp);
            }

            double ret = discountedPrice(pp.productId, pp.price);
            if (mode == Mode.Record) {
                RecorderAndMockerTest.recorder.record(dpk2, traceid, spanid,
                        parentSpanid,
                        ret, pp);
            }
            return ret;
        }

        String getPromoName() {

            if (gpnk == null) {
                Method function = new Object(){}.getClass().getEnclosingMethod();
                gpnk = new FnKey(CUSTID, APPID, INSTANCEID, SERVICE, function);
            }

            if (mode == Mode.Mock) {
                return (String) mocker.mock(gpnk, traceid, spanid,
                        parentSpanid,
                        timestamp);
            }

            String ret = name;
            if (mode == Mode.Record) {
                RecorderAndMockerTest.recorder.record(gpnk, traceid, spanid,
                        parentSpanid,
                        ret);
            }
            return ret;
        }

        static FnKey dpk; // for discountedPrice
        static FnKey dpk2;
        static FnKey gpnk; // for getPromoName
    }




    @Test
    void testFnNoArgs() {

        // start recording

        // set mode to record
        mode = Mode.Record;
        traceid = Optional.of("testFnNoArgst1");

        // call fn
        String ret1 = prodDiscount1.getPromoName();
        String ret2 = prodDiscount2.getPromoName();
        String ret3 = prodDiscount3.getPromoName();


        // stop recording
        // start replay

        // set mode to mock
        mode = Mode.Mock;

        // call fn
        String replayRet1 = prodDiscount1.getPromoName();
        String replayRet2 = prodDiscount2.getPromoName();
        String replayRet3 = prodDiscount3.getPromoName();

        // stop replay

        // compare values
        assertEquals(ret1, replayRet1);
        assertEquals(ret2, replayRet2);
        assertEquals(ret3, replayRet3);
    }
}