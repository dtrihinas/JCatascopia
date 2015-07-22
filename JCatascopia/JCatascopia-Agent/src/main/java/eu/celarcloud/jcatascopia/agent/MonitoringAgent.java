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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import eu.celarcloud.jcatascopia.agent.aggregators.IAggregator;
import eu.celarcloud.jcatascopia.agent.connectors.IServerConnector;
import eu.celarcloud.jcatascopia.agent.distributors.IDistributor;
import eu.celarcloud.jcatascopia.agent.exceptions.CatascopiaException;
import eu.celarcloud.jcatascopia.agent.utilities.JCPackageListing;
import eu.celarcloud.jcatascopia.agent.utilities.JCProbeFactory;
import eu.celarcloud.jcatascopia.agent.utilities.JCLogger;
import eu.celarcloud.jcatascopia.agent.utilities.JCNetworking;
import eu.celarcloud.jcatascopia.probes.IProbe;
import eu.celarcloud.jcatascopia.probes.ProbeProperty;

/**
 * JCatascopia Monitoring Agent manages metric collecting probes
 * and distributes metrics to the Monitoring Server
 *  
 * @author Demetris Trihinas
 *
 */
public class MonitoringAgent implements IMonitoringAgent{
	//path to JCatascopia Agent Directory
	private String JCATASCOPIA_AGENT_HOME;
	//path to config file
	private static final String CONFIG_PATH = "resources" + File.separator + "agent.properties";
	//path to internal private file
	private static final String AGENT_PRIVATE_FILE = "resources" + File.separator + "agent_private.properties";
	//path to probe library
	private static final String PROBE_LIB_PATH = "eu.celarcloud.jcatascopia.probes.probeLibrary.";
	
	private Properties config;
	
	private String agentID;
	
	private String agentIP;
	
	private String agentName;
	
	private String tags;
	
	private boolean loggingFlag;
	
	private HashMap<String,IProbe> probes;

	private Logger logger;
	
	private String serverIP;
	
	/**
	 * collector grabs metrics from metricQueue to be parsed
	 */
	private MetricCollector collector;
	
	private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<String>(64);
		
	/**
	 * distributor publishes metrics to the MS Server
	 */
	private DistributorWorker distributorWorker;
	
	protected IAggregator aggregator;
	
	private ProbeController probeController;
	
	private boolean debugMode;
	
	private boolean useServer;
	private IServerConnector sconnector;
		
	/**
	 * 
	 * @param agentDirPath
	 * @throws CatascopiaException
	 */
	public MonitoringAgent(String agentDirPath, String lockPath) throws CatascopiaException {
		//remove daemon lock when JVM terminates if Agent is ran in daemon mode
		if (lockPath != null){
			File f = new File (lockPath);
			f.deleteOnExit();
		}
		
		//path to JCatascopia Agent Directory
		this.JCATASCOPIA_AGENT_HOME = agentDirPath;
		
		//parse config file
		this.parseConfig();
		
		//init logging
		this.initLogging(); 
		
		//get agentID or create one
		this.agentID = this.getAgentIDFromFile();
		
		//get ip address
		this.agentIP = JCNetworking.getMyIP();
		
		this.agentName = this.config.getProperty("agent.name", this.agentIP);
		
		this.tags = this.config.getProperty("agent.tags", "");		
		
		//create probe map and instantiate probes
		this.probes = new HashMap<String,IProbe>();
		
		this.instantiateProbes();

		//ping server, tell it we are here and report available metrics
		//if successful then we can start distributing metrics
		this.useServer = Boolean.parseBoolean(this.config.getProperty("agent.use_server", "true"));

		if (this.useServer) {
			long tstart = System.currentTimeMillis();
			this.connect();
			this.writeToLog(Level.INFO, "time to join cluster: " + (System.currentTimeMillis() - tstart));
		}
		
		this.initAggregator();
		
		this.initDistributor();
		
		this.collector = new MetricCollector(this.queue,this.aggregator,this);
		this.collector.activate();		
		
		this.initProbeController();
		
		this.debugMode = Boolean.parseBoolean(this.config.getProperty("agent.debug", "false"));
		
		//register shutdown hook
//		Runtime.getRuntime().addShutdownHook(new Thread() {
//			public void run() {
//				terminate();
//			}
//		});		
	}
	
