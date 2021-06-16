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

import io.md.logger.LogStoreDTO;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

//Todo
public class CubeLogger implements Logger {

    private final String source;
    private final CubeWsClient wsClient;
    private final Level level;

    public CubeLogger(String source , Level level, CubeWsClient wsClient){
        this.source  =source;
        this.level = level;
        this.wsClient = wsClient;
    }
    @Override
    public String getName() {
        return "CubeLogger";
    }

    private boolean isLevelEnabled(Level other){
        return CubeLogMgr.isLoggingEnabled() && level.compareTo(other) >=0 ;
    }

    private void log(String message , Level level){

        CubeDeployment cubeDeply = CubeLogMgr.cubeDeployment;
        LogStoreDTO dto = new LogStoreDTO(cubeDeply.app , cubeDeply.instance , cubeDeply.service , cubeDeply.customerId , cubeDeply.version,  source , message , level);

        if (!CubeLogMgr.msgPackTransport) this.wsClient.send(LogUtils.toJson(dto));
        else this.wsClient.send(LogUtils.toMsgPack(dto));
    }
    private void log(String message , Throwable throwable ,  Level level){
        log(String.format("%s : %s", message , ExceptionUtils.getStackTrace(throwable) ) , level);
    }

    private void logInt(Level level, String s ){
        if(isLevelEnabled(level)){
            log(s , level);
        }
    }
    private void logInt(Level level, String s,Object o ){
        if(isLevelEnabled(level)){
            log(MessageFormatter.format(s , o).getMessage(), level);
        }
    }
    private void logInt(Level level, String s, Object o, Object o1 ){
        if(isLevelEnabled(level)){
            log(MessageFormatter.format(s , o , o1).getMessage(), level);
        }
    }
    private void logInt(Level level, String s, Object...objects ){
        if(isLevelEnabled(level)){
            FormattingTuple ft =  MessageFormatter.arrayFormat(s , objects);
            log(ft.getMessage() , ft.getThrowable() , level);
        }
    }
    private void logInt(Level level, String s, Throwable throwable){
        if(isLevelEnabled(level)){
            log(s, throwable , level);
        }
    }

    @Override
    public boolean isTraceEnabled() {
        return isLevelEnabled(Level.TRACE);
    }

    @Override
    public void trace(String s) {
        logInt(Level.TRACE , s);
    }

    @Override
    public void trace(String s, Object o) {
        logInt(Level.TRACE , s , o);
    }

    @Override
    public void trace(String s, Object o, Object o1) {
        logInt(Level.TRACE , s , o , o1);
    }

    @Override
    public void trace(String s, Object... objects) {
        logInt(Level.TRACE , s , objects);
    }

    @Override
    public void trace(String s, Throwable throwable) {
        logInt(Level.TRACE , s , throwable);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return false;
    }
    
    private void notSupported(){
        throw new UnsupportedOperationException("Marker Operations are not supported");
    }

    @Override
    public void trace(Marker marker, String s) {
        notSupported();
    }

    @Override
    public void trace(Marker marker, String s, Object o) {
        notSupported();
    }

    @Override
    public void trace(Marker marker, String s, Object o, Object o1) {
        notSupported();
    }

    @Override
    public void trace(Marker marker, String s, Object... objects) {
        notSupported();
    }

    @Override
    public void trace(Marker marker, String s, Throwable throwable) {
        notSupported();
    }

    @Override
    public boolean isDebugEnabled() {
        return isLevelEnabled(Level.DEBUG);
    }

    @Override
    public void debug(String s) {
        logInt(Level.DEBUG , s);
    }

    @Override
    public void debug(String s, Object o) {
        logInt(Level.DEBUG , s , o);
    }

    @Override
    public void debug(String s, Object o, Object o1) {
        logInt(Level.DEBUG , s , o , o1);
    }

    @Override
    public void debug(String s, Object... objects) {
        logInt(Level.DEBUG , s , objects);
    }

