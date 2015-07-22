/*******************************************************************************
 * Copyright 2014-2015, 
 * Laboratory of Internet Computing (LInC), Department of Computer Science, University of Cyprus
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
package eu.celarcloud.jcatascopia.agent;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.celarcloud.jcatascopia.agent.exceptions.CatascopiaException;
import eu.celarcloud.jcatascopia.agent.sockets.Router;

public class ProbeController extends Deamon{
	
	private static final long CHECK_PERIOD = 5000;

	private Router router;
	private boolean firstFlag;
	private LinkedBlockingQueue<String> metricQueue;
	private IMonitoringAgent agent;
	
	private static final Pattern containerPattern = Pattern.compile("container\":\"[^\\\"]+");
	private static final Pattern classPattern = Pattern.compile("class\":\"[^\\\"]+");

	
	public ProbeController(String ip, String port, LinkedBlockingQueue<String> metricQueue, IMonitoringAgent agent){
		super("ProbeController", CHECK_PERIOD, agent);

		this.router = new Router(ip ,port);
		
		this.firstFlag = true;	
		this.metricQueue = metricQueue;
		this.agent =agent;
	}

	
	public void doWork(){
		try{
			String[] msg;
			msg = router.receiveNonBlocking(); //router does not block
			if (msg != null){ //process incoming request
				if (msg[1].equals("XPROBE.METRIC")) //request is a metric from XProbe
					processXProbe(msg);
				else if (msg[1].equals("AGENT.RECONNECT"))
					processReconnect(msg);
				else if (msg[1].equals("AGENT.ADD_PROBE"))
					deployProbe(msg);
			} 
		} 
		catch(Exception e) {
			this.agent.writeToLog(Level.SEVERE, e);
			e.printStackTrace();
		}	      
	}	
	
	private void processXProbe(String[] msg) throws InterruptedException {
		this.router.send(msg[0],msg[1],"{\"status\":\"OK\"}");
		
		//offer metric to queue
		this.metricQueue.offer(msg[2], 500, TimeUnit.MILLISECONDS); 
		if(this.agent.inDebugMode())
			System.out.println("ProbeController>> Received metric from XProbe and enqueued it to metric queue: " + msg[2]);
	}
	
	private void processReconnect(String[] msg) throws CatascopiaException {
		this.router.send(msg[0],msg[1],"{\"status\":\"OK\"}");
		agent.writeToLog(Level.INFO,"ProbeController>> Received request from Monitoring Server to RECONNECT");
		if(this.agent.inDebugMode())
			System.out.println("ProbeController>> Received request from Monitoring Server to RECONNECT");
		
		//reconnect
		String serverIP = null;
		if (msg[2] != null && msg[2].length() > 0) 
			serverIP = msg[2].trim(); //MaaS mode
		else	
			serverIP = agent.getConfig().getProperty("server.endpoint", "127.0.0.1");
		String port = agent.getConfig().getProperty("server.control.port", "4245");
		
		agent.reconnect(serverIP);
	}
	
	private void deployProbe(String[] msg) {
		agent.writeToLog(Level.INFO, "Probe Controller>> Received request to deploy new Probe");
		if(this.agent.inDebugMode())
			System.out.println("Probe Controller>> Received request to deploy new Probe");
		
		//deploy new Probe
		String probeClassContainer = "";
		String probeClass = "";
		Matcher m = containerPattern.matcher(msg[2]);
		if (m.find())
			probeClassContainer = m.group().split("\":\"")[1];
		
		m = classPattern.matcher(msg[2]);
		if (m.find())
			probeClass = m.group().split("\":\"")[1];
	
		try {
			agent.deployProbeAtRuntime(probeClassContainer, probeClass);
			this.router.send(msg[0],msg[1],"{\"status\":\"OK\"}");
		} 
		catch (CatascopiaException e){
			this.router.send(msg[0],msg[1],"{\"status\":\"FAILED\"}");
			this.agent.writeToLog(Level.SEVERE, "ProbeController>> Failed to deploy new Monitoring Probe, " + e);
		}
		catch (Exception e){
			this.router.send(msg[0],msg[1],"{\"status\":\"FAILED\"}");
			this.agent.writeToLog(Level.SEVERE, "ProbeController>> Failed to deploy new Monitoring Probe, " + e);
		}
	}
}