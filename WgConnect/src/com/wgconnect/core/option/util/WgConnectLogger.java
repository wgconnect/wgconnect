/*
 * Copyright 2024 wgconnect@proton.me. All Rights Reserved.
 *
 * WgConnect is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WgConnect is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WgConnect.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.wgconnect.core.util;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.simple.SimpleLoggerFactory;

/**
 * WgConnectLogger
 *
 * @author: wgconnect@proton.me
 */
public class WgConnectLogger implements Logger {

    private final Class<?> clazz;
    private final Logger simpleLogger;
    
    private boolean debugEnabled = false;
    private boolean errorEnabled = false;
    private boolean infoEnabled = true;
    private boolean traceEnabled = false;
    private boolean warnEnabled = false;
    
    public WgConnectLogger(Class<?> clazz) {
        this.clazz = clazz;
        simpleLogger = new SimpleLoggerFactory().getLogger(clazz.getName());
    }
    
    public static WgConnectLogger getLogger(Class<?> clazz) {
        return new WgConnectLogger(clazz);
    }
    
    @Override
    public String getName() {
        return clazz.getName();
    }

    @Override
    public boolean isTraceEnabled() {
        return traceEnabled;
    }

    public void setTraceEnabled(boolean enabled) {
        traceEnabled = enabled;
    }
    
    @Override
    public void trace(String msg) {
        simpleLogger.trace(msg);
    }

    @Override
    public void trace(String format, Object arg) {
        simpleLogger.trace(format, arg);
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        simpleLogger.trace(format, arg1, arg2);
    }

    @Override
    public void trace(String format, Object... arguments) {
        simpleLogger.trace(format, arguments);
    }

    @Override
    public void trace(String msg, Throwable t) {
        simpleLogger.trace(msg, t);
    }

    @Override
    public boolean isTraceEnabled(Marker marker) {
        return traceEnabled;
    }

    @Override
    public void trace(Marker marker, String msg) {
        simpleLogger.trace(marker, msg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg) {
        simpleLogger.trace(marker, format, arg);
    }

    @Override
    public void trace(Marker marker, String format, Object arg1, Object arg2) {
        simpleLogger.trace(marker, format, arg1, arg2);
    }

    @Override
    public void trace(Marker marker, String format, Object... argArray) {
        simpleLogger.trace(marker, format, argArray);
    }

    @Override
    public void trace(Marker marker, String msg, Throwable t) {
        simpleLogger.trace(marker, msg, t);
    }

    @Override
    public boolean isDebugEnabled() {
        return debugEnabled;
    }
    
    public void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }

    @Override
    public void debug(String msg) {
        simpleLogger.debug(msg);
    }

    @Override
    public void debug(String format, Object arg) {
        simpleLogger.debug(format, arg);
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        simpleLogger.debug(format, arg1, arg2);
    }

    @Override
    public void debug(String format, Object... arguments) {
        simpleLogger.debug(format, arguments);
    }

    @Override
    public void debug(String msg, Throwable t) {
        simpleLogger.debug(msg, t);
    }

    @Override
    public boolean isDebugEnabled(Marker marker) {
        return simpleLogger.isDebugEnabled(marker);
    }

