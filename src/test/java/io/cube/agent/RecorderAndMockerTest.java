package io.cube.agent;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-10
 * @author Prasad M D
 */
class RecorderAndMockerTest {

    private static final Logger LOGGER = LogManager.getLogger(RecorderAndMockerTest.class);


    private final ObjectMapper jsonMapper;
    private final CubeClient cubeClient;
    private final Random randomGen;

    enum Mode {
        Regular,
        Record,
        Mock
    }

    static final String CUSTID = "Test";
    static final String APPID = "Test";
    static final String INSTANCEID = "Test";
    static final String SERVICE = "ProdDiscount";
    private static final String COLLECTION = "RecorderAndMockTest";
    private static final String ENDPOINT = "Dummy";

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

            public ProdPrice(String productId, float price) {
                this.productId = productId;
                this.price = price;
            }
        }


        double discountedPrice(String productId, float price) {

            if (dpk == null) {
                Method function = new Object(){}.getClass().getEnclosingMethod();
                dpk = new FnKey(CUSTID, APPID, INSTANCEID, SERVICE, function);
            }

            if (mode == Mode.Mock) {
                return (double) mocker.mock(dpk, traceid, spanid, parentSpanid,
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
                return (double) mocker.mock(dpk2, traceid, spanid, parentSpanid,
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

    public RecorderAndMockerTest() {
        this.jsonMapper = new ObjectMapper();
        jsonMapper.registerModule(new Jdk8Module());
        jsonMapper.registerModule(new JavaTimeModule());
        jsonMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        this.cubeClient = new CubeClient(jsonMapper);
        this.randomGen = new Random();
    }

    @Test
    void testFnNoArgs() {

        // start recording
        cubeClient.startRecording(CUSTID, APPID, INSTANCEID, COLLECTION);

        // set mode to record
        mode = Mode.Record;
        String trace = "testFnNoArgst1." + randomGen.nextInt();
        spanid=Optional.of("1");

        // call fn
        traceid = Optional.of(trace + ".1");
        String ret1 = prodDiscount1.getPromoName();
        traceid = Optional.of(trace + ".2");
        String ret2 = prodDiscount2.getPromoName();
        traceid = Optional.of(trace + ".3");
        String ret3 = prodDiscount3.getPromoName();


        // stop recording
        cubeClient.stopRecording(CUSTID, APPID, COLLECTION);

        // start replay
        Optional<String> resp = cubeClient.initReplay(CUSTID, APPID, INSTANCEID, COLLECTION, ENDPOINT);
        Optional<String> replayid = resp.flatMap(r -> {
            try {
                Map<String, String> attrs = jsonMapper.readValue(r, Map.class);
                return Optional.ofNullable(attrs.get("replayid"));
            } catch (Exception e) {
                LOGGER.error("Error in reading json resp about Replay: " + r + " : " + e.getMessage());
                return Optional.empty();
            }
        });

        replayid.ifPresentOrElse(replayidv -> {

                    // force start replay (for testing - this only sets replay state to Running)
                    cubeClient.forceStartReplay(replayidv);

                    // set mode to mock
                    mode = Mode.Mock;

                    // call fn
                    traceid = Optional.of(trace + ".1");
                    String replayRet1 = prodDiscount1.getPromoName();
                    traceid = Optional.of(trace + ".2");
                    String replayRet2 = prodDiscount2.getPromoName();
                    traceid = Optional.of(trace + ".3");
                    String replayRet3 = prodDiscount3.getPromoName();

                    // stop replay
                    cubeClient.forceCompleteReplay(replayidv);

                    // compare values
                    assertEquals(ret1, replayRet1);
                    assertEquals(ret2, replayRet2);
                    assertEquals(ret3, replayRet3);
                },
                () -> fail("Replay cannot be inited or started"));

    }

    private double callProdDisc(ProdDiscount prodDiscount, ProdDiscount.ProdPrice prodPrice, boolean asObj) {
        if (asObj) {
            return prodDiscount.discountedPrice(prodPrice);
        } else {
            return prodDiscount.discountedPrice(prodPrice.productId, prodPrice.price);
        }
    }

    private Double[] callProdDisc(ProdDiscount prodDiscount, ProdDiscount.ProdPrice[] prodPrice, boolean asObj) {
        return Arrays.stream(prodPrice).map(pp -> {
            if (asObj) {
                return prodDiscount.discountedPrice(pp);
            } else {
                return prodDiscount.discountedPrice(pp.productId, pp.price);
            }
        }).toArray(Double[]::new);
    }

    private void testFnMultiArgs(boolean asObj) {

        // start recording
        cubeClient.startRecording(CUSTID, APPID, INSTANCEID, COLLECTION);

        // set mode to record
        mode = Mode.Record;
        String trace = "testFnMultiArgst1." + randomGen.nextInt();
        spanid=Optional.of("1");

        ProdDiscount.ProdPrice[] ppArr = new ProdDiscount.ProdPrice[4];

        ppArr[0] = new ProdDiscount.ProdPrice("Prod1", 50);
        ppArr[1] = new ProdDiscount.ProdPrice("Prod1", (float) 90.5);
        ppArr[2] = new ProdDiscount.ProdPrice("Prod2", (float) 110.10);
        ppArr[3] = new ProdDiscount.ProdPrice("Prod3", 120);

        // call fn
        traceid = Optional.of(trace + ".1");
        Double[] ret1 = callProdDisc(prodDiscount1, ppArr, asObj);
        traceid = Optional.of(trace + ".2");
        Double[] ret2 = callProdDisc(prodDiscount2, ppArr, asObj);
        traceid = Optional.of(trace + ".3");
        Double[] ret3 = callProdDisc(prodDiscount3, ppArr, asObj);


        // stop recording
        cubeClient.stopRecording(CUSTID, APPID, COLLECTION);

        // start replay
        Optional<String> resp = cubeClient.initReplay(CUSTID, APPID, INSTANCEID, COLLECTION, ENDPOINT);
        Optional<String> replayid = resp.flatMap(r -> {
            try {
                Map<String, String> attrs = jsonMapper.readValue(r, Map.class);
                return Optional.ofNullable(attrs.get("replayid"));
            } catch (Exception e) {
                LOGGER.error("Error in reading json resp about Replay: " + r + " : " + e.getMessage());
                return Optional.empty();
            }
        });

        replayid.ifPresentOrElse(replayidv -> {

                    // force start replay (for testing - this only sets replay state to Running)
                    cubeClient.forceStartReplay(replayidv);

                    // set mode to mock
                    mode = Mode.Mock;

                    // call fn
                    traceid = Optional.of(trace + ".1");
                    Double[] rr1 = callProdDisc(prodDiscount1, ppArr, asObj);
                    traceid = Optional.of(trace + ".2");
                    Double[] rr2 = callProdDisc(prodDiscount2, ppArr, asObj);
                    traceid = Optional.of(trace + ".3");
                    Double[] rr3 = callProdDisc(prodDiscount3, ppArr, asObj);

                    // stop replay
                    cubeClient.forceCompleteReplay(replayidv);

                    // compare values
                    LOGGER.debug("Comparing " + ret1.toString() + " to " + rr1.toString());
                    assertArrayEquals(ret1, rr1);
                    LOGGER.debug("Comparing " + ret2.toString() + " to " + rr3.toString());
                    assertArrayEquals(ret2, rr2);
                    LOGGER.debug("Comparing " + ret3.toString() + " to " + rr3.toString());
                    assertArrayEquals(ret3, rr3);
                },
                () -> fail("Replay cannot be inited or started"));

    }


    @Test
    void testFnMultipleArgs() {
        testFnMultiArgs(false);
    }

    @Test
    void testFnObjArgs() {
        testFnMultiArgs(true);
    }
}