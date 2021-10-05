package org.sag.main.logging;

import java.io.OutputStream;
import java.io.PrintStream;

import org.sag.common.logging.ILogger;

public class PrintStreamLoggerWrapper extends PrintStream{
	
	public PrintStreamLoggerWrapper(ILogger logger, String uniqueId) {
		super(new LoggerOutputStream(logger,uniqueId));
	}
	
	private static class LoggerOutputStream extends OutputStream{
		
		private final StringBuffer buffer;
		private final ILogger logger;
		private final String ln;
		private final String id;
		
		public LoggerOutputStream(ILogger logger, String id){
			super();
			if(logger == null)
				throw new RuntimeException("Error: A logger must be provided for the LoggerOutputStream in PrintStreamLoggerWrapper.");
			if(id != null)
				this.id = id;
			else
				this.id = "";
			buffer = new StringBuffer(this.id);
			this.logger = logger;
			ln = System.getProperty("line.separator");
		}
		
		@Override
		public synchronized void write(int b) {
			char c = (char)b;
			buffer.append(c);
			
			if(c == '\n'){
				if(buffer.lastIndexOf(ln) >= 0){
					logger.info(buffer.substring(0, buffer.length() - ln.length()));
					buffer.setLength(id.length());
				}else{
					//handle when newline is unix but system is not
					logger.info(buffer.substring(0, buffer.length() - 1));
					buffer.setLength(id.length());
				}
			}
		}
	}
}
