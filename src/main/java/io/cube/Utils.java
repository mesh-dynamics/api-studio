package io.cube;

import io.cube.agent.CommonUtils;
import io.cube.agent.FnKey;
import io.cube.agent.FnReqResponse;
import io.cube.agent.FnResponseObj;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Optional;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-05-06
 * @author Prasad M D
 */
public class Utils {

    private static final Logger LOGGER = LogManager.getLogger(Utils.class);

    public static String recordOrMockString(String toReturn, Config config,
                                            FnKey fnKey, boolean RECORD, Object... args) {
        if (RECORD) {
            config.recorder.record(fnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), toReturn,
                    FnReqResponse.RetStatus.Success, Optional.empty(), args);

        } else {
            FnResponseObj ret = config.mocker.mock(fnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(),
                    Optional.empty(), args);
            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }

            toReturn = ret.retVal.toString();
        }

        return toReturn;
    }

    public static boolean recordOrMockBoolean(boolean toReturn, Config config,
                                              FnKey fnKey, boolean RECORD, Object... args) {
        if (RECORD) {
            config.recorder.record(fnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), toReturn,
                    FnReqResponse.RetStatus.Success, Optional.empty(), args);

        } else {
            FnResponseObj ret = config.mocker.mock(fnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(),
                    Optional.empty(), args);
            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }

            toReturn = (boolean)ret.retVal;
        }
        return toReturn;
    }

    //for recording/mocking all of byte,short,int and long types
    public static long recordOrMockLong(long toReturn, Config config,
                                       FnKey fnKey, boolean RECORD, Object... args) {
        if (RECORD) {
            config.recorder.record(fnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), toReturn,
                    FnReqResponse.RetStatus.Success, Optional.empty(), args);

        } else {
            FnResponseObj ret = config.mocker.mock(fnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(),
                    Optional.empty(), args);
            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }

            toReturn = (long)ret.retVal;
        }
        return toReturn;
    }

    public static BigDecimal recordOrMockBigDecimal(BigDecimal toReturn, Config config,
                                              FnKey fnKey, boolean RECORD, Object... args) {
        if (RECORD) {
            config.recorder.record(fnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), toReturn,
                    FnReqResponse.RetStatus.Success, Optional.empty(), args);

        } else {
            FnResponseObj ret = config.mocker.mock(fnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(),
                    Optional.empty(), args);
            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }

            toReturn = (BigDecimal) ret.retVal;
        }
        return toReturn;
    }

    public static Date recordOrMockDate(Date toReturn, Config config,
                                              FnKey fnKey, boolean RECORD, Object... args) {
        if (RECORD) {
            config.recorder.record(fnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), toReturn,
                    FnReqResponse.RetStatus.Success, Optional.empty(), args);

        } else {
            FnResponseObj ret = config.mocker.mock(fnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(),
                    Optional.empty(), args);
            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }

            toReturn = (Date) ret.retVal;
        }
        return toReturn;
    }

    public static Time recordOrMockTime(Time toReturn, Config config,
                                        FnKey fnKey, boolean RECORD, Object... args) {
        if (RECORD) {
            config.recorder.record(fnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), toReturn,
                    FnReqResponse.RetStatus.Success, Optional.empty(), args);

        } else {
            FnResponseObj ret = config.mocker.mock(fnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(),
                    Optional.empty(), args);
            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }

            toReturn = (Time) ret.retVal;
        }
        return toReturn;
    }

    public static Timestamp recordOrMockTimestamp(Timestamp toReturn, Config config,
                                             FnKey fnKey, boolean RECORD, Object... args) {
        if (RECORD) {
            config.recorder.record(fnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), toReturn,
                    FnReqResponse.RetStatus.Success, Optional.empty(), args);

        } else {
            FnResponseObj ret = config.mocker.mock(fnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(),
                    Optional.empty(), args);
            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }

            toReturn = (Timestamp) ret.retVal;
        }
        return toReturn;
    }
}
