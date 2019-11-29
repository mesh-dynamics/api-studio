package io.cube;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Optional;

import com.google.gson.reflect.TypeToken;

import io.cube.agent.FnKey;

public class CubeStatement implements Statement {

    private static Type integerType = new TypeToken<Integer>() {}.getType();
    private final Statement statement;
    private final CubeConnection cubeConnection;
    private final Config config;
    private int moreResultsCount = 0;
    private FnKey exqFnKey;
    private FnKey exFnKey;
    private FnKey gwFnKey;
    private FnKey icFnKey;
    private FnKey ipFnKey;
    private FnKey exusagFnKey;
    private FnKey exusciFnKey;
    private FnKey exuscnFnKey;
    private FnKey exsagFnKey;
    private FnKey exsciFnKey;
    private FnKey exscnFnKey;
    private FnKey grshFnKey;
    private FnKey icocFnKey;
    private FnKey gmrFnKey;
    private FnKey gucFnKey;
    private FnKey gmrcFnKey;
    private FnKey gfdFnKey;
    private FnKey gfsFnKey;
    private FnKey exbFnKey;
    private FnKey grscFnKey;
    private FnKey grstFnKey;
    private FnKey gqtFnKey;
    private FnKey gmaxfsFnKey;
    private FnKey gmaxFnKey;
    private FnKey exusFnKey;
    private FnKey grsFnKey;
    private FnKey ggkFnKey;
    protected final int statementInstanceId;
    protected String lastExecutedQuery = null;

