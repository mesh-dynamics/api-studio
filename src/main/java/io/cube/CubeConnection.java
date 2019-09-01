package io.cube;

import com.google.gson.reflect.TypeToken;
import io.cube.agent.CommonUtils;
import io.cube.agent.FnKey;
import io.cube.agent.FnReqResponse;
import io.cube.agent.FnResponseObj;
import io.cube.agent.UtilException;
import org.apache.logging.log4j.LogManager;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Executor;

public class CubeConnection implements Connection {

    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(CubeConnection.class);
    private static Type type = new TypeToken<Integer>() {}.getType();
    private final Driver driver;
    private final Connection connection;
    private final String url;
    private final Config config;
    private final int connectionInstanceId;
    private FnKey csFnKey;
    private FnKey psFnkey;
    private FnKey pcFnKey;
    private FnKey irFnKey;
    private FnKey gntFnKey;
    private FnKey icFnKey;
    private FnKey ivFnKey;
    private FnKey gmdFnkey;
    private FnKey gtiFnKey;
    private FnKey gacFnKey;
    private FnKey gcFnKey;
    private FnKey nsFnKey;
    private FnKey gwFnKey;
    private FnKey ghFnKey;
    private FnKey csrsFnKey;
    private FnKey csrshFnKey;
    private FnKey psaFnkey;
    private FnKey pscFnkey;
    private FnKey pscnFnkey;
    private FnKey psrsFnkey;
    private FnKey psrshFnkey;
    private FnKey pcrsFnKey;
    private FnKey pcrshFnKey;
    private FnKey ssFnKey;
    private FnKey ssnFnKey;
    private FnKey gsFnKey;
    private FnKey ccFnkey;
    private FnKey cbFnkey;
    private FnKey cncFnkey;
    private FnKey caoFnkey;
    private FnKey cstFnkey;
    private FnKey gciFnKey;
    private FnKey gcipFnkey;

    public CubeConnection(Config config, int connectionInstanceId) {
        this.driver = null;
        this.connection = null;
        this.url = null;
        this.config = config;
        this.connectionInstanceId = connectionInstanceId;
    }

    public CubeConnection(Connection connection, Driver driver, String url, Config config) {
        this.connection = connection;
        this.driver = driver;
        this.url = url;
        this.config = config;
        this.connectionInstanceId = System.identityHashCode(this);
    }

    public int getConnectionInstanceId() {
        return connectionInstanceId;
    }

    @Override
    public Statement createStatement() throws SQLException {
        if (null == csFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            csFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(csFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(),
                    Optional.of(type), this.connectionInstanceId);

            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }

            CubeStatement mockStatement = new CubeStatement(this, config, (int) ret.retVal);
            return mockStatement;
        }

