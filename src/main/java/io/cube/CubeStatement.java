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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Optional;

public class CubeStatement implements Statement {

    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(CubeStatement.class);
    private static Type type = new TypeToken<Integer>() {}.getType();
    private final Statement statement;
    private final CubeConnection cubeConnection;
    private final int statementInstanceId;
    private final Config config;
    private FnKey statementFnKey;
    private FnKey exFnKey;
    private FnKey gwFnKey;
    private FnKey icFnKey;

    public CubeStatement (Config config, int statementInstanceId) {
        this.statement = null;
        this.cubeConnection = null;
        this.config = config;
        this.statementInstanceId = statementInstanceId;
    }

    public CubeStatement (Statement statement, CubeConnection cubeConnection, Config config) {
        this.statement = statement;
        this.cubeConnection = cubeConnection;
        this.config = config;
        this.statementInstanceId = System.identityHashCode(this);
    }

    public int getStatementInstanceId() {
        return statementInstanceId;
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        if (null == statementFnKey) {
            Method method = new Object() {
            }.getClass().getEnclosingMethod();
            statementFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(statementFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(), Optional.of(type),
                    this.statementInstanceId, sql);
            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }
            CubeResultSet mockResultSet = new CubeResultSet.Builder(config).cubeStatement(this).resultSetInstanceId((int)ret.retVal).build();
            return mockResultSet;
        }

        CubeResultSet cubeResultSet = new CubeResultSet.Builder(config).resultSet(statement.executeQuery(sql)).query(sql).cubeStatement(this).build();
        if (config.intentResolver.isIntentToRecord()) {
            config.recorder.record(statementFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), cubeResultSet.getResultSetInstanceId(), FnReqResponse.RetStatus.Success,
                    Optional.empty(), Optional.empty(), this.statementInstanceId, sql);
        }

        return cubeResultSet;
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        return statement.executeUpdate(sql);
    }

    @Override
    public void close() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            statement.close();
        }
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        return statement.getMaxFieldSize();
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        statement.setMaxFieldSize(max);
    }

    @Override
    public int getMaxRows() throws SQLException {
        return statement.getMaxRows();
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        statement.setMaxRows(max);
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        statement.setEscapeProcessing(enable);
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        return statement.getQueryTimeout();
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        statement.setQueryTimeout(seconds);
    }

    @Override
    public void cancel() throws SQLException {
        statement.cancel();
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
                    this.statementInstanceId);
            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }

            return (SQLWarning) ret.retVal;
        }

        SQLWarning warnings = statement.getWarnings();
        if (config.intentResolver.isIntentToRecord()) {
            config.recorder.record(gwFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), warnings, FnReqResponse.RetStatus.Success,
                    Optional.empty(), this.statementInstanceId);
        }

        return warnings;
    }

    @Override
    public void clearWarnings() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            statement.clearWarnings();
        }
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        statement.setCursorName(name);
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        if (null == exFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            exFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, exFnKey, false, this.statementInstanceId, sql);
        }

        boolean toReturn = statement.execute(sql);
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, exFnKey, true, this.statementInstanceId, sql);
        }

        return toReturn;
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        return statement.getResultSet();
    }

    @Override
    public int getUpdateCount() throws SQLException {
        return statement.getUpdateCount();
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        return statement.getMoreResults();
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        statement.setFetchDirection(direction);
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return statement.getFetchDirection();
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        statement.setFetchSize(rows);
    }

    @Override
    public int getFetchSize() throws SQLException {
        return statement.getFetchSize();
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        return statement.getResultSetConcurrency();
    }

    @Override
    public int getResultSetType() throws SQLException {
        return statement.getResultSetType();
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        statement.addBatch(sql);
    }

    @Override
    public void clearBatch() throws SQLException {
        statement.clearBatch();
    }

    @Override
    public int[] executeBatch() throws SQLException {
        return statement.executeBatch();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return statement.getConnection();
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        return statement.getMoreResults(current);
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return statement.getGeneratedKeys();
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        return statement.executeUpdate(sql, autoGeneratedKeys);
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        return statement.executeUpdate(sql, columnIndexes);
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        return statement.executeUpdate(sql, columnNames);
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        return statement.execute(sql, autoGeneratedKeys);
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        return statement.execute(sql, columnIndexes);
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        return statement.execute(sql, columnNames);
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return statement.getResultSetHoldability();
    }

    @Override
    public boolean isClosed() throws SQLException {
        if (null == icFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            icFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, icFnKey, false, this.statementInstanceId);
        }

        boolean toReturn = statement.isClosed();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, icFnKey, true, this.statementInstanceId);
        }

        return toReturn;
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        statement.setPoolable(poolable);
    }

    @Override
    public boolean isPoolable() throws SQLException {
        return statement.isPoolable();
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        statement.closeOnCompletion();
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        return statement.isCloseOnCompletion();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return statement.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return statement.isWrapperFor(iface);
    }
}
