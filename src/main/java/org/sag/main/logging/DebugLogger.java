package org.sag.main.logging;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.sag.common.concurrent.TaskConsumer;
import org.sag.common.concurrent.TaskConsumer.Task;
import org.sag.common.concurrent.TaskConsumer.TaskProducer;
import org.sag.common.io.FileHelpers;
import org.sag.common.logging.BlankLogger;
import org.sag.common.logging.IDebugLogger;
import org.sag.common.logging.ILogger;
import org.sag.common.logging.LoggerWrapperSLF4J;
import org.slf4j.helpers.MessageFormatter;

import com.google.common.io.Files;

import soot.SootClass;
import soot.SootMethod;

public class DebugLogger implements IDebugLogger {
	
	private static final String cn = DebugLogger.class.getSimpleName();
	
	private static volatile ILogger mainLogger = null;
	private static volatile ILogger.LogLevel defaultLogLevel = null;
	private static volatile TaskConsumer consumer = null;
	private static volatile Map<String,AtomicInteger> existingFiles = null;
	private static volatile Map<DebugLogger,Boolean> activeLoggers = null;
	private static volatile AtomicLong idCount = new AtomicLong(0);
	private static volatile boolean enabled = false;
	private static volatile boolean simpleLoggerEnabled = false;
	
	private static volatile ILogger.LogLevel FATAL = null;
	private static volatile ILogger.LogLevel WARN = null;
	private static volatile ILogger.LogLevel INFO = null;
	private static volatile ILogger.LogLevel FINE_INFO = null;
	private static volatile ILogger.LogLevel DEBUG = null;
	private static volatile ILogger.LogLevel TRACE = null;
	private static volatile ILogger.LogLevel FINE_TRACE = null;
	
	private static final boolean normal() {
		return enabled && !simpleLoggerEnabled;
	}
	
	private static final boolean simpleLogger() {
		return enabled && simpleLoggerEnabled;
	}
	
	public static void init(boolean enable, boolean forceToConsole, boolean simpleLogger){
		enabled = enable;
		simpleLoggerEnabled = simpleLogger;
		if(normal()){
			if(mainLogger == null)
				mainLogger = CentralLogger.getLogger(DebugLogger.class.getName());
			if(mainLogger == null){
				throw new RuntimeException("Could not get instance of main logger!");
			}
			if(defaultLogLevel == null){
				if(forceToConsole){
					defaultLogLevel = CentralLogger.getLogLevelMain();
					FATAL = defaultLogLevel.compareTo(ILogger.LogLevel.FATAL) > 0 ? defaultLogLevel : ILogger.LogLevel.FATAL;
					WARN = defaultLogLevel.compareTo(ILogger.LogLevel.WARN) > 0 ? defaultLogLevel : ILogger.LogLevel.WARN;
					INFO = defaultLogLevel.compareTo(ILogger.LogLevel.INFO) > 0 ? defaultLogLevel : ILogger.LogLevel.INFO;
					FINE_INFO = defaultLogLevel.compareTo(ILogger.LogLevel.FINE_INFO) > 0 ? defaultLogLevel : ILogger.LogLevel.FINE_INFO;
					DEBUG = defaultLogLevel.compareTo(ILogger.LogLevel.DEBUG) > 0 ? defaultLogLevel : ILogger.LogLevel.DEBUG;
					TRACE = defaultLogLevel.compareTo(ILogger.LogLevel.TRACE) > 0 ? defaultLogLevel : ILogger.LogLevel.TRACE;
					FINE_TRACE = defaultLogLevel.compareTo(ILogger.LogLevel.FINE_TRACE) > 0 ? defaultLogLevel : ILogger.LogLevel.FINE_TRACE;
				}else{
					FATAL = ILogger.LogLevel.FATAL;
					WARN = ILogger.LogLevel.WARN;
					INFO = ILogger.LogLevel.INFO;
					FINE_INFO = ILogger.LogLevel.FINE_INFO;
					DEBUG = ILogger.LogLevel.DEBUG;
					TRACE = ILogger.LogLevel.TRACE;
					FINE_TRACE = ILogger.LogLevel.FINE_TRACE;
					defaultLogLevel = CentralLogger.getLogLevelDebug();
				}
			}
			if(consumer == null)
				consumer = new TaskConsumer(true,true,50,25);
			if(existingFiles == null)
				existingFiles = Collections.synchronizedMap(new HashMap<String,AtomicInteger>());
			if(activeLoggers == null)
				activeLoggers = Collections.synchronizedMap(new HashMap<DebugLogger,Boolean>());
			idCount = new AtomicLong(0);
		}
	}
	
