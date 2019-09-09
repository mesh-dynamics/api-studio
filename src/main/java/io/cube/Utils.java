package io.cube;

import com.google.gson.reflect.TypeToken;
import io.cube.agent.CommonUtils;
import io.cube.agent.FnKey;
import io.cube.agent.FnReqResponse;
import io.cube.agent.FnResponseObj;
import io.cube.agent.UtilException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-08-05
 * @author Ashoke S
 */
public class Utils {

    private static final Logger LOGGER = LogManager.getLogger(Utils.class);
    private static Type integerType = new TypeToken<Integer>() {}.getType();

    public static void record(Object toReturn, Config config, FnKey fnKey, Object... args) {
        config.recorder.record(fnKey, Optional.of(CommonUtils.getCurrentTraceId().orElse("#INIT_TRACEID#")),
                Optional.of(CommonUtils.getCurrentSpanId().orElse("#INIT_SPANID#")),
                Optional.of(CommonUtils.getParentSpanId().orElse("#INIT_PARENTSPANID#")), toReturn,
                FnReqResponse.RetStatus.Success, Optional.empty(), args);
    }

    public static Object mock(Config config, FnKey fnKey, Optional<Type> returnType, Object... args) {
        FnResponseObj ret = config.mocker.mock(fnKey, Optional.of(CommonUtils.getCurrentTraceId().orElse("#INIT_TRACEID#")),
                Optional.of(CommonUtils.getCurrentSpanId().orElse("#INIT_SPANID#")),
                Optional.of(CommonUtils.getParentSpanId().orElse("#INIT_PARENTSPANID#")), Optional.empty(),
                returnType, args);
        if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
            LOGGER.info("Throwing exception as a result of mocking function");
            UtilException.throwAsUnchecked((Throwable) ret.retVal);
        }

        return ret.retVal;
    }

    public static Object recordOrMockResultSet(Config config, FnKey fnKey, CubeDatabaseMetaData metaData,
                                               UtilException.Function_WithExceptions<Object[], Object, SQLException> function,
                                               Object... args) throws SQLException {
        Object toReturn;

        if (config.intentResolver.isIntentToMock()) {
            Object retVal = mock(config, fnKey, Optional.of(integerType), args);
            CubeResultSet mockResultSet = new CubeResultSet.Builder(config).metaData(metaData).resultSetInstanceId((int)retVal).build();
            return mockResultSet;
        }

        toReturn = function.apply(args);
        CubeResultSet cubeResultSet = new CubeResultSet.Builder(config).resultSet((ResultSet) toReturn).
                metaData(metaData).build();
        if (config.intentResolver.isIntentToRecord()) {
            record(cubeResultSet.getResultSetInstanceId(), config, fnKey, args);
        }

        return cubeResultSet;
    }

    public static Object recordOrMock(Config config, FnKey fnKey,
                                      UtilException.Function_WithExceptions<Object[], Object, SQLException> function,
                                      Object... args) throws SQLException {
        Object toReturn;

        if (config.intentResolver.isIntentToMock()) {
            return mock(config, fnKey, Optional.empty(), args);
        }

        toReturn = function.apply(args);
        if (config.intentResolver.isIntentToRecord()) {
            record(toReturn, config, fnKey, args);
        }

        return toReturn;
    }
}
