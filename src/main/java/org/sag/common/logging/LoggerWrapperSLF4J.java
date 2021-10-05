package org.sag.common.logging;

import java.nio.file.Path;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.slf4j.impl.SimpleLogger;

public class LoggerWrapperSLF4J implements IDebugLogger {

	private final Logger logger;
	
	private static final Marker FATAL = MarkerFactory.getMarker("FATAL");
	private static final Marker WARN = MarkerFactory.getMarker("WARN");
	private static final Marker INFO = MarkerFactory.getMarker("INFO");
	private static final Marker FINE_INFO = MarkerFactory.getMarker("FINE_INFO");
	private static final Marker DEBUG = MarkerFactory.getMarker("DEBUG");
	private static final Marker TRACE = MarkerFactory.getMarker("TRACE");
	private static final Marker FINE_TRACE = MarkerFactory.getMarker("FINE_TRACE");
	
	public LoggerWrapperSLF4J(String name){
		this(name,false);
	}
	
	public LoggerWrapperSLF4J(Class<?> clazz){
		this(clazz.getSimpleName());
	}
	
	public LoggerWrapperSLF4J(String name, boolean enableDebug) {
		if(enableDebug)
			System.setProperty(SimpleLogger.LOG_KEY_PREFIX+name, "debug");
		logger = LoggerFactory.getLogger(name);
	}
	
	@Override
	public int hashCode(){
		return Objects.hashCode(logger);
	}
	
	@Override
	public boolean equals(Object o){
		if(this == o)
			return true;
		if(o == null || !(o instanceof LoggerWrapperSLF4J))
			return false;
		return Objects.equals(logger, ((LoggerWrapperSLF4J)o).logger);
	}
	
	@Override
	public String toString(){
		return Objects.toString(logger);
	}
	
	@Override
	public void fatal(String msg) {
		logger.error(FATAL,msg);
	}

	@Override
	public void fatal(String format, Object param1) {
		logger.error(FATAL,format,param1);
	}

	@Override
	public void fatal(String format, Object param1, Object param2) {
		logger.error(FATAL,format,param1,param2);
	}

	@Override
	public void fatal(String format, Object... params) {
		logger.error(FATAL,format,params);
	}

	@Override
	public void fatal(String msg, Throwable t) {
		logger.error(FATAL, msg, t);
	}

	@Override
	public void fatal(String format, Throwable t, Object param1) {
		logger.error(FATAL, format, param1, t);
	}

	@Override
	public void fatal(String format, Throwable t, Object param1, Object param2) {
		logger.error(FATAL, format, param1, param2, t);
	}

	@Override
	public void fatal(String format, Throwable t, Object... params) {
		Object[] newArr = new Object[params.length+1];
		System.arraycopy(params, 0, newArr, 0, params.length);
		newArr[params.length] = t;
		logger.error(FATAL, format,newArr);
	}

	@Override
	public void warn(String msg) {
		logger.warn(WARN,msg);
	}

	@Override
	public void warn(String format, Object param1) {
		logger.warn(WARN,format,param1);
	}

	@Override
	public void warn(String format, Object param1, Object param2) {
		logger.warn(WARN,format,param1,param2);
	}

	@Override
	public void warn(String format, Object... params) {
		logger.warn(WARN,format,params);
	}

	@Override
	public void warn(String msg, Throwable t) {
		logger.warn(WARN,msg,t);
	}

	@Override
	public void warn(String format, Throwable t, Object param1) {
		logger.warn(WARN,format,param1,t);
	}

	@Override
	public void warn(String format, Throwable t, Object param1, Object param2) {
		logger.warn(WARN,format,param1,param2,t);
	}

	@Override
	public void warn(String format, Throwable t, Object... params) {
		Object[] newArr = new Object[params.length+1];
		System.arraycopy(params, 0, newArr, 0, params.length);
		newArr[params.length] = t;
		logger.warn(WARN,format,newArr);
	}

	@Override
	public void info(String msg) {
		logger.info(INFO,msg);
	}

	@Override
	public void info(String format, Object param1) {
		logger.info(INFO,format,param1);
	}

	@Override
	public void info(String format, Object param1, Object param2) {
		logger.info(INFO,format,param1,param2);
	}

	@Override
	public void info(String format, Object... params) {
		logger.info(INFO,format,params);
	}

	@Override
	public void info(String msg, Throwable t) {
		logger.info(INFO,msg,t);
	}

