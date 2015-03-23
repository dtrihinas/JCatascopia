package cy.ac.ucy.linc.jcatascopia.maas;

public class ServerMeta implements Comparable<ServerMeta> {
	public enum ServerStatus{JOINING,UP,DOWN,TERMINATING};
	private ServerStatus status;
	private String serverID;
	private String serverIP;
	private int agentCnt;
	private String agents;
	private byte attempts;
	
	public ServerMeta(String id, String ip, int agentCnt, String agents){
		this.serverIP = ip;
		this.agentCnt = agentCnt;
		this.agents = agents;
		this.status = ServerStatus.UP;
		this.attempts = 0;
	}
	
	public ServerMeta(String id, String ip){
		this(id, ip, 0, null);
	}
	
	public boolean isRunning() {
		return (this.status == ServerMeta.ServerStatus.UP) ? true : false;
	}

	public void setStatus(ServerStatus status) {
		this.status = status;
	}
	
	public String getServerID() {
		return serverID;
	}

	public void setServerID(String serverID) {
		this.serverID = serverID;
	}

	public String getServerIP() {
		return serverIP;
	}

	public void setServerIP(String serverIP) {
		this.serverIP = serverIP;
	}

	public int getAgentCnt() {
		return agentCnt;
	}

	public void setAgentCnt(int agentCnt) {
		this.agentCnt = agentCnt;
	}

	public String getAgents() {
		return agents;
	}

	public void setAgents(String agents) {
		this.agents = agents;
	}
	
	public void incrementAttempts(){
		this.attempts++;
	}
	
	public void clearAttempts(){
		this.attempts = 0;
	}
	
	public byte getAttempts(){
		return this.attempts;
	}

	public int compareTo(ServerMeta other) {
		return Integer.compare(this.agentCnt, other.agentCnt);
	}
	
	public String toString(){
		return "ServerMeta>> id: " + this.serverID + ", ip: " + this.serverIP + ", agentCnt: " + this.agentCnt + ", status: " + this.status.name();
	}
}
