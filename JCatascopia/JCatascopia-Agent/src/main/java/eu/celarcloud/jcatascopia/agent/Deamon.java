package eu.celarcloud.jcatascopia.agent;

import java.util.logging.Level;

public abstract class Deamon extends Thread {
	
	public enum DeamonStatus{INACTIVE,ACTIVE,DYING};
	
	private IMonitoringAgent agent;
	private boolean firstFlag;
	private DeamonStatus status;
	private long period;
	
	public Deamon(String name, long period, IMonitoringAgent agent) {
		super(name + "Deamon");
		
		this.status = DeamonStatus.INACTIVE;
		this.firstFlag = true;
		this.period = period;
		this.agent = agent;
	}
		
	public synchronized void activate() {
		if (this.status == DeamonStatus.INACTIVE) {
			if (this.firstFlag) {
				super.start();
				this.firstFlag = false;
			}
			else this.notify();
			this.status = DeamonStatus.ACTIVE;	
		}	
	}
	
	public synchronized void terminate() {
		this.status = DeamonStatus.DYING;
		this.notify();
	}
	
	@Override
	public void start() {
		this.activate();
	}
	
	@Override
	public void run() {
		while(this.status != DeamonStatus.DYING) {
			if(this.status == DeamonStatus.ACTIVE) {
				try {
					this.doWork();
					Thread.sleep(period);
				}
				catch(InterruptedException e) {
					this.agent.writeToLog(Level.SEVERE, e.getMessage());
					continue;
				}
				catch(Exception e) {
					this.agent.writeToLog(Level.SEVERE, e.getMessage());
					continue;
				}
			}
			else {
				synchronized(this) {
					while(this.status == DeamonStatus.INACTIVE)
						try {
							this.wait();
						} 
						catch (InterruptedException e) {
							this.agent.writeToLog(Level.SEVERE, e.getMessage());
						}
				}
			}
		}
	}
	
	public abstract void doWork();
}
