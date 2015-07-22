package eu.celarcloud.jcatascopia.agent.aggregators;

import eu.celarcloud.jcatascopia.agent.IMonitoringAgent;

public class StringAggregator implements IAggregator{

	private StringBuffer aggregator;
	private String agentID;
	private String agentIP;
	private IMonitoringAgent agent;
	
	public StringAggregator(String agentID, String agentIP, IMonitoringAgent agent){
		this.aggregator = new StringBuffer();
		this.agentID = agentID;
		this.agentIP = agentIP;
		this.agent = agent;
	}

	public void add(String metric) {
		if(this.aggregator.length() > 0)
			this.aggregator.append("," + metric);
		else
			this.aggregator.append("{\"events\":[" + metric);
	}
	
	public String toMessage() {
		if(this.aggregator.length() == 0)
			this.add("");
		this.aggregator.append("],\"agentID\":\"" + this.agentID + "\"");
		this.aggregator.append(",\"host\":\"" + this.agentIP + "\"}");
		
		if (this.agent.inDebugMode())
			System.out.println("StringAggregator>> Message Ready for Distribution: " + this.aggregator.toString());
		return this.aggregator.toString();
	}
	
	public void clear() {
		this.aggregator.setLength(0);
	}
	
	public int length() {
		return this.aggregator.length();
	}
}
