package io.cube;

import static io.cube.Utils.getFnKey;

import java.lang.reflect.Method;
import java.sql.ParameterMetaData;
import java.sql.SQLException;

import io.md.utils.FnKey;

public class MDParameterMetaData implements ParameterMetaData {
	private final ParameterMetaData parameterMetaData;
	private final MDPreparedStatement mdPreparedStatement;
	private final Config config;
	private final int parameterMetaDataInstanceId;
	private FnKey gpcFnKey;
	private FnKey gppFnKey;
	private FnKey gptFnKey;
	private FnKey gptnFnKey;
	private FnKey gpcnFnKey;
	private FnKey gpmFnKey;
	private FnKey ispFnKey;
	private FnKey inpFnKey;
	private FnKey gspFnKey;

	public MDParameterMetaData(MDPreparedStatement mdPreparedStatement, Config config, int parameterMetaDataInstanceId) {
		this.parameterMetaData = null;
		this.mdPreparedStatement = mdPreparedStatement;
		this.config = config;
		this.parameterMetaDataInstanceId = parameterMetaDataInstanceId;
	}

	public MDParameterMetaData(ParameterMetaData parameterMetaData, MDPreparedStatement mdPreparedStatement, Config config) {
		this.parameterMetaData = parameterMetaData;
		this.mdPreparedStatement = mdPreparedStatement;
		this.config = config;
		this.parameterMetaDataInstanceId = System.identityHashCode(this);
	}

	public int getParameterMetaDataInstanceId() {
		return parameterMetaDataInstanceId;
	}

	@Override
	public int getParameterCount() throws SQLException {
		if (null == gpcFnKey) {
			Method method = new Object() {}.getClass().getEnclosingMethod();
			gpcFnKey = getFnKey(method);
		}

		return (int) Utils.recordOrMock(config, gpcFnKey,
			(fnArgs) -> parameterMetaData.getParameterCount(), this.parameterMetaDataInstanceId);
	}

	@Override
	public int isNullable(int param) throws SQLException {
		if (null == inpFnKey) {
			Method method = new Object() {}.getClass().getEnclosingMethod();
			inpFnKey = getFnKey(method);
		}

		return (int) Utils.recordOrMock(config, inpFnKey,
			(fnArgs) -> parameterMetaData.isNullable(param), param, this.parameterMetaDataInstanceId);
	}

	@Override
	public boolean isSigned(int param) throws SQLException {
		if (null == ispFnKey) {
			Method method = new Object() {}.getClass().getEnclosingMethod();
			ispFnKey = getFnKey(method);
		}

		return (boolean) Utils.recordOrMock(config, ispFnKey,
			(fnArgs) -> parameterMetaData.isSigned(param), param, this.parameterMetaDataInstanceId);
	}

	@Override
	public int getPrecision(int param) throws SQLException {
		if (null == gppFnKey) {
			Method method = new Object() {}.getClass().getEnclosingMethod();
			gppFnKey = getFnKey(method);
		}

		return (int) Utils.recordOrMock(config, gppFnKey,
			(fnArgs) -> parameterMetaData.getPrecision(param), param, this.parameterMetaDataInstanceId);
	}

	@Override
	public int getScale(int param) throws SQLException {
		if (null == gspFnKey) {
			Method method = new Object() {}.getClass().getEnclosingMethod();
			gspFnKey = getFnKey(method);
		}

		return (int) Utils.recordOrMock(config, gspFnKey,
			(fnArgs) -> parameterMetaData.getScale(param), param, this.parameterMetaDataInstanceId);
	}

	@Override
	public int getParameterType(int param) throws SQLException {
		if (null == gptFnKey) {
			Method method = new Object() {}.getClass().getEnclosingMethod();
			gptFnKey = getFnKey(method);
		}

		return (int) Utils.recordOrMock(config, gptFnKey,
			(fnArgs) -> parameterMetaData.getParameterType(param), param, this.parameterMetaDataInstanceId);
	}

	@Override
	public String getParameterTypeName(int param) throws SQLException {
		if (null == gptnFnKey) {
			Method method = new Object() {}.getClass().getEnclosingMethod();
			gptnFnKey = getFnKey(method);
		}

		return (String) Utils.recordOrMock(config, gptnFnKey,
			(fnArgs) -> parameterMetaData.getParameterTypeName(param), param, this.parameterMetaDataInstanceId);
	}

	@Override
	public String getParameterClassName(int param) throws SQLException {
		if (null == gpcnFnKey) {
			Method method = new Object() {}.getClass().getEnclosingMethod();
			gpcnFnKey = getFnKey(method);
		}

		return (String) Utils.recordOrMock(config, gpcnFnKey,
			(fnArgs) -> parameterMetaData.getParameterClassName(param), param, this.parameterMetaDataInstanceId);
	}

	@Override
	public int getParameterMode(int param) throws SQLException {
		if (null == gpmFnKey) {
			Method method = new Object() {}.getClass().getEnclosingMethod();
			gpmFnKey = getFnKey(method);
		}

		return (int) Utils.recordOrMock(config, gpmFnKey,
			(fnArgs) -> parameterMetaData.getParameterMode(param), param, this.parameterMetaDataInstanceId);
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		if (config.intentResolver.isIntentToMock()) {
			throw new SQLException("This method is not supported yet!");
		}
		return parameterMetaData.unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		if (config.intentResolver.isIntentToMock()) {
			throw new SQLException("This method is not supported yet!");
		}
		return parameterMetaData.isWrapperFor(iface);
	}
}
