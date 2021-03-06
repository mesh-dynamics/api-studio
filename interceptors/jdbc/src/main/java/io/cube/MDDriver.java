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
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;

import org.slf4j.LoggerFactory;

import com.google.gson.reflect.TypeToken;

import io.md.utils.FnKey;


public class MDDriver implements Driver {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MDDriver.class);
    private static final Driver INSTANCE = new MDDriver();
    private static Type integerType = new TypeToken<Integer>() {}.getType();
    private static final Config config;
    private int driverInstanceId;
    private Driver driver;
    private FnKey connectFnKey;
    private FnKey driverFnKey;
    private FnKey propertyFnKey;

    public MDDriver() {
    }

    public MDDriver(Driver driver, int instanceId) {
        this.driver = driver;
        this.driverInstanceId = instanceId;
    }

    static {
        try {
            DriverManager.registerDriver(INSTANCE);
            config = new Config();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not register MD Driver with DriverManager", e);
        }
    }

    /**
     * Get a Connection to the database from the real driver that this
     * wrapper is tracing on. In record phase, a MDConnection object which
     * wraps the real Connection is returned. In mock phase, the recorded
     * Connection object is returned.
     *
     * @param url  JDBC connection URL .
     * @param info a list of arbitrary string tag/value pairs as connection
     *             arguments. Normally at least a "user" and "password" property should
     *             be included.
     * @return a <code>Connection</code> object that represents a connection to
     * the URL.
     * @throws SQLException if a database access error occurs
     */
    @Override
    public Connection connect(String url, Properties info) throws SQLException {

        if (null == connectFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            connectFnKey = getFnKey(method);
        }

        Optional<MDDriver> realDriver = getRealDriver(url);

        String realUrl = extractRealURL(url);

        if (!realDriver.isPresent()) {
            throw new SQLException("Unable to find a driver that accepts the url " + realUrl);
        }

        if (config.intentResolver.isIntentToMock()) {
            Object retVal = Utils.mock(config, connectFnKey, Optional.of(integerType), url, info, realDriver.get().driverInstanceId);
            MDConnection mockConnection = new MDConnection(config, (int) retVal);
            return mockConnection;
        }

        Connection conn = realDriver.get().driver.connect(realUrl, info);

        if (null == conn) {
            throw new SQLException("Invalid or unknown driver url : " + realUrl);
        }

        MDConnection mdConnection = new MDConnection(conn, realDriver.get().driver, realUrl, config);
        if (config.intentResolver.isIntentToRecord()) {
            Utils.record(mdConnection.getConnectionInstanceId(), config, connectFnKey, url, info, realDriver.get().driverInstanceId);
        }

        return mdConnection;
    }

    /**
     * Given a jdbc:cube: type URL, find the underlying real driver
     * that accepts the URL.
     *
     * @param url JDBC connection URL.
     * @return Real driver for the given URL. Optional.empty is returned
     * if the URL is not a jdbc:cube: type URL or there is
     * no underlying driver that accepts the URL.
     * @throws SQLException if a database access error occurs.
     */
    private Optional<MDDriver> getRealDriver(String url) throws SQLException {
        if (null == url || url.trim().isEmpty()) {
            throw new IllegalArgumentException("url is required");
        }

        if (null == driverFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            driverFnKey = getFnKey(method);
        }

        Optional<Driver> realDriver;
        int instanceId;

        if (url.startsWith(getURLPrefix())) {

            String realUrl = extractRealURL(url);

            if (config.intentResolver.isIntentToMock()) {
                Object retVal = Utils.mock(config, driverFnKey, Optional.of(integerType), url);
                return Optional.of(new MDDriver(null, (int) retVal));
            }

            try {
                realDriver = Collections.list(DriverManager.getDrivers()).stream().
                        filter(driver -> {
                            LOGGER.debug("Found this driver : " + driver.getClass());
                            try {
                                return driver.acceptsURL(realUrl);
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        }).findFirst();

                instanceId = realDriver.map(System::identityHashCode).orElse(-1);

                if (config.intentResolver.isIntentToRecord()) {
                    Utils.record(instanceId, config, driverFnKey, url);
                }

                if (!realDriver.isPresent()) {
                    throw new SQLException(
                        "Unable to find the underlying real driver, Please check if the driver is part of the class path!");
                }

                return Optional.of(new MDDriver(realDriver.get(), instanceId));

            } catch (Exception e) {
                throw new SQLException(e);
            }
        }
        return Optional.empty();
    }

    /**
     * Given a <code>jdbc:cube:</code> type URL, extract and return the real URL
     *
     * @return Real URL client wanted to connect with.
     * if the URL is not a <code>jdbc:cube:</code> return the given input
     */
    private String extractRealURL(String url) {
        return url.replaceFirst(getURLPrefix(), "");
    }

    /**
     * Get the String used to prefix real URL
     *
     * @return the URL prefix
     */
    private String getURLPrefix() {
        return "jdbc:cube:";
    }

    /**
     * Returns true if this is a <code>jdbc:cube:</code> URL
     *
     * @param url JDBC URL.
     * @return true if this Driver can handle the URL.
     * @throws SQLException if a database access error occurs
     */
    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return url != null && url.startsWith(getURLPrefix());
    }

    /**
     * Gets information about the possible properties for the real driver.
     *
        * @param url  the URL of the database to which to connect
     * @param info a proposed list of tag/value pairs that will be sent on connect
     *             open
     * @return an array of <code>DriverPropertyInfo</code> objects describing
     * possible properties. This array may be an empty array if no
     * properties are required.
     * @throws SQLException if a database access error occurs
     */
    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        if (null == propertyFnKey) {
            Method method = new Object() {}.getClass().getEnclosingMethod();
            propertyFnKey = getFnKey(method);
        }

        return (DriverPropertyInfo[]) Utils.recordOrMock(config, propertyFnKey, (fnArgs) -> {
                String fnArg1 = (String)fnArgs[0];
                Properties fnArg2 = (Properties)fnArgs[1];
                Optional<MDDriver> realDriver = getRealDriver(fnArg1);
                DriverPropertyInfo[] propertyInfo;
                if (!realDriver.isPresent()) {
                    propertyInfo = new DriverPropertyInfo[0];
                } else {
                    propertyInfo = realDriver.get().driver.getPropertyInfo(fnArg1, fnArg2);
                }
            return propertyInfo;}, url, info);
    }

    /**
     * Returns 1 for the major version of the wrapped driver.
     *
     * @return Major version of the wrapped driver
     */
    @Override
    public int getMajorVersion() {
        return 1;
    }

    /**
     * Returns 0 for the minor version of the wrapped driver.
     *
     * @return Minor version of the wrapped driver
     */
    @Override
    public int getMinorVersion() {
        return 0;
    }

    /**
     * Returns true for the wrapped driver. Only to support jdbc compliant drivers.
     *
     * @return <code>true</code>
     */
    @Override
    public boolean jdbcCompliant() {
        return true;
    }

    /**
     * @return the parent Logger for the wrapped driver. Not implemented.
     * @throws SQLFeatureNotSupportedException if the driver does not use
     *                                         {@code java.util.logging}.
     */
    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("getParentLogger is not yet implemented for " + this.getClass().getName());
    }
}