	public String getAgentID() {
		return this.agentID;
	}
	
	public String getAgentIP() {
		return this.agentIP;
	}
	
	public String getAgentName() {
		return this.agentName;
	}
	
	public String getAgentTags() {
		return this.tags;
	}
	
	public HashMap<String,IProbe> getProbes() {
		return this.probes;
	}
	
	public Logger getLogger() {
		return this.logger;
	}

	public boolean inDebugMode() {
		return this.debugMode;
	}
	
	public Properties getConfig() {
		return this.config;
	}
	
	//parse the configuration file
	private void parseConfig() throws CatascopiaException {
		this.config = new Properties();
		//load config properties file
		try {				
			FileInputStream fis = new FileInputStream(JCATASCOPIA_AGENT_HOME + File.separator + CONFIG_PATH);
			config.load(fis);
			if (fis != null)
	    		fis.close();
		} 
		catch (FileNotFoundException e) {
			throw new CatascopiaException("config file not found", CatascopiaException.ExceptionType.FILE_ERROR);
		} 
		catch (IOException e) {
			throw new CatascopiaException("config file parsing error", CatascopiaException.ExceptionType.FILE_ERROR);
		}
	}
	
	//very important for vertical scaling support with VM restart
	private String getAgentIDFromFile() throws CatascopiaException {
    	Properties prop = new Properties();
    	String id;
    	try {
			String agentfile = JCATASCOPIA_AGENT_HOME + File.separator + AGENT_PRIVATE_FILE;
			if((new File(agentfile).isFile())) {
				//load agent_private properties file
				FileInputStream fis = new FileInputStream(agentfile);
				prop.load(fis);
				if (fis != null)
		    		fis.close();
				id = prop.getProperty("agentID",UUID.randomUUID().toString().replace("-", ""));
			}
			else {
				//first time agent started. Store assigned id to file
				id = UUID.randomUUID().toString().replace("-", "");
				prop.setProperty("agentID", id);
				prop.store(new FileOutputStream(agentfile), null);
			}
    	} 
		catch (FileNotFoundException e) {
			throw new CatascopiaException("agent_private file not found", CatascopiaException.ExceptionType.FILE_ERROR);
		} 
		catch (IOException e) {
			throw new CatascopiaException("agent_file parsing error", CatascopiaException.ExceptionType.FILE_ERROR);
		}	
    	return id;
	}
	
	//initialize logging
	private void initLogging() {
		this.loggingFlag = Boolean.parseBoolean(this.config.getProperty("agent.logging", "false"));
		if (this.loggingFlag)
			try{
				this.logger = JCLogger.createLogger(this.JCATASCOPIA_AGENT_HOME, "MonitoringAgent");
				this.writeToLog(Level.INFO, "created and initialized");
				this.writeToLog(Level.INFO, "logging turned ON");
			}
			catch (Exception e) {
				//Unable to log events
				this.loggingFlag = false;
			}
		else this.loggingFlag = false; //logging turned off by user
	}
	
	/**
	 * method that logs messages to the MS Agent log
	 */
	public void writeToLog(Level level, Object msg) {
		if(this.loggingFlag)
			this.logger.log(level, "MonitoringAgent" + ": " + msg);
	}
	
