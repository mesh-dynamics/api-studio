package io.cube.agent;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.md.dao.FnReqRespPayload.RetStatus;
import io.md.constants.Constants;
import io.md.utils.FnKey;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-10
 * @author Prasad M D
 */
class RecorderAndMockerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecorderAndMockerTest.class);


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

    static Gson gson = new GsonBuilder().registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory())
            .registerTypeAdapter(Pattern.class, new GsonPatternDeserializer()).create();

    static Map<String, Double> disc1 = new HashMap<String, Double>();
    static Map<String, Double> disc2 = new HashMap<String, Double>();
    static Map<String, Double> disc3 = new HashMap<String, Double>();
    static Recorder recorder;
    static Mocker mocker;
    static {
        try {
            recorder = new SimpleHttpRecorder(gson);
            mocker = new SimpleMocker(gson);
        } catch (Exception e) {
            LOGGER.error("Unable to initialize recorder/mocker", e);
        }
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

    static class ProdDiscount {

        public ProdDiscount(Map<String, Double> discountMap, String name) {
            this.discountMap = discountMap;
            this.name = name;
            randomGen = new Random();
            resTimeStamp = Optional.empty();
        }

        final Map<String, Double> discountMap;
        final String name;
        final Random randomGen;

        // instrumented for mocking
        Optional<Instant> resTimeStamp;

        static class ProdPrice {
            String productId;
            float price;

            public ProdPrice(String productId, float price) {
                this.productId = productId;
                this.price = price;
            }
        }


        double discountedPrice(String productId, float price) {

            if (discountedPriceFnKey == null) {
                Method function = new Object(){}.getClass().getEnclosingMethod();
                discountedPriceFnKey = new FnKey(CUSTID, APPID, INSTANCEID, SERVICE, function);
            }

            if (mode == Mode.Mock) {
                FnResponseObj ret = mocker.mock(discountedPriceFnKey,
                        resTimeStamp, Optional.empty(), productId, price);
                resTimeStamp = ret.timeStamp;
                return (double) ret.retVal;
            }

            double discount = Optional.ofNullable(discountMap.get(productId)).orElse(Double.valueOf(0));
            // add a random variation to make this function non idempotent - for testing record/mock based on timestamp
            discount += randomGen.nextDouble()*0.1; // random delta < 0.1

            double ret = price*(1-discount);
            if (mode == Mode.Record) {
                RecorderAndMockerTest.recorder.record(discountedPriceFnKey, traceid, spanid,
                        parentSpanid,
                        Double.valueOf(ret), RetStatus.Success, Optional.empty(), productId, price);
            }
            return ret;
        }

        double discountedPrice(ProdPrice pp) {
            if (discountedPriceFnKey2 == null) {
                Method function = new Object(){}.getClass().getEnclosingMethod();
                discountedPriceFnKey2 = new FnKey(CUSTID, APPID, INSTANCEID, SERVICE, function);
            }

            if (mode == Mode.Mock) {
                FnResponseObj ret = mocker.mock(discountedPriceFnKey,
                        resTimeStamp, Optional.empty(), pp);
                resTimeStamp = ret.timeStamp;
                return (double) ret.retVal;
            }

            double ret = (pp != null) ? discountedPrice(pp.productId, pp.price) : 0;
            if (mode == Mode.Record) {
                RecorderAndMockerTest.recorder.record(discountedPriceFnKey2, traceid, spanid,
                        parentSpanid,
                        ret, RetStatus.Success, Optional.empty(), pp);
            }
            return ret;
        }

        /**
         * can throw runtime null pointer exception
         * @param pp
         * v@return
         */
        double discountedPriceRuntimeException(ProdPrice pp) {
            if (discountedPriceRuntimeExceptionFnKey == null) {
                Method function = new Object(){}.getClass().getEnclosingMethod();
                discountedPriceRuntimeExceptionFnKey = new FnKey(CUSTID, APPID, INSTANCEID, SERVICE, function);
            }

            if (mode == Mode.Mock) {
                FnResponseObj ret = mocker.mock(discountedPriceRuntimeExceptionFnKey,
                        resTimeStamp, Optional.empty(), pp);
                resTimeStamp = ret.timeStamp;
                if (ret.retStatus == RetStatus.Exception) {
                    UtilException.throwAsUnchecked((Throwable)ret.retVal);
                }
                return ret.retVal != null ? (double) ret.retVal : 0;
            }

            // no null check
            try {
                double ret = discountedPrice(pp.productId, pp.price);
                if (mode == Mode.Record) {
                    RecorderAndMockerTest.recorder.record(discountedPriceRuntimeExceptionFnKey, traceid, spanid,
                            parentSpanid,
                            ret, RetStatus.Success, Optional.empty(), pp);
                }
                return ret;
            } catch (Throwable e) {
                if (mode == Mode.Record) {
                    RecorderAndMockerTest.recorder.record(discountedPriceRuntimeExceptionFnKey, traceid, spanid,
                            parentSpanid,
                            e, RetStatus.Exception, Optional.of(e.getClass().getName()), pp);
                }
                throw e;
            }
        }

        static class PriceException extends  Exception {

            public PriceException(String msg) {
                super(msg);
            }
        }

        /**
         * can throw checked exception
         * @param pp
         * v@return
         */
        double discountedPriceTypedException(ProdPrice pp) throws PriceException {
            if (discountedPriceTypedExceptionFnKey == null) {
                Method function = new Object(){}.getClass().getEnclosingMethod();
                discountedPriceTypedExceptionFnKey = new FnKey(CUSTID, APPID, INSTANCEID, SERVICE, function);
            }

            if (mode == Mode.Mock) {
                FnResponseObj ret = mocker.mock(discountedPriceTypedExceptionFnKey,
                        resTimeStamp, Optional.empty(), pp);
                resTimeStamp = ret.timeStamp;
                if (ret.retStatus == RetStatus.Exception) {
                    UtilException.throwAsUnchecked((Throwable)ret.retVal);
                }
                return ret.retVal != null ? (double) ret.retVal : 0;
            }

            // no null check
            try {
                double ret = 0D;
                if (pp == null) throw new PriceException("Price Exception");
                if (mode == Mode.Record) {
                    RecorderAndMockerTest.recorder.record(discountedPriceTypedExceptionFnKey, traceid, spanid,
                            parentSpanid,
                            ret, RetStatus.Success, Optional.empty(), pp);
                }
                return ret;
            } catch (Throwable e) {
                if (mode == Mode.Record) {
                    RecorderAndMockerTest.recorder.record(discountedPriceTypedExceptionFnKey, traceid, spanid,
                            parentSpanid,
                            e, RetStatus.Exception, Optional.of(e.getClass().getName()), pp);
                }
                throw e;
            }
        }


        String getPromoName() {

            if (getPromoNameFnKey == null) {
                Method function = new Object(){}.getClass().getEnclosingMethod();
                getPromoNameFnKey = new FnKey(CUSTID, APPID, INSTANCEID, SERVICE, function);
            }

            if (mode == Mode.Mock) {
                FnResponseObj ret = mocker.mock(getPromoNameFnKey,
                        resTimeStamp, Optional.empty());
                resTimeStamp = ret.timeStamp;
                return (String) ret.retVal;
            }

            String ret = name;
            if (mode == Mode.Record) {
                RecorderAndMockerTest.recorder.record(getPromoNameFnKey, traceid, spanid,
                        parentSpanid,
                        ret, RetStatus.Success, Optional.empty());
            }
            return ret;
        }


        static FnKey discountedPriceFnKey; // for discountedPrice
        static FnKey discountedPriceFnKey2;
        static FnKey discountedPriceRuntimeExceptionFnKey;
        static FnKey discountedPriceTypedExceptionFnKey;
        static FnKey getPromoNameFnKey; // for getPromoName
    }

    public RecorderAndMockerTest() throws Exception {
        this.jsonMapper = new ObjectMapper();
        jsonMapper.registerModule(new Jdk8Module());
        jsonMapper.registerModule(new JavaTimeModule());
        jsonMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        this.cubeClient = new CubeClient(jsonMapper);
        this.randomGen = new Random();
    }

    void testGetPromoName(ProdDiscount[] prodDiscounts) {

        // start recording
        cubeClient.startRecording(CUSTID, APPID, INSTANCEID, COLLECTION);

        // set mode to record
        mode = Mode.Record;
        String trace = "testFnNoArgst1." + randomGen.nextInt();
        spanid=Optional.of("1");

        // call fn
        String[] ret = IntStream.range(0, prodDiscounts.length).mapToObj(i -> {
            traceid = Optional.of(trace + "." + i);
            return prodDiscounts[i].getPromoName();
        }).toArray(String[]::new);


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
                    String[] replayRet = IntStream.range(0, prodDiscounts.length).mapToObj(i -> {
                        traceid = Optional.of(trace + "." + i);
                        return prodDiscounts[i].getPromoName();
                    }).toArray(String[]::new);

                    // stop replay
                    cubeClient.forceCompleteReplay(replayidv);

                    // compare values
                    assertArrayEquals(ret, replayRet);
                },
                () -> fail("Replay cannot be inited or started"));

    }


    @Test
    void testFnNoArgs() {

        ProdDiscount[] prodDiscounts = {prodDiscount1, prodDiscount2, prodDiscount3};
        testGetPromoName(prodDiscounts);
    }

    private double callProdDisc(ProdDiscount prodDiscount, ProdDiscount.ProdPrice prodPrice, boolean asObj) {
        if (asObj) {
            return prodDiscount.discountedPrice(prodPrice);
        } else {
            return prodDiscount.discountedPrice(prodPrice.productId, prodPrice.price);
        }
    }

    enum ExceptionTestType {
        None,
        ExceptionRuntime,
        ExceptionChecked
    }

    private Double[] callProdDisc(ProdDiscount prodDiscount, ProdDiscount.ProdPrice[] prodPrice, boolean asObj,
                                  boolean nullObj, int seqSize, ExceptionTestType testException) {
        return Arrays.stream(prodPrice).flatMap(pp -> {
            // call the same function multiple times
            prodDiscount.resTimeStamp = Optional.empty();
            return IntStream.range(0, seqSize).mapToObj(i -> {
                if (testException == ExceptionTestType.ExceptionRuntime) {
                    try {
                        return prodDiscount.discountedPriceRuntimeException(null);
                    } catch (Exception e) {
                        LOGGER.info("Got exception in calling discountedPriceRuntimeException");
                        return 100D; // any value other than 0
                    }
                } else if (testException == ExceptionTestType.ExceptionChecked) {
                    try {
                        return prodDiscount.discountedPriceTypedException(null);
                    } catch (Exception e) {
                        LOGGER.info("Got exception in calling discountedPriceTypedException");
                        return 100D; // any value other than 0
                    }
                } else if (nullObj) {
                    return prodDiscount.discountedPrice(null);
                } else if (asObj) {
                    return prodDiscount.discountedPrice(pp);
                } else {
                    return prodDiscount.discountedPrice(pp.productId, pp.price);
                }
            });
        }).toArray(Double[]::new);
    }

    private void testFnMultiArgs(boolean asObj, boolean nullObj, int seqSize, ExceptionTestType testException) {

        // start recording
        cubeClient.startRecording(CUSTID, APPID, INSTANCEID, COLLECTION);

        // set mode to record
        mode = Mode.Record;
        String trace = "testFnMultiArgst1." + randomGen.nextInt();
        spanid=Optional.of("1");

        ProdDiscount.ProdPrice[] ppArr = new ProdDiscount.ProdPrice[4];

        ppArr[0] = new ProdDiscount.ProdPrice("Prod0", 50);
        ppArr[1] = new ProdDiscount.ProdPrice("Prod1", (float) 90.5);
        ppArr[2] = new ProdDiscount.ProdPrice("Prod2", (float) 110.10);
        ppArr[3] = new ProdDiscount.ProdPrice("Prod3", 120);

        // call fn
        traceid = Optional.of(trace + ".1");
        Double[] ret1 = callProdDisc(prodDiscount1, ppArr, asObj, nullObj, seqSize, testException);
        traceid = Optional.of(trace + ".2");
        Double[] ret2 = callProdDisc(prodDiscount2, ppArr, asObj, nullObj, seqSize, testException);
        traceid = Optional.of(trace + ".3");
        Double[] ret3 = callProdDisc(prodDiscount3, ppArr, asObj, nullObj, seqSize, testException);


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
                    Double[] rr1 = callProdDisc(prodDiscount1, ppArr, asObj, nullObj, seqSize, testException);
                    traceid = Optional.of(trace + ".2");
                    Double[] rr2 = callProdDisc(prodDiscount2, ppArr, asObj, nullObj, seqSize, testException);
                    traceid = Optional.of(trace + ".3");
                    Double[] rr3 = callProdDisc(prodDiscount3, ppArr, asObj, nullObj, seqSize, testException);

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
        testFnMultiArgs(false, false, 1, ExceptionTestType.None);
    }

    @Test
    void testFnObjArgs() {
        testFnMultiArgs(true, false, 1, ExceptionTestType.None);
    }


    @Test
    void testFnNullObjArgs() {
        testFnMultiArgs(true, true, 1, ExceptionTestType.None);
    }

    @Test
    void testFnException() {
        testFnMultiArgs(true, true, 1, ExceptionTestType.ExceptionRuntime);
    }

    @Test
    void testFnExceptionChecked() {
        testFnMultiArgs(true, true, 1, ExceptionTestType.ExceptionChecked);
    }

    @Test
    void testFnNullReturnVal() {
        // create ProdDiscount with null promo name
        ProdDiscount[] prodDiscounts = {new ProdDiscount(new HashMap<String, Double>(), null)};
        testGetPromoName(prodDiscounts);
    }


    @Test
    void testFnSeqOfCalls() {
        testFnMultiArgs(false, false, 5, ExceptionTestType.None);
    }

}