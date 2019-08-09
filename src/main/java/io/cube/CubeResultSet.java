package io.cube;

import io.cube.agent.CommonUtils;
import io.cube.agent.FnKey;
import io.cube.agent.FnReqResponse;
import io.cube.agent.FnResponseObj;
import org.apache.logging.log4j.LogManager;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;
import java.util.Optional;

public class CubeResultSet implements ResultSet {

    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(CubeResultSet.class);
    private final ResultSet resultSet;
    private final CubeStatement cubeStatement;
    private final Config config;
    private final int resultSetInstanceId;
    private CubeDatabaseMetaData metaData;
    private String query;
    private int rowIndex;
    private int columnIndex; /* Needed for wasNull call */

    private FnKey resultSetFnKey;
    private FnKey nxtFnKey;
    private FnKey wnFnKey;
    private FnKey gsFnKey;
    private FnKey gbFnKey;
    private FnKey gbcFnKey;
    private FnKey gscFnKey;
    private FnKey giFnKey;
    private FnKey gicFnKey;
    private FnKey glcFnKey;
    private FnKey gshFnKey;
    private FnKey gshcFnKey;
    private FnKey glclFnKey;
    private FnKey gcFnkey;
    private FnKey gbFnkey;
    private FnKey gbyFnKey;
    private FnKey ifFnKey;
    private FnKey ilFnKey;
    private FnKey ibfFnKey;
    private FnKey ialFnKey;
    private FnKey fFnKey;
    private FnKey lFnKey;
    private FnKey grFnKey;
    private FnKey gtFnKey;
    private FnKey gtcFnKey;
    private FnKey gtclFnKey;
    private FnKey gtccFnKey;
    private FnKey gfFnKey;
    private FnKey gfcFnKey;
    private FnKey gdFnKey;
    private FnKey gdcFnKey;
    private FnKey gbdFnKey;
    private FnKey gbdcFnKey;
    private FnKey gbdcsFnKey;
    private FnKey gbdclsFnKey;
    private FnKey gdclFnKey;
    private FnKey gdciFnKey;
    private FnKey gdcicFnKey;
    private FnKey gdclcFnKey;
    private FnKey gtiFnKey;
    private FnKey gticFnKey;
    private FnKey gticlFnKey;
    private FnKey gticlcFnKey;
    private FnKey gwFnKey;
    private FnKey gcnFnKey;
    private FnKey fcFnKey;
    private FnKey prevFnKey;
    private FnKey icFnKey;

    public CubeResultSet(Config config, int resultSetInstanceId) {
        this.resultSet = null;
        this.cubeStatement = null;
        this.rowIndex = -1;
        this.columnIndex = -1;
        this.config = config;
        this.resultSetInstanceId = resultSetInstanceId;
    }

    public CubeResultSet (ResultSet resultSet, CubeStatement cubeStatement, Config config) {
        this.resultSet = resultSet;
        this.cubeStatement = cubeStatement;
        this.rowIndex = -1;
        this.columnIndex = -1;
        this.config = config;
        this.resultSetInstanceId = System.identityHashCode(this);
    }

    public CubeResultSet (ResultSet resultSet, CubeDatabaseMetaData metaData, Config config) {
        this.resultSet = resultSet;
        this.cubeStatement = null;
        this.metaData = metaData;
        this.rowIndex = -1;
        this.columnIndex = -1;
        this.config = config;
        this.resultSetInstanceId = System.identityHashCode(this);
    }

    public CubeResultSet (ResultSet resultSet, String query, CubeStatement cubeStatement, Config config) {
        this.resultSet = resultSet;
        this.query = query;
        this.cubeStatement = cubeStatement;
        this.rowIndex = -1;
        this.columnIndex = -1;
        this.config = config;
        this.resultSetInstanceId = System.identityHashCode(this);
    }

    public int getResultSetInstanceId() {
        return resultSetInstanceId;
    }

    @Override
    public boolean next() throws SQLException {
        if (null == nxtFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            nxtFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        boolean toReturn = false;
        if (config.intentResolver.isIntentToMock()) {
            toReturn = Utils.recordOrMockBoolean(toReturn, config, nxtFnKey, false, this.resultSetInstanceId, this.rowIndex);
            this.rowIndex = toReturn ? this.rowIndex++ : -1;
            return toReturn;
        }

        toReturn = resultSet.next();
        this.rowIndex = toReturn ? this.rowIndex++ : -1;
        if (config.intentResolver.isIntentToRecord()) {
            Utils.recordOrMockBoolean(toReturn, config, nxtFnKey, true, this.resultSetInstanceId, this.rowIndex);
        }

        return toReturn;
    }

    @Override
    public void close() throws SQLException {
        this.rowIndex = -1;
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.close();
        }
    }

