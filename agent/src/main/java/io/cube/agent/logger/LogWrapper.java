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

package io.cube.agent.logger;

import org.slf4j.Logger;
import org.slf4j.Marker;

public class LogWrapper implements Logger {
    private final Logger sl4jLogger;
    private final Logger cubeLogger;

    public LogWrapper(Logger sl4jLogger, Logger cubeLogger){
        this.sl4jLogger = sl4jLogger;
        this.cubeLogger = cubeLogger;
    }

    @Override
    public String getName() {
        return sl4jLogger.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return sl4jLogger.isTraceEnabled();
    }

    @Override
    public void trace(String s) {
        sl4jLogger.trace(s);
        cubeLogger.trace(s);
    }

    @Override
    public void trace(String s, Object o) {
        sl4jLogger.trace(s , o);
        cubeLogger.trace(s , o);
    }

    @Override
    public void trace(String s, Object o, Object o1) {
        sl4jLogger.trace(s, o, o1);
        cubeLogger.trace(s, o, o1);
    }

    @Override
    public void trace(String s, Object... objects) {
        sl4jLogger.trace(s , objects);
        cubeLogger.trace(s , objects);
    }

    @Override
    public void trace(String s, Throwable throwable) {
        sl4jLogger.trace(s, throwable);
        cubeLogger.trace(s, throwable);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return sl4jLogger.isTraceEnabled();
    }

    @Override
    public void trace(Marker marker, String s) {
        sl4jLogger.trace(marker , s);
        cubeLogger.trace(marker , s);
    }

    @Override
    public void trace(Marker marker, String s, Object o) {
        sl4jLogger.trace(marker , s , o);
        cubeLogger.trace(marker , s , o);
    }

    @Override
    public void trace(Marker marker, String s, Object o, Object o1) {
        sl4jLogger.trace(marker, s, o, o1);
        cubeLogger.trace(marker, s, o, o1);
    }

    @Override
    public void trace(Marker marker, String s, Object... objects) {
        sl4jLogger.trace(marker , s , objects);
        cubeLogger.trace(marker , s , objects);
    }

    @Override
    public void trace(Marker marker, String s, Throwable throwable) {
        sl4jLogger.trace(marker , s , throwable);
        cubeLogger.trace(marker , s , throwable);
    }

    @Override
    public boolean isDebugEnabled() {
        return sl4jLogger.isDebugEnabled();
    }

    @Override
    public void debug(String s) {
        sl4jLogger.debug(s);
        cubeLogger.debug(s);
    }

    @Override
    public void debug(String s, Object o) {
        sl4jLogger.debug(s , o);
        cubeLogger.debug(s , o);
    }

    @Override
    public void debug(String s, Object o, Object o1) {
        sl4jLogger.debug(s , o , o1);
        cubeLogger.debug(s , o , o1);
    }

    @Override
    public void debug(String s, Object... objects) {
        sl4jLogger.debug(s , objects);
        cubeLogger.debug(s , objects);
    }

    @Override
    public void debug(String s, Throwable throwable) {
        sl4jLogger.debug(s , throwable);
        cubeLogger.debug(s , throwable);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return sl4jLogger.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String s) {
        sl4jLogger.debug(marker, s);
        cubeLogger.debug(marker, s);
    }

    @Override
    public void debug(Marker marker, String s, Object o) {
        sl4jLogger.debug(marker , s , o);
        cubeLogger.debug(marker , s , o);
    }

    @Override
    public void debug(Marker marker, String s, Object o, Object o1) {
        sl4jLogger.debug(marker , s , o , o1);
        cubeLogger.debug(marker , s , o , o1);
    }

    @Override
    public void debug(Marker marker, String s, Object... objects) {
        sl4jLogger.debug(marker , s , objects);
        cubeLogger.debug(marker , s , objects);
    }

    @Override
    public void debug(Marker marker, String s, Throwable throwable) {
        sl4jLogger.debug(marker , s , throwable);
        cubeLogger.debug(marker , s , throwable);
    }

    @Override
    public boolean isInfoEnabled() {
        return sl4jLogger.isInfoEnabled();
    }

    @Override
    public void info(String s) {
        sl4jLogger.info(s);
        cubeLogger.info(s);
    }

    @Override
    public void info(String s, Object o) {
        sl4jLogger.info(s , o);
        cubeLogger.info(s , o);
    }

    @Override
    public void info(String s, Object o, Object o1) {
        sl4jLogger.info(s , o , o1);
        cubeLogger.info(s , o , o1);
    }

