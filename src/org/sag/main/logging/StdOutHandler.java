package org.sag.main.logging;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

public class StdOutHandler extends StreamHandler{

	public StdOutHandler(Formatter formatter){
		super(System.out, formatter);
	}
	
	@Override
	public synchronized void close(){
		flush();
	}
	
	@Override
	public synchronized void publish(LogRecord record){
		super.publish(record);
		flush();
	}
}
