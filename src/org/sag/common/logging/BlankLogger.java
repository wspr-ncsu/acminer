package org.sag.common.logging;

import java.nio.file.Path;

public class BlankLogger implements IDebugLogger {
	
	private static final BlankLogger bl = new BlankLogger();
	
	public static final BlankLogger v() {
		return bl;
	}
	
	private BlankLogger() {}

	@Override
	public void fatal(String msg) {
		
	}

	@Override
	public void fatal(String format, Object param1) {
		
	}

	@Override
	public void fatal(String format, Object param1, Object param2) {
		
	}

	@Override
	public void fatal(String format, Object... params) {
		
	}

	@Override
	public void fatal(String msg, Throwable t) {
		
	}

	@Override
	public void fatal(String format, Throwable t, Object param1) {
		
	}

	@Override
	public void fatal(String format, Throwable t, Object param1, Object param2) {
		
	}

	@Override
	public void fatal(String format, Throwable t, Object... params) {
		
	}

	@Override
	public void warn(String msg) {
		
	}

	@Override
	public void warn(String format, Object param1) {
		
	}

	@Override
	public void warn(String format, Object param1, Object param2) {
		
	}

	@Override
	public void warn(String format, Object... params) {
		
	}

	@Override
	public void warn(String msg, Throwable t) {
		
	}

	@Override
	public void warn(String format, Throwable t, Object param1) {
		
	}

	@Override
	public void warn(String format, Throwable t, Object param1, Object param2) {
		
	}

	@Override
	public void warn(String format, Throwable t, Object... params) {
		
	}

	@Override
	public void info(String msg) {
		
	}

	@Override
	public void info(String format, Object param1) {
		
	}

	@Override
	public void info(String format, Object param1, Object param2) {
		
	}

	@Override
	public void info(String format, Object... params) {
		
	}

	@Override
	public void info(String msg, Throwable t) {
		
	}

	@Override
	public void info(String format, Throwable t, Object param1) {
		
	}

	@Override
	public void info(String format, Throwable t, Object param1, Object param2) {
		
	}

	@Override
	public void info(String format, Throwable t, Object... params) {
		
	}

	@Override
	public void fineInfo(String msg) {
		
	}

	@Override
	public void fineInfo(String format, Object param1) {
		
	}

	@Override
	public void fineInfo(String format, Object param1, Object param2) {
		
	}

	@Override
	public void fineInfo(String format, Object... params) {
		
	}

	@Override
	public void fineInfo(String msg, Throwable t) {
		
	}

	@Override
	public void fineInfo(String format, Throwable t, Object param1) {
		
	}

	@Override
	public void fineInfo(String format, Throwable t, Object param1, Object param2) {
		
	}

	@Override
	public void fineInfo(String format, Throwable t, Object... params) {
		
	}

	@Override
	public void debug(String msg) {
		
	}

	@Override
	public void debug(String format, Object param1) {
		
	}

	@Override
	public void debug(String format, Object param1, Object param2) {
		
	}

	@Override
	public void debug(String format, Object... params) {
		
	}

	@Override
	public void debug(String msg, Throwable t) {
		
	}

	@Override
	public void debug(String format, Throwable t, Object param1) {
		
	}

	@Override
	public void debug(String format, Throwable t, Object param1, Object param2) {
		
	}

	@Override
	public void debug(String format, Throwable t, Object... params) {
		
	}

	@Override
	public void trace(String msg) {
		
	}

	@Override
	public void trace(String format, Object param1) {
		
	}

	@Override
	public void trace(String format, Object param1, Object param2) {
		
	}

	@Override
	public void trace(String format, Object... params) {
		
	}

	@Override
	public void trace(String msg, Throwable t) {
		
	}

	@Override
	public void trace(String format, Throwable t, Object param1) {
		
	}

	@Override
	public void trace(String format, Throwable t, Object param1, Object param2) {
		
	}

	@Override
	public void trace(String format, Throwable t, Object... params) {
		
	}

	@Override
	public void fineTrace(String msg) {
		
	}

	@Override
	public void fineTrace(String format, Object param1) {
		
	}

	@Override
	public void fineTrace(String format, Object param1, Object param2) {
		
	}

	@Override
	public void fineTrace(String format, Object... params) {
		
	}

	@Override
	public void fineTrace(String msg, Throwable t) {
		
	}

	@Override
	public void fineTrace(String format, Throwable t, Object param1) {
		
	}

	@Override
	public void fineTrace(String format, Throwable t, Object param1, Object param2) {
		
	}

	@Override
	public void fineTrace(String format, Throwable t, Object... params) {
		
	}

	@Override
	public void log(LogLevel level, String msg) {
		
	}

	@Override
	public void log(LogLevel level, String format, Object param1) {
		
	}

	@Override
	public void log(LogLevel level, String format, Object param1, Object param2) {
		
	}

	@Override
	public void log(LogLevel level, String format, Object... params) {
		
	}

	@Override
	public void log(LogLevel level, String msg, Throwable t) {
		
	}

	@Override
	public void log(LogLevel level, String format, Throwable t, Object param1) {
		
	}

	@Override
	public void log(LogLevel level, String format, Throwable t, Object param1, Object param2) {
		
	}

	@Override
	public void log(LogLevel level, String format, Throwable t, Object... params) {
		
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public boolean close() {
		return true;
	}

	@Override
	public boolean flush() {
		return true;
	}

	@Override
	public String getFileNameWithoutExtension() {
		return null;
	}

	@Override
	public Path getFilePath() {
		return null;
	}

	@Override
	public void setRemove(boolean remove) {
		
	}

	@Override
	public void closeOrCloseAndRemove() {
		
	}

	@Override
	public void closeAndRemove() {
		
	}

}