	@Override
	public void info(String format, Throwable t, Object param1) {
		logger.info(INFO,format,param1,t);
	}

	@Override
	public void info(String format, Throwable t, Object param1, Object param2) {
		logger.info(INFO,format,param1,param2,t);
	}

	@Override
	public void info(String format, Throwable t, Object... params) {
		Object[] newArr = new Object[params.length+1];
		System.arraycopy(params, 0, newArr, 0, params.length);
		newArr[params.length] = t;
		logger.info(INFO,format,newArr);
	}

	@Override
	public void fineInfo(String msg) {
		logger.info(FINE_INFO,msg);
	}

	@Override
	public void fineInfo(String format, Object param1) {
		logger.info(FINE_INFO,format,param1);
	}

	@Override
	public void fineInfo(String format, Object param1, Object param2) {
		logger.info(FINE_INFO,format,param1,param2);
	}

	@Override
	public void fineInfo(String format, Object... params) {
		logger.info(FINE_INFO,format,params);
	}

	@Override
	public void fineInfo(String msg, Throwable t) {
		logger.info(FINE_INFO,msg,t);
	}

	@Override
	public void fineInfo(String format, Throwable t, Object param1) {
		logger.info(FINE_INFO,format,param1,t);
	}

	@Override
	public void fineInfo(String format, Throwable t, Object param1,
			Object param2) {
		logger.info(FINE_INFO,format,param1,param2,t);
	}

	@Override
	public void fineInfo(String format, Throwable t, Object... params) {
		Object[] newArr = new Object[params.length+1];
		System.arraycopy(params, 0, newArr, 0, params.length);
		newArr[params.length] = t;
		logger.info(FINE_INFO,format,newArr);
	}

	@Override
	public void debug(String msg) {
		logger.debug(DEBUG,msg);
	}

	@Override
	public void debug(String format, Object param1) {
		logger.debug(DEBUG,format,param1);
	}

	@Override
	public void debug(String format, Object param1, Object param2) {
		logger.debug(DEBUG,format,param1,param2);
	}

	@Override
	public void debug(String format, Object... params) {
		logger.debug(DEBUG,format,params);
	}

	@Override
	public void debug(String msg, Throwable t) {
		logger.debug(DEBUG,msg,t);
	}

	@Override
	public void debug(String format, Throwable t, Object param1) {
		logger.debug(DEBUG,format,param1,t);
	}

	@Override
	public void debug(String format, Throwable t, Object param1, Object param2) {
		logger.debug(DEBUG,format,param1,param2,t);
	}

	@Override
	public void debug(String format, Throwable t, Object... params) {
		Object[] newArr = new Object[params.length+1];
		System.arraycopy(params, 0, newArr, 0, params.length);
		newArr[params.length] = t;
		logger.debug(DEBUG,format,newArr);
	}

	@Override
	public void trace(String msg) {
		logger.trace(TRACE,msg);
	}

	@Override
	public void trace(String format, Object param1) {
		logger.trace(TRACE,format,param1);
	}

	@Override
	public void trace(String format, Object param1, Object param2) {
		logger.trace(TRACE,format,param1,param2);
	}

	@Override
	public void trace(String format, Object... params) {
		logger.trace(TRACE,format,params);
	}

	@Override
	public void trace(String msg, Throwable t) {
		logger.trace(TRACE,msg,t);
	}

	@Override
	public void trace(String format, Throwable t, Object param1) {
		logger.trace(TRACE,format,param1,t);
	}

	@Override
	public void trace(String format, Throwable t, Object param1, Object param2) {
		logger.trace(TRACE,format,param1,param2,t);
	}

	@Override
	public void trace(String format, Throwable t, Object... params) {
		Object[] newArr = new Object[params.length+1];
		System.arraycopy(params, 0, newArr, 0, params.length);
		newArr[params.length] = t;
		logger.trace(TRACE,format,newArr);
	}

	@Override
	public void fineTrace(String msg) {
		logger.trace(FINE_TRACE,msg);
	}

	@Override
	public void fineTrace(String format, Object param1) {
		logger.trace(FINE_TRACE,format,param1);
	}

	@Override
	public void fineTrace(String format, Object param1, Object param2) {
		logger.trace(FINE_TRACE,format,param1,param2);
	}

	@Override
	public void fineTrace(String format, Object... params) {
		logger.trace(FINE_TRACE,format,params);
	}

	@Override
	public void fineTrace(String msg, Throwable t) {
		logger.trace(FINE_TRACE,msg,t);
	}

