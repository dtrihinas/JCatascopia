package eu.celarcloud.jcatascopia.agent;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import eu.celarcloud.jcatascopia.agent.aggregators.IAggregator;

public class MetricCollector extends Deamon {
	
	private IAggregator aggregator;
	private IMonitoringAgent agent;
	private LinkedBlockingQueue<String> queue;

	public MetricCollector(LinkedBlockingQueue<String> queue, IAggregator aggregator, IMonitoringAgent agent) {
		super("MetricCollector", 2000, agent);
		
		this.aggregator = aggregator;
		this.agent = agent;
		this.queue = queue;
	}

	@Override
	public void doWork() {
		try {
			String m = this.queue.poll(500, TimeUnit.MILLISECONDS);
			if (m != null) {
				if(this.agent.inDebugMode())
					System.out.println("MetricCollector>> DeQueued Metric: " + m);
				this.aggregator.add(m);
			}
		}
		catch(Exception e) {
			this.agent.writeToLog(Level.SEVERE, "MetricCollector>> " + e.getMessage());
		}	
	}
}