    @Override
    public void info(String s, Object... objects) {
        sl4jLogger.info(s , objects);
        cubeLogger.info(s , objects);
    }

    @Override
    public void info(String s, Throwable throwable) {
        sl4jLogger.info(s , throwable);
        cubeLogger.info(s , throwable);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return sl4jLogger.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String s) {
        sl4jLogger.info(marker , s);
        cubeLogger.info(marker , s);
    }

    @Override
    public void info(Marker marker, String s, Object o) {
        sl4jLogger.info(marker , s , o);
        cubeLogger.info(marker , s , o);
    }

    @Override
    public void info(Marker marker, String s, Object o, Object o1) {
        sl4jLogger.info(marker , s , o , o1);
        cubeLogger.info(marker , s , o , o1);
    }

    @Override
    public void info(Marker marker, String s, Object... objects) {
        sl4jLogger.info(marker ,s , objects);
        cubeLogger.info(marker ,s , objects);
    }

    @Override
    public void info(Marker marker, String s, Throwable throwable) {
        sl4jLogger.info(marker ,s , throwable);
        cubeLogger.info(marker ,s , throwable);
    }

    @Override
    public boolean isWarnEnabled() {
        return sl4jLogger.isWarnEnabled();
    }

    @Override
    public void warn(String s) {
        sl4jLogger.warn(s);
        cubeLogger.warn(s);
    }

    @Override
    public void warn(String s, Object o) {
        sl4jLogger.warn(s , o);
        cubeLogger.warn(s , o);
    }

    @Override
    public void warn(String s, Object... objects) {
        sl4jLogger.warn(s , objects);
        cubeLogger.warn(s , objects);
    }

    @Override
    public void warn(String s, Object o, Object o1) {
        sl4jLogger.warn(s , o , o1);
        cubeLogger.warn(s , o , o1);
    }

    @Override
    public void warn(String s, Throwable throwable) {
        sl4jLogger.warn(s , throwable);
        cubeLogger.warn(s , throwable);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return sl4jLogger.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String s) {
        sl4jLogger.warn(marker , s);
        cubeLogger.warn(marker , s);
    }

    @Override
    public void warn(Marker marker, String s, Object o) {
        sl4jLogger.warn(marker ,s , o);
        cubeLogger.warn(marker ,s , o);
    }

    @Override
    public void warn(Marker marker, String s, Object o, Object o1) {
        sl4jLogger.warn(marker , s , o , o1);
        cubeLogger.warn(marker , s , o , o1);
    }

    @Override
    public void warn(Marker marker, String s, Object... objects) {
        sl4jLogger.warn(marker ,s , objects);
        cubeLogger.warn(marker ,s , objects);
    }

    @Override
    public void warn(Marker marker, String s, Throwable throwable) {
        sl4jLogger.warn(marker ,s , throwable);
        cubeLogger.warn(marker ,s , throwable);
    }

    @Override
    public boolean isErrorEnabled() {
        return sl4jLogger.isErrorEnabled();
    }

    @Override
    public void error(String s) {
        sl4jLogger.error(s);
        cubeLogger.error(s);
    }

    @Override
    public void error(String s, Object o) {
        sl4jLogger.error(s , o );
        cubeLogger.error(s , o );
    }

    @Override
    public void error(String s, Object o, Object o1) {
        sl4jLogger.error(s , o , o1);
        cubeLogger.error(s , o , o1);
    }

    @Override
    public void error(String s, Object... objects) {
        sl4jLogger.error(s , objects);
        cubeLogger.error(s , objects);
    }

    @Override
    public void error(String s, Throwable throwable) {
        sl4jLogger.error(s , throwable);
        cubeLogger.error(s , throwable);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return sl4jLogger.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String s) {
        sl4jLogger.error(marker , s );
        cubeLogger.error(marker , s );
    }

    @Override
    public void error(Marker marker, String s, Object o) {
        sl4jLogger.error(marker ,s , o);
        cubeLogger.error(marker ,s , o);
    }

    @Override
    public void error(Marker marker, String s, Object o, Object o1) {
        sl4jLogger.error(marker , s , o , o1);
        cubeLogger.error(marker , s , o , o1);
    }

    @Override
    public void error(Marker marker, String s, Object... objects) {
        sl4jLogger.error(marker , s , objects);
        cubeLogger.error(marker , s , objects);
    }

    @Override
    public void error(Marker marker, String s, Throwable throwable) {
        sl4jLogger.error(marker , s , throwable);
        cubeLogger.error(marker , s , throwable);
    }
}
