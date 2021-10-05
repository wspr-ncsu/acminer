package org.sag.common.logging;

import java.util.Objects;

import org.slf4j.helpers.MessageFormatter;

public class LoggableRuntimeException extends RuntimeException {
	private static final long serialVersionUID = 4414912504483756170L;

	private final ILogger.LogLevel level;
	private final String format;
	private final Throwable t;
	private final Object param1;
	private final Object param2;
	private final Object[] params;
	
	public LoggableRuntimeException(String format) {
		this(ILogger.LogLevel.FATAL,format,(Throwable)null);
	}
	
	public LoggableRuntimeException(ILogger.LogLevel level, String format) {
		this(level,format,(Throwable)null);
	}
	
	public LoggableRuntimeException(String format, Throwable t) {
		this(ILogger.LogLevel.FATAL,format,t);
	}
	
	public LoggableRuntimeException(ILogger.LogLevel level, String format, Throwable t) {
		Objects.requireNonNull(level);
		Objects.requireNonNull(format);
		this.level = level;
		this.format = format;
		this.t = t;
		param1 = null;
		param2 = null;
		params = null;
	}
	
	public LoggableRuntimeException(String format, Object param1) {
		this(ILogger.LogLevel.FATAL,format,null,param1);
	}
	
	public LoggableRuntimeException(ILogger.LogLevel level, String format, Object param1) {
		this(level,format,null,param1);
	}
	
	public LoggableRuntimeException(String format, Throwable t, Object param1) {
		this(ILogger.LogLevel.FATAL,format,t,param1);
	}
	
	public LoggableRuntimeException(ILogger.LogLevel level, String format, Throwable t, Object param1) {
		Objects.requireNonNull(level);
		Objects.requireNonNull(format);
		this.level = level;
		this.format = format;
		this.t = t;
		this.param1 = param1;
		this.param2 = null;
		this.params = null;
	}
	
	public LoggableRuntimeException(String format, Object param1, Object param2) {
		this(ILogger.LogLevel.FATAL,format,null,param1,param2);
	}
	
	public LoggableRuntimeException(ILogger.LogLevel level, String format, Object param1, Object param2) {
		this(level,format,null,param1,param2);
	}
	
	public LoggableRuntimeException(String format, Throwable t, Object param1, Object param2) {
		this(ILogger.LogLevel.FATAL,format,t,param1,param2);
	}
	
	public LoggableRuntimeException(ILogger.LogLevel level, String format, Throwable t, Object param1, Object param2) {
		Objects.requireNonNull(level);
		Objects.requireNonNull(format);
		this.level = level;
		this.format = format;
		this.t = t;
		if(param1 == null) {
			this.param1 = param2;
			this.param2 = null;
		} else {
			this.param1 = param1;
			this.param2 = param2;
		}
		this.params = null;
	}
	
	public LoggableRuntimeException(String format, Object... params) {
		this(ILogger.LogLevel.FATAL,format,null,params);
	}
	
	public LoggableRuntimeException(ILogger.LogLevel level, String format, Object... params) {
		this(level,format,null,params);
	}
	
	public LoggableRuntimeException(String format, Throwable t, Object... params) {
		this(ILogger.LogLevel.FATAL,format,t,params);
	}
	
	public LoggableRuntimeException(ILogger.LogLevel level, String format, Throwable t, Object... params) {
		Objects.requireNonNull(level);
		Objects.requireNonNull(format);
		this.level = level;
		this.format = format;
		this.t = t;
		this.params = params;
		this.param1 = null;
		this.param2 = null;
	}
	
	public void log(ILogger logger) {
		if(param1 == null && param2 == null && params == null) {
			if(t == null) 
				logger.log(level, format);
			else
				logger.log(level, format, t);
		} else if(param1 != null && param2 == null) {
			if(t == null)
				logger.log(level, format, param1);
			else
				logger.log(level, format, t, param1);
		} else if(param1 != null && param2 != null) {
			if(t == null)
				logger.log(level, format, param1, param2);
			else
				logger.log(level, format, t, param1, param2);
		} else if(params != null) {
			if(t == null)
				logger.log(level, format, params);
			else
				logger.log(level, format, t, params);
		}
	}
	
	@Override
	public String getMessage() {
		if(param1 == null && param2 == null && params == null) {
			return format;
		} else if(param1 != null && param2 == null) {
			return MessageFormatter.format(format, param1, null).getMessage();
		} else if(param1 != null && param2 != null) {
			return MessageFormatter.format(format, param1, param2).getMessage();
		} else if(params != null) {
			return MessageFormatter.arrayFormat(format, params).getMessage();
		} else {
			return null;
		}
	}
	
	@Override
	public String getLocalizedMessage() {
		return getMessage();
	}
	
	@Override
	public synchronized Throwable getCause() {
		return t;
	}
	
	@Override
	public synchronized Throwable initCause(Throwable cause) {
		return this;
	}
	
}
