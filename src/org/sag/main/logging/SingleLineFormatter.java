package org.sag.main.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.sag.common.logging.ILogger;
import org.sag.main.AndroidInfo;

public class SingleLineFormatter extends Formatter {
	
	public static final String SOOTID = "--SOOT-- ";
	public static final String SMALIID = "--SMALI-- ";
	public static final String BAKSMALIID = "--BAKSMALI-- ";
	
	private final String header;
	private final String tail;
	private final AndroidInfo ai;
	
	public SingleLineFormatter(String header, String tail, AndroidInfo ai){
		super();
		this.header = header;
		this.tail = tail;
		this.ai = ai;
	}
	
	@Override
	public String format(LogRecord record) {
		StringBuffer sb = new StringBuffer();
		Throwable t = record.getThrown();
		String msg = formatMessage(record);
		String level;
		
		if(msg.startsWith(SOOTID)){
			msg = msg.substring(SOOTID.length());
			level = "SOOT";
		}else if(msg.startsWith(SMALIID)){
			msg = msg.substring(SMALIID.length());
			level = "SMALI";
		}else if(msg.startsWith(BAKSMALIID)){
			msg = msg.substring(BAKSMALIID.length());
			level = "BAKSMALI";
		}else{
			level = ILogger.LogLevel.getName(record.getLevel());
		}
		
		sb.append(formatMsg(calcDate(record.getMillis()),level,msg));
		
		if(t != null){
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			t.printStackTrace(pw);
			pw.close();
			sb.append(sw.toString());
		}
		
		return sb.toString();
	}
	
	private String formatMsg(String date, String level, String msg) {
		StringBuilder sb = new StringBuilder();
		sb.append("[").append(date).append("] <").append(level).append(">: ").append(msg).append("\n");
		return sb.toString();
	}
	
	private String calcDate(long millisecs) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yy-MM-dd HH:mm:ss.SSS");
		Date resultdate = new Date(millisecs);
		return dateFormat.format(resultdate);
	}
	
	public String getHead(Handler h) {
		if(header.isEmpty()) {
			return header;
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append(formatMsg(calcDate(Calendar.getInstance().getTimeInMillis()),"BEGIN",header.trim()));
			if(ai != null) {
				sb.append(formatMsg(calcDate(Calendar.getInstance().getTimeInMillis()),"ANDROID_INFO",ai.toString()));
			}
			return sb.toString();
		}
	}
	
	public String getTail(Handler h) {
		if(tail.isEmpty()) {
			return tail;
		} else {
			StringBuilder sb = new StringBuilder();
			if(ai != null) {
				sb.append(formatMsg(calcDate(Calendar.getInstance().getTimeInMillis()),"ANDROID_INFO",ai.toString()));
			}
			sb.append(formatMsg(calcDate(Calendar.getInstance().getTimeInMillis()),"END",tail.trim()));
			return sb.toString();
		}
	}
}
