package eu.celarcloud.jcatascopia.agent.utilities;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;

import eu.celarcloud.jcatascopia.agent.exceptions.CatascopiaException;


/**
 * JCNetworking is a library of functions related to Networking
 * 
 * @author Demetris Trihinas
 *
 */
public class JCNetworking {
	
	public static final String getMyIP() throws CatascopiaException {
		String myIP = "";
		ArrayList<String> publicIPs = new ArrayList<String>();
		ArrayList<String> privateIPs = new ArrayList<String>();
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while(interfaces.hasMoreElements()) {
				NetworkInterface ni = (NetworkInterface) interfaces.nextElement();

				if (!ni.isUp() || ni.isVirtual()) 
		        	continue;
		        Enumeration<InetAddress> e2 = ni.getInetAddresses();

		        while (e2.hasMoreElements()) {
		        	InetAddress ip = e2.nextElement();
		        	myIP = ip.getHostAddress();
		        	if (JCNetworking.validateIPv4Address(myIP) && 
		        			!myIP.startsWith("127.") && !myIP.startsWith("224.") && !myIP.startsWith("255."))
		        		if (myIP.startsWith("192."))
			        		privateIPs.add(myIP);
		        		else 
		        			publicIPs.add(myIP);
		        }
			} 
		    if (publicIPs.size() > 0)
	        	return publicIPs.get(0);
	        else if (privateIPs.size() > 0)
	        	return privateIPs.get(0);
	        else
	        	return InetAddress.getLocalHost().getHostAddress();	
		}
		catch (SocketException e) {
			throw new CatascopiaException("JCNetworking>> SocketException thrown", CatascopiaException.ExceptionType.NETWORKING);
		}
		catch (UnknownHostException e) {
			throw new CatascopiaException("JCNetworking>> UnknownHostException thrown", CatascopiaException.ExceptionType.NETWORKING);
		}
	}
	
	public final static boolean validateIPv4Address(String ipAddress) {		
		String[] parts = ipAddress.split("\\.");
	    
	    if (parts.length != 4)
	        return false;

	    for (String s : parts) {
	        int i = Integer.parseInt(s);
	        if ((i < 0) || (i > 255))
	            return false;
	    }

	    return true;
	}
	
	public final static String getHostname() throws CatascopiaException {
		try {
			return InetAddress.getLocalHost().getHostName();
		} 
		catch (Exception e) {
			throw new CatascopiaException("JCNetworking>> could not retrieve hostname", CatascopiaException.ExceptionType.NETWORKING);
		}
	}
}
