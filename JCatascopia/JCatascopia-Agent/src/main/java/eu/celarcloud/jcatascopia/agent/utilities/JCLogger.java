package eu.celarcloud.jcatascopia.agent.utilities;

import java.io.File;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import eu.celarcloud.jcatascopia.agent.exceptions.CatascopiaException;

public class JCLogger {
	private static final int FILE_SIZE = 2*1024*1024;
	
	public static final int INFO = 0;
	public static final int SEVERE = 1;
	public static final int WARNING = 2;
	public static final int FINE = 3;

	private Logger logger;
	private String pretext;
	
	public JCLogger(String path, String name, String pretext) throws CatascopiaException {
		this.logger = JCLogger.createLogger(path, name);
		this.pretext = (pretext != null) ? pretext + ": " : "";
	}
	
	public JCLogger(String path, String name) throws CatascopiaException {
		this(path, name, null);
	}
	
	/**
	 * 
	 * method that logs messages to file
	 */
	public void writeToLog(int level, Object msg) {
			this.logger.log(levelMapping(level), pretext + msg);
	}

	/**
	 * 
	 * can be accessed statically if no handling is required
	 */
	public static Logger createLogger(String path, String name) throws CatascopiaException {
		Logger LOGGER = null;
		try {
			File logfolder = new File(path + File.separator + "logs");
			
			if (!logfolder.isDirectory())
				logfolder.mkdir();			
			
			String logpath = logfolder + File.separator + name + ".log";
			LOGGER = Logger.getLogger(name); 
			FileHandler fileHandler = new FileHandler(logpath,FILE_SIZE, 5, true); 
			fileHandler.setFormatter(new SimpleFormatter());
			LOGGER.addHandler(fileHandler);
		}
		catch (Exception e) {
			throw new CatascopiaException("Instantiating logging failed", CatascopiaException.ExceptionType.LOGGING);
		}
		return LOGGER;
	}
	
	private Level levelMapping(int l) {
		switch(l){
			case 0 : return Level.INFO;
			case 1 : return Level.SEVERE;
			case 2 : return Level.WARNING;
			case 3 : return Level.FINE;
			default : return Level.INFO;
		}
	}
}
