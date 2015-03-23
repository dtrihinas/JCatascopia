package cy.ac.ucy.linc.jcatascopia.maas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import eu.celarcloud.jcatascopia.serverpack.IJCatascopiaServer;
import eu.celarcloud.jcatascopia.serverpack.sockets.Dealer;

public class MaaSMonitor extends Thread{
	
	private IJCatascopiaServer server;
	private int NUM_OF_RETRYS;
	private long PERIOD; 
	
	public MaaSMonitor(IJCatascopiaServer server, int period, int retrys){
		this.server = server;
		this.NUM_OF_RETRYS = retrys;
		this.PERIOD = period * 1000;
	}
	
	public void run(){
		while (true) {
			try{
				if (this.server.inDebugMode())
					System.out.println("ServerMonitor>> Performing a Server Health Check");
				ServerMeta s;
				for (Entry<String,ServerMeta> entry : this.server.getMaaSMap().entrySet()){
					s = entry.getValue();
					if (!s.isRunning()){
						s.incrementAttempts();
						if (s.getAttempts() >= NUM_OF_RETRYS){
			    			this.server.getMaaSMap().remove(entry.getKey());
							this.serverDown(s);
							this.server.writeToLog(Level.WARNING, "Monitoring Server: "+s.getServerIP()+" is not available, Agents of this Server have been distributed to other Servers");
						}
					}
					s.setStatus(ServerMeta.ServerStatus.DOWN);
				}
				Thread.sleep(PERIOD);
			}
			catch (Exception e){
				e.printStackTrace();
				this.server.writeToLog(Level.SEVERE, e);
			}
		}
	}
		
	private void serverDown(ServerMeta s){
		long tstart = System.currentTimeMillis();
		String newServer = null;
		Dealer d = null; 
		ArrayList<ServerMeta> slist = MaaSMonitor.fairness(this.server.getMaaSMap());
		int sint = 0;
		String[] agentlist = s.getAgents().split(",");
		for(String a : agentlist){
			newServer = slist.get(sint).getServerIP();
			System.out.println("Requesting from Agent: " + a + ", to redirect traffic to Server: " + newServer);
			d = new Dealer(a, "4243", UUID.randomUUID().toString());
			d.send("", "AGENT.RECONNECT", newServer);
			d.receive(2000);
			if (sint < (slist.size()-1)) ++sint; else sint = 0;
		}
		System.out.println("ServerMonitor>> Rebalance Time: "+ (System.currentTimeMillis()-tstart));
	}
	
	/*
	 * FAIRNESS ALGORITHM
	 * selects the Monitoring Server with the least Monitoring Agents. 
	 * If all Monitoring Servers are 'balanced' then this becomes a Round Robin selection.
	 */
	public String getServer(){
		ServerMeta s = MaaSMonitor.fairness(this.server.getMaaSMap()).get(0);
		//weird case: if Monitoring Server was just re-started as a seed then Agents already 
		//running will request a Server but no heartbeat has been sent so add them to seed which is also a Server
		return (s != null) ? s.getServerIP() : this.server.getServerIP(); 
	}
	
	private static ArrayList<ServerMeta> fairness(final ConcurrentHashMap<String, ServerMeta> sMap){
		ArrayList<ServerMeta> list = new ArrayList<ServerMeta>(sMap.values());
		Collections.sort(list);
		return (list.size() > 0) ? list : null;
	}
}