    @Override
    public boolean wasNull() throws SQLException {
        if (null == wnFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            wnFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, wnFnKey, false, this.resultSetInstanceId, this.rowIndex, this.columnIndex);
        }

        boolean toReturn = resultSet.wasNull();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, wnFnKey, true, this.resultSetInstanceId, this.rowIndex, this.columnIndex);
        }

        return toReturn;
    }

    @Override
    public String getString(int columnIndex) throws SQLException {
        if (null == gsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockString(null, config, gsFnKey, false, this.resultSetInstanceId, this.rowIndex, columnIndex);
        }

        String toReturn = resultSet.getString(columnIndex);
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockString(toReturn, config, gsFnKey, true, this.resultSetInstanceId, this.rowIndex, columnIndex);
        }

        return toReturn;
    }

    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {
        if (null == gbFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, gbFnKey, false, this.resultSetInstanceId, this.rowIndex, columnIndex);
        }

        boolean toReturn = resultSet.getBoolean(columnIndex);
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, gbFnKey, true, this.resultSetInstanceId, this.rowIndex, columnIndex);
        }

        return toReturn;
    }

    @Override
    public byte getByte(int columnIndex) throws SQLException {
        if (null == gbyFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbyFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        if (config.intentResolver.isIntentToMock()) {
            return (byte) Utils.recordOrMockLong(-1, config, gbyFnKey, false, this.resultSetInstanceId, this.rowIndex, columnIndex);
        }

        byte toReturn = resultSet.getByte(columnIndex);
        if (config.intentResolver.isIntentToRecord()) {
            return (byte) Utils.recordOrMockLong(toReturn, config, gbyFnKey, true, this.resultSetInstanceId, this.rowIndex, columnIndex);
        }

        return toReturn;
    }

    @Override
    public short getShort(int columnIndex) throws SQLException {
        if (null == gshcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gshcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        if (config.intentResolver.isIntentToMock()) {
            return (short) Utils.recordOrMockLong(-1, config, gshcFnKey, false, this.resultSetInstanceId, this.rowIndex, columnIndex);
        }

        short toReturn = resultSet.getShort(columnIndex);
        if (config.intentResolver.isIntentToRecord()) {
            return (short) Utils.recordOrMockLong(toReturn, config, gshcFnKey, true, this.resultSetInstanceId, this.rowIndex, columnIndex);
        }

        return toReturn;
    }

    @Override
    public int getInt(int columnIndex) throws SQLException {
        if (null == gicFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gicFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        if (config.intentResolver.isIntentToMock()) {
            return (int)Utils.recordOrMockLong(-1, config, gicFnKey, false, this.resultSetInstanceId, this.rowIndex, columnIndex);
        }

        int toReturn = resultSet.getInt(columnIndex);
        if (config.intentResolver.isIntentToRecord()) {
            return (int)Utils.recordOrMockLong(toReturn, config, gicFnKey, true, this.resultSetInstanceId, this.rowIndex, columnIndex);
        }

        return toReturn;
    }

    @Override
    public long getLong(int columnIndex) throws SQLException {
        if (null == glcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            glcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockLong(-1, config, glcFnKey, false, this.resultSetInstanceId, this.rowIndex, columnIndex);
        }

        long toReturn = resultSet.getLong(columnIndex);
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockLong(toReturn, config, glcFnKey, true, this.resultSetInstanceId, this.rowIndex, columnIndex);
        }

        return toReturn;
    }

    @Override
    public float getFloat(int columnIndex) throws SQLException {
        if (null == gfFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gfFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(gfFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(),
                    Optional.empty(), this.resultSetInstanceId, this.rowIndex, columnIndex);
            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }

            return (float) ret.retVal;
        }

        float toReturn = resultSet.getFloat(columnIndex);
        if (config.intentResolver.isIntentToRecord()) {
            config.recorder.record(gfFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), toReturn,
                    FnReqResponse.RetStatus.Success, Optional.empty(), this.resultSetInstanceId, this.rowIndex, columnIndex);
        }

        return toReturn;
    }

    @Override
    public double getDouble(int columnIndex) throws SQLException {
        if (null == gdFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(gdFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(),
                    Optional.empty(), this.resultSetInstanceId, this.rowIndex, columnIndex);
            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }

            return (double) ret.retVal;
        }

        double toReturn = resultSet.getDouble(columnIndex);
        if (config.intentResolver.isIntentToRecord()) {
            config.recorder.record(gdFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), toReturn,
                    FnReqResponse.RetStatus.Success, Optional.empty(), this.resultSetInstanceId, this.rowIndex, columnIndex);
        }

        return toReturn;
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        if (null == gbdcsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbdcsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBigDecimal(BigDecimal.valueOf(-1), config, gbdcsFnKey, false, this.resultSetInstanceId, this.rowIndex, columnIndex, scale);
        }

        BigDecimal toReturn = resultSet.getBigDecimal(columnIndex, scale);
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBigDecimal(toReturn, config, gbdcsFnKey, true, this.resultSetInstanceId, this.rowIndex, columnIndex, scale);
        }

        return toReturn;
    }

    @Override
    public byte[] getBytes(int columnIndex) throws SQLException {
        return new byte[0];
    }

    @Override
    public Date getDate(int columnIndex) throws SQLException {
        if (null == gdciFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdciFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockDate(null, config, gdciFnKey, false, this.resultSetInstanceId, this.rowIndex, columnIndex);
        }

        Date toReturn = resultSet.getDate(columnIndex);
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockDate(toReturn, config, gdciFnKey, true, this.resultSetInstanceId, this.rowIndex, columnIndex);
        }

        return toReturn;
    }

    @Override
    public Time getTime(int columnIndex) throws SQLException {
        if (null == gtiFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gtiFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockTime(null, config, gtiFnKey, false, this.resultSetInstanceId, this.rowIndex, columnIndex);
        }

        Time toReturn = resultSet.getTime(columnIndex);
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockTime(toReturn, config, gtiFnKey, true, this.resultSetInstanceId, this.rowIndex, columnIndex);
        }

        return toReturn;
    }

    @Override
    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        if (null == gtFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gtFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockTimestamp(null, config, gtFnKey, false, this.resultSetInstanceId, this.rowIndex, columnIndex);
        }

        Timestamp toReturn = resultSet.getTimestamp(columnIndex);
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockTimestamp(toReturn, config, gtFnKey, true, this.resultSetInstanceId, this.rowIndex, columnIndex);
        }

        return toReturn;
    }

    @Override
    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        if (null == gscFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gscFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockString(null, config, gscFnKey, false, this.resultSetInstanceId, this.rowIndex, columnLabel);
        }

        String toReturn = resultSet.getString(columnLabel);
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockString(toReturn, config, gscFnKey, true, this.resultSetInstanceId, this.rowIndex, columnLabel);
        }

        return toReturn;
    }

    @Override
    public boolean getBoolean(String columnLabel) throws SQLException {
        if (null == gbcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, gbcFnKey, false, this.resultSetInstanceId, this.rowIndex, columnLabel);
        }

        boolean toReturn = resultSet.getBoolean(columnLabel);
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, gbcFnKey, true, this.resultSetInstanceId, this.rowIndex, columnLabel);
        }

        return toReturn;
    }

    @Override
    public byte getByte(String columnLabel) throws SQLException {
        if (null == gbyFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbyFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return (byte) Utils.recordOrMockLong(-1, config, gbyFnKey, false, this.resultSetInstanceId, this.rowIndex, columnLabel);
        }

        byte toReturn = resultSet.getByte(columnLabel);
        if (config.intentResolver.isIntentToRecord()) {
            return (byte) Utils.recordOrMockLong(toReturn, config, gbyFnKey, true, this.resultSetInstanceId, this.rowIndex, columnLabel);
        }

        return toReturn;
    }

    @Override
    public short getShort(String columnLabel) throws SQLException {
        if (null == gshFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gshFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return (short)Utils.recordOrMockLong(-1, config, gshFnKey, false, this.resultSetInstanceId, this.rowIndex, columnLabel);
        }

        short toReturn = resultSet.getShort(columnLabel);
        if (config.intentResolver.isIntentToRecord()) {
            return (short)Utils.recordOrMockLong(toReturn, config, gshFnKey, true, this.resultSetInstanceId, this.rowIndex, columnLabel);
        }

        return toReturn;
    }

    @Override
    public int getInt(String columnLabel) throws SQLException {
        if (null == giFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            giFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return (int)Utils.recordOrMockLong(-1, config, giFnKey, false, this.resultSetInstanceId, this.rowIndex, columnLabel);
        }

        int toReturn = resultSet.getInt(columnLabel);
        if (config.intentResolver.isIntentToRecord()) {
            return (int)Utils.recordOrMockLong(toReturn, config, giFnKey, true, this.resultSetInstanceId, this.rowIndex, columnLabel);
        }

        return toReturn;
    }

    @Override
    public long getLong(String columnLabel) throws SQLException {
        if (null == glclFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            glclFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockLong(-1, config, glclFnKey, false, this.resultSetInstanceId, this.rowIndex, columnLabel);
        }

        long toReturn = resultSet.getLong(columnLabel);
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockLong(toReturn, config, glclFnKey, true, this.resultSetInstanceId, this.rowIndex, columnLabel);
        }

        return toReturn;
    }

    @Override
    public float getFloat(String columnLabel) throws SQLException {
        if (null == gfcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gfcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(gfcFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(),
                    Optional.empty(), this.resultSetInstanceId, this.rowIndex, columnLabel);
            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }

            return (float) ret.retVal;
        }

        float toReturn = resultSet.getFloat(columnLabel);
        if (config.intentResolver.isIntentToRecord()) {
            config.recorder.record(gfcFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), toReturn,
                    FnReqResponse.RetStatus.Success, Optional.empty(), this.resultSetInstanceId, this.rowIndex, columnLabel);
        }

        return toReturn;
    }

    @Override
    public double getDouble(String columnLabel) throws SQLException {
        if (null == gdcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            FnResponseObj ret = config.mocker.mock(gdcFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), Optional.empty(),
                    Optional.empty(), this.resultSetInstanceId, this.rowIndex, columnLabel);
            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }

            return (double) ret.retVal;
        }

        double toReturn = resultSet.getDouble(columnLabel);
        if (config.intentResolver.isIntentToRecord()) {
            config.recorder.record(gdcFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), toReturn,
                    FnReqResponse.RetStatus.Success, Optional.empty(), this.resultSetInstanceId, this.rowIndex, columnLabel);
        }

        return toReturn;
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
        if (null == gbdclsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbdclsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBigDecimal(BigDecimal.valueOf(-1), config, gbdclsFnKey, false, this.resultSetInstanceId, this.rowIndex, columnLabel, scale);
        }

        BigDecimal toReturn = resultSet.getBigDecimal(columnLabel, scale);
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBigDecimal(toReturn, config, gbdclsFnKey, true, this.resultSetInstanceId, this.rowIndex, columnLabel, scale);
        }

        return toReturn;
    }

    @Override
    public byte[] getBytes(String columnLabel) throws SQLException {
        return new byte[0];
    }

    @Override
    public Date getDate(String columnLabel) throws SQLException {
        if (null == gdclFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdclFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockDate(null, config, gdclFnKey, false, this.resultSetInstanceId, this.rowIndex, columnLabel);
        }

        Date toReturn = resultSet.getDate(columnLabel);
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockDate(toReturn, config, gdclFnKey, true, this.resultSetInstanceId, this,rowIndex, columnLabel);
        }

        return toReturn;
    }

    @Override
    public Time getTime(String columnLabel) throws SQLException {
        if (null == gticlFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gticlFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockTime(null, config, gticlFnKey, false, this.resultSetInstanceId, this.rowIndex, columnLabel);
        }

        Time toReturn = resultSet.getTime(columnLabel);
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockTime(toReturn, config, gticlFnKey, true, this.resultSetInstanceId, this.rowIndex, columnLabel);
        }

        return toReturn;
    }

    @Override
    public Timestamp getTimestamp(String columnLabel) throws SQLException {
        if (null == gtclFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gtclFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockTimestamp(null, config, gtclFnKey, false, this.resultSetInstanceId, this.rowIndex, columnLabel);
        }

        Timestamp toReturn = resultSet.getTimestamp(columnLabel);
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockTimestamp(toReturn, config, gtclFnKey, true, this.resultSetInstanceId, this.rowIndex, columnLabel);
        }

        return toReturn;
    }

    @Override
    public InputStream getAsciiStream(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public InputStream getUnicodeStream(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public InputStream getBinaryStream(String columnLabel) throws SQLException {
        return null;
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
                    this.resultSetInstanceId, this.rowIndex);
            if (ret.retStatus == FnReqResponse.RetStatus.Exception) {
                LOGGER.info("Throwing exception as a result of mocking function");
                UtilException.throwAsUnchecked((Throwable) ret.retVal);
            }

            return (SQLWarning) ret.retVal;
        }

        SQLWarning warnings = resultSet.getWarnings();
        if (config.intentResolver.isIntentToRecord()) {
            config.recorder.record(gwFnKey, CommonUtils.getCurrentTraceId(),
                    CommonUtils.getCurrentSpanId(), CommonUtils.getParentSpanId(), warnings, FnReqResponse.RetStatus.Success,
                    Optional.empty(), this.resultSetInstanceId, this.rowIndex);
        }

        return warnings;
    }

    @Override
    public void clearWarnings() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.clearWarnings();
        }
    }

    @Override
    public String getCursorName() throws SQLException {
        if (null == gcnFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gcnFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockString(null, config, gcnFnKey, false,
                    this.resultSetInstanceId);
        }

        String toReturn = resultSet.getCursorName();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockString(toReturn, config,
                    gcnFnKey, true, this.resultSetInstanceId);
        }

        return toReturn;
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        return null;
    }

    @Override
    public Object getObject(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public Object getObject(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public int findColumn(String columnLabel) throws SQLException {
        if (null == fcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            fcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return (int)Utils.recordOrMockLong(-1, config, fcFnKey, false, this.resultSetInstanceId, columnLabel);
        }

        int toReturn = resultSet.findColumn(columnLabel);
        if (config.intentResolver.isIntentToRecord()) {
            return (int)Utils.recordOrMockLong(toReturn, config, fcFnKey, true, this.resultSetInstanceId, columnLabel);
        }

        return toReturn;
    }

    @Override
    public Reader getCharacterStream(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public Reader getCharacterStream(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        if (null == gbdFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbdFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBigDecimal(BigDecimal.valueOf(-1), config, gbdFnKey, false, this.resultSetInstanceId, this.rowIndex, columnIndex);
        }

        BigDecimal toReturn = resultSet.getBigDecimal(columnIndex);
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBigDecimal(toReturn, config, gbdFnKey, true, this.resultSetInstanceId, this.rowIndex, columnIndex);
        }

        return toReturn;
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
        if (null == gbdcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbdcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBigDecimal(BigDecimal.valueOf(-1), config, gbdcFnKey, false, this.resultSetInstanceId, this.rowIndex, columnLabel);
        }

        BigDecimal toReturn = resultSet.getBigDecimal(columnLabel);
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBigDecimal(toReturn, config, gbdcFnKey, true, this.resultSetInstanceId, this.rowIndex, columnLabel);
        }

        return toReturn;
    }

    @Override
    public boolean isBeforeFirst() throws SQLException {
        if (null == ibfFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ibfFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, ibfFnKey, false, this.resultSetInstanceId);
        }

        boolean toReturn = resultSet.isBeforeFirst();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, ibfFnKey, true, this.resultSetInstanceId);
        }

        return toReturn;
    }

    @Override
    public boolean isAfterLast() throws SQLException {
        if (null == ialFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ialFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, ialFnKey, false, this.resultSetInstanceId);
        }

        boolean toReturn = resultSet.isAfterLast();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, ialFnKey, true, this.resultSetInstanceId);
        }

        return toReturn;
    }

    @Override
    public boolean isFirst() throws SQLException {
        if (null == ifFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ifFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, ifFnKey, false, this.resultSetInstanceId);
        }

        boolean toReturn = resultSet.isFirst();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, ifFnKey, true, this.resultSetInstanceId);
        }

        return toReturn;
    }

    @Override
    public boolean isLast() throws SQLException {
        if (null == ilFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ilFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, ilFnKey, false, this.resultSetInstanceId);
        }

        boolean toReturn = resultSet.isLast();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, ilFnKey, true, this.resultSetInstanceId);
        }

        return toReturn;
    }

    @Override
    public void beforeFirst() throws SQLException {
        this.rowIndex = -1;
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.beforeFirst();
        }
    }

    @Override
    public void afterLast() throws SQLException {
        this.rowIndex = -1;
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.afterLast();
        }
    }

    @Override
    public boolean first() throws SQLException {
        if (null == fFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            fFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        boolean toReturn;
        if (config.intentResolver.isIntentToMock()) {
            toReturn = Utils.recordOrMockBoolean(false, config, fFnKey, false, this.resultSetInstanceId);
            this.rowIndex = toReturn ? 0 : -1;
            return toReturn;
        }

        toReturn = resultSet.first();
        this.rowIndex = toReturn ? 0 : -1;
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, fFnKey, true, this.resultSetInstanceId);
        }

        return toReturn;
    }

    @Override
    public boolean last() throws SQLException {
        if (null == lFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            lFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        boolean toReturn;
        if (config.intentResolver.isIntentToMock()) {
            this.rowIndex = (int)Utils.recordOrMockLong(-1, config, lFnKey, false, this.resultSetInstanceId);
            return this.rowIndex == -1 ? false : true;
        }

        toReturn = resultSet.last();
        this.rowIndex = toReturn ? resultSet.getRow() : -1;
        if (config.intentResolver.isIntentToRecord()) {
            Utils.recordOrMockLong(this.rowIndex, config, lFnKey, true, this.resultSetInstanceId);
        }

        return toReturn;
    }

    @Override
    public int getRow() throws SQLException {
        if (null == grFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            grFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return (int)Utils.recordOrMockLong(-1, config, grFnKey, false, this.resultSetInstanceId, this.rowIndex);
        }

        int toReturn = resultSet.getRow();
        if (config.intentResolver.isIntentToRecord()) {
            return (int)Utils.recordOrMockLong(toReturn, config, grFnKey, true, this.resultSetInstanceId, this.rowIndex);
        }

        return toReturn;
    }

    @Override
    public boolean absolute(int row) throws SQLException {
        return false;
    }

    @Override
    public boolean relative(int rows) throws SQLException {
        return false;
    }

    @Override
    public boolean previous() throws SQLException {
        if (null == prevFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            prevFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        //Everytime previous() is called, columnIndex to be reset
        this.columnIndex = 0;

        boolean toReturn = false;
        if (config.intentResolver.isIntentToMock()) {
            toReturn = Utils.recordOrMockBoolean(toReturn, config, prevFnKey, false, this.resultSetInstanceId, this.rowIndex);
            if (toReturn) {
                this.rowIndex--;
            } else {
                this.rowIndex = -1;
            }
            return toReturn;
        }

        toReturn = resultSet.previous();
        if (config.intentResolver.isIntentToRecord()) {
            Utils.recordOrMockBoolean(toReturn, config, prevFnKey, true, this.resultSetInstanceId, this.rowIndex);
        }

        if (toReturn) {
            this.rowIndex--;
        } else {
            this.rowIndex = -1;
        }

        return toReturn;
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.setFetchDirection(direction);
        }
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return 0;
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.setFetchSize(rows);
        }
    }

    @Override
    public int getFetchSize() throws SQLException {
        return 0;
    }

    @Override
    public int getType() throws SQLException {
        return 0;
    }

    @Override
    public int getConcurrency() throws SQLException {
        return 0;
    }

    @Override
    public boolean rowUpdated() throws SQLException {
        return false;
    }

    @Override
    public boolean rowInserted() throws SQLException {
        return false;
    }

    @Override
    public boolean rowDeleted() throws SQLException {
        return false;
    }

    @Override
    public void updateNull(int columnIndex) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateNull(columnIndex);
        }
    }

    @Override
    public void updateBoolean(int columnIndex, boolean x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBoolean(columnIndex, x);
        }
    }

    @Override
    public void updateByte(int columnIndex, byte x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateByte(columnIndex, x);
        }
    }

    @Override
    public void updateShort(int columnIndex, short x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateShort(columnIndex, x);
        }
    }

    @Override
    public void updateInt(int columnIndex, int x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateInt(columnIndex, x);
        }
    }

    @Override
    public void updateLong(int columnIndex, long x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateLong(columnIndex, x);
        }
    }

    @Override
    public void updateFloat(int columnIndex, float x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateFloat(columnIndex, x);
        }
    }

    @Override
    public void updateDouble(int columnIndex, double x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateDouble(columnIndex, x);
        }
    }

    @Override
    public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBigDecimal(columnIndex, x);
        }
    }

    @Override
    public void updateString(int columnIndex, String x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateString(columnIndex, x);
        }
    }

    @Override
    public void updateBytes(int columnIndex, byte[] x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBytes(columnIndex, x);
        }
    }

    @Override
    public void updateDate(int columnIndex, Date x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateDate(columnIndex, x);
        }
    }

    @Override
    public void updateTime(int columnIndex, Time x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateTime(columnIndex, x);
        }
    }

    @Override
    public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateTimestamp(columnIndex, x);
        }
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateAsciiStream(columnIndex, x, length);
        }
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBinaryStream(columnIndex, x, length);
        }
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateCharacterStream(columnIndex, x, length);
        }
    }

    @Override
    public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateObject(columnIndex, x, scaleOrLength);
        }
    }

    @Override
    public void updateObject(int columnIndex, Object x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateObject(columnIndex, x);
        }
    }

    @Override
    public void updateNull(String columnLabel) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateNull(columnLabel);
        }
    }

    @Override
    public void updateBoolean(String columnLabel, boolean x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBoolean(columnLabel, x);
        }
    }

    @Override
    public void updateByte(String columnLabel, byte x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateByte(columnLabel, x);
        }
    }

    @Override
    public void updateShort(String columnLabel, short x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateShort(columnLabel, x);
        }
    }

    @Override
    public void updateInt(String columnLabel, int x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateInt(columnLabel, x);
        }
    }

    @Override
    public void updateLong(String columnLabel, long x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateLong(columnLabel, x);
        }
    }

    @Override
    public void updateFloat(String columnLabel, float x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateFloat(columnLabel, x);
        }
    }

    @Override
    public void updateDouble(String columnLabel, double x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateDouble(columnLabel, x);
        }
    }

    @Override
    public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBigDecimal(columnLabel, x);
        }
    }

    @Override
    public void updateString(String columnLabel, String x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateString(columnLabel, x);
        }
    }

    @Override
    public void updateBytes(String columnLabel, byte[] x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBytes(columnLabel, x);
        }
    }

    @Override
    public void updateDate(String columnLabel, Date x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateDate(columnLabel, x);
        }
    }

    @Override
    public void updateTime(String columnLabel, Time x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateTime(columnLabel, x);
        }
    }

    @Override
    public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateTimestamp(columnLabel, x);
        }
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateAsciiStream(columnLabel, x, length);
        }
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBinaryStream(columnLabel, x, length);
        }
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateCharacterStream(columnLabel, reader, length);
        }
    }

    @Override
    public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateObject(columnLabel, x, scaleOrLength);
        }
    }

    @Override
    public void updateObject(String columnLabel, Object x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateObject(columnLabel, x);
        }
    }

    @Override
    public void insertRow() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.insertRow();
        }
    }

    @Override
    public void updateRow() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateRow();
        }
    }

    @Override
    public void deleteRow() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.deleteRow();
        }
    }

    @Override
    public void refreshRow() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.refreshRow();
        }
    }

    @Override
    public void cancelRowUpdates() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.cancelRowUpdates();
        }
    }

    @Override
    public void moveToInsertRow() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.moveToInsertRow();
        }
    }

    @Override
    public void moveToCurrentRow() throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.moveToCurrentRow();
        }
    }

    @Override
    public Statement getStatement() throws SQLException {
        return null;
    }

    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
        return null;
    }

    @Override
    public Ref getRef(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public Blob getBlob(int columnIndex) throws SQLException {
        //TODO
        return null;
    }

    @Override
    public Clob getClob(int columnIndex) throws SQLException {
        //TODO
        return null;
    }

    @Override
    public Array getArray(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
        return null;
    }

    @Override
    public Ref getRef(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public Blob getBlob(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public Clob getClob(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public Array getArray(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        if (null == gdcicFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdcicFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockDate(null, config, gdcicFnKey, false, this.resultSetInstanceId, this.rowIndex, columnIndex, cal);
        }

        Date toReturn = resultSet.getDate(columnIndex, cal);
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockDate(toReturn, config, gdcicFnKey, true, this.resultSetInstanceId, this.rowIndex, columnIndex, cal);
        }

        return toReturn;
    }

    @Override
    public Date getDate(String columnLabel, Calendar cal) throws SQLException {
        if (null == gdclcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdclcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockDate(null, config, gdclcFnKey, false, this.resultSetInstanceId, this.rowIndex, columnLabel, cal);
        }

        Date toReturn = resultSet.getDate(columnLabel, cal);
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockDate(toReturn, config, gdclcFnKey, true, this.resultSetInstanceId, this.rowIndex, columnLabel, cal);
        }

        return toReturn;
    }

    @Override
    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        if (null == gticFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gticFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockTime(null, config, gticFnKey, false, this.resultSetInstanceId, this.rowIndex, columnIndex, cal);
        }

        Time toReturn = resultSet.getTime(columnIndex, cal);
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockTime(toReturn, config, gticFnKey, true, this.resultSetInstanceId, this.rowIndex, columnIndex, cal);
        }

        return toReturn;
    }

    @Override
    public Time getTime(String columnLabel, Calendar cal) throws SQLException {
        if (null == gticlcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gticlcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockTime(null, config, gticlcFnKey, false, this.resultSetInstanceId, this.rowIndex, columnLabel, cal);
        }

        Time toReturn = resultSet.getTime(columnLabel, cal);
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockTime(toReturn, config, gticlcFnKey, true, this.resultSetInstanceId, this.rowIndex, columnLabel, cal);
        }

        return toReturn;
    }

    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
        if (null == gtcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gtcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockTimestamp(null, config, gtcFnKey, false, this.resultSetInstanceId, this.rowIndex, columnIndex, cal);
        }

        Timestamp toReturn = resultSet.getTimestamp(columnIndex, cal);
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockTimestamp(toReturn, config, gtcFnKey, true, this.resultSetInstanceId, this.rowIndex, columnIndex, cal);
        }

        return toReturn;
    }

    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
        if (null == gtccFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gtccFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockTimestamp(null, config, gtccFnKey, false, this.resultSetInstanceId, this.rowIndex, columnLabel, cal);
        }

        Timestamp toReturn = resultSet.getTimestamp(columnLabel, cal);
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockTimestamp(toReturn, config, gtccFnKey, true, this.resultSetInstanceId, this.rowIndex, columnLabel, cal);
        }

        return toReturn;
    }

    @Override
    public URL getURL(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public URL getURL(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public void updateRef(int columnIndex, Ref x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateRef(columnIndex, x);
        }
    }

    @Override
    public void updateRef(String columnLabel, Ref x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateRef(columnLabel, x);
        }
    }

    @Override
    public void updateBlob(int columnIndex, Blob x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBlob(columnIndex, x);
        }
    }

    @Override
    public void updateBlob(String columnLabel, Blob x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBlob(columnLabel, x);
        }
    }

    @Override
    public void updateClob(int columnIndex, Clob x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateClob(columnIndex, x);
        }
    }

    @Override
    public void updateClob(String columnLabel, Clob x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateClob(columnLabel, x);
        }
    }

    @Override
    public void updateArray(int columnIndex, Array x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateArray(columnIndex, x);
        }
    }

    @Override
    public void updateArray(String columnLabel, Array x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateArray(columnLabel, x);
        }
    }

    @Override
    public RowId getRowId(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public RowId getRowId(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public void updateRowId(int columnIndex, RowId x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateRowId(columnIndex, x);
        }
    }

    @Override
    public void updateRowId(String columnLabel, RowId x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateRowId(columnLabel, x);
        }
    }

    @Override
    public int getHoldability() throws SQLException {
        return 0;
    }

    @Override
    public boolean isClosed() throws SQLException {
        if (null == icFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            icFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        if (config.intentResolver.isIntentToMock()) {
            return Utils.recordOrMockBoolean(false, config, icFnKey, false, this.resultSetInstanceId);
        }

        boolean toReturn = resultSet.isClosed();
        if (config.intentResolver.isIntentToRecord()) {
            return Utils.recordOrMockBoolean(toReturn, config, icFnKey, true, this.resultSetInstanceId);
        }

        return toReturn;
    }

    @Override
    public void updateNString(int columnIndex, String nString) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateNString(columnIndex, nString);
        }
    }

    @Override
    public void updateNString(String columnLabel, String nString) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateNString(columnLabel, nString);
        }
    }

    @Override
    public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateNClob(columnIndex, nClob);
        }
    }

    @Override
    public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateNClob(columnLabel, nClob);
        }
    }

    @Override
    public NClob getNClob(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public NClob getNClob(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateSQLXML(columnIndex, xmlObject);
        }
    }

    @Override
    public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateSQLXML(columnLabel, xmlObject);
        }
    }

    @Override
    public String getNString(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public String getNString(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        return null;
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateNCharacterStream(columnIndex, x, length);
        }
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateNCharacterStream(columnLabel, reader, length);
        }
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateAsciiStream(columnIndex, x, length);
        }
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBinaryStream(columnIndex, x, length);
        }
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateCharacterStream(columnIndex, x, length);
        }
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateAsciiStream(columnLabel, x, length);
        }
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBinaryStream(columnLabel, x, length);
        }
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateCharacterStream(columnLabel, reader, length);
        }
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBlob(columnIndex, inputStream, length);
        }
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBlob(columnLabel, inputStream, length);
        }
    }

    @Override
    public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateClob(columnIndex, reader, length);
        }
    }

    @Override
    public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateClob(columnLabel, reader, length);
        }
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateNClob(columnIndex, reader, length);
        }
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateNClob(columnLabel, reader, length);
        }
    }

    @Override
    public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateNCharacterStream(columnIndex, x);
        }
    }

    @Override
    public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateNCharacterStream(columnLabel, reader);
        }
    }

    @Override
    public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateAsciiStream(columnIndex, x);
        }
    }

    @Override
    public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBinaryStream(columnIndex, x);
        }
    }

    @Override
    public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateCharacterStream(columnIndex, x);
        }
    }

    @Override
    public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateAsciiStream(columnLabel, x);
        }
    }

    @Override
    public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBinaryStream(columnLabel, x);
        }
    }

    @Override
    public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateCharacterStream(columnLabel, reader);
        }
    }

    @Override
    public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBlob(columnIndex, inputStream);
        }
    }

    @Override
    public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateBlob(columnLabel, inputStream);
        }
    }

    @Override
    public void updateClob(int columnIndex, Reader reader) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateClob(columnIndex, reader);
        }
    }

    @Override
    public void updateClob(String columnLabel, Reader reader) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateClob(columnLabel, reader);
        }
    }

    @Override
    public void updateNClob(int columnIndex, Reader reader) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateNClob(columnIndex, reader);
        }
    }

    @Override
    public void updateNClob(String columnLabel, Reader reader) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.updateNClob(columnLabel, reader);
        }
    }

    @Override
    public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
        return null;
    }

    @Override
    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        return null;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return null;
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;
    }
}
