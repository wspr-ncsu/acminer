package org.sag.main.logging;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sag.common.io.FileHelpers;
import org.sag.common.logging.ILogger;
import org.sag.main.AndroidInfo;
import org.slf4j.helpers.MessageFormatter;

/* A static class that keeps track of the different loggers for the PolicyGenerator tool.
 * There can be only one logger per class. There is only one FileHandler per logger.
 * There is only one FileHandler per file. Multiple loggers can use the same FileHandler 
 * (i.e. write to the same file in the same format). The Formatter for a FileHandler will
 * not change one it is set.
 * 
 * This class assumes the existence of a main log file that most classes use. The main
 * log file is the log file seen in the first logger created by this class. For this 
 * reason, setupLogger should always be called before getLogger (to properly initialize
 * the main log info. All calls to getLogger afterwards will default to the main log 
 * file if a logger for the class does not already exist. If one wishes to use a 
 * different log file for a certain class then call setupLogger for that class before
 * getLogger.
 */

public class CentralLogger implements ILogger {
	
	private static final HashMap<String, CentralLogger> loggerList = new HashMap<>();
	private static final HashMap<Path, Handler> handlerList = new HashMap<>();
	private static final HashMap<Handler,Set<Logger>> handlersToLoggers = new HashMap<>();
	private static volatile boolean first = true;
	
	private static volatile Path mainLogFile = FileHelpers.getNormAndAbsPath(Paths.get("MainLog.log"));
	private static final ILogger.LogLevel logLevelDebug = ILogger.LogLevel.DEBUG;
	private static final ILogger.LogLevel logLevelOther = ILogger.LogLevel.FINE_INFO;
	private static final ILogger.LogLevel logLevelMain = ILogger.LogLevel.INFO;
	private static volatile Path logDir = FileHelpers.getNormAndAbsPath(Paths.get(""));
	private static volatile AndroidInfo ai = null;
	
	public static void setAndroidInfo(AndroidInfo i) {
		ai = i;
	}
	
	public static void setMainLogFile(Path s){
		mainLogFile = FileHelpers.getNormAndAbsPath(s);
	}
	
	public static Path getMainLogFile(){
		return mainLogFile;
	}
	
	public static ILogger.LogLevel getLogLevelDebug(){
		return logLevelDebug;
	}
	
	public static ILogger.LogLevel getLogLevelOther(){
		return logLevelOther;
	}
	
	public static ILogger.LogLevel getLogLevelMain(){
		return logLevelMain;
	}
	
	public static void setLogDir(Path s){
		logDir = FileHelpers.getNormAndAbsPath(s);
	}
	
	public static Path getLogDir(){
		return logDir;
	}
	
	public static CentralLogger getLogger(String name){
		return getNonCleanLogger(cleanName(name));
	}
	
	public static CentralLogger getNonCleanLogger(String name){
		try{
			return setupLogger(name, mainLogFile, getLogLevelMain().getValue(), "", "", true);
		}catch(IOException e){
			return null;
		}
	}
	
	public static CentralLogger startLogger(String name, Path logFile, ILogger.LogLevel l, boolean append){
		return startNonCleanLogger(cleanName(name),logFile,l,append);
	}
	
	public static CentralLogger startNonCleanLogger(String name, Path logFile, ILogger.LogLevel l, boolean append){
		try{
			return CentralLogger.setupLogger(name, logFile, l.getValue(), 
					"--------------------------Start " + name + "--------------------------\n", 
					"---------------------------End " + name + "---------------------------\n", 
					append);
		}catch(IOException e){
			return null;
		}
	}
	
	public static CentralLogger startLogger(String name, Path logFile, ILogger.LogLevel l, String h, String t, boolean append){
		return startNonCleanLogger(cleanName(name),logFile,l,h,t,append);
	}
	
	public static CentralLogger startNonCleanLogger(String name, Path logFile, ILogger.LogLevel l, String h, String t, boolean append){
		try {
			return CentralLogger.setupLogger(name, logFile, l.getValue(), h, t, append);
		} catch (IOException e) {
			return null;
		}
	}
	
