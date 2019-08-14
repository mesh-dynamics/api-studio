package io.cube;

import io.cube.agent.FnKey;
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

public class CubeResultSet implements ResultSet {

    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(CubeResultSet.class);
    private final ResultSet resultSet;
    private final CubeStatement cubeStatement;
    private final Config config;
    private final int resultSetInstanceId;
    private final CubeDatabaseMetaData metaData;
    private final String query;
    private int rowIndex;
    private int columnIndex; /* Needed for wasNull call */

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
    private FnKey gbysFnKey;
    private FnKey gbyscFnKey;
    private FnKey goFnKey;
    private FnKey gocFnKey;
    private FnKey gfdFnKey;
    private FnKey gfsFnKey;
    private FnKey gftFnKey;
    private FnKey gfcoFnKey;
    private FnKey ruFnKey;
    private FnKey riFnKey;
    private FnKey rdFnKey;
    private FnKey gobFnKey;
    private FnKey gobjFnKey;
    private FnKey guFnKey;
    private FnKey gurFnKey;
    private FnKey ghFnKey;
    private FnKey gnsFnKey;
    private FnKey gnscFnKey;
    private FnKey abFnKey;
    private FnKey reFnKey;

    public static class Builder {
        private final Config config;

        private ResultSet resultSet = null;
        private CubeStatement cubeStatement = null;
        private CubeDatabaseMetaData metaData = null;
        private int resultSetInstanceId = System.identityHashCode(this);
        private String query = null;
        private int rowIndex = 0;
        private int columnIndex = 0;

        public Builder(Config config) {
            this.config = config;
        }

        public Builder resultSet(ResultSet val) {
            resultSet = val;
            return this;
        }

        public Builder cubeStatement(CubeStatement val) {
            cubeStatement = val;
            return this;
        }

        public Builder metaData(CubeDatabaseMetaData val) {
            metaData = val;
            return this;
        }

        public Builder resultSetInstanceId(int val) {
            resultSetInstanceId = val;
            return this;
        }

        public Builder query(String val) {
            query = val;
            return this;
        }

        public Builder rowIndex(int val) {
            rowIndex = val;
            return this;
        }

        public Builder columnIndex(int val) {
            columnIndex = val;
            return this;
        }

        public CubeResultSet build() {
            return new CubeResultSet(this);
        }
    }

    private CubeResultSet(Builder builder) {
        config = builder.config;
        resultSet = builder.resultSet;
        cubeStatement = builder.cubeStatement;
        metaData = builder.metaData;
        resultSetInstanceId = builder.resultSetInstanceId;
        query = builder.query;
        rowIndex = builder.rowIndex;
        columnIndex = builder.columnIndex;
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

        boolean toReturn = (boolean) Utils.recordOrMock(config, nxtFnKey, (fnArgs) -> resultSet.next(),
                this.resultSetInstanceId, this.rowIndex);
        this.rowIndex = toReturn ? this.rowIndex++ : 0;
        this.columnIndex = 0;

        return toReturn;
    }

    @Override
    public void close() throws SQLException {
        this.rowIndex = 0;
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

        return (boolean) Utils.recordOrMock(config, wnFnKey, (fnArgs) -> resultSet.wasNull(),
                this.resultSetInstanceId, this.rowIndex, this.columnIndex);
    }

