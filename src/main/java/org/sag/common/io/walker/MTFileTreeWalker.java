package org.sag.common.io.walker;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MTFileTreeWalker {
	
	protected MTDirectoryWalkTask startTask;
	protected final BlockingQueue<Throwable> errs;

	public MTFileTreeWalker(MTDirectoryWalkTask startTask) {
		Objects.requireNonNull(startTask);
		this.startTask = startTask;
		this.errs = new LinkedBlockingQueue<>();
		startTask.setErrsList(this.errs);
	}
	
	public void walkFileTree() throws Exception {
		ForkJoinPool pool = null;
		try{
			pool = new ForkJoinPool();
			//Starts thread and joins on thread
			//Will only return when all other threads have exited which is when their child threads exit with an exception or normal
			//I.e. all threads that spawn other threads wait for them to exit by join
			pool.invoke(startTask);
		}catch(Throwable t){
			errs.offer(new Exception("Error: Unexpected exception when walking the file tree starting at '" + startTask.getDirectory() + "'.",t));
		}finally{
			try{
				if(pool != null)
					pool.shutdown();
			} catch(Throwable t){
				errs.offer(new Exception("Error: Unexpected exception when trying to shutdown the thread pool for the file tree walk starting at '" 
						+ startTask.getDirectory() + "'.",t));
			}
			try{
				if(pool != null)
					pool.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS);//wait until the end of time for all threads to exit
			} catch(Throwable t){
				errs.offer(new Exception("Error: Unexpected exception when trying to wait for all threads to terminate for the file tree walk "
						+ "starting at '" + startTask.getDirectory() + "'.",t));
			}
		}
		
		if(!errs.isEmpty()){
			String msg = null;
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try(PrintStream ps = new PrintStream(baos,true,"utf-8")){
				ps.println("Error: Failed to completly walk the file tree starting at '" + startTask.getDirectory()
						+ "' and/or complete all required tasks on the stroll. The following exceptions were thrown in the process:");
				int i = 0;
				for(Throwable t : errs){
					ps.print("Exception ");
					ps.print(i++);
					ps.print(": ");
					t.printStackTrace(ps);
				}
				msg = new String(baos.toByteArray(), StandardCharsets.UTF_8);
			}catch(Throwable t){
				throw new Exception("Error: Something went wrong when combining all exceptions into one exception.",t);
			}
			throw new Exception(msg);
		}
	}
	
}