	/**
	 * method that LOADS and ACTIVATES Probes defined in config file
	 * 
	 */
	private void instantiateProbes() {
		String probe_str = this.config.getProperty("probes.include", "all");
		String probe_exclude_str = this.config.getProperty("probes.exclude", "");
		String probe_external = this.config.getProperty("probes.external", "");
				
		try {
			ArrayList<String> availableProbeList = this.listAvailableProbeClasses();
			//user wants to instantiate all available probes with default params
			//TODO - allow user to parameterize probes when selecting to add all probes
			if (probe_str.equals("all")) {
				for(String s : availableProbeList) 
					this.probes.put(s, null);
				
				//user wants to instantiate all available probes except the ones specified for exclusion
				if (!probe_exclude_str.equals("")) {
					String[] probe_list = probe_exclude_str.split(";");
					for(String s:probe_list)
						this.probes.remove(s.split(",")[0]);
				}
				
				//instantiate
				for(Entry<String,IProbe> p:this.probes.entrySet()){
					try{
						IProbe tempProbe = JCProbeFactory.newInstance(PROBE_LIB_PATH, p.getKey());
						tempProbe.attachQueue(this.queue);
						tempProbe.attachLogger(this.logger);
						p.setValue(tempProbe);
					}
					catch (CatascopiaException e) {
						this.writeToLog(Level.SEVERE, e);
						continue;
					}	
				}
			}
			//user wants to instantiate specific probes (and may set custom params)
			else{
				String[] probe_list = probe_str.split(";");
				for(String s:probe_list) {
					try{
						String[] params = s.split(",");
						IProbe tempProbe = JCProbeFactory.newInstance(PROBE_LIB_PATH,params[0]);
						tempProbe.attachQueue(this.queue);
						tempProbe.attachLogger(this.logger);
						if(params.length > 1) //user wants to define custom collecting period
							tempProbe.setCollectPeriod(Integer.parseInt(params[1]));
						this.probes.put(params[0], tempProbe);
					}
					catch (CatascopiaException e) {
						this.writeToLog(Level.SEVERE, e);
						continue;
					}	
				}
				
			}
						
			//activate Agent probes
			this.activateAllProbes();
			
			//deploy external probes located in a custom user-defined path
			if (!probe_external.equals("")) {
				String[] probe_list = probe_external.split(";");
				for(String s:probe_list){
					try{
						String[] params = s.split(",");
						String pclass = params[0];
						String ppath = params[1];
						this.deployProbeAtRuntime(ppath, pclass);
					}
					catch (ArrayIndexOutOfBoundsException e){
						this.writeToLog(Level.SEVERE, "External Probe deployment error. Either the probe class name of classpath are not correctly provided");
					}
				}
			}				
			
			//log probe list added to Agent
			String l = " ";
			for(Entry<String,IProbe> entry:this.probes.entrySet())
				l += entry.getKey() + ",";
			this.writeToLog(Level.INFO,"Probes Activated: "+l.substring(0, l.length()-1));
		}
		catch (CatascopiaException e){
			this.writeToLog(Level.SEVERE, e);
		}
	}
	
	/**
	 * 
	 * @return list of available probes in Probe Library
	 * @throws CatascopiaException
	 */
	public ArrayList<String> listAvailableProbeClasses() throws CatascopiaException{
		ArrayList<String> list =  JCPackageListing.listClassesInPackage(PROBE_LIB_PATH);
		return list;
	}
	
	public void activateAllProbes() {
		for(String name : this.probes.keySet())
			try{
				this.activateProbe(name);
			} 
			catch (CatascopiaException e) {
				continue;
			}
	}
	
	public void activateProbe(String probeName) throws CatascopiaException{
		if (this.probes.containsKey(probeName)) {
			this.probes.get(probeName).activate();
		}
		else
			throw new CatascopiaException("Activate Probe Failed, probe name given does not exist: "+probeName, 
										   CatascopiaException.ExceptionType.KEY);
	}
	
	
	public void deactivateProbe(String probeName) throws CatascopiaException {
		if (this.probes.containsKey(probeName)) {
			this.probes.get(probeName).deactivate();
		}
		else
			throw new CatascopiaException("Deactivate Probe Failed, probe name given does not exist: "+probeName, 
										   CatascopiaException.ExceptionType.KEY);
	}
		