	public static void end() throws InterruptedException{
		if(normal()){
			synchronized(activeLoggers) {
				if(!activeLoggers.isEmpty()){
					for(Iterator<Map.Entry<DebugLogger,Boolean>> it = activeLoggers.entrySet().iterator(); it.hasNext();){
						Map.Entry<DebugLogger,Boolean> e = it.next();
						if(e.getValue()){
							e.getKey().removeInner();
						}else{
							e.getKey().closeInner();
						}
						it.remove();
					}
				}
			}
			consumer.end();//blocks until all io has finished and consumer thread exits
			mainLogger = null;
			defaultLogLevel = null;
			FATAL = null;
			WARN = null;
			INFO = null;
			FINE_INFO = null;
			DEBUG = null;
			TRACE = null;
			FINE_TRACE = null;
			consumer = null;
			existingFiles = null;
			activeLoggers = null;
			idCount = new AtomicLong(0);
		}
		enabled = false;
		simpleLoggerEnabled = false;
	}
	
	//Since a logger is needed and this must be run after end which clears everything we have to pass in a logger
	public static void removeEmptyDirs(Path rootOutDir, ILogger logger){
		if(normal()){
			try{
				FileHelpers.removeEmptyDirectories(rootOutDir);
			} catch(Throwable t){
				logger.warn("", t);
			}
		}
	}
	
	public static IDebugLogger getNewDebugLogger(Class<?> clazz, Path rootOutDir) {
		if(normal()) {
			Objects.requireNonNull(clazz);
			Objects.requireNonNull(rootOutDir);
			Path logFile = createOutFilePath(clazz.getSimpleName(), rootOutDir);
			if(logFile != null) {
				String loggerName = Files.getNameWithoutExtension(logFile.toString());
				if(loggerName != null){
					IDebugLogger dbl = getNewDebugLoggerInner(loggerName,logFile);
					if(dbl != null) {
						mainLogger.info("{}: Created debug logger for clazz '{}' at '{}'",cn,clazz,logFile);
						return dbl;
					} else {
						mainLogger.fatal("{}: Failed to create debug logger for clazz '{}' at '{}'",cn,clazz,logFile);
					}
				} else{
					mainLogger.fatal("{}: Failed to parse logger name from '{}'.",cn,logFile);
				}
			} else {
				mainLogger.fatal("{}: Failed to create file path for clazz '{}' in directory '{}'.",cn,clazz,rootOutDir);
			}
			return null;
		} else if(simpleLogger()) {
			Objects.requireNonNull(clazz);
			return getNewDebugLoggerInner(clazz.getSimpleName(),null);
		} else {
			return getNewDebugLoggerInner(null,null);
		}
	}
	
	public static IDebugLogger getNewDebugLogger(SootClass stub, SootMethod method, Path rootOutDir) {
		if(normal()){
			Objects.requireNonNull(stub);
			Objects.requireNonNull(method);
			Objects.requireNonNull(rootOutDir);
			Path logFile = createOutFilePath(stub.getName(),method,rootOutDir);
			if(logFile != null) {
				String loggerName = Files.getNameWithoutExtension(logFile.toString());
				if(loggerName != null) {
					IDebugLogger dbl = getNewDebugLoggerInner(loggerName,logFile);
					if(dbl != null) {
						mainLogger.info("{}: Created debug logger for stub '{}' and method '{}' at '{}'",cn,stub,method,logFile);
						return dbl;
					} else {
						mainLogger.fatal("{}: Failed to create debug logger for stub '{}' and method '{}' at '{}'",cn,stub,method,logFile);
					}
				}else{
					mainLogger.fatal("{}: Failed to parse logger name from '{}'.",cn,logFile);
				}
			}else{
				mainLogger.fatal("{}: Failed to create file path for stub '{}' and method '{}' in directory '{}'.",cn,stub,method,rootOutDir);
			}
			return null;
		} else if(simpleLogger()) {
			Objects.requireNonNull(method);
			return getNewDebugLoggerInner(getBaseLoggerName(method),null);
		} else {
			return getNewDebugLoggerInner(null,null);
		}
	}
	
