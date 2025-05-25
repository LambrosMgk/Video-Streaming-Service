package backend;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class MainServer 
{
	private static final Logger log = LogManager.getLogger(MainServer.class);
	private static final String serverName = "localhost";
	private static final int MainServerPort = 1111;
	
	private static int totalServers;
	private static int currentServer;
	private static ArrayList<String> serverPorts;
	
	
	public static void main(String[] args)
	{
		ServerSocket balancer;
		try 
		{
			// Note: could implement a health check in case a server fails in the future.
			
			totalServers = 0;
			currentServer = -1;		// No servers available.
			serverPorts = new ArrayList<String>();
			balancer = new ServerSocket(MainServerPort);
			log.info("Load balancing server started at port " + MainServerPort);
			while(true)
			{
				// Get the next server using the Round Robin technique.
				currentServer = totalServers > 0 ? ((currentServer + 1) % totalServers): -1;
			    Socket clientSocket = balancer.accept();
			    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
			    log.debug("Accepted connection. Waiting for \"Hello\". Currect Server is " + currentServer);
			    
			    // If a server connected it will send a message with format "SERVER:XXXX" where XXXX is the port number its listening to.
		        String message = in.readLine();
		        if(message != null && message.contains("SERVER") == true)
		        {
		        	// Add the new server to the list.
			        String[] parts = message.split(":");
			        String serverPort = parts[1];
			        serverPorts.add(serverPort);
			        totalServers++;
			        log.info("Added server with port: " + serverPort);
			        log.debug("Total servers are now " + totalServers);
			        
			        out.println("OKAY:" + (totalServers-1));
			        clientSocket.close(); // Done
			        continue;
		        }
		        
			    if(currentServer != -1)
			    {		
			    	log.info("Client connected, redirecting to server port: " + serverPorts.get(currentServer));
			    	out.println("STREAM_SERVER:" + serverPorts.get(currentServer));
			    }
			    else
			    {
			    	log.warn("Server Error: No available server. Closing connection...");
			    	out.println("Server Error: No available server. Closing connection...");
			    }
			    clientSocket.close(); // Done
			}
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	
	/*
	 * 
	 */
	public synchronized static boolean releasePort(String port) 
    {
		totalServers--;
        return serverPorts.remove(port);
    }
	
}