	@Override
	public void fineTrace(String format, Throwable t, Object param1) {
		logger.trace(FINE_TRACE,format,param1,t);
	}

	@Override
	public void fineTrace(String format, Throwable t, Object param1,
			Object param2) {
		logger.trace(FINE_TRACE,format,param1,t);
	}

	@Override
	public void fineTrace(String format, Throwable t, Object... params) {
		Object[] newArr = new Object[params.length+1];
		System.arraycopy(params, 0, newArr, 0, params.length);
		newArr[params.length] = t;
		logger.trace(FINE_TRACE,format,newArr);
	}
	
	private static interface InnerLogWrapper {
		public void log(LoggerWrapperSLF4J l, String msg);
		public void log(LoggerWrapperSLF4J l, String format, Object param1);
		public void log(LoggerWrapperSLF4J l, String format, Object param1, Object param2);
		public void log(LoggerWrapperSLF4J l, String format, Object... params);
		public void log(LoggerWrapperSLF4J l, String msg, Throwable t);
		public void log(LoggerWrapperSLF4J l, String format, Throwable t, Object param1);
		public void log(LoggerWrapperSLF4J l, String format, Throwable t, Object param1, Object param2);
		public void log(LoggerWrapperSLF4J l, String format, Throwable t, Object... params);
	}
	
	private static class FatalInnerLogWrapper implements InnerLogWrapper {
		@Override
		public void log(LoggerWrapperSLF4J l, String msg) { l.fatal(msg); }
		@Override
		public void log(LoggerWrapperSLF4J l,String format, Object param1) { l.fatal(format,param1); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Object param1, Object param2) { l.fatal(format,param1,param2); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Object... params) { l.fatal(format,params); }
		@Override
		public void log(LoggerWrapperSLF4J l, String msg, Throwable t) { l.fatal(msg,t); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Throwable t, Object param1) { l.fatal(format,t,param1); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Throwable t, Object param1, Object param2) { l.fatal(format,t,param1,param2); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Throwable t, Object... params) { l.fatal(format,t,params); }
	}
	
	private static class WarnInnerLogWrapper implements InnerLogWrapper {
		@Override
		public void log(LoggerWrapperSLF4J l, String msg) { l.warn(msg); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Object param1) { l.warn(format,param1); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Object param1, Object param2) { l.warn(format,param1,param2); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Object... params) { l.warn(format,params); }
		@Override
		public void log(LoggerWrapperSLF4J l, String msg, Throwable t) { l.warn(msg,t); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Throwable t, Object param1) { l.warn(format,t,param1); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Throwable t, Object param1, Object param2) { l.warn(format,t,param1,param2); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Throwable t, Object... params) { l.warn(format,t,params); }
	}
	
	private static class InfoInnerLogWrapper implements InnerLogWrapper {
		@Override
		public void log(LoggerWrapperSLF4J l, String msg) { l.info(msg); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Object param1) { l.info(format,param1); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Object param1, Object param2) { l.info(format,param1,param2); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Object... params) { l.info(format,params); }
		@Override
		public void log(LoggerWrapperSLF4J l, String msg, Throwable t) { l.info(msg,t); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Throwable t, Object param1) { l.info(format,t,param1); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Throwable t, Object param1, Object param2) { l.info(format,t,param1,param2); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Throwable t, Object... params) { l.info(format,t,params); }
	}
	
	private static class FineInfoInnerLogWrapper implements InnerLogWrapper {
		@Override
		public void log(LoggerWrapperSLF4J l, String msg) { l.fineInfo(msg); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Object param1) { l.fineInfo(format,param1); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Object param1, Object param2) { l.fineInfo(format,param1,param2); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Object... params) { l.fineInfo(format,params); }
		@Override
		public void log(LoggerWrapperSLF4J l, String msg, Throwable t) { l.fineInfo(msg,t); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Throwable t, Object param1) { l.fineInfo(format,t,param1); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Throwable t, Object param1, Object param2) { l.fineInfo(format,t,param1,param2); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Throwable t, Object... params) { l.fineInfo(format,t,params); }
	}
	
	private static class DebugInnerLogWrapper implements InnerLogWrapper {
		@Override
		public void log(LoggerWrapperSLF4J l, String msg) { l.debug(msg); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Object param1) { l.debug(format,param1); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Object param1, Object param2) { l.debug(format,param1,param2); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Object... params) { l.debug(format,params); }
		@Override
		public void log(LoggerWrapperSLF4J l, String msg, Throwable t) { l.debug(msg,t); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Throwable t, Object param1) { l.debug(format,t,param1); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Throwable t, Object param1, Object param2) { l.debug(format,t,param1,param2); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Throwable t, Object... params) { l.debug(format,t,params); }
	}
	