	private static IDebugLogger getNewDebugLoggerInner(String loggerName, Path logFile) {
		long id = idCount.getAndIncrement();
		loggerName = id + "_"  + loggerName;
		if(normal()) {
			ILogger logger = CentralLogger.startLogger(loggerName, logFile, defaultLogLevel, true);
			if(logger != null){
				DebugLogger dbl = new DebugLogger(logger,consumer.startProducer(),logFile,id);
				activeLoggers.put(dbl,false);
				return dbl;
			}else{
				mainLogger.fatal("{}: Failed to initilize logger '{}' for '{}'.",cn,loggerName,logFile);
				return null;
			}
		} else if(simpleLogger()) {
			return new LoggerWrapperSLF4J(loggerName, true);
		} else {
			return BlankLogger.v();
		}
	}
	
	private static Path createOutFilePath(String name, Path dirPath) {
		StringBuilder sb = new StringBuilder(dirPath.toString());
		try {
			FileHelpers.processDirectory(Paths.get(sb.toString()).toAbsolutePath(), true, false);
		} catch (Throwable t) {
			mainLogger.fatal("Failed to initilize logger file directories for path " + sb.toString(),t);
			return null;
		}
		sb.append(File.separator).append(FileHelpers.replaceWhitespace(FileHelpers.cleanFileName(name)));
		
		StringBuilder sb2 = new StringBuilder();
		synchronized(existingFiles) {
			AtomicInteger atom = existingFiles.get(sb.toString());
			if(atom == null){
				atom = new AtomicInteger(0);
				existingFiles.put(sb.toString(), atom);
			}
			sb2.append("_").append(atom.incrementAndGet()).append(".log");
		}
		
		try{
			sb2.insert(0,FileHelpers.trimFullFilePath(sb.toString(), false, sb2.length()));
		}catch(Exception e){
			mainLogger.fatal(e.getMessage(),e);
			return null;
		}
		return Paths.get(sb2.toString()).toAbsolutePath();
	}
	
	private static Path createOutFilePath(String stub, SootMethod popSource, Path dirPath) {
		StringBuilder sb = new StringBuilder(dirPath.toString());
		String[] parts = stub.split("\\.");
		for(int i = 0; i < parts.length; i++){
			sb.append(File.separator).append(FileHelpers.replaceWhitespace(FileHelpers.cleanFileName(parts[i])));
		}
		
		try {
			FileHelpers.processDirectory(Paths.get(sb.toString()).toAbsolutePath(), true, false);
		} catch (Throwable t) {
			mainLogger.fatal("Failed to initilize logger file directories for path " + sb.toString(),t);
			return null;
		}
		
		sb.append(getBaseLoggerName(popSource));
		StringBuilder sb2 = new StringBuilder();
		synchronized(existingFiles) {
			AtomicInteger atom = existingFiles.get(sb.toString());
			if(atom == null){
				atom = new AtomicInteger(0);
				existingFiles.put(sb.toString(), atom);
			}
			sb2.append("_").append(atom.incrementAndGet()).append(".log");
		}
		try{
			sb2.insert(0,FileHelpers.trimFullFilePath(sb.toString(), false, sb2.length()));
		}catch(Exception e){
			mainLogger.fatal(e.getMessage(),e);
			return null;
		}
		return Paths.get(sb2.toString()).toAbsolutePath();
	}
	
	private static String getBaseLoggerName(SootMethod m) {
		StringBuilder sb = new StringBuilder();
		String className = m.getDeclaringClass().getShortName();
		int i3 = className.lastIndexOf('$');
		if(i3 > 0 && className.length() > 1){
			className = className.substring(i3+1);
		}
		sb.append(File.separator).append(FileHelpers.replaceWhitespace(FileHelpers.cleanFileName(className))).append("_");
		String retType = m.getReturnType().toString();
		int i = retType.lastIndexOf('.');
		if(i > 0 && retType.length() > 1) {
			retType = retType.substring(i+1);
		}
		int i2 = retType.lastIndexOf('$');
		if(i2 > 0 && retType.length() > 1){
			retType = retType.substring(i2+1);
		}
		sb.append(FileHelpers.replaceWhitespace(FileHelpers.cleanFileName(retType))).append("_");
		sb.append(FileHelpers.replaceWhitespace(FileHelpers.cleanFileName(m.getName())));
		return sb.toString();
	}
	
