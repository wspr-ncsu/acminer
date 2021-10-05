package org.sag.common.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

public class PrintStreamUnixEOL extends PrintStream{

	public PrintStreamUnixEOL(String fileName, String csn) throws FileNotFoundException,
			UnsupportedEncodingException {
		super(fileName, csn);
	}

	public PrintStreamUnixEOL(String fileName) throws FileNotFoundException {
		super(fileName);
	}

	public PrintStreamUnixEOL(OutputStream out, boolean autoFlush, String encoding)
			throws UnsupportedEncodingException {
		super(out, autoFlush, encoding);
	}

	public PrintStreamUnixEOL(OutputStream out, boolean autoFlush) {
		super(out, autoFlush);
	}

	public PrintStreamUnixEOL(OutputStream out) {
		super(out);
	}

	public PrintStreamUnixEOL(File file, String csn) throws FileNotFoundException,
			UnsupportedEncodingException {
		super(file, csn);
	}

	public PrintStreamUnixEOL(File file) throws FileNotFoundException {
		super(file);
	}

	private void newLine(){
		write('\n');
	}
	
	@Override
	public void println(){
		newLine();
	}
	
	@Override
	public void println(boolean x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }
	
	@Override
	public void println(char x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }
	
	@Override
	public void println(int x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }
	
	@Override
	public void println(long x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }
	
	@Override
	public void println(float x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }
	
	@Override
	public void println(double x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }
	
	@Override
	public void println(char x[]) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }
	
	@Override
	public void println(String x) {
        synchronized (this) {
            print(x);
            newLine();
        }
    }
	
	@Override
	public void println(Object x) {
        String s = String.valueOf(x);
        synchronized (this) {
            print(s);
            newLine();
        }
    }
}
