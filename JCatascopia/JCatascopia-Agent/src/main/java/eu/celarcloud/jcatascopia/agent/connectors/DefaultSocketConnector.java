package eu.celarcloud.jcatascopia.agent.connectors;

import java.util.UUID;

import eu.celarcloud.jcatascopia.agent.sockets.Dealer;

public class DefaultSocketConnector implements IServerConnector {

	private String endpoint;
	private String port;
	
	public DefaultSocketConnector(String endpoint, String port) {
		this.endpoint = endpoint;
		this.port = port;
	}
	
	public String joinCluster(String seed, String agentID, String msg) {
		String r = sendRequest(endpoint, port, "AGENT.JOIN", msg);
		if (r.contains("OK"))
			return r.split("\\|")[1].trim();
		else return null;
	}
	
	public boolean initAgent(String serverIP, String agentID, String msg) {
		this.endpoint = serverIP;
		String r = sendRequest(serverIP, port, "AGENT.METRICS", msg);
		return r.contains("OK") ? true : false;
	}
	
	private String sendRequest(String endpoint, String port, String header, String body) {
		Dealer dealer = new Dealer(endpoint, port, UUID.randomUUID().toString().replace("-", ""));
		int attempts = 0; 
		boolean connected = false;
    	String[] response = null;
    	String resp = "";
    	try {			
			while(((attempts++) < 3) && (!connected)) {
	    		dealer.send("", header, body);
	            response = dealer.receive(12000L);
	            if (response != null) {
	            	connected = true;
	            	resp = response[1];
	            	break;
	            }
				else	
					Thread.sleep(3000);
	    	}
		} 
    	catch(InterruptedException e) {
    		e.printStackTrace();	
    	}  
    	catch(Exception e) {	
    		e.printStackTrace();
    	}
    	finally {
    		dealer.close();
    	}
    	
    	return resp;
	}

	public boolean termAgent(String agentID) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean updateAgent(String serverIP, String agentID, String msg) {
		// TODO Auto-generated method stub
		return false;
	}
}
