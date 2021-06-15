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

package io.cube;

import static io.cube.Utils.getFnKey;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Optional;

import com.google.gson.reflect.TypeToken;

import io.md.utils.FnKey;


public class MDStatement implements Statement {

    private static Type integerType = new TypeToken<Integer>() {}.getType();
    private final Statement statement;
    private final MDConnection mdConnection;
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

    public MDStatement(MDConnection mdConnection, Config config, int statementInstanceId) {
        this.statement = null;
        this.mdConnection = mdConnection;
        this.config = config;
        this.statementInstanceId = statementInstanceId;
    }

    public MDStatement(Statement statement, MDConnection mdConnection, Config config) {
        this.statement = statement;
        this.mdConnection = mdConnection;
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
            exqFnKey = getFnKey(method);
        }

        this.lastExecutedQuery = sql;
        if (config.intentResolver.isIntentToMock()) {
            Object retVal = Utils.mock(config, exqFnKey, Optional.of(integerType), this.statementInstanceId, sql);
            MDResultSet mockResultSet = new MDResultSet.Builder(config).mdStatement(this).resultSetInstanceId((int)retVal).build();
            return mockResultSet;
        }

        MDResultSet mdResultSet = new MDResultSet.Builder(config).resultSet(statement.executeQuery(sql)).query(sql).mdStatement(this).build();
        if (config.intentResolver.isIntentToRecord()) {
            Utils.record(mdResultSet.getResultSetInstanceId(), config, exqFnKey, this.statementInstanceId, sql);
        }

        return mdResultSet;
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        if (null == exusFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            exusFnKey = getFnKey(method);
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
            gmaxfsFnKey = getFnKey(method);
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
            gmaxFnKey = getFnKey(method);
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
            gqtFnKey = getFnKey(method);
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
            gwFnKey = getFnKey(method);
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
            exFnKey = getFnKey(method);
        }

        this.lastExecutedQuery = sql;
        return (boolean) Utils.recordOrMock(config, exFnKey, (fnArgs) -> statement.execute(sql), sql, this.statementInstanceId);
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        if (null == grsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            grsFnKey = getFnKey(method);
        }

        if (config.intentResolver.isIntentToMock()) {
            Object retVal = Utils.mock(config, grsFnKey, Optional.of(integerType), this.lastExecutedQuery, this.statementInstanceId);
            MDResultSet mockResultSet = new MDResultSet.Builder(config).mdStatement(this).resultSetInstanceId((int)retVal).build();
            return mockResultSet;
        }

        MDResultSet mdResultSet = new MDResultSet.Builder(config).resultSet(statement.getResultSet()).mdStatement(this).build();
        if (config.intentResolver.isIntentToRecord()) {
            Utils.record(mdResultSet.getResultSetInstanceId(), config, grsFnKey, this.lastExecutedQuery, this.statementInstanceId);
        }

        return mdResultSet;
    }

    @Override
    public int getUpdateCount() throws SQLException {
        if (null == gucFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gucFnKey = getFnKey(method);
        }

        return (int) Utils.recordOrMock(config, gucFnKey, (fnArgs) -> statement.getUpdateCount(), this.lastExecutedQuery, this.statementInstanceId);
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        if (null == gmrFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gmrFnKey = getFnKey(method);
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
            gfdFnKey = getFnKey(method);
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
            gfsFnKey = getFnKey(method);
        }

        return (int) Utils.recordOrMock(config, gfsFnKey, (fnArgs) -> statement.getFetchSize(), this.statementInstanceId);
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        if (null == grscFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            grscFnKey = getFnKey(method);
        }

        return (int) Utils.recordOrMock(config, grscFnKey, (fnArgs) -> statement.getResultSetConcurrency(), this.statementInstanceId);
    }

    @Override
    public int getResultSetType() throws SQLException {
        if (null == grstFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            grstFnKey = getFnKey(method);
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
            exbFnKey = getFnKey(method);
        }

        return (int[]) Utils.recordOrMock(config, exbFnKey, (fnArgs) -> statement.executeBatch(), this.statementInstanceId);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return this.mdConnection;
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        if (null == gmrcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gmrcFnKey = getFnKey(method);
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
            ggkFnKey = getFnKey(method);
        }

        if (config.intentResolver.isIntentToMock()) {
            Object retVal = Utils.mock(config, ggkFnKey, Optional.of(integerType), this.statementInstanceId);
            MDResultSet mockResultSet = new MDResultSet.Builder(config).mdStatement(this).resultSetInstanceId((int)retVal).build();
            return mockResultSet;
        }

        MDResultSet mdResultSet = new MDResultSet.Builder(config).resultSet(statement.getGeneratedKeys()).mdStatement(this).build();
        if (config.intentResolver.isIntentToRecord()) {
            Utils.record(mdResultSet.getResultSetInstanceId(), config, ggkFnKey, this.statementInstanceId);
        }

        return mdResultSet;
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        if (null == exusagFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            exusagFnKey = getFnKey(method);
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
            exusciFnKey = getFnKey(method);
        }

        this.lastExecutedQuery = sql;
        return (int) Utils.recordOrMock(config, exusciFnKey,
                (fnArgs) -> statement.executeUpdate(sql, columnIndexes), sql, columnIndexes, this.statementInstanceId);
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        if (null == exuscnFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            exuscnFnKey = getFnKey(method);
        }

        this.lastExecutedQuery = sql;
        return (int) Utils.recordOrMock(config, exuscnFnKey,
                (fnArgs) -> statement.executeUpdate(sql, columnNames), sql, columnNames, this.statementInstanceId);
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        if (null == exsagFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            exsagFnKey = getFnKey(method);
        }

        this.lastExecutedQuery = sql;
        return (boolean) Utils.recordOrMock(config, exsagFnKey,
                (fnArgs) -> statement.execute(sql, autoGeneratedKeys), sql, autoGeneratedKeys, this.statementInstanceId);
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        if (null == exsciFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            exsciFnKey = getFnKey(method);
        }

        this.lastExecutedQuery = sql;
        return (boolean) Utils.recordOrMock(config, exsciFnKey,
                (fnArgs) -> statement.execute(sql, columnIndexes), sql, columnIndexes, this.statementInstanceId);
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        if (null == exscnFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            exscnFnKey = getFnKey(method);
        }

        this.lastExecutedQuery = sql;
        return (boolean) Utils.recordOrMock(config, exscnFnKey,
                (fnArgs) -> statement.execute(sql, columnNames), sql, columnNames, this.statementInstanceId);
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        if (null == grshFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            grshFnKey = getFnKey(method);
        }

        return (int) Utils.recordOrMock(config, grshFnKey, (fnArgs) -> statement.getResultSetHoldability(), this.statementInstanceId);
    }

    @Override
    public boolean isClosed() throws SQLException {
        if (null == icFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            icFnKey = getFnKey(method);
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
            ipFnKey = getFnKey(method);
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
            icocFnKey = getFnKey(method);
        }

        return (boolean) Utils.recordOrMock(config, icocFnKey, (fnArgs) -> statement.isCloseOnCompletion(), this.statementInstanceId);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return statement.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        if (config.intentResolver.isIntentToMock()) {
            throw new SQLException("This method is not supported yet!");
        }
        return statement.isWrapperFor(iface);
    }
}