	@SuppressWarnings("unchecked")
	public void deployProbeAtRuntime(String probeClassContainer, String probeClass) throws CatascopiaException {		
		try {
			URL myurl = null;
            if (System.getProperty("os.name").toLowerCase().contains("win"))
                //e.g. probes_external=ExampleProbe,C:/Users/dtrihinas/Desktop/ExampleProbe.jar
                myurl = new URL("file:///"+probeClassContainer);
            else
                myurl = new URL("file://"+probeClassContainer);
            
			URLClassLoader classloader = URLClassLoader.newInstance(new URL[]{myurl});
			Class<IProbe> myclass = (Class<IProbe>) classloader.loadClass(probeClass);
			IProbe p = myclass.newInstance();
			p.attachQueue(this.queue);
			p.attachLogger(this.logger);
			p.activate();
			this.probes.put(p.getProbeName(), p);
			this.writeToLog(Level.INFO, "new Probe activated: " + p.getProbeName() + ", container path: " + probeClassContainer);
		} 
		catch (MalformedURLException e) {
			throw new CatascopiaException(e.getMessage(),CatascopiaException.ExceptionType.PROBE_EXISTANCE);
		} 
		catch (ClassNotFoundException e) {
			throw new CatascopiaException(e.getMessage(),CatascopiaException.ExceptionType.PROBE_EXISTANCE);
		} 
		catch (InstantiationException e) {
			throw new CatascopiaException(e.getMessage(),CatascopiaException.ExceptionType.PROBE_EXISTANCE);
		} 
		catch (IllegalAccessException e) {
			throw new CatascopiaException(e.getMessage(),CatascopiaException.ExceptionType.PROBE_EXISTANCE);
		}
	}
	
	@SuppressWarnings("unchecked")
	public void connect() throws CatascopiaException {    
		String seed = this.config.getProperty("server.seed");
    	String endpoint = this.config.getProperty("server.endpoint", "localhost");
    	if (seed == null) seed = endpoint;
    	
    	String port = this.config.getProperty("server.controller.port", "4245");
		String i = this.config.getProperty("server.controller.interface", "DefaultSocketConnector");
				
    	Class<?>[] myArgs = new Class[2];
		myArgs[0] = String.class;
        myArgs[1] = String.class;

		if (this.useServer) {
			try {
				//instantiate connector interface
		        String path = "eu.celarcloud.jcatascopia.agent.connectors." + i;
		        Class<IServerConnector> _tempClass = (Class<IServerConnector>) Class.forName(path);
		        Constructor<IServerConnector> _tempConst = _tempClass.getDeclaredConstructor(myArgs);
				this.sconnector = _tempConst.newInstance(seed, port);
				this.writeToLog(Level.INFO, i + ">> successully instantiated and initialized");	
		        
		    	String ping = "{}";
		    	
		    	endpoint = this.sconnector.joinCluster(seed, this.agentID, ping);
				if (endpoint != null) {
					this.writeToLog(Level.INFO, "Successfuly JOINED JCatascopia Cluster via seed: " + seed);
					if (this.sconnector.initAgent(endpoint, this.agentID, compileMetricList(null)))
						this.writeToLog(Level.INFO, "successfuly CONNECTED to Monitoring Server at: " + endpoint);
					else {
						this.writeToLog(Level.SEVERE, "FAILED to CONNECT to Monitoring Server at: " + endpoint);
						throw new CatascopiaException("FAILED to CONNECT to Monitoring Server", CatascopiaException.ExceptionType.CONNECTION);
					}
					this.serverIP = endpoint;
				}
				else {	
					this.writeToLog(Level.SEVERE, "FAILED to JOIN cluster via seed node: " + seed);
					throw new CatascopiaException("FAILED to JOIN cluster via seed node: " + seed, CatascopiaException.ExceptionType.CONNECTION);
				}
			}
			catch (ClassNotFoundException e) {
				this.writeToLog(Level.SEVERE, "could not CONNECT to Monitoring Server");
				throw new CatascopiaException("could not CONNECT to Monitoring Server", CatascopiaException.ExceptionType.CONNECTION);
			}
			catch(Exception e) {
				this.writeToLog(Level.SEVERE, e.getMessage());
				throw new CatascopiaException("could not CONNECT to Monitoring Server", CatascopiaException.ExceptionType.CONNECTION);
			}
		}	
	}
	
