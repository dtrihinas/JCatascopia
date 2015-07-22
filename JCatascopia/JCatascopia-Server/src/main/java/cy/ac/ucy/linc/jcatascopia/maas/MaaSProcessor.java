/*******************************************************************************
 * Copyright 2014, Laboratory of Internet Computing (LInC), Department of Computer Science, University of Cyprus
 * 
 * For any information relevant to JCatascopia Monitoring System,
 * please contact Demetris Trihinas, trihinas{at}cs.ucy.ac.cy
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package cy.ac.ucy.linc.jcatascopia.maas;

import java.util.logging.Level;

import eu.celarcloud.jcatascopia.serverpack.MonitoringServer;
import eu.celarcloud.jcatascopia.serverpack.exceptions.CatascopiaException;
import eu.celarcloud.jcatascopia.serverpack.sockets.ISocket;
import eu.celarcloud.jcatascopia.serverpack.utils.JCompression;

public class MaaSProcessor implements Runnable{
	public enum Status {OK,ERROR,SYNTAX_ERROR,NOT_FOUND,WARNING,CONFLICT};	
	private String[] msg;
	private ISocket router;
	private MonitoringServer server;
	

	public MaaSProcessor(String[] msg, ISocket router, MonitoringServer server){
		this.msg = msg;
		this.router = router;
		this.server = server;
	}
	
	public void run(){
		if (this.server.inDebugMode())
			System.out.println("MaaSProcessor>> Received: " + msg[0] + " " + msg[1] + " " + msg[2]);	
		try {
			if (msg[1].equals("MaaS.JOIN"))
				this.join();
			else if (msg[1].equals("MaaS.HEARTBEAT"))
				this.heartbeat();
			else
				this.response(Status.ERROR, msg[1]+" request does not exist");
		}	
		catch (NullPointerException e){
			this.server.writeToLog(Level.SEVERE, e);
		}
		catch (Exception e){
			this.server.writeToLog(Level.SEVERE, e);
			e.printStackTrace();
		}
	}
	
	private void join(){
		this.response(Status.OK, "");
	}
	
	private void heartbeat() {
		//String[] tokenz = JCompression.decode(msg[2].getBytes()).split("\\|");
		String[] tokenz = msg[2].split("\\|");
		String sID = msg[0];
		String sIP = tokenz[0];
		int sCnt = Integer.parseInt(tokenz[1]);
		String sAgents = (sCnt != 0) ? tokenz[2] : null;
		
		ServerMeta s = this.server.getMaaSMap().get(sID);
		if (s != null){
			s.setAgentCnt(sCnt);
			s.setAgents(sAgents);
			s.setStatus(ServerMeta.ServerStatus.UP);
			s.clearAttempts();
		}
		else{
			//Server hasn't JOIN-ed yet
			s = new ServerMeta(sID, sIP, sCnt, sAgents);
			this.server.getMaaSMap().putIfAbsent(sID, s);
			this.server.writeToLog(Level.INFO, "MaaSProcessor>> Monitoring Server with IP: "+sIP+", and ID: "+sID+" to JOIN the cluster");
		}
	}
	
	private void response(Status status,String body){
		try{
			String obj = ((body.equals("")) ? status.toString() : (status+"|"+body));
			this.router.send(msg[0], msg[1], obj);
		} 
		catch (CatascopiaException e){
			this.server.writeToLog(Level.SEVERE, e);
		}
	}
}
