/*
 * Copyright 2021 MeshDynamics.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

import io.md.constants.Constants;
import io.md.dao.FnReqRespPayload.RetStatus;
import io.md.utils.FnKey;
import net.dongliu.gson.GsonJava8TypeAdapterFactory;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-10
 * @author Prasad M D
 */
public class RecorderAndMockerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecorderAndMockerTest.class);


    private final ObjectMapper jsonMapper;
    private final CubeClient cubeClient;
    private final Random randomGen;

    enum Mode {
        Regular,
        Record,
        Mock
    }

    static final String CUSTID = "CubeCorp";
    static final String APPID = "Test";
    static final String INSTANCEID_BASE = "Test";
    static String INSTANCEID = "Test"; // non final, set differently for each test since recordings take 15s to stop
    static final String SERVICE = "ProdDiscount";
    private static final String GOLDENNAME = "RecorderAndMockTest";
    private static final String ENDPOINT = "Dummy";
    private static final String TESTUSER = "AgentTest";
    private static final String TEMPLATEVERSION = "Dummy";
    private static final String RUNID = "Test_runId";


    static Mode mode;

    static Gson gson = new GsonBuilder().registerTypeAdapterFactory(new GsonJava8TypeAdapterFactory())
            .registerTypeAdapter(Pattern.class, new GsonPatternDeserializer()).create();

    static Map<String, Double> disc1 = new HashMap<String, Double>();
    static Map<String, Double> disc2 = new HashMap<String, Double>();
    static Map<String, Double> disc3 = new HashMap<String, Double>();
    static Recorder recorder;
    static FnMockerHelper mocker;
    static {
        try {
            recorder = new SimpleHttpRecorder();
            mocker = new FnMockerHelper(new NonIdempotentMocker(new ProxyMocker()));
            //mocker = new FnMockerHelper(new NonIdempotentMocker(new RealMocker(new ProxyDataStore())));
        } catch (Exception e) {
            LOGGER.error("Unable to initialize recorder/mocker", e);
        }
        disc1.put("Prod0", 0.50); disc1.put("Prod1", 0.50); disc1.put("Prod2", 0.5); disc1.put("Prod3", 0.5);
        disc2.put("Prod0", 0.25); disc2.put("Prod1", 0.25); disc2.put("Prod2", 0.25); disc2.put("Prod3", 0.25);
        disc3.put("Prod0", 0.10); disc3.put("Prod1", 0.20); disc3.put("Prod2", 0.30); disc3.put("Prod3", 0.4);
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
        static Optional<Instant> resTimeStamp;

        static class ProdPrice {
            String productId;
            float price;

            public ProdPrice(String productId, float price) {
                this.productId = productId;
                this.price = price;
            }
        }


        double discountedPrice(String productId, float price) {

            // always create a new key since INSTANCEID is no longer final
//            if (discountedPriceFnKey == null) {
                Method function = new Object(){}.getClass().getEnclosingMethod();
                discountedPriceFnKey = new FnKey(CUSTID, APPID, INSTANCEID, SERVICE, function);
//            }

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
                RecorderAndMockerTest.recorder.record(discountedPriceFnKey,
                        Double.valueOf(ret), RetStatus.Success, Optional.empty(), productId, price);
            }
            return ret;
        }

        double discountedPrice(ProdPrice pp) {

            // always create a new key since INSTANCEID is no longer final
//            if (discountedPriceFnKey2 == null) {
                Method function = new Object(){}.getClass().getEnclosingMethod();
                discountedPriceFnKey2 = new FnKey(CUSTID, APPID, INSTANCEID, SERVICE, function);
//            }

            if (mode == Mode.Mock) {
                FnResponseObj ret = mocker.mock(discountedPriceFnKey2,
                        resTimeStamp, Optional.empty(), RUNID,pp);
                resTimeStamp = ret.timeStamp;
                return (double) ret.retVal;
            }

            double ret = (pp != null) ? discountedPrice(pp.productId, pp.price) : 0;
            if (mode == Mode.Record) {
                RecorderAndMockerTest.recorder.record(discountedPriceFnKey2,
                        ret, RetStatus.Success, Optional.empty(), RUNID,pp);
            }
            return ret;
        }

        /**
         * can throw runtime null pointer exception
         * @param pp
         * v@return
         */
        double discountedPriceRuntimeException(ProdPrice pp) {

            // always create a new key since INSTANCEID is no longer final
//            if (discountedPriceRuntimeExceptionFnKey == null) {
                Method function = new Object(){}.getClass().getEnclosingMethod();
                discountedPriceRuntimeExceptionFnKey = new FnKey(CUSTID, APPID, INSTANCEID, SERVICE, function);
//            }

            if (mode == Mode.Mock) {
                FnResponseObj ret = mocker.mock(discountedPriceRuntimeExceptionFnKey,
                        resTimeStamp, Optional.empty(), RUNID,pp);
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
                    RecorderAndMockerTest.recorder.record(discountedPriceRuntimeExceptionFnKey,
                            ret, RetStatus.Success, Optional.empty(), RUNID,pp);
                }
                return ret;
            } catch (Throwable e) {
                if (mode == Mode.Record) {
                    RecorderAndMockerTest.recorder.record(discountedPriceRuntimeExceptionFnKey,
                            e, RetStatus.Exception, Optional.of(e.getClass().getName()), RUNID,pp);
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

            // always create a new key since INSTANCEID is no longer final
//            if (discountedPriceTypedExceptionFnKey == null) {
                Method function = new Object(){}.getClass().getEnclosingMethod();
                discountedPriceTypedExceptionFnKey = new FnKey(CUSTID, APPID, INSTANCEID, SERVICE, function);
//            }

            if (mode == Mode.Mock) {
                FnResponseObj ret = mocker.mock(discountedPriceTypedExceptionFnKey,
                        resTimeStamp, Optional.empty(), RUNID,pp);
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
                    RecorderAndMockerTest.recorder.record(discountedPriceTypedExceptionFnKey,
                            ret, RetStatus.Success, Optional.empty(), RUNID,pp);
                }
                return ret;
            } catch (Throwable e) {
                if (mode == Mode.Record) {
                    RecorderAndMockerTest.recorder.record(discountedPriceTypedExceptionFnKey,
                            e, RetStatus.Exception, Optional.of(e.getClass().getName()), RUNID,pp);
                }
                throw e;
            }
        }


        String getPromoName() {

            // always create a new key since INSTANCEID is no longer final
//            if (getPromoNameFnKey == null) {
                Method function = new Object(){}.getClass().getEnclosingMethod();
                getPromoNameFnKey = new FnKey(CUSTID, APPID, INSTANCEID, SERVICE, function);
//            }

            if (mode == Mode.Mock) {
                FnResponseObj ret = mocker.mock(getPromoNameFnKey,
                        resTimeStamp, Optional.empty(), RUNID);
                resTimeStamp = ret.timeStamp;
                return (String) ret.retVal;
            }

            String ret = name;
            if (mode == Mode.Record) {
                RecorderAndMockerTest.recorder.record(getPromoNameFnKey,
                        ret, RetStatus.Success, Optional.empty(), RUNID);
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

    void testGetPromoName(ProdDiscount[] prodDiscounts) throws InterruptedException {

        Long currentTime = Instant.now().toEpochMilli();
        String label = Instant.now().toString();
        INSTANCEID = INSTANCEID_BASE + "_Record_" + currentTime;
        String replayInstanceId = INSTANCEID_BASE + "_Replay_" + currentTime;

        String goldenName = GOLDENNAME + "_" + currentTime;

        // start recording
        cubeClient.startRecording(CUSTID, APPID, INSTANCEID, goldenName, TESTUSER, label,
                TEMPLATEVERSION);

        Thread.sleep(3000);

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
        cubeClient.stopRecording(CUSTID, APPID, goldenName, label);

        // start replay
        Optional<String> resp = cubeClient.initReplay(CUSTID, APPID, replayInstanceId, goldenName, ENDPOINT, TESTUSER);
        Optional<String> replayid = resp.flatMap(r -> {
            try {
                Map<String, String> attrs = jsonMapper.readValue(r, Map.class);
                return Optional.ofNullable(attrs.get(Constants.REPLAY_ID_FIELD));
            } catch (Exception e) {
                LOGGER.error("Error in reading json resp about Replay: " + r + " : " + e.getMessage());
                return Optional.empty();
            }
        });

        if (!replayid.isPresent()) {
            fail("Replay cannot be inited or started");
        }

        Thread.sleep(15000);

        INSTANCEID = replayInstanceId; // set instance id to replay instance

        replayid.ifPresent(replayidv -> {

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
        });

    }


    @Test
    public void testFnNoArgs() throws InterruptedException {

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
            // prodDiscount.resTimeStamp = Optional.empty();
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

    private void testFnMultiArgs(boolean asObj, boolean nullObj, int seqSize, ExceptionTestType testException) throws InterruptedException {

        ProdDiscount.resTimeStamp = Optional.empty();

        Long currentTime = Instant.now().toEpochMilli();
        String label = Instant.now().toString();
        INSTANCEID = INSTANCEID_BASE + "_Record_" + currentTime;
        String replayInstanceId = INSTANCEID_BASE + "_Replay_" + currentTime;
        String goldenName = GOLDENNAME + "_" + currentTime;

        // start recording
        cubeClient.startRecording(CUSTID, APPID, INSTANCEID, goldenName, TESTUSER, label, TEMPLATEVERSION);

        Thread.sleep(3000);

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
        cubeClient.stopRecording(CUSTID, APPID, goldenName, label);

        Thread.sleep(25000);

        // start replay
        Optional<String> resp = cubeClient.initReplay(CUSTID, APPID, replayInstanceId, goldenName, ENDPOINT, TESTUSER);
        Optional<String> replayid = resp.flatMap(r -> {
            try {
                Map<String, String> attrs = jsonMapper.readValue(r, Map.class);
                return Optional.ofNullable(attrs.get(Constants.REPLAY_ID_FIELD));
            } catch (Exception e) {
                LOGGER.error("Error in reading json resp about Replay: " + r + " : " + e.getMessage());
                return Optional.empty();
            }
        });

        if(!replayid.isPresent()) {
            fail("Replay cannot be inited or started");
        }

        INSTANCEID = replayInstanceId; // set instance id to replay instance

        replayid.ifPresent(replayidv -> {

            // force start replay (for testing - this only sets replay state to Running)
            cubeClient.forceStartReplay(replayidv);

            // set mode to mock
            mode = Mode.Mock;

            // call fn
            traceid = Optional.of(trace + ".1");
            Double[] rr1 = callProdDisc(prodDiscount1, ppArr, asObj, nullObj, seqSize,
                testException);
            traceid = Optional.of(trace + ".2");
            Double[] rr2 = callProdDisc(prodDiscount2, ppArr, asObj, nullObj, seqSize,
                testException);
            traceid = Optional.of(trace + ".3");
            Double[] rr3 = callProdDisc(prodDiscount3, ppArr, asObj, nullObj, seqSize,
                testException);

            // stop replay
            cubeClient.forceCompleteReplay(replayidv);

            // compare values
            LOGGER.debug("Comparing " + ret1.toString() + " to " + rr1.toString());
            assertArrayEquals(ret1, rr1);
            LOGGER.debug("Comparing " + ret2.toString() + " to " + rr3.toString());
            assertArrayEquals(ret2, rr2);
            LOGGER.debug("Comparing " + ret3.toString() + " to " + rr3.toString());
            assertArrayEquals(ret3, rr3);
        });

    }


    @Test
    public void testFnMultipleArgs() throws InterruptedException {
        testFnMultiArgs(false, false, 1, ExceptionTestType.None);
    }

    @Test
    public void testFnObjArgs() throws InterruptedException {
        testFnMultiArgs(true, false, 1, ExceptionTestType.None);
    }


    @Test
    public void testFnNullObjArgs() throws InterruptedException {
        testFnMultiArgs(true, true, 1, ExceptionTestType.None);
    }

    @Test
    public void testFnException() throws InterruptedException {
        testFnMultiArgs(true, true, 1, ExceptionTestType.ExceptionRuntime);
    }

    @Test
    public void testFnExceptionChecked() throws InterruptedException {
        testFnMultiArgs(true, true, 1, ExceptionTestType.ExceptionChecked);
    }

    @Test
    public void testFnNullReturnVal() throws InterruptedException {
        // create ProdDiscount with null promo name
        ProdDiscount[] prodDiscounts = {new ProdDiscount(new HashMap<String, Double>(), null)};
           testGetPromoName(prodDiscounts);
    }


    @Test
    public void testFnSeqOfCalls() throws InterruptedException {
        testFnMultiArgs(false, false, 5, ExceptionTestType.None);
    }

}