	public void reconnect(String ip) throws CatascopiaException {
		if (this.sconnector.initAgent(ip, agentID, this.compileMetricList(null)))
			this.writeToLog(Level.SEVERE, "successfuly CONNECTED to Monitoring Server at: " + ip);
		else{	
			this.writeToLog(Level.SEVERE, "FAILED to RECONNECT to Monitoring Server at: " + ip);
			throw new CatascopiaException("could not CONNECT to Monitoring Server", CatascopiaException.ExceptionType.CONNECTION);
		}
		this.serverIP = ip;
		this.initDistributor();
	}
	
	private String compileMetricList(String p) {
		StringBuilder sb = new StringBuilder();
		sb.append("{\"agentID\":\"" + agentID + "\"");
		sb.append(",\"agentIP\":\"" + agentIP + "\"");
		sb.append(",\"probes\":[");
		
		for (Entry<String, IProbe> entry : probes.entrySet()) {
			String probeName = entry.getKey();
			if (p == null || p.equalsIgnoreCase(probeName)) {
				IProbe probe = entry.getValue();
				if(probe.getProbeStatus() == IProbe.ProbeStatus.ACTIVE) {
					sb.append("{\"probeName\":\""+probeName+"\",\"metrics\":[");
					ArrayList<ProbeProperty> propertylist = probe.getProbePropertiesAsList();
					for(int i=0; i<propertylist.size();i++)
						sb.append("{\"name\":\""+propertylist.get(i).getPropertyName()+"\""+
								   ",\"type\":\""+propertylist.get(i).getPropertyType()+"\""+
								   ",\"units\":\""+propertylist.get(i).getPropertyUnits()+"\"},");
					sb.replace(sb.length()-1, sb.length(), "");
					sb.append("]},");
				}
			}
		}
		sb.replace(sb.length()-1, sb.length(), "");
		sb.append("]");
		sb.append(",\"name\":\"" + agentName + "\"");
		sb.append(",\"tags\":\""+tags+"\"");
		sb.append("}");
    	
		return sb.toString();
	}
	
	/**
	 * instantiate and initialize Aggregator interface 
	 * @throws CatascopiaException 
	 */
	@SuppressWarnings("unchecked")
	private void initAggregator() throws CatascopiaException{
		String inter = this.config.getProperty("aggregator.interface", "StringAggregator");
		
		Class<?>[] myArgs = new Class[3];
		myArgs[0] = String.class;
        myArgs[1] = String.class;
        myArgs[2] = IMonitoringAgent.class;
        String path = "eu.celarcloud.jcatascopia.agent.aggregators."+inter;
        try{
	        Class<IAggregator> _tempClass = (Class<IAggregator>) Class.forName(path);
	        Constructor<IAggregator> _tempConst = _tempClass.getDeclaredConstructor(myArgs);
			this.aggregator = _tempConst.newInstance(this.agentID, this.agentIP, this);
			this.writeToLog(Level.INFO, inter+">> Successully instantiated and initialized");	
        }
        catch (ClassNotFoundException e){
			this.writeToLog(Level.SEVERE, e);
			throw new CatascopiaException(e.getMessage(),CatascopiaException.ExceptionType.AGGREGATOR);
		}
		catch(Exception e){
			this.writeToLog(Level.SEVERE, e);
			throw new CatascopiaException(e.getMessage(),CatascopiaException.ExceptionType.AGGREGATOR);
		}
	}
	
