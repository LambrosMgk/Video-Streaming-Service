package backend;

import java.net.*;
import java.util.*;

public class NetUtil
{
	
	/**
     * Try to find the most likely IPv4 of this machine.
     *  - Prefers site-local (192.168.x.x, 10.x.x.x, 172.16–31.x.x)
     *  - Falls back to first non-loopback IPv4
     */
    public static String getLocalIPv4()
    {
        try
        {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            
            while (interfaces.hasMoreElements())
            {
                NetworkInterface iface = interfaces.nextElement();
                

                if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) continue;
                
                
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) 
                {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress())
                    {
                        String ip = addr.getHostAddress();
                        // Prefer site-local (private) addresses
                        if (addr.isSiteLocalAddress())
                        {
                            return ip;
                        }
                    }
                }
            }

            
            // fallback: try again without requiring site-local
            interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements())
            {
                NetworkInterface iface = interfaces.nextElement();

                if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) continue;

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements())
                {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress())
                    {
                        return addr.getHostAddress();
                    }
                }
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return "Unknown";
    }

    
    
    public static void main(String[] args) throws Exception 
    {
        System.out.println("IPv4: " + getLocalIPv4());
    }
}
