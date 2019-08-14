package io.cube;

import io.cube.agent.CommonUtils;
import io.cube.agent.FnKey;
import io.cube.agent.FnReqResponse;
import io.cube.agent.FnResponseObj;
import io.cube.agent.UtilException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.sql.SQLException;
import java.util.Optional;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-08-05
 * @author Ashoke S
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

    public static void record(Object toReturn, Config config, FnKey fnKey, Object... args) {
        config.recorder.record(fnKey, CommonUtils.getCurrentTraceId(),
                CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), toReturn,
                FnReqResponse.RetStatus.Success, Optional.empty(), args);
    }

    public static Object mock(Config config, FnKey fnKey, Object... args) {
        FnResponseObj ret = config.mocker.mock(fnKey, CommonUtils.getCurrentTraceId(),
                CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(),
                Optional.empty(), args);
        if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
            LOGGER.info("Throwing exception as a result of mocking function");
            UtilException.throwAsUnchecked((Throwable) ret.retVal);
        }

        return ret.retVal;
    }

    public static Object recordOrMock(Config config, FnKey fnKey, UtilException.Function_WithSQLExceptions<Object[],Object, SQLException> function,
                                      Object... args) throws SQLException{
        Object toReturn;
        if (config.intentResolver.isIntentToMock()) {
            return mock(config, fnKey, args);
        }

        toReturn = function.apply(args);
        if (true) {
            record(toReturn, config, fnKey, args);
        }

        return toReturn;
    }
}