	/**
	 * initialize the Distributer
	 * @throws CatascopiaException 
	 */
	@SuppressWarnings("unchecked")
	public void initDistributor() throws CatascopiaException{
		//Distributor settings
    	String inter = this.config.getProperty("distributor.interface", "TCPDistributor");
		String port = this.config.getProperty("distributor.port", "4242");
    	String ip = (this.useServer) ? this.serverIP : "localhost";

    	//Aggregator settings
    	long agg_interval = Long.parseLong(this.config.getProperty("aggregator.interval")) * 1000;
    	int agg_buf = Integer.parseInt(this.config.getProperty("aggregator.buffer_size"));
    			
		Class<?>[] myArgs = new Class[3];
		myArgs[0] = String.class;
        myArgs[1] = String.class;
        myArgs[2] = IMonitoringAgent.class;

        String path = "eu.celarcloud.jcatascopia.agent.distributors." + inter;
        try{
	        Class<IDistributor> _tempClass = (Class<IDistributor>) Class.forName(path);
	        Constructor<IDistributor> _tempConst = _tempClass.getDeclaredConstructor(myArgs);
	        IDistributor distributor = _tempConst.newInstance(ip, port, null);
			
	        if (this.distributorWorker != null)
	        	this.distributorWorker.terminate(); //in case of a RECONNECT to a different Monitoring Server (MaaS mode)
	        this.distributorWorker = new DistributorWorker(distributor, this.aggregator, agg_interval, agg_buf, this);
			distributorWorker.activate();
			
			this.writeToLog(Level.INFO, inter+">> Successully instantiated and initialized with params (" + ip + "," + port + ")");	
        }
        catch (ClassNotFoundException e){
			this.writeToLog(Level.SEVERE, e);
			throw new CatascopiaException(e.getMessage(),CatascopiaException.ExceptionType.AGGREGATOR);
		}
		catch(Exception e){
			this.writeToLog(Level.SEVERE, e);
			throw new CatascopiaException(e.getMessage(),CatascopiaException.ExceptionType.AGGREGATOR);
		} 
	}
	
	private void initProbeController(){
		if (this.config.getProperty("listener.enable", "false").equals("true")) {
			String ip = this.config.getProperty("listener.ip","*");
			String port = this.config.getProperty("listener.port","4243");
			
			this.probeController = new ProbeController(ip, port, this.queue, this);
			this.probeController.activate();
			this.writeToLog(Level.INFO, "Listener enabled with parameters: " + ip + ", " + port);
		}
	}
	
	
	/**
	 * add probe to agent
	 * @param p
	 */
	public void addProbe(IProbe p) {
		this.probes.put(p.getProbeName(), p);
		p.attachQueue(this.queue);
		p.attachLogger(this.logger);
	}
	
	/**
	 * remove probe from agent
	 * @param probeID
	 * @throws CatascopiaException
	 */
	public void removeProbeByName(String probeName) throws CatascopiaException{
		if (this.probes.containsKey(probeName)){
			IProbe probe = this.probes.get(probeName);
			probe.removeQueue();
			probe.terminate();
			this.probes.remove(probeName);
		}
		else
			throw new CatascopiaException("Remove Probe Failed, probe name given does not exist: "+probeName, 
										   CatascopiaException.ExceptionType.KEY);
	}
	
	/**
	 * 
	 * @param probeName
	 * @return probe specified by probeName
	 * @throws CatascopiaException
	 */
	public IProbe getProbe(String probeName) throws CatascopiaException{
		if (this.probes.containsKey(probeName))
			return this.probes.get(probeName);
		else
			throw new CatascopiaException("Get Probe Failed, probe name given does not exist: "+probeName, 
										   CatascopiaException.ExceptionType.KEY);
	}
	
	/**
	 * terminate agent. all probes are first terminated.
	 */
	public void terminate() {
		//terminate probes
		for (Entry<String,IProbe> entry :this.probes.entrySet())
			entry.getValue().terminate();
		//terminate collector
		this.collector.terminate();
		this.distributorWorker.terminate();
	}
		
	/**
	 * @param args
	 * @throws CatascopiaException 
	 */
	public static void main(String[] args) throws CatascopiaException{
		try{	
			if (args.length > 0)
				new MonitoringAgent(args[0], args[1]);
			else
				new MonitoringAgent(".", null);
		}catch(Exception e){
			e.printStackTrace();
			System.exit(0);
		}
	}
}
