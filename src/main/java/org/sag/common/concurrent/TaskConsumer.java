package org.sag.common.concurrent;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.sag.common.logging.ILogger;
import org.sag.main.logging.CentralLogger;

public class TaskConsumer implements Runnable {

	private static volatile int consumerCount = 0;
	
	private volatile LinkedBlockingDeque<Task> queue;
	private final AtomicInteger activeProducers;
	private volatile boolean isAlive;
	private final KillTask killTask;
	private Thread activeConsumer;
	private volatile ILogger mainLogger;
	private boolean keepAlive;
	private TaskProducer keepAliveProducer;
	private final int activeProducersLimit;
	private final int openSlots;
	
	public TaskConsumer(boolean logger, boolean keepAlive, int activeProducersLimit, int openSlots){
		this.killTask = new KillTask();
		this.keepAlive = keepAlive;
		this.activeProducers = new AtomicInteger();
		if(keepAlive && activeProducersLimit > 0){//if keepAlive is enabled then append 1 to limit to account for the keepAliveProducer
			this.activeProducersLimit = activeProducersLimit + 1;
		}else{//unlimited active producers if limit is neg or 0
			this.activeProducersLimit = activeProducersLimit;
		}
		this.openSlots = openSlots;
		if(logger){
			this.mainLogger = CentralLogger.getLogger(this.getClass().getName());
		}else{
			this.mainLogger = null;
		}
		start();
	}

	private void start() {
		this.isAlive = true;
		this.activeProducers.set(0);
		this.queue = new LinkedBlockingDeque<Task>();
		this.keepAliveProducer = keepAlive ? startProducer() : null;//this is fine because java synchronized blocks are Reentrant
		this.activeConsumer = new Thread(this, "ConsumerThread"+consumerCount++);
		activeConsumer.start();
	}
	
	public void end() throws InterruptedException{
		if(keepAlive && keepAliveProducer != null){
			keepAliveProducer.killProducer();
			keepAliveProducer = null;
			activeConsumer.join();
		}
	}

	@Override
	public void run() {
		log(ILogger.LogLevel.INFO,"Starting Task Consumer",null);
		while(true){
			try{
				Task t = queue.take();
				if(t.equals(killTask)){
					synchronized(activeProducers){
						if(activeProducersLimit > 0 && openSlots > 0 && activeProducersLimit - activeProducers.get() > openSlots){
							activeProducers.notify();//other threads can't re-acquire lock on activeProducers until it is released here
						}
						if(activeProducers.decrementAndGet() <= 0){//should be fine to have this after notify for the above reason
							isAlive = false;
							break;//make sure the current consumer dies before another is created
						}
					}
				}else if(t instanceof ReturnTask){
					ReturnTask<?> rt = (ReturnTask<?>)t;
					Object ret = rt.doWorkReturn();
					if(ret == null)
						ret = rt.getNullReturnType();
					rt.returnValueConsumer(ret);
				}
				t.doWork();
				t = null;
			}catch(Throwable t){
				log(ILogger.LogLevel.WARN,"Error: Something went wrong executating a task!",t);
			}
		}
		log(ILogger.LogLevel.INFO,"Ending Task Consumer",null);
	}
	
	private void log(ILogger.LogLevel l, String msg, Throwable t){
		if(mainLogger == null){
			System.out.println(msg);
			if(t != null)
				t.printStackTrace();
		}else{
			mainLogger.log(l,msg,t);
		}
	}
	
	public TaskProducer startProducer(){
		synchronized(activeProducers){
			if(!isAlive){
				start();
			}
			if(activeProducersLimit > 0 && openSlots > 0 && activeProducersLimit <= activeProducers.get()){
				while(activeProducersLimit - activeProducers.get() <= openSlots){
					log(ILogger.LogLevel.INFO,"Active producers limit reached. Waiting to get TaskProducer. Active/Limit: " 
							+ activeProducers.get() + "/" + activeProducersLimit + " Open Slots Required: " + openSlots,null);
					try {
						activeProducers.wait(300000);
					} catch (InterruptedException e) {}
				}
			}
			activeProducers.incrementAndGet();
			return new TaskProducer();
		}
	}
	
	private boolean addTask(Task t){
		return queue.offer(t);
	}
	
	private boolean killProducer(){
		return addTask(killTask);
	}
	
	public static interface Task {
		public void doWork();
	}
	
	private static class KillTask implements Task {
		@Override
		public void doWork() {}
	}
	
	public static abstract class ReturnTask<E> implements Task {
		private final ArrayBlockingQueue<Object> queue = new ArrayBlockingQueue<Object>(1);

		public void returnValueConsumer(Object ret) throws InterruptedException{
			queue.offer(ret);
		}
		
		@SuppressWarnings("unchecked")
		public E takeValueProducer() throws InterruptedException{
			return (E)queue.take();
		}
		
		@SuppressWarnings("unchecked")
		public E pollValueProducer() throws InterruptedException{
			return (E)queue.poll();
		}
		
		@SuppressWarnings("unchecked")
		public E pollTimeValueProducer(long timeout, TimeUnit unit) throws InterruptedException{
			return (E)queue.poll(timeout,unit);
		}
		
		public abstract E getNullReturnType();
		
		public abstract E doWorkReturn();
		
		@Override
		public void doWork() {}
		
	}
	
	
	/* Once the kill message has been sent successfully
	 * by this producer it becomes inActive so both kill 
	 * and add fail. Tasks can only be added from active
	 * TaskProducers. Thus, a situation should never arise
	 * where the consumer is killed but there are still
	 * tasks on the queue.
	 */
	public class TaskProducer {
		private boolean isAlive;
		
		private TaskProducer(){
			this.isAlive = true;
		}
		
		public boolean addTask(Task t){
			if(isAlive)
				return TaskConsumer.this.addTask(t);
			return false;
		}
		
		public boolean killProducer(){
			if(isAlive){
				boolean ret = TaskConsumer.this.killProducer();
				if(ret)
					isAlive = false;
				return ret;
			}
			return false;
		}
	}
}
