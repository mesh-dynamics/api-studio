package io.cube;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.reflect.TypeToken;

import io.cube.agent.CommonConfig;
import io.cube.agent.FnResponseObj;
import io.cube.agent.UtilException;
import io.md.dao.FnReqRespPayload.RetStatus;
import io.md.utils.FnKey;

/*
 * Created by IntelliJ IDEA.
 * Date: 2019-08-05
 * @author Ashoke S
 */
public class Utils {

	private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);
	private static Type integerType = new TypeToken<Integer>() {
	}.getType();

	public static void record(Object toReturn, Config config, FnKey fnKey, Object... args) {
		config.recorder.record(fnKey, toReturn, RetStatus.Success, Optional.empty(), args);
	}

	public static Object mock(Config config, FnKey fnKey, Optional<Type> returnType,
		Object... args) {
		FnResponseObj ret = config.mocker.mock(fnKey, Optional.empty(), returnType, args);
		if (ret.retStatus == RetStatus.Exception) {
			LOGGER.info("Throwing exception as a result of mocking function");
			UtilException.throwAsUnchecked((Throwable) ret.retVal);
		}

		return ret.retVal;
	}

	public static Object recordOrMockResultSet(Config config, FnKey fnKey,
		MDDatabaseMetaData metaData,
		UtilException.Function_WithExceptions<Object[], Object, SQLException> function,
		Object... args) throws SQLException {
		Object toReturn;

		if (config.intentResolver.isIntentToMock()) {
			Object retVal = mock(config, fnKey, Optional.of(integerType), args);
			MDResultSet mockResultSet = new MDResultSet.Builder(config).metaData(metaData)
				.resultSetInstanceId((int) retVal).build();
			return mockResultSet;
		}

		toReturn = function.apply(args);
		MDResultSet mdResultSet = new MDResultSet.Builder(config)
			.resultSet((ResultSet) toReturn).
				metaData(metaData).build();
		if (config.intentResolver.isIntentToRecord()) {
			record(mdResultSet.getResultSetInstanceId(), config, fnKey, args);
		}

		return mdResultSet;
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

	public static FnKey getFnKey(Method method) {
		CommonConfig commonConfig = CommonConfig.getInstance();
		return new FnKey(commonConfig.customerId, commonConfig.app, commonConfig.instance,
			commonConfig.serviceName, method);
	}
}