	private static CentralLogger setupLogger(String name, Path logFile, Level l, String header, String tail, boolean append) throws IOException{
		synchronized(loggerList){
			if(loggerList.containsKey(name)){
				return loggerList.get(name);
			}
			
			if(first){
				first = false;
				Logger rootLogger = Logger.getLogger("");
				Handler[] handlers = rootLogger.getHandlers();
				for(Handler h : handlers){
					rootLogger.removeHandler(h);
				}
				Handler newH = new StdOutHandler(new SingleLineFormatter(header,tail,ai));
				newH.setLevel(logLevelMain.getValue());
				rootLogger.addHandler(newH);
				rootLogger.setLevel(logLevelMain.getValue());
			}
			
			Handler handle;
			Logger logger = Logger.getLogger(name);
			
			
			if(handlerList.containsKey(logFile)){
				handle = handlerList.get(logFile);
			}else{
				handle = new FileHandler(logFile.toString(),append);
				handle.setFormatter(new SingleLineFormatter(header,tail,ai));
				handlerList.put(logFile, handle);
			}
			
			Set<Logger> loggersForHandler = handlersToLoggers.get(handle);
			if(loggersForHandler == null){
				loggersForHandler = new HashSet<Logger>();
				handlersToLoggers.put(handle, loggersForHandler);
			}
			loggersForHandler.add(logger);
			
			logger.setLevel(l);
			logger.addHandler(handle);
			CentralLogger ret = new CentralLogger(logger);
			loggerList.put(name, ret);
			return ret;
		}
	}
	
	public static String cleanName(String name){
		return name.replace('.','~');
	}
	
	public static Path getLogPath(String name){
		return logDir.resolve(name + ".log").toAbsolutePath();
	}
	
	protected final Logger logger;
	
	protected CentralLogger(Logger logger){
		this.logger = logger;
	}
	
	public int hashCode(){
		return Objects.hashCode(logger);
	}
	
	public boolean equals(Object o){
		if(this == o){
			return true;
		}
		if(o == null || !(o instanceof CentralLogger)){
			return false;
		}
		return Objects.equals(logger, ((CentralLogger)o).logger);
	}
	
	public String toString(){
		return Objects.toString(logger);
	}
	
	public void fatal(String msg){
		logger.log(ILogger.LogLevel.FATAL.getValue(),msg);
	}
	
	public void fatal(String format, Object param1){
		logger.log(ILogger.LogLevel.FATAL.getValue(),MessageFormatter.format(format, param1, null).getMessage());
	}
	
	public void fatal(String format, Object param1, Object param2){
		logger.log(ILogger.LogLevel.FATAL.getValue(),MessageFormatter.format(format, param1, param2).getMessage());
	}
	
	public void fatal(String format, Object... params){
		logger.log(ILogger.LogLevel.FATAL.getValue(),MessageFormatter.arrayFormat(format, params).getMessage());
	}
	
	public void fatal(String msg, Throwable t){
		logger.log(ILogger.LogLevel.FATAL.getValue(), msg, t);
	}
	
	public void fatal(String format, Throwable t, Object param1){
		logger.log(ILogger.LogLevel.FATAL.getValue(),MessageFormatter.format(format, param1, null).getMessage(),t);
	}
	
	public void fatal(String format, Throwable t, Object param1, Object param2){
		logger.log(ILogger.LogLevel.FATAL.getValue(),MessageFormatter.format(format, param1, param2).getMessage(),t);
	}
	
	public void fatal(String format, Throwable t, Object... params){
		logger.log(ILogger.LogLevel.FATAL.getValue(),MessageFormatter.arrayFormat(format, params).getMessage(),t);
	}
	
	public void warn(String msg){
		logger.log(ILogger.LogLevel.WARN.getValue(),msg);
	}
	
	public void warn(String format, Object param1){
		logger.log(ILogger.LogLevel.WARN.getValue(),MessageFormatter.format(format, param1, null).getMessage());
	}
	
	public void warn(String format, Object param1, Object param2){
		logger.log(ILogger.LogLevel.WARN.getValue(),MessageFormatter.format(format, param1, param2).getMessage());
	}
	
	public void warn(String format, Object... params){
		logger.log(ILogger.LogLevel.WARN.getValue(),MessageFormatter.arrayFormat(format, params).getMessage());
	}
	
	public void warn(String msg, Throwable t){
		logger.log(ILogger.LogLevel.WARN.getValue(),msg,t);
	}
	
	public void warn(String format, Throwable t, Object param1){
		logger.log(ILogger.LogLevel.WARN.getValue(),MessageFormatter.format(format, param1, null).getMessage(),t);
	}
	
	public void warn(String format, Throwable t, Object param1, Object param2){
		logger.log(ILogger.LogLevel.WARN.getValue(),MessageFormatter.format(format, param1, param2).getMessage(),t);
	}
	
	public void warn(String format, Throwable t, Object... params){
		logger.log(ILogger.LogLevel.WARN.getValue(),MessageFormatter.arrayFormat(format, params).getMessage(),t);
	}
	
	public void info(String msg){
		logger.log(ILogger.LogLevel.INFO.getValue(),msg);
	}
	
	public void info(String format, Object param1){
		logger.log(ILogger.LogLevel.INFO.getValue(),MessageFormatter.format(format, param1, null).getMessage());
	}
	
