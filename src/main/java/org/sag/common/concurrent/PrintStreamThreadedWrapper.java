package org.sag.common.concurrent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.sag.common.concurrent.TaskConsumer.*;

public class PrintStreamThreadedWrapper extends PrintStream {
	
	private TaskProducer producer;
	private PrintStream out;
	boolean trouble;
	
	private PrintStreamThreadedWrapper(TaskConsumer consumer){
		super(new OutputStream(){
			@Override
			public void write(int b) throws IOException {}
		});
		producer = consumer.startProducer();
		trouble = false;
	}
	
	private void addTask(Task t){
		if(!producer.addTask(t)){
			trouble = true;
		}
	}
	
	private void killProducer(){
		if(!producer.killProducer()){
			trouble = true;
		}
	}
	
	public PrintStreamThreadedWrapper(PrintStream out, TaskConsumer consumer) {
		this(consumer);
		this.out = out;
	}
	
	public PrintStreamThreadedWrapper(OutputStream out, TaskConsumer consumer) {
		this(consumer);
		this.out = new PrintStream(out);
	}

	public PrintStreamThreadedWrapper(String fileName, TaskConsumer consumer) throws FileNotFoundException {
		this(consumer);
		this.out = new PrintStream(fileName);
	}

	public PrintStreamThreadedWrapper(File file, TaskConsumer consumer) throws FileNotFoundException {
		this(consumer);
		this.out = new PrintStream(file);
	}

	public PrintStreamThreadedWrapper(OutputStream out, boolean autoFlush, TaskConsumer consumer) {
		this(consumer);
		this.out = new PrintStream(out,autoFlush);
	}

	public PrintStreamThreadedWrapper(String fileName, String csn, TaskConsumer consumer) throws FileNotFoundException, UnsupportedEncodingException {
		this(consumer);
		this.out = new PrintStream(fileName,csn);
	}

	public PrintStreamThreadedWrapper(File file, String csn, TaskConsumer consumer) throws FileNotFoundException, UnsupportedEncodingException {
		this(consumer);
		this.out = new PrintStream(file,csn);
	}

	public PrintStreamThreadedWrapper(OutputStream out, boolean autoFlush, String encoding, TaskConsumer consumer) throws UnsupportedEncodingException {
		this(consumer);
		this.out = new PrintStream(out,autoFlush,encoding);
	}

	@Override
	public PrintStream append(final char arg0) {
		addTask(new Task(){
			@Override
			public void doWork() {
				out.append(arg0);
			}
		});
		return this;
	}

	@Override
	public PrintStream append(final CharSequence csq, final int start, final int end) {
		addTask(new Task(){
			@Override
			public void doWork() {
				out.append(csq, start, end);
			}
		});
		return this;
	}

	@Override
	public PrintStream append(final CharSequence csq) {
		addTask(new Task(){
			@Override
			public void doWork() {
				out.append(csq);
			}
		});
		return this;
	}

