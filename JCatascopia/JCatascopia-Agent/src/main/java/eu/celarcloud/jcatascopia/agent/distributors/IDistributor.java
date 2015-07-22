package eu.celarcloud.jcatascopia.agent.distributors;

import eu.celarcloud.jcatascopia.agent.exceptions.CatascopiaException;

public interface IDistributor {
	
	public void send(String msg) throws CatascopiaException;
	
	public void terminate();
}