	private TaskProducer producer;
	private ILogger logger;
	private Path filePath;
	private String fileName;
	private long id;
	
	protected DebugLogger(ILogger logger, TaskProducer producer, Path filePath, long id){
		this.logger = logger;
		this.producer = producer;
		this.filePath = filePath;
		this.fileName = null;
		this.id = id;
	}
	
	public Path getFilePath(){
		return filePath;
	}
	
	public String getFileNameWithoutExtension(){
		if(fileName == null && filePath != null)
			fileName = Files.getNameWithoutExtension(filePath.toString());
		return fileName;
	}
	
	public int hashCode(){
		return (int)id;
	}
	
	public boolean equals(Object o){
		if(this == o){
			return true;
		}
		if(o == null || !(o instanceof DebugLogger)){
			return false;
		}
		DebugLogger dbl = (DebugLogger)o;
		return this.id == dbl.id;
	}
	
	public String toString(){
		return Objects.toString(logger);
	}
	
	public void fatal(String msg){
		log(FATAL,msg);
	}
	
	public void fatal(String format, Object param1){
		log(FATAL,format, param1);
	}
	
	public void fatal(String format, Object param1, Object param2){
		log(FATAL,format, param1, param2);
	}
	
	public void fatal(String format, Object... params){
		log(FATAL,format, params);
	}
	
	public void fatal(String msg, Throwable t){
		log(FATAL,msg, t);
	}
	
	public void fatal(String format, Throwable t, Object param1){
		log(FATAL,format, t, param1);
	}
	
	public void fatal(String format, Throwable t, Object param1, Object param2){
		log(FATAL,format, t, param1, param2);
	}
	
	public void fatal(String format, Throwable t, Object... params){
		log(FATAL,format, t, params);
	}
	
	public void warn(String msg){
		log(WARN,msg);
	}
	
	public void warn(String format, Object param1){
		log(WARN,format, param1);
	}
	
	public void warn(String format, Object param1, Object param2){
		log(WARN,format, param1, param2);
	}
	
	public void warn(String format, Object... params){
		log(WARN,format, params);
	}
	
	public void warn(String msg, Throwable t){
		log(WARN,msg, t);
	}
	
	public void warn(String format, Throwable t, Object param1){
		log(WARN,format, t, param1);
	}
	
	public void warn(String format, Throwable t, Object param1, Object param2){
		log(WARN,format, t, param1, param2);
	}
	
	public void warn(String format, Throwable t, Object... params){
		log(WARN,format, t, params);
	}
	
	public void info(String msg){
		log(INFO,msg);
	}
	
	public void info(String format, Object param1){
		log(INFO,format, param1);
	}
	
	public void info(String format, Object param1, Object param2){
		log(INFO,format, param1, param2);
	}
	
	public void info(String format, Object... params){
		log(INFO,format, params);
	}
	
	public void info(String msg, Throwable t){
		log(INFO,msg, t);
	}
	
	public void info(String format, Throwable t, Object param1){
		log(INFO,format, t, param1);
	}
	
	public void info(String format, Throwable t, Object param1, Object param2){
		log(INFO,format, t, param1, param2);
	}
	
	public void info(String format, Throwable t, Object... params){
		log(INFO,format, t, params);
	}
	
	public void fineInfo(String msg){
		log(FINE_INFO,msg);
	}
	
	public void fineInfo(String format, Object param1){
		log(FINE_INFO,format, param1);
	}
	
	public void fineInfo(String format, Object param1, Object param2){
		log(FINE_INFO,format, param1, param2);
	}
	
	public void fineInfo(String format, Object... params){
		log(FINE_INFO,format, params);
	}
	
	public void fineInfo(String msg, Throwable t){
		log(FINE_INFO,msg, t);
	}
	
	public void fineInfo(String format, Throwable t, Object param1){
		log(FINE_INFO,format, t, param1);
	}
	
	public void fineInfo(String format, Throwable t, Object param1, Object param2){
		log(FINE_INFO,format, t, param1, param2);
	}
	
	public void fineInfo(String format, Throwable t, Object... params){
		log(FINE_INFO,format, t, params);
	}
	
	public void debug(String msg){
		log(DEBUG,msg);
	}
	
	public void debug(String format, Object param1){
		log(DEBUG,format, param1);
	}
	
	public void debug(String format, Object param1, Object param2){
		log(DEBUG,format, param1, param2);
	}
	