    @Override
    public String getString(int columnIndex) throws SQLException {
        if (null == gsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (String) Utils.recordOrMock(config, gsFnKey, (fnArgs) -> {
            int fnArg = (int)fnArgs[0];
            return resultSet.getString(fnArg);}, columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {
        if (null == gbFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (boolean) Utils.recordOrMock(config, gbFnKey, (fnArgs) -> {
            int fnArg = (int)fnArgs[0];
            return resultSet.getBoolean(fnArg);}, columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public byte getByte(int columnIndex) throws SQLException {
        if (null == gbyFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbyFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (byte) Utils.recordOrMock(config, gbyFnKey, (fnArgs) -> {
            int fnArg = (int)fnArgs[0];
            return resultSet.getByte(fnArg);}, columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public short getShort(int columnIndex) throws SQLException {
        if (null == gshcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gshcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (short) Utils.recordOrMock(config, gshcFnKey, (fnArgs) -> {
            int fnArg = (int)fnArgs[0];
            return resultSet.getShort(fnArg);}, columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public int getInt(int columnIndex) throws SQLException {
        if (null == gicFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gicFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (int) Utils.recordOrMock(config, gicFnKey, (fnArgs) -> {
            int fnArg = (int)fnArgs[0];
            return resultSet.getInt(fnArg);}, columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public long getLong(int columnIndex) throws SQLException {
        if (null == glcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            glcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (long) Utils.recordOrMock(config, glcFnKey, (fnArgs) -> {
            int fnArg = (int)fnArgs[0];
            return resultSet.getLong(fnArg);}, columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public float getFloat(int columnIndex) throws SQLException {
        if (null == gfFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gfFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (float) Utils.recordOrMock(config, gfFnKey, (fnArgs) -> {
            int fnArg = (int)fnArgs[0];
            return resultSet.getFloat(fnArg);}, columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public double getDouble(int columnIndex) throws SQLException {
        if (null == gdFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (Double) Utils.recordOrMock(config, gdFnKey, (fnArgs) -> {
            int fnArg = (int)fnArgs[0];
            return resultSet.getDouble(fnArg);}, columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        if (null == gbdcsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbdcsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (BigDecimal) Utils.recordOrMock(config, gbdcsFnKey, (fnArgs) -> {
            int fnArg1 = (int)fnArgs[0];
            int fnArg2 = (int)fnArgs[1];
            return resultSet.getBigDecimal(fnArg1, fnArg2);}, columnIndex, scale, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public byte[] getBytes(int columnIndex) throws SQLException {
        if (null == gbysFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbysFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (byte[]) Utils.recordOrMock(config, gbysFnKey, (fnArgs) -> {
            int fnArg = (int)fnArgs[0];
            return resultSet.getBytes(fnArg);}, columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Date getDate(int columnIndex) throws SQLException {
        if (null == gdciFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdciFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (Date) Utils.recordOrMock(config, gdciFnKey, (fnArgs) -> {
            int fnArg = (int)fnArgs[0];
            return resultSet.getDate(fnArg);}, columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Time getTime(int columnIndex) throws SQLException {
        if (null == gtiFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gtiFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (Time) Utils.recordOrMock(config, gtiFnKey, (fnArgs) -> {
            int fnArg = (int)fnArgs[0];
            return resultSet.getTime(fnArg);}, columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        if (null == gtFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gtFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (Timestamp) Utils.recordOrMock(config, gtFnKey, (fnArgs) -> {
            int fnArg = (int)fnArgs[0];
            return resultSet.getTimestamp(fnArg);}, columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        //TODO
        return null;
    }

    @Override
    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        //TODO
        return null;
    }

    @Override
    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        //TODO
        return null;
    }

    @Override
    public String getString(String columnLabel) throws SQLException {
        if (null == gscFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gscFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = findColumn(columnLabel);
        return (String) Utils.recordOrMock(config, gscFnKey, (fnArgs) -> {
            String fnArg = (String)fnArgs[0];
            return resultSet.getString(fnArg);}, columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public boolean getBoolean(String columnLabel) throws SQLException {
        if (null == gbcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = findColumn(columnLabel);
        return (boolean) Utils.recordOrMock(config, gbcFnKey, (fnArgs) -> {
            String fnArg = (String)fnArgs[0];
            return resultSet.getBoolean(fnArg);}, columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public byte getByte(String columnLabel) throws SQLException {
        if (null == gbyFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbyFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = findColumn(columnLabel);
        return (byte) Utils.recordOrMock(config, gbyFnKey, (fnArgs) -> {
            String fnArg = (String)fnArgs[0];
            return resultSet.getByte(fnArg);}, columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public short getShort(String columnLabel) throws SQLException {
        if (null == gshFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gshFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = findColumn(columnLabel);
        return (short) Utils.recordOrMock(config, gshFnKey, (fnArgs) -> {
            String fnArg = (String)fnArgs[0];
            return resultSet.getShort(fnArg);}, columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public int getInt(String columnLabel) throws SQLException {
        if (null == giFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            giFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = findColumn(columnLabel);
        return (int) Utils.recordOrMock(config, giFnKey, (fnArgs) -> {
            String fnArg = (String)fnArgs[0];
            return resultSet.getInt(fnArg);}, columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public long getLong(String columnLabel) throws SQLException {
        if (null == glclFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            glclFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = findColumn(columnLabel);
        return (long) Utils.recordOrMock(config, glclFnKey, (fnArgs) -> {
            String fnArg = (String)fnArgs[0];
            return resultSet.getLong(fnArg);}, columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public float getFloat(String columnLabel) throws SQLException {
        if (null == gfcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gfcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = findColumn(columnLabel);
        return (float) Utils.recordOrMock(config, gfcFnKey, (fnArgs) -> {
            String fnArg = (String)fnArgs[0];
            return resultSet.getFloat(fnArg);}, columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public double getDouble(String columnLabel) throws SQLException {
        if (null == gdcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = findColumn(columnLabel);
        return (double) Utils.recordOrMock(config, gdcFnKey, (fnArgs) -> {
            String fnArg = (String)fnArgs[0];
            return resultSet.getDouble(fnArg);}, columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
        if (null == gbdclsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbdclsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = findColumn(columnLabel);
        return (BigDecimal) Utils.recordOrMock(config, gbdclsFnKey, (fnArgs) -> {
            String fnArg1 = (String)fnArgs[0];
            int fnArg2 = (int)fnArgs[1];
            return resultSet.getBigDecimal(fnArg1, fnArg2);}, columnLabel, scale, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public byte[] getBytes(String columnLabel) throws SQLException {
        if (null == gbyscFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbyscFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = findColumn(columnLabel);
        return (byte[]) Utils.recordOrMock(config, gbyscFnKey, (fnArgs) -> {
            String fnArg = (String)fnArgs[0];
            return resultSet.getBytes(fnArg);}, columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Date getDate(String columnLabel) throws SQLException {
        if (null == gdclFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdclFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = findColumn(columnLabel);
        return (Date) Utils.recordOrMock(config, gdclFnKey, (fnArgs) -> {
            String fnArg = (String)fnArgs[0];
            return resultSet.getDate(fnArg);}, columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Time getTime(String columnLabel) throws SQLException {
        if (null == gticlFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gticlFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = findColumn(columnLabel);
        return (Time) Utils.recordOrMock(config, gticlFnKey, (fnArgs) -> {
            String fnArg = (String)fnArgs[0];
            return resultSet.getTime(fnArg);}, columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Timestamp getTimestamp(String columnLabel) throws SQLException {
        if (null == gtclFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gtclFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = findColumn(columnLabel);
        return (Timestamp) Utils.recordOrMock(config, gtclFnKey, (fnArgs) -> {
            String fnArg = (String)fnArgs[0];
            return resultSet.getTimestamp(fnArg);}, columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public InputStream getAsciiStream(String columnLabel) throws SQLException {
        //TODO
        return null;
    }

    @Override
    public InputStream getUnicodeStream(String columnLabel) throws SQLException {
        //TODO
        return null;
    }

    @Override
    public InputStream getBinaryStream(String columnLabel) throws SQLException {
        //TODO
        return null;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        if (null == gwFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gwFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (SQLWarning) Utils.recordOrMock(config, gwFnKey, (fnArgs) -> resultSet.getWarnings(), this.resultSetInstanceId, this.rowIndex);
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

        return (String) Utils.recordOrMock(config, gtclFnKey, (fnArgs) -> resultSet.getCursorName(), this.resultSetInstanceId);
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        //TODO
        return null;
    }

    @Override
    public Object getObject(int columnIndex) throws SQLException {
        if (null == goFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            goFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return Utils.recordOrMock(config, goFnKey, (fnArgs) -> {
            int fnArg = (int)fnArgs[0];
            return resultSet.getObject(fnArg);}, columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Object getObject(String columnLabel) throws SQLException {
        if (null == gocFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gocFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = findColumn(columnLabel);
        return Utils.recordOrMock(config, gocFnKey, (fnArgs) -> {
            String fnArg = (String)fnArgs[0];
            return resultSet.getObject(fnArg);}, columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public int findColumn(String columnLabel) throws SQLException {
        if (null == fcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            fcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int)Utils.recordOrMock(config, fcFnKey, (fnArgs) -> {
            String fnArg = (String)fnArgs[0];
            return resultSet.findColumn(fnArg);}, columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Reader getCharacterStream(int columnIndex) throws SQLException {
        //TODO
        return null;
    }

    @Override
    public Reader getCharacterStream(String columnLabel) throws SQLException {
        //TODO
        return null;
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        if (null == gbdFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbdFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (BigDecimal) Utils.recordOrMock(config, gbdFnKey, (fnArgs) -> {
            int fnArg = (int)fnArgs[0];
            return resultSet.getBigDecimal(fnArg);}, columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
        if (null == gbdcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gbdcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = findColumn(columnLabel);
        return (BigDecimal) Utils.recordOrMock(config, gbdcFnKey, (fnArgs) -> {
            String fnArg = (String)fnArgs[0];
            return resultSet.getBigDecimal(fnArg);}, columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public boolean isBeforeFirst() throws SQLException {
        if (null == ibfFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ibfFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, ibfFnKey, (fnArgs) -> resultSet.isBeforeFirst(),
                this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public boolean isAfterLast() throws SQLException {
        if (null == ialFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ialFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, ialFnKey, (fnArgs) -> resultSet.isAfterLast(),
                this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public boolean isFirst() throws SQLException {
        if (null == ifFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ifFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, ifFnKey, (fnArgs) -> resultSet.isFirst(),
                this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public boolean isLast() throws SQLException {
        if (null == ilFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ilFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, ilFnKey, (fnArgs) -> resultSet.isLast(),
                this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public void beforeFirst() throws SQLException {
        this.rowIndex = 0;
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.beforeFirst();
        }
    }

    @Override
    public void afterLast() throws SQLException {
        this.rowIndex = 0;
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

        boolean toReturn = (boolean) Utils.recordOrMock(config, fFnKey, (fnArgs) -> resultSet.first(),
                this.resultSetInstanceId);
        this.rowIndex = toReturn ? 1 : 0;

        return toReturn;
    }

    @Override
    public boolean last() throws SQLException {
        if (null == lFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            lFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.rowIndex = (int) Utils.recordOrMock(config, lFnKey, (fnArgs) -> resultSet.last() ? resultSet.getRow() : 0,
                this.resultSetInstanceId);
        return this.rowIndex == 0 ? false : true;
    }

    @Override
    public int getRow() throws SQLException {
        if (null == grFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            grFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, grFnKey, (fnArgs) -> resultSet.getRow(),
                this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public boolean absolute(int row) throws SQLException {
        if (null == abFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            abFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        boolean toReturn = (boolean) Utils.recordOrMock(config, abFnKey, (fnArgs) -> {
            int fnArg = (int)fnArgs[0];
            return resultSet.absolute(fnArg);}, row, this.resultSetInstanceId, this.rowIndex);
        this.rowIndex = toReturn && row >= 0 ? row : getRow();

        return toReturn;
    }

    @Override
    public boolean relative(int rows) throws SQLException {
        if (null == reFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            reFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        boolean toReturn = (boolean) Utils.recordOrMock(config, reFnKey, (fnArgs) -> {
            int fnArg = (int)fnArgs[0];
            return resultSet.relative(fnArg);}, rows, this.resultSetInstanceId, this.rowIndex);
        this.rowIndex = toReturn && rows >= 0 ? rows : getRow();

        return toReturn;
    }

    @Override
    public boolean previous() throws SQLException {
        if (null == prevFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            prevFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        boolean toReturn = (boolean) Utils.recordOrMock(config, prevFnKey, (fnArgs) -> resultSet.previous(),
                this.resultSetInstanceId, this.rowIndex);
        this.rowIndex = toReturn ? this.rowIndex-- : 0;
        this.columnIndex = 0; //reset column index

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
        if (null == gfdFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gfdFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gfdFnKey, (fnArgs) -> resultSet.getFetchDirection(),
                this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        if (!config.intentResolver.isIntentToMock()) {
            resultSet.setFetchSize(rows);
        }
    }

    @Override
    public int getFetchSize() throws SQLException {
        if (null == gfsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gfsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gfsFnKey, (fnArgs) -> resultSet.getFetchSize(),
                this.resultSetInstanceId);
    }

    @Override
    public int getType() throws SQLException {
        if (null == gftFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gftFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gftFnKey, (fnArgs) -> resultSet.getType(),
                this.resultSetInstanceId);
    }

    @Override
    public int getConcurrency() throws SQLException {
        if (null == gfcoFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gfcoFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, gfcoFnKey, (fnArgs) -> resultSet.getConcurrency(),
                this.resultSetInstanceId);
    }

    @Override
    public boolean rowUpdated() throws SQLException {
        if (null == ruFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ruFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, ruFnKey, (fnArgs) -> resultSet.rowUpdated(),
                this.resultSetInstanceId);
    }

    @Override
    public boolean rowInserted() throws SQLException {
        if (null == riFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            riFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, riFnKey, (fnArgs) -> resultSet.rowUpdated(),
                this.resultSetInstanceId);
    }

    @Override
    public boolean rowDeleted() throws SQLException {
        if (null == rdFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            rdFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, rdFnKey, (fnArgs) -> resultSet.rowUpdated(),
                this.resultSetInstanceId);
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
        return this.cubeStatement;
    }

    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
        if (null == gobFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gobFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return Utils.recordOrMock(config, gobFnKey, (fnArgs) -> {
            int fnArg1 = (int)fnArgs[0];
            Map<String, Class<?>> fnArg2 = (Map)fnArgs[1];
            return resultSet.getObject(fnArg1, fnArg2);}, columnIndex, map, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Ref getRef(int columnIndex) throws SQLException {
        //TODO
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
        //TODO
        return null;
    }

    @Override
    public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
        if (null == gobjFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gobjFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = findColumn(columnLabel);
        return Utils.recordOrMock(config, gobjFnKey, (fnArgs) -> {
            String fnArg1 = (String)fnArgs[0];
            Map<String, Class<?>> fnArg2 = (Map)fnArgs[1];
            return resultSet.getObject(fnArg1, fnArg2);}, columnLabel, map, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Ref getRef(String columnLabel) throws SQLException {
        //TODO
        return null;
    }

    @Override
    public Blob getBlob(String columnLabel) throws SQLException {
        //TODO
        return null;
    }

    @Override
    public Clob getClob(String columnLabel) throws SQLException {
        //TODO
        return null;
    }

    @Override
    public Array getArray(String columnLabel) throws SQLException {
        //TODO
        return null;
    }

    @Override
    public Date getDate(int columnIndex, Calendar cal) throws SQLException {
        if (null == gdcicFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdcicFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (Date) Utils.recordOrMock(config, gdcicFnKey, (fnArgs) -> {
            int fnArg1 = (int)fnArgs[0];
            Calendar fnArg2 = (Calendar)fnArgs[1];
            return resultSet.getDate(fnArg1, fnArg2);}, columnIndex, cal, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Date getDate(String columnLabel, Calendar cal) throws SQLException {
        if (null == gdclcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gdclcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = findColumn(columnLabel);
        return (Date) Utils.recordOrMock(config, gdclcFnKey, (fnArgs) -> {
            String fnArg1 = (String)fnArgs[0];
            Calendar fnArg2 = (Calendar)fnArgs[1];
            return resultSet.getDate(fnArg1, fnArg2);}, columnLabel, cal, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Time getTime(int columnIndex, Calendar cal) throws SQLException {
        if (null == gticFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gticFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (Time) Utils.recordOrMock(config, gticFnKey, (fnArgs) -> {
            int fnArg1 = (int)fnArgs[0];
            Calendar fnArg2 = (Calendar)fnArgs[1];
            return resultSet.getTime(fnArg1, fnArg2);}, columnIndex, cal, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Time getTime(String columnLabel, Calendar cal) throws SQLException {
        if (null == gticlcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gticlcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = findColumn(columnLabel);
        return (Time) Utils.recordOrMock(config, gticlcFnKey, (fnArgs) -> {
            String fnArg1 = (String)fnArgs[0];
            Calendar fnArg2 = (Calendar)fnArgs[1];
            return resultSet.getTime(fnArg1, fnArg2);}, columnLabel, cal, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
        if (null == gtcFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gtcFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (Timestamp) Utils.recordOrMock(config, gtcFnKey, (fnArgs) -> {
            int fnArg1 = (int)fnArgs[0];
            Calendar fnArg2 = (Calendar)fnArgs[1];
            return resultSet.getTimestamp(fnArg1, fnArg2);}, columnIndex, cal, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
        if (null == gtccFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gtccFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = findColumn(columnLabel);
        return (Timestamp) Utils.recordOrMock(config, gtccFnKey, (fnArgs) -> {
            String fnArg1 = (String)fnArgs[0];
            Calendar fnArg2 = (Calendar)fnArgs[1];
            return resultSet.getTimestamp(fnArg1, fnArg2);}, columnLabel, cal, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public URL getURL(int columnIndex) throws SQLException {
        if (null == guFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            guFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (URL) Utils.recordOrMock(config, guFnKey, (fnArgs) -> {
            int fnArg = (int)fnArgs[0];
            return resultSet.getURL(fnArg);}, columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public URL getURL(String columnLabel) throws SQLException {
        if (null == gurFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gurFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = findColumn(columnLabel);
        return (URL) Utils.recordOrMock(config, gurFnKey, (fnArgs) -> {
            String fnArg = (String)fnArgs[0];
            return resultSet.getURL(fnArg);}, columnLabel, this.resultSetInstanceId, this.rowIndex);
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
        //TODO
        return null;
    }

    @Override
    public RowId getRowId(String columnLabel) throws SQLException {
        //TODO
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
        if (null == ghFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            ghFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (int) Utils.recordOrMock(config, ghFnKey, (fnArgs) -> resultSet.getHoldability(), this.resultSetInstanceId);
    }

    @Override
    public boolean isClosed() throws SQLException {
        if (null == icFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            icFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        return (boolean) Utils.recordOrMock(config, icFnKey, (fnArgs) -> resultSet.isClosed(), this.resultSetInstanceId);
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
        //TODO
        return null;
    }

    @Override
    public NClob getNClob(String columnLabel) throws SQLException {
        //TODO
        return null;
    }

    @Override
    public SQLXML getSQLXML(int columnIndex) throws SQLException {
        //TODO
        return null;
    }

    @Override
    public SQLXML getSQLXML(String columnLabel) throws SQLException {
        //TODO
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
        if (null == gnsFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gnsFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = columnIndex;
        return (String) Utils.recordOrMock(config, gnsFnKey, (fnArgs) -> {
            int fnArg = (int)fnArgs[0];
            return resultSet.getNString(fnArg);}, columnIndex, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public String getNString(String columnLabel) throws SQLException {
        if (null == gnscFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            gnscFnKey = new FnKey(config.commonConfig.customerId, config.commonConfig.app, config.commonConfig.instance,
                    config.commonConfig.serviceName, method);
        }

        this.columnIndex = findColumn(columnLabel);
        return (String) Utils.recordOrMock(config, gnscFnKey, (fnArgs) -> {
            String fnArg = (String)fnArgs[0];
            return resultSet.getNString(fnArg);}, columnLabel, this.resultSetInstanceId, this.rowIndex);
    }

    @Override
    public Reader getNCharacterStream(int columnIndex) throws SQLException {
        //TODO
        return null;
    }

    @Override
    public Reader getNCharacterStream(String columnLabel) throws SQLException {
        //TODO
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
        //TODO
        return null;
    }

    @Override
    public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
        //TODO
        return null;
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