	public void info(String format, Object param1, Object param2){
		logger.log(ILogger.LogLevel.INFO.getValue(),MessageFormatter.format(format, param1, param2).getMessage());
	}
	
	public void info(String format, Object... params){
		logger.log(ILogger.LogLevel.INFO.getValue(),MessageFormatter.arrayFormat(format, params).getMessage());
	}
	
	public void info(String msg, Throwable t){
		logger.log(ILogger.LogLevel.INFO.getValue(),msg,t);
	}
	
	public void info(String format, Throwable t, Object param1){
		logger.log(ILogger.LogLevel.INFO.getValue(),MessageFormatter.format(format, param1, null).getMessage(),t);
	}
	
	public void info(String format, Throwable t, Object param1, Object param2){
		logger.log(ILogger.LogLevel.INFO.getValue(),MessageFormatter.format(format, param1, param2).getMessage(),t);
	}
	
	public void info(String format, Throwable t, Object... params){
		logger.log(ILogger.LogLevel.INFO.getValue(),MessageFormatter.arrayFormat(format, params).getMessage(),t);
	}
	
	public void fineInfo(String msg){
		logger.log(ILogger.LogLevel.FINE_INFO.getValue(),msg);
	}
	
	public void fineInfo(String format, Object param1){
		logger.log(ILogger.LogLevel.FINE_INFO.getValue(),MessageFormatter.format(format, param1, null).getMessage());
	}
	
	public void fineInfo(String format, Object param1, Object param2){
		logger.log(ILogger.LogLevel.FINE_INFO.getValue(),MessageFormatter.format(format, param1, param2).getMessage());
	}
	
	public void fineInfo(String format, Object... params){
		logger.log(ILogger.LogLevel.FINE_INFO.getValue(),MessageFormatter.arrayFormat(format, params).getMessage());
	}
	
	public void fineInfo(String msg, Throwable t){
		logger.log(ILogger.LogLevel.FINE_INFO.getValue(),msg,t);
	}
	
	public void fineInfo(String format, Throwable t, Object param1){
		logger.log(ILogger.LogLevel.FINE_INFO.getValue(),MessageFormatter.format(format, param1, null).getMessage(),t);
	}
	
	public void fineInfo(String format, Throwable t, Object param1, Object param2){
		logger.log(ILogger.LogLevel.FINE_INFO.getValue(),MessageFormatter.format(format, param1, param2).getMessage(),t);
	}
	
	public void fineInfo(String format, Throwable t, Object... params){
		logger.log(ILogger.LogLevel.FINE_INFO.getValue(),MessageFormatter.arrayFormat(format, params).getMessage(),t);
	}
	
	public void debug(String msg){
		logger.log(ILogger.LogLevel.DEBUG.getValue(),msg);
	}
	
	public void debug(String format, Object param1){
		logger.log(ILogger.LogLevel.DEBUG.getValue(),MessageFormatter.format(format, param1, null).getMessage());
	}
	
	public void debug(String format, Object param1, Object param2){
		logger.log(ILogger.LogLevel.DEBUG.getValue(),MessageFormatter.format(format, param1, param2).getMessage());
	}
	
	public void debug(String format, Object... params){
		logger.log(ILogger.LogLevel.DEBUG.getValue(),MessageFormatter.arrayFormat(format, params).getMessage());
	}
	
	public void debug(String msg, Throwable t){
		logger.log(ILogger.LogLevel.DEBUG.getValue(),msg,t);
	}
	
	public void debug(String format, Throwable t, Object param1){
		logger.log(ILogger.LogLevel.DEBUG.getValue(),MessageFormatter.format(format, param1, null).getMessage(),t);
	}
	
	public void debug(String format, Throwable t, Object param1, Object param2){
		logger.log(ILogger.LogLevel.DEBUG.getValue(),MessageFormatter.format(format, param1, param2).getMessage(),t);
	}
	
	public void debug(String format, Throwable t, Object... params){
		logger.log(ILogger.LogLevel.DEBUG.getValue(),MessageFormatter.arrayFormat(format, params).getMessage(),t);
	}
	
	public void trace(String msg){
		logger.log(ILogger.LogLevel.TRACE.getValue(),msg);
	}
	
	public void trace(String format, Object param1){
		logger.log(ILogger.LogLevel.TRACE.getValue(),MessageFormatter.format(format, param1, null).getMessage());
	}
	
	public void trace(String format, Object param1, Object param2){
		logger.log(ILogger.LogLevel.TRACE.getValue(),MessageFormatter.format(format, param1, param2).getMessage());
	}
	
	public void trace(String format, Object... params){
		logger.log(ILogger.LogLevel.TRACE.getValue(),MessageFormatter.arrayFormat(format, params).getMessage());
	}
	