	public void debug(String format, Object... params){
		log(DEBUG,format, params);
	}
	
	public void debug(String msg, Throwable t){
		log(DEBUG,msg, t);
	}
	
	public void debug(String format, Throwable t, Object param1){
		log(DEBUG,format, t, param1);
	}
	
	public void debug(String format, Throwable t, Object param1, Object param2){
		log(DEBUG,format, t, param1, param2);
	}
	
	public void debug(String format, Throwable t, Object... params){
		log(DEBUG,format, t, params);
	}
	
	public void trace(String msg){
		log(TRACE,msg);
	}
	
	public void trace(String format, Object param1){
		log(TRACE,format, param1);
	}
	
	public void trace(String format, Object param1, Object param2){
		log(TRACE,format, param1, param2);
	}
	
	public void trace(String format, Object... params){
		log(TRACE,format, params);
	}
	
	public void trace(String msg, Throwable t){
		log(TRACE,msg, t);
	}
	
	public void trace(String format, Throwable t, Object param1){
		log(TRACE,format, t, param1);
	}
	
	public void trace(String format, Throwable t, Object param1, Object param2){
		log(TRACE,format, t, param1, param2);
	}
	
	public void trace(String format, Throwable t, Object... params){
		log(TRACE,format, t, params);
	}
	
	public void fineTrace(String msg){
		log(FINE_TRACE,msg);
	}
	
	public void fineTrace(String format, Object param1){
		log(FINE_TRACE,format, param1);
	}
	
	public void fineTrace(String format, Object param1, Object param2){
		log(FINE_TRACE,format, param1, param2);
	}
	
	public void fineTrace(String format, Object... params){
		log(FINE_TRACE,format, params);
	}
	
	public void fineTrace(String msg, Throwable t){
		log(FINE_TRACE,msg, t);
	}
	
	public void fineTrace(String format, Throwable t, Object param1){
		log(FINE_TRACE,format, t, param1);
	}
	
	public void fineTrace(String format, Throwable t, Object param1, Object param2){
		log(FINE_TRACE,format, t, param1, param2);
	}
	
	public void fineTrace(String format, Throwable t, Object... params){
		log(FINE_TRACE,format, t, params);
	}
	
	public void log(final ILogger.LogLevel level, final String msg){
		if(normal()){
			boolean ret = producer.addTask(new Task(){
				private final ILogger logger = DebugLogger.this.logger;
				@Override
				public void doWork() {
					logger.log(level,msg);
				}
			});
			if(!ret)
				mainLogger.warn("Error: Failed to log message to " + logger.getName());
		}
	}
	
	public void log(final ILogger.LogLevel level, final String format, final Object param1){
		if(normal()){
			boolean ret = producer.addTask(new Task(){
				private final ILogger logger = DebugLogger.this.logger;
				@Override
				public void doWork() {
					logger.log(level,MessageFormatter.format(format, param1, null).getMessage());
				}
			});
			if(!ret)
				mainLogger.warn("Error: Failed to log message to " + logger.getName());
		}
	}
	
	public void log(final ILogger.LogLevel level, final String format, final Object param1, final Object param2){
		if(normal()){
			boolean ret = producer.addTask(new Task(){
				private final ILogger logger = DebugLogger.this.logger;
				@Override
				public void doWork() {
					logger.log(level,MessageFormatter.format(format, param1, param2).getMessage());
				}
			});
			if(!ret)
				mainLogger.warn("Error: Failed to log message to " + logger.getName());
		}
	}
	
	public void log(final ILogger.LogLevel level, final String format, final Object... params){
		if(normal()){
			boolean ret = producer.addTask(new Task(){
				private final ILogger logger = DebugLogger.this.logger;
				@Override
				public void doWork() {
					logger.log(level,MessageFormatter.arrayFormat(format, params).getMessage());
				}
			});
			if(!ret)
				mainLogger.warn("Error: Failed to log message to " + logger.getName());
		}
	}
	
	public void log(final ILogger.LogLevel level, final String msg, final Throwable t){
		if(normal()){
			boolean ret = producer.addTask(new Task(){
				private final ILogger logger = DebugLogger.this.logger;
				@Override
				public void doWork() {
					logger.log(level,msg,t);
				}
			});
			if(!ret)
				mainLogger.warn("Error: Failed to log message to " + logger.getName());
		}
	}
	
