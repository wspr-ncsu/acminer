package org.sag.common.logging;

import java.util.logging.Level;

public interface ILogger {
	
	public static enum LogLevel{
		ALL(Level.ALL),
		FINE_TRACE(Level.FINEST),
		TRACE(Level.FINER),
		DEBUG(Level.FINE),
		FINE_INFO(Level.CONFIG),
		INFO(Level.INFO),
		WARN(Level.WARNING),
		FATAL(Level.SEVERE),
		OFF(Level.OFF);
		
		private final Level id;
		private final String name;
		LogLevel(Level id){
			this.id = id;
			this.name = getName(id);
		}
		public static String getName(Level id){
			String name;
			if(id.intValue() == Level.OFF.intValue()){
				name = "OFF";
			}else if(id.intValue() == Level.SEVERE.intValue()){//all errors and failures
				name = "FATAL";
			}else if(id.intValue() == Level.WARNING.intValue()){//all potential problems
				name = "WARN";
			}else if(id.intValue() == Level.INFO.intValue()){//all start and success messages from a high level
				name = "INFO";
			}else if(id.intValue() == Level.CONFIG.intValue()){//all start and success messages from a low level + anything else
				name = "FINE_INFO";
			}else if(id.intValue() == Level.FINE.intValue()){//debug info
				name = "DEBUG";
			}else if(id.intValue() == Level.FINER.intValue()){
				name = "TRACE";
			}else if(id.intValue() == Level.FINEST.intValue()){
				name = "FINE_TRACE";
			}else{
				name = "ALL";
			}
			return name;
		}
		public static LogLevel getLogLevel(String ll){
			for(LogLevel l : LogLevel.values()){
				if(l.getName().compareToIgnoreCase(ll) == 0){
					return l;
				}
			}
			return null;
		}
		public Level getValue(){ return id; }
		public String getName(){ return name; }
	}
	
	public void fatal(String msg);
	public void fatal(String format, Object param1);
	public void fatal(String format, Object param1, Object param2);
	public void fatal(String format, Object... params);
	public void fatal(String msg, Throwable t);
	public void fatal(String format, Throwable t, Object param1);
	public void fatal(String format, Throwable t, Object param1, Object param2);
	public void fatal(String format, Throwable t, Object... params);
	public void warn(String msg);
	public void warn(String format, Object param1);
	public void warn(String format, Object param1, Object param2);
	public void warn(String format, Object... params);
	public void warn(String msg, Throwable t);
	public void warn(String format, Throwable t, Object param1);
	public void warn(String format, Throwable t, Object param1, Object param2);
	public void warn(String format, Throwable t, Object... params);
	public void info(String msg);
	public void info(String format, Object param1);
	public void info(String format, Object param1, Object param2);
	public void info(String format, Object... params);
	public void info(String msg, Throwable t);
	public void info(String format, Throwable t, Object param1);
	public void info(String format, Throwable t, Object param1, Object param2);
	public void info(String format, Throwable t, Object... params);
	public void fineInfo(String msg);
	public void fineInfo(String format, Object param1);
	public void fineInfo(String format, Object param1, Object param2);
	public void fineInfo(String format, Object... params);
	public void fineInfo(String msg, Throwable t);
	public void fineInfo(String format, Throwable t, Object param1);
	public void fineInfo(String format, Throwable t, Object param1, Object param2);
	public void fineInfo(String format, Throwable t, Object... params);
	public void debug(String msg);
	public void debug(String format, Object param1);
	public void debug(String format, Object param1, Object param2);
	public void debug(String format, Object... params);
	public void debug(String msg, Throwable t);
	public void debug(String format, Throwable t, Object param1);
	public void debug(String format, Throwable t, Object param1, Object param2);
	public void debug(String format, Throwable t, Object... params);
	public void trace(String msg);
	public void trace(String format, Object param1);
	public void trace(String format, Object param1, Object param2);
	public void trace(String format, Object... params);
	public void trace(String msg, Throwable t);
	public void trace(String format, Throwable t, Object param1);
	public void trace(String format, Throwable t, Object param1, Object param2);
	public void trace(String format, Throwable t, Object... params);
	public void fineTrace(String msg);
	public void fineTrace(String format, Object param1);
	public void fineTrace(String format, Object param1, Object param2);
	public void fineTrace(String format, Object... params);
	public void fineTrace(String msg, Throwable t);
	public void fineTrace(String format, Throwable t, Object param1);
	public void fineTrace(String format, Throwable t, Object param1, Object param2);
	public void fineTrace(String format, Throwable t, Object... params);
	public void log(ILogger.LogLevel level, String msg);
	public void log(ILogger.LogLevel level, String format, Object param1);
	public void log(ILogger.LogLevel level, String format, Object param1, Object param2);
	public void log(ILogger.LogLevel level, String format, Object... params);
	public void log(ILogger.LogLevel level, String msg, Throwable t);
	public void log(ILogger.LogLevel level, String format, Throwable t, Object param1);
	public void log(ILogger.LogLevel level, String format, Throwable t, Object param1, Object param2);
	public void log(ILogger.LogLevel level, String format, Throwable t, Object... params);
	public String getName();
	public boolean close();
	public boolean flush();
	
}