    @Override
    public void debug(String s, Throwable throwable) {
        logInt(Level.DEBUG , s , throwable);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return false;
    }

    @Override
    public void debug(Marker marker, String s) {
        notSupported();
    }

    @Override
    public void debug(Marker marker, String s, Object o) {
        notSupported();
    }

    @Override
    public void debug(Marker marker, String s, Object o, Object o1) {
        notSupported();
    }

    @Override
    public void debug(Marker marker, String s, Object... objects) {
        notSupported();
    }

    @Override
    public void debug(Marker marker, String s, Throwable throwable) {
        notSupported();
    }

    @Override
    public boolean isInfoEnabled() {
        return false;
    }

    @Override
    public void info(String s) {
        logInt(Level.INFO , s);
    }

    @Override
    public void info(String s, Object o) {
        logInt(Level.INFO , s , o );
    }

    @Override
    public void info(String s, Object o, Object o1) {
        logInt(Level.INFO , s , o , o1);
    }

    @Override
    public void info(String s, Object... objects) {
        logInt(Level.INFO , s , objects);
    }

    @Override
    public void info(String s, Throwable throwable) {
        logInt(Level.INFO , s , throwable);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return false;
    }

    @Override
    public void info(Marker marker, String s) {
        notSupported();
    }

    @Override
    public void info(Marker marker, String s, Object o) {
        notSupported();
    }

    @Override
    public void info(Marker marker, String s, Object o, Object o1) {
        notSupported();
    }

    @Override
    public void info(Marker marker, String s, Object... objects) {
        notSupported();
    }

    @Override
    public void info(Marker marker, String s, Throwable throwable) {
        notSupported();
    }

    @Override
    public boolean isWarnEnabled() {
        return false;
    }

    @Override
    public void warn(String s) {
        logInt(Level.WARN , s);
    }

    @Override
    public void warn(String s, Object o) {
        logInt(Level.WARN , s , o);
    }

    @Override
    public void warn(String s, Object... objects) {
        logInt(Level.WARN , s , objects);
    }

    @Override
    public void warn(String s, Object o, Object o1) {
        logInt(Level.WARN , s , o , o1);
    }

    @Override
    public void warn(String s, Throwable throwable) {
        logInt(Level.WARN , s, throwable);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return false;
    }

    @Override
    public void warn(Marker marker, String s) {
        notSupported();
    }

    @Override
    public void warn(Marker marker, String s, Object o) {
        notSupported();
    }

    @Override
    public void warn(Marker marker, String s, Object o, Object o1) {
        notSupported();
    }

    @Override
    public void warn(Marker marker, String s, Object... objects) {
        notSupported();
    }

    @Override
    public void warn(Marker marker, String s, Throwable throwable) {
        notSupported();
    }

    @Override
    public boolean isErrorEnabled() {
        return false;
    }

    @Override
    public void error(String s) {
        logInt(Level.ERROR , s);
    }

    @Override
    public void error(String s, Object o) {
        logInt(Level.ERROR , s , o);
    }

    @Override
    public void error(String s, Object o, Object o1) {
        logInt(Level.ERROR , s , o , o1);
    }

    @Override
    public void error(String s, Object... objects) {
        logInt(Level.ERROR , s , objects);
    }

    @Override
    public void error(String s, Throwable throwable) {
        logInt(Level.ERROR , s , throwable);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return false;
    }

    @Override
    public void error(Marker marker, String s) {
        notSupported();
    }

    @Override
    public void error(Marker marker, String s, Object o) {
        notSupported();
    }

    @Override
    public void error(Marker marker, String s, Object o, Object o1) {
        notSupported();
    }

    @Override
    public void error(Marker marker, String s, Object... objects) {
        notSupported();
    }

    @Override
    public void error(Marker marker, String s, Throwable throwable) {
        notSupported();
    }
}
