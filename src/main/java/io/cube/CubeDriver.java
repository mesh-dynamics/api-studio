package io.cube;

import org.apache.logging.log4j.LogManager;

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

public class CubeDriver implements Driver {

    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(CubeDriver.class);

    /**
     * Get a Connection to the database from the real driver that this
     * wrapper is tracing on. In record phase, a CubeConnection object which
     * wraps the real Connection is returned. In mock phase, the recorded
     * Connection object is returned.
     *
     * @param url JDBC connection URL .
     * @param info a list of arbitrary string tag/value pairs as connection
     *        arguments. Normally at least a "user" and "password" property should
     *        be included.
     *
     * @return a <code>Connection</code> object that represents a connection to
     *         the URL.
     *
     * @throws SQLException if a database access error occurs
     */
    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        Optional<Driver> realDriver = getRealDriver(url);

        String realUrl = extractRealURL(url);

        if (!realDriver.isPresent()) {
            throw new SQLException("Unable to find a driver that accepts the url " + realUrl);
        }

        if (!realDriver.get().jdbcCompliant()) {
            throw new SQLException("Not JDBC compliant!");
        }

        Connection conn = realDriver.get().connect(realUrl, info);

        if (null == conn) {
            throw new SQLException("Invalid or unknown driver url : " + realUrl);
        }

        //Tracing data to be added.

        return conn;
    }

    /**
     * Given a jdbc:cube: type URL, find the underlying real driver
     * that accepts the URL.
     *
     * @param url JDBC connection URL.
     *
     * @return Real driver for the given URL. Optional.empty is returned
     *         if the URL is not a jdbc:cube: type URL or there is
     *         no underlying driver that accepts the URL.
     *
     * @throws SQLException if a database access error occurs.
     */
    private Optional<Driver> getRealDriver(String url) throws SQLException {
        if (null == url || url.trim().isEmpty()) {
            throw new IllegalArgumentException("url is required");
        }

        if (url.startsWith(getURLPrefix())) {

            String realUrl = extractRealURL(url);

            Optional<Driver> realDriver;
            try {
                realDriver = Collections.list(DriverManager.getDrivers()).stream().
                        filter(driver -> {
                            try {
                                return driver.acceptsURL(realUrl);
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        }).findFirst();

                return realDriver;
            } catch (Exception e) {
                throw new SQLException(e);
            }
        }
        return Optional.empty();
    }

    /**
     * Given a <code>jdbc:cube:</code> type URL, extract and return the real URL
     *
     *
     * @return Real URL client wanted to connect with.
     *         if the URL is not a <code>jdbc:cube:</code> return the given input
     *
     */
    private String extractRealURL(String url) {
        return url.replaceFirst(getURLPrefix(), "");
    }

    /**
     * Get the String used to prefix real URL
     *
     *
     * @return the URL prefix
     *
     */
    private String getURLPrefix() {
        return "jdbc:cube:";
    }

    /**
     * Returns true if this is a <code>jdbc:cube:</code> URL and if the URL is for
     * a real driver that is wrapped on
     *
     * @param url JDBC URL.
     *
     * @return true if this Driver can handle the URL.
     *
     * @throws SQLException if a database access error occurs
     */
    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return getRealDriver(url).isPresent();
    }

    /**
     * Gets information about the possible properties for the real driver.
     *
     * @param url the URL of the database to which to connect
     *
     * @param info a proposed list of tag/value pairs that will be sent on connect
     *        open
     * @return an array of <code>DriverPropertyInfo</code> objects describing
     *         possible properties. This array may be an empty array if no
     *         properties are required.
     *
     * @throws SQLException if a database access error occurs
     */
    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        Optional<Driver> realDriver = getRealDriver(url);

        if (realDriver.isEmpty()) {
            return new DriverPropertyInfo[0];
        }

        return realDriver.get().getPropertyInfo(url, info);
    }

    /**
     * Returns 1 for the major version of the wrapped driver.
     *
     * @return Major version of the wrapped driver
     *
     */
    @Override
    public int getMajorVersion() {
        return 1;
    }

    /**
     * Returns 0 for the minor version of the wrapped driver.
     *
     * @return Minor version of the wrapped driver
     *
     */
    @Override
    public int getMinorVersion() {
        return 0;
    }

    /**
     * Returns true for the wrapped driver. Only to support jdbc compliant drivers.
     *
     * @return <code>true</code>
     *
     */
    @Override
    public boolean jdbcCompliant() {
        return true;
    }

    /**
     *
     * @return the parent Logger for the wrapped driver. Not implemented.
     * @throws SQLFeatureNotSupportedException if the driver does not use
     * {@code java.util.logging}.
     *
     */
    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("getParentLogger is not yet implemented for " + this.getClass().getName());
    }
}

