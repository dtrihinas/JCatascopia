package cy.ac.ucy.linc.jcatascopia.maas;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import eu.celarcloud.jcatascopia.serverpack.IJCatascopiaServer;
import eu.celarcloud.jcatascopia.serverpack.beans.AgentObj;
import eu.celarcloud.jcatascopia.serverpack.sockets.Dealer;
import eu.celarcloud.jcatascopia.serverpack.utils.JCompression;

public class MaaSPeer extends Thread{
	private static String MSG_TYPE = "MaaS.HEARTBEAT";
	private Dealer dealer;
	private IJCatascopiaServer server;
	private ConcurrentHashMap<String, AgentObj> agents;
	private String serverID;
	private String serverIP;
	
	public MaaSPeer(String seedIP, String seedPort, IJCatascopiaServer server){
		this.server = server;
		this.agents = this.server.getAgentMap();
		this.serverID = this.server.getServerID();
		this.serverIP = this.server.getServerIP();
		this.dealer = new Dealer(seedIP, seedPort, this.serverID);

		this.server.writeToLog(Level.INFO, "MaaSPeer>> Enabled");
	}
	
	public void run() {
		try {
			while(true) {
				StringBuilder sb = new StringBuilder();
				sb.append(this.serverIP + "|");
				sb.append(this.agents.size() + "|");
				boolean first = true;
				for (Entry<String, AgentObj> a : this.agents.entrySet()){
					sb.append((!first) ? "," + a.getValue().getAgentIP() : a.getValue().getAgentIP());
					first = false;	
				}
				String msg = sb.toString();
//				try {
//					//msg = new String(JCompression.encode(sb.toString()));
//				}
//				catch(IOException e1) {
//					e1.printStackTrace();
//				}
				this.dealer.send("", MaaSPeer.MSG_TYPE, msg);
				Thread.sleep(10000);
			}	
		} 
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
