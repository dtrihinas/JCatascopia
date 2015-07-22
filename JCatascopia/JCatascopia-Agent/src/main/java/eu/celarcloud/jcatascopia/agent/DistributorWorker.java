package eu.celarcloud.jcatascopia.agent;

import java.util.logging.Level;

import eu.celarcloud.jcatascopia.agent.aggregators.IAggregator;
import eu.celarcloud.jcatascopia.agent.distributors.IDistributor;

public class DistributorWorker extends Deamon {
	
	private static final long DIST_CHECK_PERIOD = 2000;
		
	private IDistributor distributor;
	private IAggregator aggregator;
	
	//aggregator settings
	private long INTERVAL;
	private int BUF_SIZE;
	
	private long current;
	
	private IMonitoringAgent agent;
	
	public DistributorWorker(IDistributor distributor, IAggregator aggregator,long interval, int buf_size, IMonitoringAgent agent) {
		super("Distributor", DIST_CHECK_PERIOD, agent);
		
		this.distributor = distributor;
		this.aggregator = aggregator;
		
		this.INTERVAL = interval;
		this.BUF_SIZE = buf_size;
		this.current = 0;
		
		this.agent = agent;
	}
	
	@Override
	public void doWork() {
		try {
			if(this.aggregator.length() > 0) { //check if there are any new messages
				if (current > INTERVAL || aggregator.length() > BUF_SIZE) { //time to send message to server
					this.distributor.send(aggregator.toMessage());
					if (!this.agent.inDebugMode())
						System.out.println("DistributorWorker>> Message sent to Monitoring Server");
					current = 0;
					this.aggregator.clear();
				}
				else current += DIST_CHECK_PERIOD;
			}
		}
		catch(Exception e) {
			this.agent.writeToLog(Level.SEVERE, e.getMessage());
			this.aggregator.clear();
		}
	}
}
