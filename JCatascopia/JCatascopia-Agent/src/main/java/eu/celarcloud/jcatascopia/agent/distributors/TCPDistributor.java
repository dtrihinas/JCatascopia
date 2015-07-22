package eu.celarcloud.jcatascopia.agent.distributors;

import eu.celarcloud.jcatascopia.agent.IMonitoringAgent;
import eu.celarcloud.jcatascopia.agent.exceptions.CatascopiaException;
import eu.celarcloud.jcatascopia.agent.sockets.Publisher;
import eu.celarcloud.jcatascopia.agent.sockets.ISocket;

public class TCPDistributor implements IDistributor {

	private Publisher publisher;
	private IMonitoringAgent agent;
	
	public TCPDistributor(String ip, String port, IMonitoringAgent agent){
		this.publisher = new Publisher(ip, port, ISocket.ConnectType.CONNECT);
	}
	
	public void send(String msg) throws CatascopiaException {
		this.publisher.send(msg);
	}

	public void terminate() {
		this.publisher.close();
	}
}