	public void log(final ILogger.LogLevel level, final String format, final Throwable t, final Object param1){
		if(normal()){
			boolean ret = producer.addTask(new Task(){
				private final ILogger logger = DebugLogger.this.logger;
				@Override
				public void doWork() {
					logger.log(level,MessageFormatter.format(format, param1, null).getMessage(),t);
				}
			});
			if(!ret)
				mainLogger.warn("Error: Failed to log message to " + logger.getName());
		}
	}
	
	public void log(final ILogger.LogLevel level, final String format, final Throwable t, final Object param1, final Object param2){
		if(normal()){
			boolean ret = producer.addTask(new Task(){
				private final ILogger logger = DebugLogger.this.logger;
				@Override
				public void doWork() {
					logger.log(level,MessageFormatter.format(format, param1, param2).getMessage(),t);
				}
			});
			if(!ret)
				mainLogger.warn("Error: Failed to log message to " + logger.getName());
		}
	}
	
	public void log(final ILogger.LogLevel level, final String format, final Throwable t, final Object... params){
		if(normal()){
			boolean ret = producer.addTask(new Task(){
				private final ILogger logger = DebugLogger.this.logger;
				@Override
				public void doWork() {
					logger.log(level,MessageFormatter.arrayFormat(format, params).getMessage(),t);
				}
			});
			if(!ret)
				mainLogger.warn("Error: Failed to log message to " + logger.getName());
		}
	}
	
	public boolean flush(){
		if(normal()){
			boolean ret = producer.addTask(new Task(){
				private final ILogger logger = DebugLogger.this.logger;
				@Override
				public void doWork() {
					logger.flush();
				}
			});
			if(!ret){
				mainLogger.warn("Error: Failed to flush message for {}", logger.getName());
				return false;
			}
		}
		return true;
	}
	
	private void closeInner2(){
		logger = null;
		producer.killProducer();
		producer = null;
	}
	
	private void closeInner(){
		boolean ret = producer.addTask(new Task(){
			private final ILogger logger = DebugLogger.this.logger;
			@Override
			public void doWork() {
				logger.flush();
				logger.close();
			}
		});
		if(!ret)
			mainLogger.warn("Error: Failed to close logger {}", logger.getName());
		closeInner2();
	}
	
	private void removeInner(){
		boolean ret = producer.addTask(new Task(){
			private final Path filePath = DebugLogger.this.filePath;
			private final ILogger logger = DebugLogger.this.logger;
			@Override
			public void doWork() {
				logger.flush();
				logger.close();
				try{
					for(int i = 0; i < 10; i++){
						if(java.nio.file.Files.notExists(filePath)){
							break;
						}
						java.nio.file.Files.deleteIfExists(filePath);
						if(java.nio.file.Files.exists(filePath)){
							Thread.sleep(5000);
							logger.flush();
							logger.close();
						}else{
							break;
						}
					}
				}catch(Throwable t){
					throw new RuntimeException("Something went wrong when closing and removing logger file " + filePath,t);
				}
			}
		});
		if(!ret)
			mainLogger.warn("Error: Failed to remove logger file {}", logger.getName());
		closeInner2();
	}
	
	public void setRemove(boolean remove){
		if(normal()){
			synchronized(activeLoggers) {
				if(activeLoggers.containsKey(this)){
					activeLoggers.put(this, remove);
				}
			}
		}
	}
	
	public void closeOrCloseAndRemove(){
		if(normal()){
			synchronized(activeLoggers) {
				Boolean ret = activeLoggers.get(this);
				boolean b = false;
				if(ret != null){
					b = ret;
				}
				if(b){
					removeInner();
				}else{
					closeInner();
				}
				activeLoggers.remove(this);
			}
		}
	}
	
	public void closeAndRemove(){
		if(normal()){
			synchronized(activeLoggers) {
				if(activeLoggers.containsKey(this)){
					activeLoggers.put(this, true);
				}
				removeInner();
				activeLoggers.remove(this);
			}
		}
	}
	
	public boolean close(){
		if(normal()){
			synchronized(activeLoggers) {
				if(activeLoggers.containsKey(this)){
					activeLoggers.put(this, false);
				}
				closeInner();
				activeLoggers.remove(this);
			}
		}
		return true;
	}

	public String getName() {
		if(normal()){
			logger.getName();
		}
		return null;
	}
	
}