	@Override
	public boolean checkError() {
		if(trouble){
			return true;
		}else{
			ReturnTask<Boolean> retTask = new ReturnTask<Boolean>(){
				@Override
				public Boolean getNullReturnType() {
					return Boolean.TRUE;
				}
				@Override
				public Boolean doWorkReturn() {
					return out.checkError();
				}
			};
			addTask(retTask);
			Boolean ret = null;
			for(int i = 1; i <= 10; i++){
				try {
					ret = retTask.pollTimeValueProducer(10, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					ret = null;
				}
				if(ret != null)
					break;
			}
			if(ret == null){
				ret = Boolean.TRUE;
				trouble = true;
			}
			return ret;
			
		}
	}
	
	@Override
	protected void clearError() {
		trouble = false;
	}
	
	@Override
	protected void setError() {
		trouble = true;
	}

	@Override
	public void close() {
		addTask(new Task(){
			@Override
			public void doWork() {
				out.close();
			}
		});
		killProducer();
	}

	@Override
	public void flush() {
		addTask(new Task(){
			@Override
			public void doWork() {
				out.flush();
			}
		});
	}

	@Override
	public PrintStream format(final Locale l, final String format, final Object... args) {
		addTask(new Task(){
			@Override
			public void doWork() {
				out.format(l, format, args);
			}
		});
		return this;
	}

	@Override
	public PrintStream format(final String format, final Object... args) {
		addTask(new Task(){
			@Override
			public void doWork() {
				out.format(format, args);
			}
		});
		return this;
	}

	@Override
	public void print(final boolean b) {
		addTask(new Task(){
			@Override
			public void doWork() {
				out.print(b);
			}
		});
	}

	@Override
	public void print(final char c) {
		addTask(new Task(){
			@Override
			public void doWork() {
				out.print(c);
			}
		});
	}

	@Override
	public void print(final char[] s) {
		addTask(new Task(){
			@Override
			public void doWork() {
				out.print(s);
			}
		});
	}

	@Override
	public void print(final double d) {
		addTask(new Task(){
			@Override
			public void doWork() {
				out.print(d);
			}
		});
	}

	@Override
	public void print(final float f) {
		addTask(new Task(){
			@Override
			public void doWork() {
				out.print(f);
			}
		});
	}

	@Override
	public void print(final int i) {
		addTask(new Task(){
			@Override
			public void doWork() {
				out.print(i);
			}
		});
	}

	@Override
	public void print(final long l) {
		addTask(new Task(){
			@Override
			public void doWork() {
				out.print(l);
			}
		});
	}

	@Override
	public void print(final Object obj) {
		addTask(new Task(){
			@Override
			public void doWork() {
				out.print(obj);
			}
		});
	}

	@Override
	public void print(final String s) {
		addTask(new Task(){
			@Override
			public void doWork() {
				out.print(s);
			}
		});
	}

	@Override
	public PrintStream printf(final Locale l, final String format, final Object... args) {
		addTask(new Task(){
			@Override
			public void doWork() {
				out.printf(l, format, args);
			}
		});
		return this;
	}

	@Override
	public PrintStream printf(final String format, final Object... args) {
		addTask(new Task(){
			@Override
			public void doWork() {
				out.printf(format, args);
			}
		});
		return this;
	}

	@Override
	public void println() {
		addTask(new Task(){
			@Override
			public void doWork() {
				out.println();
			}
		});
	}

	@Override
	public void println(final boolean x) {
		addTask(new Task(){
			@Override
			public void doWork() {
				out.println(x);
			}
		});
	}

	@Override
	public void println(final char x) {
		addTask(new Task(){
			@Override
			public void doWork() {
				out.println(x);
			}
		});
	}

	@Override
	public void println(final char[] x) {
		addTask(new Task(){
			@Override
			public void doWork() {
				out.println(x);
			}
		});
	}

	@Override
	public void println(final double x) {
		addTask(new Task(){
			@Override
			public void doWork() {
				out.println(x);
			}
		});
	}

	@Override
	public void println(final float x) {
		addTask(new Task(){
			@Override
			public void doWork() {
				out.println(x);
			}
		});
	}

	@Override
	public void println(final int x) {
		addTask(new Task(){
			@Override
			public void doWork() {
				out.println(x);
			}
		});
	}

	@Override
	public void println(final long x) {
		addTask(new Task(){
			@Override
			public void doWork() {
				out.println(x);
			}
		});
	}

	@Override
	public void println(final Object x) {
		addTask(new Task(){
			@Override
			public void doWork() {
				out.println(x);
			}
		});
	}

	@Override
	public void println(final String x) {
		addTask(new Task(){
			@Override
			public void doWork() {
				out.println(x);
			}
		});
	}

	@Override
	public void write(final byte[] buf, final int off, final int len) {
		addTask(new Task(){
			@Override
			public void doWork() {
				out.write(buf, off, len);
			}
		});
	}

	@Override
	public void write(final int b) {
		addTask(new Task(){
			@Override
			public void doWork() {
				out.write(b);
			}
		});
	}

	@Override
	public void write(final byte[] b) throws IOException {
		addTask(new Task(){
			@Override
			public void doWork() {
				try {
					out.write(b);
				} catch (IOException e) {
					setError();
				}
			}
		});
	}
	
}