	public void trace(String msg, Throwable t){
		logger.log(ILogger.LogLevel.TRACE.getValue(),msg,t);
	}
	
	public void trace(String format, Throwable t, Object param1){
		logger.log(ILogger.LogLevel.TRACE.getValue(),MessageFormatter.format(format, param1, null).getMessage(),t);
	}
	
	public void trace(String format, Throwable t, Object param1, Object param2){
		logger.log(ILogger.LogLevel.TRACE.getValue(),MessageFormatter.format(format, param1, param2).getMessage(),t);
	}
	
	public void trace(String format, Throwable t, Object... params){
		logger.log(ILogger.LogLevel.TRACE.getValue(),MessageFormatter.arrayFormat(format, params).getMessage(),t);
	}
	
	public void fineTrace(String msg){
		logger.log(ILogger.LogLevel.FINE_TRACE.getValue(),msg);
	}
	
	public void fineTrace(String format, Object param1){
		logger.log(ILogger.LogLevel.FINE_TRACE.getValue(),MessageFormatter.format(format, param1, null).getMessage());
	}
	
	public void fineTrace(String format, Object param1, Object param2){
		logger.log(ILogger.LogLevel.FINE_TRACE.getValue(),MessageFormatter.format(format, param1, param2).getMessage());
	}
	
	public void fineTrace(String format, Object... params){
		logger.log(ILogger.LogLevel.FINE_TRACE.getValue(),MessageFormatter.arrayFormat(format, params).getMessage());
	}
	
	public void fineTrace(String msg, Throwable t){
		logger.log(ILogger.LogLevel.FINE_TRACE.getValue(),msg,t);
	}
	
	public void fineTrace(String format, Throwable t, Object param1){
		logger.log(ILogger.LogLevel.FINE_TRACE.getValue(),MessageFormatter.format(format, param1, null).getMessage(),t);
	}
	
	public void fineTrace(String format, Throwable t, Object param1, Object param2){
		logger.log(ILogger.LogLevel.FINE_TRACE.getValue(),MessageFormatter.format(format, param1, param2).getMessage(),t);
	}
	
	public void fineTrace(String format, Throwable t, Object... params){
		logger.log(ILogger.LogLevel.FINE_TRACE.getValue(),MessageFormatter.arrayFormat(format, params).getMessage(),t);
	}
	
	public void log(ILogger.LogLevel level, String msg){
		logger.log(level.getValue(),msg);
	}
	
	public void log(ILogger.LogLevel level, String format, Object param1){
		logger.log(level.getValue(),MessageFormatter.format(format, param1, null).getMessage());
	}
	
	public void log(ILogger.LogLevel level, String format, Object param1, Object param2){
		logger.log(level.getValue(),MessageFormatter.format(format, param1, param2).getMessage());
	}
	
	public void log(ILogger.LogLevel level, String format, Object... params){
		logger.log(level.getValue(),MessageFormatter.arrayFormat(format, params).getMessage());
	}
	
	public void log(ILogger.LogLevel level, String msg, Throwable t){
		logger.log(level.getValue(),msg,t);
	}
	
	public void log(ILogger.LogLevel level, String format, Throwable t, Object param1){
		logger.log(level.getValue(),MessageFormatter.format(format, param1, null).getMessage(),t);
	}
	
	public void log(ILogger.LogLevel level, String format, Throwable t, Object param1, Object param2){
		logger.log(level.getValue(),MessageFormatter.format(format, param1, param2).getMessage(),t);
	}
	
	public void log(ILogger.LogLevel level, String format, Throwable t, Object... params){
		logger.log(level.getValue(),MessageFormatter.arrayFormat(format, params).getMessage(),t);
	}
	
	public String getName(){
		return logger.getName();
	}
	
	public boolean close(){
		synchronized(loggerList){
			try{
				for(Handler h : logger.getHandlers()){
					Set<Logger> loggersForHandler = handlersToLoggers.get(h);
					loggersForHandler.remove(logger);
					logger.removeHandler(h);
					if(loggersForHandler.isEmpty()){
						handlersToLoggers.remove(h);
						for(Iterator<Entry<Path,Handler>> it = handlerList.entrySet().iterator(); it.hasNext();){
							Entry<Path,Handler> cur = it.next();
							if(h.equals(cur.getValue())){
								it.remove();
							}
						}
						h.close();
					}
				}
				loggerList.remove(logger.getName());
			}catch(Throwable t){
				return false;
			}
			return true;
		}
	}
	
	public boolean flush(){
		try{
			for(Handler h : logger.getHandlers()){
				h.flush();
			}
		}catch(Throwable t){
			return false;
		}
		return true;
	}
}