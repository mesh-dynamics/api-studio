package io.cube;

import java.lang.reflect.Method;
import java.sql.ParameterMetaData;
import java.sql.SQLException;

import io.cube.agent.FnKey;

public class CubeParameterMetaData implements ParameterMetaData {
	private final ParameterMetaData parameterMetaData;
	private final CubePreparedStatement cubePreparedStatement;
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

	public CubeParameterMetaData (CubePreparedStatement cubePreparedStatement, Config config, int parameterMetaDataInstanceId) {
		this.parameterMetaData = null;
		this.cubePreparedStatement = cubePreparedStatement;
		this.config = config;
		this.parameterMetaDataInstanceId = parameterMetaDataInstanceId;
	}

	public CubeParameterMetaData (ParameterMetaData parameterMetaData, CubePreparedStatement cubePreparedStatement, Config config) {
		this.parameterMetaData = parameterMetaData;
		this.cubePreparedStatement = cubePreparedStatement;
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
			gpcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
				config.commonConfig.serviceName, method);
		}

		return (int) Utils.recordOrMock(config, gpcFnKey,
			(fnArgs) -> parameterMetaData.getParameterCount(), this.parameterMetaDataInstanceId);
	}

	@Override
	public int isNullable(int param) throws SQLException {
		if (null == inpFnKey) {
			Method method = new Object() {}.getClass().getEnclosingMethod();
			inpFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
				config.commonConfig.serviceName, method);
		}

		return (int) Utils.recordOrMock(config, inpFnKey,
			(fnArgs) -> parameterMetaData.isNullable(param), param, this.parameterMetaDataInstanceId);
	}

	@Override
	public boolean isSigned(int param) throws SQLException {
		if (null == ispFnKey) {
			Method method = new Object() {}.getClass().getEnclosingMethod();
			ispFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
				config.commonConfig.serviceName, method);
		}

		return (boolean) Utils.recordOrMock(config, ispFnKey,
			(fnArgs) -> parameterMetaData.isSigned(param), param, this.parameterMetaDataInstanceId);
	}

	@Override
	public int getPrecision(int param) throws SQLException {
		if (null == gppFnKey) {
			Method method = new Object() {}.getClass().getEnclosingMethod();
			gppFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
				config.commonConfig.serviceName, method);
		}

		return (int) Utils.recordOrMock(config, gppFnKey,
			(fnArgs) -> parameterMetaData.getPrecision(param), param, this.parameterMetaDataInstanceId);
	}

	@Override
	public int getScale(int param) throws SQLException {
		if (null == gspFnKey) {
			Method method = new Object() {}.getClass().getEnclosingMethod();
			gspFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
				config.commonConfig.serviceName, method);
		}

		return (int) Utils.recordOrMock(config, gspFnKey,
			(fnArgs) -> parameterMetaData.getScale(param), param, this.parameterMetaDataInstanceId);
	}

	@Override
	public int getParameterType(int param) throws SQLException {
		if (null == gptFnKey) {
			Method method = new Object() {}.getClass().getEnclosingMethod();
			gptFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
				config.commonConfig.serviceName, method);
		}

		return (int) Utils.recordOrMock(config, gptFnKey,
			(fnArgs) -> parameterMetaData.getParameterType(param), param, this.parameterMetaDataInstanceId);
	}

	@Override
	public String getParameterTypeName(int param) throws SQLException {
		if (null == gptnFnKey) {
			Method method = new Object() {}.getClass().getEnclosingMethod();
			gptnFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
				config.commonConfig.serviceName, method);
		}

		return (String) Utils.recordOrMock(config, gptnFnKey,
			(fnArgs) -> parameterMetaData.getParameterTypeName(param), param, this.parameterMetaDataInstanceId);
	}

	@Override
	public String getParameterClassName(int param) throws SQLException {
		if (null == gpcnFnKey) {
			Method method = new Object() {}.getClass().getEnclosingMethod();
			gpcnFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
				config.commonConfig.serviceName, method);
		}

		return (String) Utils.recordOrMock(config, gpcnFnKey,
			(fnArgs) -> parameterMetaData.getParameterClassName(param), param, this.parameterMetaDataInstanceId);
	}

	@Override
	public int getParameterMode(int param) throws SQLException {
		if (null == gpmFnKey) {
			Method method = new Object() {}.getClass().getEnclosingMethod();
			gpmFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
				config.commonConfig.serviceName, method);
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