    @Override
    public void debug(Marker marker, String msg) {
        simpleLogger.debug(marker, msg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg) {
        simpleLogger.debug(marker, format, arg);
    }

    @Override
    public void debug(Marker marker, String format, Object arg1, Object arg2) {
        simpleLogger.debug(marker, format, arg1, arg2);
    }

    @Override
    public void debug(Marker marker, String format, Object... arguments) {
        simpleLogger.debug(marker, format, arguments);
    }

    @Override
    public void debug(Marker marker, String msg, Throwable t) {
        simpleLogger.debug(marker, msg, t);
    }

    @Override
    public boolean isInfoEnabled() {
        return infoEnabled;
    }

    public void setInfoEnabled(boolean enabled) {
        infoEnabled = enabled;
    }
    
    @Override
    public void info(String msg) {
        simpleLogger.info(msg);
    }

    @Override
    public void info(String format, Object arg) {
        simpleLogger.info(format, arg);
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        simpleLogger.info(format, arg1, arg2);
    }

    @Override
    public void info(String format, Object... arguments) {
        simpleLogger.info(format, arguments);
    }

    @Override
    public void info(String msg, Throwable t) {
        simpleLogger.info(msg, t);
    }

    @Override
    public boolean isInfoEnabled(Marker marker) {
        return simpleLogger.isInfoEnabled(marker);
    }

    @Override
    public void info(Marker marker, String msg) {
        simpleLogger.info(marker, msg);
    }

    @Override
    public void info(Marker marker, String format, Object arg) {
        simpleLogger.info(marker, format, arg);
    }

    @Override
    public void info(Marker marker, String format, Object arg1, Object arg2) {
        simpleLogger.info(marker, format, arg1, arg2);
    }

    @Override
    public void info(Marker marker, String format, Object... arguments) {
        simpleLogger.info(marker, format, arguments);
    }

    @Override
    public void info(Marker marker, String msg, Throwable t) {
        simpleLogger.info(marker, msg, t);
    }

    @Override
    public boolean isWarnEnabled() {
        return warnEnabled;
    }

    public void setWarnEnabled(boolean enabled) {
        warnEnabled = enabled;
    }
    
    @Override
    public void warn(String msg) {
        simpleLogger.warn(msg);
    }

    @Override
    public void warn(String format, Object arg) {
        simpleLogger.warn(format, arg);
    }

    @Override
    public void warn(String format, Object... arguments) {
        simpleLogger.warn(format, arguments);
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        simpleLogger.warn(format, arg1, arg2);
    }

    @Override
    public void warn(String msg, Throwable t) {
        simpleLogger.warn(msg, t);
    }

    @Override
    public boolean isWarnEnabled(Marker marker) {
        return simpleLogger.isWarnEnabled(marker);
    }

    @Override
    public void warn(Marker marker, String msg) {
        simpleLogger.warn(marker, msg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg) {
        simpleLogger.warn(marker, format, arg);
    }

    @Override
    public void warn(Marker marker, String format, Object arg1, Object arg2) {
        simpleLogger.warn(marker, format, arg1, arg2);
    }

    @Override
    public void warn(Marker marker, String format, Object... arguments) {
        simpleLogger.warn(marker, format, arguments);
    }

    @Override
    public void warn(Marker marker, String msg, Throwable t) {
        simpleLogger.warn(marker, msg, t);
    }

    @Override
    public boolean isErrorEnabled() {
        return errorEnabled;
    }

    public void setErrorEnabled(boolean enabled) {
        errorEnabled = enabled;
    }
    
    @Override
    public void error(String msg) {
        simpleLogger.error(msg);
    }

    @Override
    public void error(String format, Object arg) {
        simpleLogger.error(format);
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        simpleLogger.error(format, arg1, arg2);
    }

    @Override
    public void error(String format, Object... arguments) {
        simpleLogger.error(format, arguments);
    }

    @Override
    public void error(String msg, Throwable t) {
        simpleLogger.error(msg, t);
    }

    @Override
    public boolean isErrorEnabled(Marker marker) {
        return simpleLogger.isErrorEnabled(marker);
    }

    @Override
    public void error(Marker marker, String msg) {
        simpleLogger.error(marker, msg);
    }

    @Override
    public void error(Marker marker, String format, Object arg) {
        simpleLogger.error(marker, format, arg);
    }

    @Override
    public void error(Marker marker, String format, Object arg1, Object arg2) {
        simpleLogger.error(marker, format, arg1, arg2);
    }

    @Override
    public void error(Marker marker, String format, Object... arguments) {
        simpleLogger.error(marker, format, arguments);
    }

    @Override
    public void error(Marker marker, String msg, Throwable t) {
        simpleLogger.error(marker, msg, t);
    }
}
