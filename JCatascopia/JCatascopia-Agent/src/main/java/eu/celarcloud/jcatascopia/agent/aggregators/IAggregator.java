package eu.celarcloud.jcatascopia.agent.aggregators;

public interface IAggregator {
	
	public void add(String metric);
	
	public String toMessage();
	
	public void clear();
	
	public int length();
}
