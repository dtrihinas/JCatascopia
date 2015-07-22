package eu.celarcloud.jcatascopia.agent.connectors;

public interface IServerConnector {
	
	public String joinCluster(String seed, String agentID, String msg);
	
	public boolean initAgent(String serverIP, String agentID, String msg);	
	
	public boolean termAgent(String agentID);
	
	public boolean updateAgent(String serverIP, String agentID, String msg);
}