        CubeStatement cubeStatement = new CubeStatement(connection.createStatement(), this, config);
        if (config.intentResolver.isIntentToRecord()) {
            config.recorder.record(csFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), cubeStatement.getStatementInstanceId(),
                    FnReqResponse.RetStatus.Success, Optional.empty(), this.connectionInstanceId);
        }

        return cubeStatement;
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        if (null == psFnkey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            psFnkey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(psFnkey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(),
                    Optional.of(type), this.connectionInstanceId, sql);
            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }
            CubePreparedStatement mockStatement = new CubePreparedStatement(this, config, (int)ret.retVal);

            return mockStatement;
        }

        CubePreparedStatement cubePreparedStatement = new CubePreparedStatement(connection.prepareStatement(sql), sql, this, config);
        if (config.intentResolver.isIntentToRecord()) {
            config.recorder.record(psFnkey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), cubePreparedStatement.getStatementInstanceId(),
                    FnReqResponse.RetStatus.Success, Optional.empty(), this.connectionInstanceId, sql);
        }

        return cubePreparedStatement;
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        if (null == pcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            pcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(pcFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(),
                    Optional.of(type), this.connectionInstanceId, sql);
            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }
            CubeCallableStatement mockStatement = new CubeCallableStatement(this, config, (int)ret.retVal);
            return mockStatement;
        }

        CubeCallableStatement cubeCallableStatement = new CubeCallableStatement(connection.prepareCall(sql), sql, this, config);
        if (config.intentResolver.isIntentToRecord()) {
            config.recorder.record(pcFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), cubeCallableStatement.getStatementInstanceId(),
                    FnReqResponse.RetStatus.Success, Optional.empty(), this.connectionInstanceId, sql);
        }

        return cubeCallableStatement;
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        if (null == nsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            nsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockString(null, config, nsFnKey,
                    false, this.connectionInstanceId, sql);
        }

        String toReturn = connection.nativeSQL(sql);
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockString(toReturn, config, nsFnKey,
                    true, this.connectionInstanceId, sql);
        }

        return toReturn;
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.setAutoCommit(autoCommit);
        }
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        if (null == gacFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gacFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, gacFnKey,
                    false, this.connectionInstanceId);
        }

        boolean toReturn = connection.getAutoCommit();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, gacFnKey,
                    true, this.connectionInstanceId);
        }

        return toReturn;
    }

    @Override
    public void commit() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.commit();
        }
    }

    @Override
    public void rollback() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.rollback();
        }
    }

    @Override
    public void close() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.close();
        }
    }

    @Override
    public boolean isClosed() throws SQLException {
        if (null == icFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            icFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, icFnKey, false, this.connectionInstanceId);
        }

        boolean toReturn = connection.isClosed();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, icFnKey, true, this.connectionInstanceId);
        }

        return toReturn;
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        if (null == gmdFnkey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gmdFnkey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(gmdFnkey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(),
                    Optional.of(type), this.connectionInstanceId);
            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }
            CubeDatabaseMetaData mockMetadata = new CubeDatabaseMetaData(config, (int)ret.retVal);

            return mockMetadata;
        }

        CubeDatabaseMetaData metaData = new CubeDatabaseMetaData(connection.getMetaData(), this, config);
        if (config.intentResolver.isIntentToRecord()) {
            config.recorder.record(gmdFnkey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), metaData.getMetadataInstanceId(),
                    FnReqResponse.RetStatus.Success, Optional.empty(), this.connectionInstanceId);
        }

         return metaData;
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.setReadOnly(readOnly);
        }
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        if (null == irFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            irFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, irFnKey, false, this.connectionInstanceId);
        }

        boolean toReturn = connection.isReadOnly();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, irFnKey, true, this.connectionInstanceId);
        }

        return toReturn;
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.setCatalog(catalog);
        }
    }

    @Override
    public String getCatalog() throws SQLException {
        if (null == gcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockString(null, config, gcFnKey, false,
                    this.connectionInstanceId);
        }

        String toReturn = connection.getCatalog();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockString(toReturn, config,
                    gcFnKey, true, this.connectionInstanceId);
        }

        return toReturn;
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.setTransactionIsolation(level);
        }
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        if (null == gtiFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gtiFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return (int)Utils.recordOrMockLong(-1, config, gtiFnKey, false, this.connectionInstanceId);
        }

        int toReturn = connection.getTransactionIsolation();
        if (config.intentResolver.isIntentToRecord()) {
            return (int)Utils.recordOrMockLong(toReturn, config, gtiFnKey, true, this.connectionInstanceId);
        }

        return toReturn;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        if (null == gwFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gwFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(gwFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(), Optional.empty(),
                    this.connectionInstanceId);
            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }

            return (SQLWarning) ret.retVal;
        }

        SQLWarning warnings = connection.getWarnings();
        if (config.intentResolver.isIntentToRecord()) {
            config.recorder.record(gwFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), warnings, FnReqResponse.RetStatus.Success,
                    Optional.empty(), this.connectionInstanceId);
        }

        return warnings;
    }

    @Override
    public void clearWarnings() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.clearWarnings();
        }
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        if (null == csrsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            csrsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(csrsFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(),
                    Optional.of(type), this.connectionInstanceId, resultSetType, resultSetConcurrency);

            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }

            CubeStatement mockStatement = new CubeStatement(this, config, (int)ret.retVal);
            return mockStatement;
        }

        CubeStatement cubeStatement = new CubeStatement(connection.createStatement(resultSetType, resultSetConcurrency), this, config);
        if (config.intentResolver.isIntentToRecord()) {
            config.recorder.record(csrsFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), cubeStatement.getStatementInstanceId(),
                    FnReqResponse.RetStatus.Success, Optional.empty(), this.connectionInstanceId, resultSetType, resultSetConcurrency);
        }

        return cubeStatement;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        if (null == psrsFnkey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            psrsFnkey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(psrsFnkey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(),
                    Optional.of(type), this.connectionInstanceId, sql, resultSetType, resultSetConcurrency);
            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }
            CubePreparedStatement mockStatement = new CubePreparedStatement(this, config, (int)ret.retVal);
            return mockStatement;
        }

        CubePreparedStatement cubePreparedStatement = new CubePreparedStatement(connection.prepareStatement(sql, resultSetType, resultSetConcurrency), sql,this, config);
        if (config.intentResolver.isIntentToRecord()) {
            config.recorder.record(psrsFnkey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), cubePreparedStatement.getStatementInstanceId(),
                    FnReqResponse.RetStatus.Success, Optional.empty(), this.connectionInstanceId, sql, resultSetType, resultSetConcurrency);
        }

        return cubePreparedStatement;
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        if (null == pcrsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            pcrsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(pcrsFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(),
                    Optional.of(type), this.connectionInstanceId, sql, resultSetType, resultSetConcurrency);
            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }
            CubeCallableStatement mockStatement = new CubeCallableStatement(this, config, (int)ret.retVal);
            return mockStatement;
        }

        CubeCallableStatement cubeCallableStatement = new CubeCallableStatement(connection.prepareCall(sql, resultSetType, resultSetConcurrency), sql, this, config);
        if (config.intentResolver.isIntentToRecord()) {
            config.recorder.record(pcrsFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), cubeCallableStatement.getStatementInstanceId(),
                    FnReqResponse.RetStatus.Success, Optional.empty(), this.connectionInstanceId, sql, resultSetType, resultSetConcurrency);
        }

        return cubeCallableStatement;
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return connection.getTypeMap();
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.setTypeMap(map);
        }
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.setHoldability(holdability);
        }
    }

    @Override
    public int getHoldability() throws SQLException {
        if (null == ghFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ghFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return (int)Utils.recordOrMockLong(-1, config, ghFnKey, false, this.connectionInstanceId);
        }

        int toReturn = connection.getHoldability();
        if (config.intentResolver.isIntentToRecord()) {
            return (int)Utils.recordOrMockLong(toReturn, config, ghFnKey, true, this.connectionInstanceId);
        }

        return toReturn;
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        if (null == ssFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ssFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(ssFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(),
                    Optional.of(type), this.connectionInstanceId);
            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }
            CubeSavepoint mockSavepoint = new CubeSavepoint(config, (int)ret.retVal);
            return mockSavepoint;
        }

        CubeSavepoint cubeSavepoint = new CubeSavepoint(connection.setSavepoint(), this, config);
        if (config.intentResolver.isIntentToRecord()) {
            config.recorder.record(ssFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), cubeSavepoint.getSavepointInstanceId(),
                    FnReqResponse.RetStatus.Success, Optional.empty(), this.connectionInstanceId);
        }

        return cubeSavepoint;
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        if (null == ssnFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ssnFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(ssnFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(),
                    Optional.of(type), this.connectionInstanceId, name);
            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }
            CubeSavepoint mockSavepoint = new CubeSavepoint(config, (int)ret.retVal);

            return mockSavepoint;
        }

        CubeSavepoint cubeSavepoint = new CubeSavepoint(connection.setSavepoint(name), this, config);
        if (config.intentResolver.isIntentToRecord()) {
            config.recorder.record(ssnFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), cubeSavepoint.getSavepointInstanceId(),
                    FnReqResponse.RetStatus.Success, Optional.empty(), this.connectionInstanceId, name);
        }

        return cubeSavepoint;
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.rollback(savepoint);
        }
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.releaseSavepoint(savepoint);
        }
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        if (null == csrshFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            csrshFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(csrshFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(),
                    Optional.of(type), this.connectionInstanceId, resultSetType, resultSetConcurrency,resultSetHoldability);

            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }

            CubeStatement mockStatement = new CubeStatement(this, config, (int)ret.retVal);
            return mockStatement;
        }

        CubeStatement cubeStatement = new CubeStatement(connection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability), this, config);
        if (config.intentResolver.isIntentToRecord()) {
            config.recorder.record(csrshFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), cubeStatement.getStatementInstanceId(),
                    FnReqResponse.RetStatus.Success, Optional.empty(), this.connectionInstanceId, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        return cubeStatement;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        if (null == psrshFnkey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            psrshFnkey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(psrshFnkey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(),
                    Optional.of(type), this.connectionInstanceId, sql, resultSetType, resultSetConcurrency, resultSetHoldability);
            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }
            CubePreparedStatement mockStatement = new CubePreparedStatement(this, config, (int)ret.retVal);
            return mockStatement;
        }

        CubePreparedStatement cubePreparedStatement = new CubePreparedStatement(connection.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability), sql, this, config);
        if (config.intentResolver.isIntentToRecord()) {
            config.recorder.record(psrshFnkey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), cubePreparedStatement.getStatementInstanceId(),
                    FnReqResponse.RetStatus.Success, Optional.empty(), this.connectionInstanceId, sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        return cubePreparedStatement;
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        if (null == pcrshFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            pcrshFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(pcrshFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(),
                    Optional.of(type), this.connectionInstanceId, sql, resultSetType, resultSetConcurrency, resultSetHoldability);
            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }
            CubeCallableStatement mockStatement = new CubeCallableStatement(this, config, (int)ret.retVal);
            return mockStatement;
        }

        CubeCallableStatement cubeCallableStatement = new CubeCallableStatement(connection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability), sql, this, config);
        if (config.intentResolver.isIntentToRecord()) {
            config.recorder.record(pcrshFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), cubeCallableStatement.getStatementInstanceId(),
                    FnReqResponse.RetStatus.Success, Optional.empty(), this.connectionInstanceId, sql, resultSetType, resultSetConcurrency, resultSetHoldability);
        }

        return cubeCallableStatement;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        if (null == psaFnkey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            psaFnkey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(psaFnkey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(),
                    Optional.of(type), this.connectionInstanceId, sql, autoGeneratedKeys);
            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }
            CubePreparedStatement mockStatement = new CubePreparedStatement(this, config, (int)ret.retVal);
            return mockStatement;
        }

        CubePreparedStatement cubePreparedStatement = new CubePreparedStatement(connection.prepareStatement(sql, autoGeneratedKeys), sql, this, config);
        if (config.intentResolver.isIntentToRecord()) {
            config.recorder.record(psaFnkey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), cubePreparedStatement.getStatementInstanceId(),
                    FnReqResponse.RetStatus.Success, Optional.empty(), this.connectionInstanceId, sql, autoGeneratedKeys);
        }

        return cubePreparedStatement;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        if (null == pscFnkey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            pscFnkey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(pscFnkey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(),
                    Optional.of(type), this.connectionInstanceId, sql, columnIndexes);
            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }
            CubePreparedStatement mockStatement = new CubePreparedStatement(this, config, (int)ret.retVal);
            return mockStatement;
        }

        CubePreparedStatement cubePreparedStatement = new CubePreparedStatement(connection.prepareStatement(sql, columnIndexes), sql, this, config);
        if (config.intentResolver.isIntentToRecord()) {
            config.recorder.record(pscFnkey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), cubePreparedStatement.getStatementInstanceId(),
                    FnReqResponse.RetStatus.Success, Optional.empty(), this.connectionInstanceId, sql, columnIndexes);
        }

        return cubePreparedStatement;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        if (null == pscnFnkey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            pscnFnkey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(pscnFnkey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(),
                    Optional.of(type), this.connectionInstanceId, sql, columnNames);
            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }
            CubePreparedStatement mockStatement = new CubePreparedStatement(this, config, (int)ret.retVal);
            return mockStatement;
        }

        CubePreparedStatement cubePreparedStatement = new CubePreparedStatement(connection.prepareStatement(sql, columnNames), sql, this, config);
        if (config.intentResolver.isIntentToRecord()) {
            config.recorder.record(pscnFnkey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), cubePreparedStatement.getStatementInstanceId(),
                    FnReqResponse.RetStatus.Success, Optional.empty(), this.connectionInstanceId, sql, columnNames);
        }

        return cubePreparedStatement;
    }

    @Override
    public Clob createClob() throws SQLException {
        //TODO
        return null;
    }

    @Override
    public Blob createBlob() throws SQLException {
        //TODO
        return null;
    }

    @Override
    public NClob createNClob() throws SQLException {
        //TODO
        return null;
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return connection.createSQLXML();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        if (null == ivFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ivFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, ivFnKey, false, this.connectionInstanceId, timeout);
        }

        boolean toReturn = connection.isValid(timeout);
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, ivFnKey, true, this.connectionInstanceId, timeout);
        }

        return toReturn;
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.setClientInfo(name, value);
        }
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.setClientInfo(properties);
        }
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        if (null == gciFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gciFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockString(null, config, gciFnKey, false, this.connectionInstanceId, name);
        }

        String toReturn = connection.getClientInfo(name);
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockString(toReturn, config, gciFnKey, true, this.connectionInstanceId, name);
        }

        return toReturn;
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        //TODO
        return null;
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        if (null == caoFnkey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            caoFnkey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(caoFnkey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(),
                    Optional.empty(), this.connectionInstanceId, typeName, elements);
            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }

            return (Array) ret.retVal;
        }

        Array toReturn = connection.createArrayOf(typeName, elements);
        if (config.intentResolver.isIntentToRecord()) {
            config.recorder.record(caoFnkey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), toReturn,
                    FnReqResponse.RetStatus.Success, Optional.empty(), this.connectionInstanceId, typeName, elements);
        }

        return toReturn;
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        if (null == cstFnkey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            cstFnkey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(cstFnkey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(),
                    Optional.empty(), this.connectionInstanceId, typeName, attributes);
            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }

            return (Struct) ret.retVal;
        }

        Struct toReturn = connection.createStruct(typeName, attributes);
        if (config.intentResolver.isIntentToRecord()) {
            config.recorder.record(cstFnkey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), toReturn,
                    FnReqResponse.RetStatus.Success, Optional.empty(), this.connectionInstanceId, typeName, attributes);
        }

        return toReturn;
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.setSchema(schema);
        }
    }

    @Override
    public String getSchema() throws SQLException {
        if (null == gsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockString(null, config, gsFnKey, false, this.connectionInstanceId);
        }

        String toReturn = connection.getSchema();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockString(toReturn, config, gsFnKey, true, this.connectionInstanceId);
        }

        return toReturn;
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.abort(executor);
        }
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            connection.setNetworkTimeout(executor, milliseconds);
        }
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        if (null == gntFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gntFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return (int)Utils.recordOrMockLong(-1, config, gntFnKey, false, this.connectionInstanceId);
        }

        int toReturn = connection.getNetworkTimeout();
        if (config.intentResolver.isIntentToRecord()) {
            return (int)Utils.recordOrMockLong(toReturn, config, gntFnKey, true, this.connectionInstanceId);
        }

        return toReturn;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return connection.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return connection.isWrapperFor(iface);
    }
}