    public CubeStatement (CubeConnection cubeConnection, Config config, int statementInstanceId) {
        this.statement = null;
        this.cubeConnection = cubeConnection;
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
        if (null == exqFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            exqFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.lastExecutedQuery = sql;
        if (config.intentResolver.isIntentToMock()) {
            Object retVal = Utils.mock(config, exqFnKey, Optional.of(integerType), this.statementInstanceId, sql);
            CubeResultSet mockResultSet = new CubeResultSet.Builder(config).cubeStatement(this).resultSetInstanceId((int)retVal).build();
            return mockResultSet;
        }

        CubeResultSet cubeResultSet = new CubeResultSet.Builder(config).resultSet(statement.executeQuery(sql)).query(sql).cubeStatement(this).build();
        if (config.intentResolver.isIntentToRecord()) {
            Utils.record(cubeResultSet.getResultSetInstanceId(), config, exqFnKey, this.statementInstanceId, sql);
        }

        return cubeResultSet;
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        if (null == exusFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            exusFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.lastExecutedQuery = sql;
        return (int) Utils.recordOrMock(config, exusFnKey, (fnArgs) -> statement.executeUpdate(sql), sql, this.statementInstanceId);
    }

    @Override
    public void close() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            statement.close();
        }
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        if (null == gmaxfsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gmaxfsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gmaxfsFnKey, (fnArgs) -> statement.getMaxFieldSize(),  this.statementInstanceId);
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            statement.setMaxFieldSize(max);
        }
    }

    @Override
    public int getMaxRows() throws SQLException {
        if (null == gmaxFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gmaxFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gmaxFnKey, (fnArgs) -> statement.getMaxRows(), this.statementInstanceId);
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            statement.setMaxRows(max);
        }
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            statement.setEscapeProcessing(enable);
        }
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        if (null == gqtFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gqtFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gqtFnKey, (fnArgs) -> statement.getQueryTimeout(),  this.statementInstanceId);
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            statement.setQueryTimeout(seconds);
        }
    }

    @Override
    public void cancel() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            statement.cancel();
        }
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        if (null == gwFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gwFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (SQLWarning) Utils.recordOrMock(config, gwFnKey, (fnArgs) -> statement.getWarnings(),  this.statementInstanceId);
    }

    @Override
    public void clearWarnings() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            statement.clearWarnings();
        }
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            statement.setCursorName(name);
        }
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        if (null == exFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            exFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.lastExecutedQuery = sql;
        return (boolean) Utils.recordOrMock(config, exFnKey, (fnArgs) -> statement.execute(sql), sql, this.statementInstanceId);
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        if (null == grsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            grsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            Object retVal = Utils.mock(config, grsFnKey, Optional.of(integerType), this.lastExecutedQuery, this.statementInstanceId);
            CubeResultSet mockResultSet = new CubeResultSet.Builder(config).cubeStatement(this).resultSetInstanceId((int)retVal).build();
            return mockResultSet;
        }

        CubeResultSet cubeResultSet = new CubeResultSet.Builder(config).resultSet(statement.getResultSet()).cubeStatement(this).build();
        if (config.intentResolver.isIntentToRecord()) {
            Utils.record(cubeResultSet.getResultSetInstanceId(), config, grsFnKey, this.lastExecutedQuery, this.statementInstanceId);
        }

        return cubeResultSet;
    }

    @Override
    public int getUpdateCount() throws SQLException {
        if (null == gucFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gucFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gucFnKey, (fnArgs) -> statement.getUpdateCount(), this.lastExecutedQuery, this.statementInstanceId);
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        if (null == gmrFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gmrFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        boolean toReturn = (boolean) Utils.recordOrMock(config, gmrFnKey, (fnArgs) -> statement.getMoreResults(),
                 this.statementInstanceId, this.moreResultsCount, this.lastExecutedQuery);
        this.moreResultsCount = toReturn ? this.moreResultsCount+1 : 0;
        return toReturn;
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            statement.setFetchDirection(direction);
        }
    }

    @Override
    public int getFetchDirection() throws SQLException {
        if (null == gfdFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gfdFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gfdFnKey, (fnArgs) -> statement.getFetchDirection(), this.statementInstanceId);
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            statement.setFetchSize(rows);
        }
    }

    @Override
    public int getFetchSize() throws SQLException {
        if (null == gfsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gfsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gfsFnKey, (fnArgs) -> statement.getFetchSize(), this.statementInstanceId);
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        if (null == grscFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            grscFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, grscFnKey, (fnArgs) -> statement.getResultSetConcurrency(), this.statementInstanceId);
    }

    @Override
    public int getResultSetType() throws SQLException {
        if (null == grstFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            grstFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, grstFnKey, (fnArgs) -> statement.getResultSetType(), this.statementInstanceId);
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            statement.addBatch(sql);
        }
    }

    @Override
    public void clearBatch() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            statement.clearBatch();
        }
    }

    @Override
    public int[] executeBatch() throws SQLException {
        if (null == exbFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            exbFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int[]) Utils.recordOrMock(config, exbFnKey, (fnArgs) -> statement.executeBatch(), this.statementInstanceId);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return this.cubeConnection;
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        if (null == gmrcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gmrcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        boolean toReturn = (boolean) Utils.recordOrMock(config, gmrcFnKey,
                (fnArgs) -> statement.getMoreResults(current), current, this.statementInstanceId,
                this.moreResultsCount, this.lastExecutedQuery);
        this.moreResultsCount = toReturn ? this.moreResultsCount+1 : 0;

        return toReturn;
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        if (null == ggkFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ggkFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            Object retVal = Utils.mock(config, ggkFnKey, Optional.of(integerType), this.statementInstanceId);
            CubeResultSet mockResultSet = new CubeResultSet.Builder(config).cubeStatement(this).resultSetInstanceId((int)retVal).build();
            return mockResultSet;
        }

        CubeResultSet cubeResultSet = new CubeResultSet.Builder(config).resultSet(statement.getGeneratedKeys()).cubeStatement(this).build();
        if (config.intentResolver.isIntentToRecord()) {
            Utils.record(cubeResultSet.getResultSetInstanceId(), config, ggkFnKey, this.statementInstanceId);
        }

        return cubeResultSet;
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        if (null == exusagFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            exusagFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.lastExecutedQuery = sql;
        return (int) Utils.recordOrMock(config, exusagFnKey,
                (fnArgs) -> statement.executeUpdate(sql, autoGeneratedKeys),
                sql, autoGeneratedKeys, this.statementInstanceId);
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        if (null == exusciFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            exusciFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.lastExecutedQuery = sql;
        return (int) Utils.recordOrMock(config, exusciFnKey,
                (fnArgs) -> statement.executeUpdate(sql, columnIndexes), sql, columnIndexes, this.statementInstanceId);
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        if (null == exuscnFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            exuscnFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.lastExecutedQuery = sql;
        return (int) Utils.recordOrMock(config, exuscnFnKey,
                (fnArgs) -> statement.executeUpdate(sql, columnNames), sql, columnNames, this.statementInstanceId);
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        if (null == exsagFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            exsagFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.lastExecutedQuery = sql;
        return (boolean) Utils.recordOrMock(config, exsagFnKey,
                (fnArgs) -> statement.execute(sql, autoGeneratedKeys), sql, autoGeneratedKeys, this.statementInstanceId);
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        if (null == exsciFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            exsciFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.lastExecutedQuery = sql;
        return (boolean) Utils.recordOrMock(config, exsciFnKey,
                (fnArgs) -> statement.execute(sql, columnIndexes), sql, columnIndexes, this.statementInstanceId);
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        if (null == exscnFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            exscnFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.lastExecutedQuery = sql;
        return (boolean) Utils.recordOrMock(config, exscnFnKey,
                (fnArgs) -> statement.execute(sql, columnNames), sql, columnNames, this.statementInstanceId);
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        if (null == grshFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            grshFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, grshFnKey, (fnArgs) -> statement.getResultSetHoldability(), this.statementInstanceId);
    }

    @Override
    public boolean isClosed() throws SQLException {
        if (null == icFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            icFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, icFnKey, (fnArgs) -> statement.isClosed(), this.statementInstanceId);
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            statement.setPoolable(poolable);
        }
    }

    @Override
    public boolean isPoolable() throws SQLException {
        if (null == ipFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ipFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, ipFnKey, (fnArgs) -> statement.isPoolable(), this.statementInstanceId);
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            statement.closeOnCompletion();
        }
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        if (null == icocFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            icocFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, icocFnKey, (fnArgs) -> statement.isCloseOnCompletion(), this.statementInstanceId);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        //TODO
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        //TODO
        return false;
    }
}