	private static class TraceInnerLogWrapper implements InnerLogWrapper {
		@Override
		public void log(LoggerWrapperSLF4J l, String msg) { l.trace(msg); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Object param1) { l.trace(format,param1); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Object param1, Object param2) { l.trace(format,param1,param2); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Object... params) { l.trace(format,params); }
		@Override
		public void log(LoggerWrapperSLF4J l, String msg, Throwable t) { l.trace(msg,t); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Throwable t, Object param1) { l.trace(format,t,param1); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Throwable t, Object param1, Object param2) { l.trace(format,t,param1,param2); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Throwable t, Object... params) { l.trace(format,t,params); }
	}
	
	private static class FineTraceInnerLogWrapper implements InnerLogWrapper {
		@Override
		public void log(LoggerWrapperSLF4J l, String msg) { l.fineTrace(msg); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Object param1) { l.fineTrace(format,param1); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Object param1, Object param2) { l.fineTrace(format,param1,param2); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Object... params) { l.fineTrace(format,params); }
		@Override
		public void log(LoggerWrapperSLF4J l, String msg, Throwable t) { l.fineTrace(msg,t); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Throwable t, Object param1) { l.fineTrace(format,t,param1); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Throwable t, Object param1, Object param2) { l.fineTrace(format,t,param1,param2); }
		@Override
		public void log(LoggerWrapperSLF4J l, String format, Throwable t, Object... params) { l.fineTrace(format,t,params); }
	}
	
	private final static InnerLogWrapper fatalInnerWrapper = new FatalInnerLogWrapper();
	private final static InnerLogWrapper warnInnerWrapper = new WarnInnerLogWrapper();
	private final static InnerLogWrapper infoInnerWrapper = new InfoInnerLogWrapper();
	private final static InnerLogWrapper fineInfoInnerWrapper = new FineInfoInnerLogWrapper();
	private final static InnerLogWrapper debugInnerWrapper = new DebugInnerLogWrapper();
	private final static InnerLogWrapper traceInnerWrapper = new TraceInnerLogWrapper();
	private final static InnerLogWrapper fineTraceInnerWrapper = new FineTraceInnerLogWrapper();
	
	private InnerLogWrapper getLogRedirection(LogLevel level){
		if(level == LogLevel.FATAL)
			return fatalInnerWrapper;
		else if(level == LogLevel.WARN)
			return warnInnerWrapper;
		else if(level == LogLevel.INFO)
			return infoInnerWrapper;
		else if(level == LogLevel.FINE_INFO)
			return fineInfoInnerWrapper;
		else if(level == LogLevel.DEBUG)
			return debugInnerWrapper;
		else if(level == LogLevel.TRACE)
			return traceInnerWrapper;
		else if(level == LogLevel.FINE_TRACE)
			return fineTraceInnerWrapper;
		else
			throw new RuntimeException("Unreconiged log level " + level.getName());
	}

	@Override
	public void log(LogLevel level, String msg) {
		getLogRedirection(level).log(this, msg);
	}

	@Override
	public void log(LogLevel level, String format, Object param1) {
		getLogRedirection(level).log(this, format, param1);
	}

	@Override
	public void log(LogLevel level, String format, Object param1, Object param2) {
		getLogRedirection(level).log(this, format, param1, param2);
	}

	@Override
	public void log(LogLevel level, String format, Object... params) {
		getLogRedirection(level).log(this, format, params);
	}

	@Override
	public void log(LogLevel level, String msg, Throwable t) {
		getLogRedirection(level).log(this, msg, t);
	}

	@Override
	public void log(LogLevel level, String format, Throwable t, Object param1) {
		getLogRedirection(level).log(this, format, t, param1);
	}

	@Override
	public void log(LogLevel level, String format, Throwable t, Object param1,
			Object param2) {
		getLogRedirection(level).log(this, format, t, param1, param2);
	}

	@Override
	public void log(LogLevel level, String format, Throwable t,
			Object... params) {
		getLogRedirection(level).log(this, format, t, params);
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
	public void setRemove(boolean remove) {}

	@Override
	public void closeOrCloseAndRemove() {}

	@Override
	public void closeAndRemove() {}

}
