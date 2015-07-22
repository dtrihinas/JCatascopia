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

import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Level;

import eu.celarcloud.jcatascopia.agent.exceptions.CatascopiaException;
import eu.celarcloud.jcatascopia.probes.IProbe;

public interface IMonitoringAgent {
	
	public String getAgentIP();
	
	public String getAgentID();
	
	public HashMap<String,IProbe> getProbes();
	
	public void writeToLog(Level level, Object msg);
	
	public boolean inDebugMode();
	
	public Properties getConfig();
	
	public void deployProbeAtRuntime(String probeClassContainer, String probeClass) throws CatascopiaException;

	public String getAgentName();

	public String getAgentTags();

	public void terminate();

	public void reconnect(String ip) throws CatascopiaException;
}
