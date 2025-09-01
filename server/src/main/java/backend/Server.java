package backend;

import java.io.*;
import java.net.*;
import java.util.*;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.builder.FFmpegBuilder;

import shared.FFmpegLoader;



public class Server
{
	private static final int BALANCER_PORT = 1111;
    private static String BALANCER_IP = "localhost";	// default value
    private static String SERVER_IP = "localhost";
    private static int SERVER_PORT;
    
    private static final int MAX_CLIENTS = 10;		// Set the maximum clients served at the same time to define the number ports.
    private static int[] availablePorts = {5000, 5002, 5004, 5006, 5008, 5010, 5012, 5014, 5016, 5018};	// The available ports will be max_clinets * 2 because RTP uses 2 ports.
    private static int Total_Clients = 15;		// Total requests that will be processed before shutting down.
    private static final Set<Integer> inUse = new HashSet<>();
    private static int balancer_index;
    
    private static ArrayList<VideoInfo> availableVideos = new ArrayList<VideoInfo>();
    private static final Logger log = LogManager.getLogger(Server.class);	// Available modes : debug.fatal.error.warn.info

    
    private static String VIDEO_FOLDER = "../Videos";
    private static final String[] EXTENSIONS = {".avi", ".mp4", ".mkv"};
    private static final String[] QUALITIES = {"240p", "360p", "480p", "720p", "1080p"};
    //private static final Double[] RECOMMENDED_Kbps = {400.0, 750.0, 1000.0, 2500.0, 4500.0};
    
    private static java.util.function.IntConsumer progressCallback = null;
    private static AtomicBoolean cancelFlag = new AtomicBoolean(false);
    
    
    /**
     * This function starts the server with arguments given from the GUI (instead of CLI that i used to do)
     */
    public static void Start(String serverIP, int serverPort, String videoFolderPath, boolean doConversion)
    {
    	if(serverIP.compareTo("") != 0)
    		SERVER_IP = serverIP;
    	
        SERVER_PORT = serverPort;
        VIDEO_FOLDER = videoFolderPath;
        boolean CONVERT_AT_START = doConversion;

        
        
        log.info("Starting server at " + SERVER_IP + ":" + SERVER_PORT);
        log.info("Server args: video folder=%s, convertAtStart=%s%n", VIDEO_FOLDER, CONVERT_AT_START);

        
        // Check the Videos folder and (if toggled) convert any videos that you can.
        log.info("Checking %s folder", VIDEO_FOLDER);
        scanAndPrepareVideos(CONVERT_AT_START);
        log.info("Going to connect to the load balancing server...");
        
        
        // Connect to the load balancer first.
        Socket socket;
		try 
		{
			socket = new Socket(BALANCER_IP, BALANCER_PORT);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
	        
	        
	        out.println("SERVER:" + SERVER_IP + ":" + SERVER_PORT);
	        String message = in.readLine();			// Get load balancer response.
	        if(message != null && message.contains("OKAY:") == true)
	        {
	        	balancer_index = Integer.parseInt(message.replace("OKAY:", ""));
	        	log.info("Connected to load balancer with port " + SERVER_PORT + " as index " + balancer_index);
	        }
	        else
	        {
	        	log.error("Load balancing server did not respond. Exiting...");
	        	return;
	        }
	        socket.close();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
			return;
		}
		
        
		// Add an offset to your ports based on the index you got from the balancer.
		System.out.print("Server " + SERVER_PORT + " has available the ports: [ "); // Usage of system.out for a cleaner output.
		for(int i = 0; i < availablePorts.length; i++)
		{
			availablePorts[i] = availablePorts[i] + balancer_index*20;
			System.out.print(availablePorts[i] + " ");		// Usage of system.out for a cleaner output.
		}
		System.out.println("]");
		
		
    	// Create a socket on the SERVER_PORT and wait for a connection (Client).
    	ServerSocket serverSocket;
		try 
		{
			int totalClients = 0;
			serverSocket = new ServerSocket(SERVER_PORT);
			log.info("Server started. Waiting for a client on port " + SERVER_PORT);
			
			
			while(totalClients < Total_Clients)
			{
				Socket clientSocket = serverSocket.accept();
				log.info("Client connected!");
				totalClients++;
				new ServerThread(clientSocket, acquirePort(), availableVideos, VIDEO_FOLDER).start();			
			}
	        
	        log.info("Server has finished and is now closing...");
	        serverSocket.close();
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		MainServer.releasePort(SERVER_PORT + "");		// Signal the load balancer that you are out.
    }
    
    

    
    /**
     * This function scans the "Videos" folder and checks what videos are available. It then tries to create any missing videos.
     * Video name format: name-quality.extension e.g. FoodWars_ep1-1080p.mp4
     */
    private static void scanAndPrepareVideos(boolean convertBool)
    {
        File folder = new File(VIDEO_FOLDER);
        File[] files = folder.listFiles();
        boolean exists = false;
        
        
        if (files == null) return;

        // Search the video folder and keep track of "video titles".
        for (File file : files) 
        {
            String name = file.getName();
            if(name.equals(".gitkeep"))		// Skip the file for github.
            	continue;
            String baseName = name.substring(0, name.lastIndexOf('-'));
            String quality = name.substring(name.lastIndexOf('-') + 1, name.lastIndexOf('.'));	// '-' is included so skip that.
            String extension = name.substring(name.lastIndexOf('.'));
            
            exists = false;
            for(VideoInfo vid : availableVideos)
            {
            	// In case of a duplicate file doesn't check if that file is already in the list. We can't have duplicate files (OS won't allow it) so we're ok!
            	if(vid.getName().compareTo(baseName) == 0)
            	{
            		log.info("Adding new variant video for name " + vid.getName() + ", " + extension + ", " + quality + " ...");
            		
            		// Check if this quality is better...
            		int bestQualityIndex = vid.getBestQualityIndex();	// Get the index for the quality list.
            		String bestQuality = vid.getQuality().get(bestQualityIndex);
            		int bestQualityint = Integer.parseInt(bestQuality.substring(0, bestQuality.lastIndexOf("p")));	// Convert to int to compare qualities
            		int newQualityint = Integer.parseInt(quality.substring(0, quality.lastIndexOf("p")));			// Convert to int to compare qualities
            		
            		log.debug("Comparing old best quality (" + bestQuality + ") with new quality (" + quality + ").");
            		if(bestQualityint < newQualityint)
            		{
            			log.debug(" (New best quality index: " + vid.getQuality().size() + ")");
            			vid.setBestQualityIndex(vid.getQuality().size());
            		}
            		
            		vid.addExtension(extension);// Add new extension so it can be used with the quality list (using the same index to get the pair quality-extension)	
            		vid.addQuality(quality);	// Add new quality at the end of the list.
            		exists = true;
            		break;
            	}
            }
            
            if(exists == false)		// If the video name is not on the list.
            {
            	log.info("Adding new video (" + name + "), <" + quality + "," + extension + "> to the list...");
            	availableVideos.add(new VideoInfo(baseName, extension, quality, 0));
            	log.debug("Done");
            }
        }
        
        
        // Check what videos are missing and try to create them with FFMPEG.
        if(convertBool)
        {
        	// availableVideos also stores the variants so it has all the UNIQUE videos, allowing easy calculation for the total missing files
        	int total = (availableVideos.size() * EXTENSIONS.length * QUALITIES.length) - files.length;
        	int done = 0;
        	
            for(VideoInfo vid : availableVideos)
            {
            	if (isCancelled())
            	{
                    System.out.println("Conversion cancelled by user.");
                    break;
                }
            	
            	ArrayList<String> vidExtensions = vid.getExtension();
            	ArrayList<String> vidQualities = vid.getQuality();
            	boolean pairExists = false;
            	
        		for (String extension : EXTENSIONS)
                {
                    for (String quality : QUALITIES)
                    {
                    	pairExists = false;
                    	for(int i = 0; i < vidExtensions.size(); i++)		// The video extensions and qualities list will have the same size.
                    	{
                    		// Does the pair <quality,extension> exist?
                        	if(vidExtensions.get(i).equals(extension) && vidQualities.get(i).equals(quality))
                    		{
                        		pairExists = true;
                    			break;
                    		}
                    	}
                    	
                    	if(!pairExists)
                    	{
                    		String vidQuality = vid.getQuality().get(vid.getBestQualityIndex());
                    		String vidExtension = vid.getExtension().get(vid.getBestQualityIndex());	// Get the extension thats paired with the quality
                    		String baseFileName = vid.getName() + "-" + vidQuality + vidExtension;
                    		String targetFileName = vid.getName() + "-" + quality + extension;
                    		
                    		
                    		log.info("Missing " + vid.getName() + ": " + quality + ", " + extension + ", trying to create the file...");
                    		// Try to create the missing extension and quality...
                    		
                    		convertVideo(folder.getAbsolutePath() + "\\" + baseFileName, vidQuality, folder.getAbsolutePath() + "\\" + targetFileName, quality, extension);
                    		
                    		log.debug("Done.");
                    	}
                    }
                }
        		done++;
        		reportProgress((done * 100) / total);
            }
        }
        // End of convert process
        
    }
    
    
    /*
     * This function uses the FFMpeg Wrapper to convert the given video to the requested size/format.
     */
    private static void convertVideo(String inputPath, String inputQuality, String outputPath, String outputQuality, String outputExtension) 
    {
    	int inputQualityInt = Integer.parseInt(inputQuality.replace("p", ""));
        int outputQualityInt = Integer.parseInt(outputQuality.replace("p", ""));

        if (inputQualityInt < outputQualityInt) 
        {
            log.error("Can't convert to a higher definition (from " + inputQuality + " to " + outputQuality + ")...");
            return;
        }

        // Map quality to resolution
        String resolution;
        switch (outputQuality) 
        {
            case "240p": resolution = "426x240"; break;
            case "360p": resolution = "640x360"; break;
            case "480p": resolution = "854x480"; break;
            case "720p": resolution = "1280x720"; break;
            case "1080p": resolution = "1920x1080"; break;
            default:
                log.error("Unknown resolution, can't convert the video.");
                return;
        }
        
        
        try 
        {
            // Initialize FFMPEG
        	File ffmpeg_file = FFmpegLoader.getFFtoolExecutable("ffmpeg");	// Get a file reference using my loader
            FFmpeg ffmpeg = new FFmpeg(ffmpeg_file.getAbsolutePath());   // I've put the files for ffmpeg in this folder instead of changing my PATH variable.
            
            
            FFmpegBuilder builder = null;

            // .mp4 and .mkv can use the same encoder but .avi is older and needs an older decoder.
            if (outputExtension.equals(".mp4") || outputExtension.equals(".mkv")) 
            {
                builder = new FFmpegBuilder()
                		.setInput(inputPath)
                        .overrideOutputFiles(true) 							// Overwrite existing file
                        .addOutput(outputPath)					// Output file
                        .setVideoCodec("libx264")				// Use the h264 encoder
                        .setAudioCodec("aac")					// Encodes the audio using AAC
                        .setVideoFilter("scale=" + resolution)	// Resize video resolution
                        .setPreset("fast")						// Adjusts encoding speed vs file size (options: ultrafast → veryslow)
                        //.setFormat(outputExtension.replace(".", ""))
                        .done();
            } 
            else if (outputExtension.equals(".avi")) 
            {
                builder = new FFmpegBuilder()
                		.setInput(inputPath)
                        .overrideOutputFiles(true) 							// Overwrite existing file
                        .addOutput(outputPath)					// Output file
                        .setVideoCodec("mpeg4")					// mpeg4 is an older decoder used for .avi files
                        .setAudioCodec("libmp3lame")			// Encodes the audio using mp3
                        .setVideoFilter("scale=" + resolution)	// Resize video resolution
                        .addExtraArgs("-q:v", "5")				// controls video quality (lower is better)
                        .setFormat("avi")
                        .done();
            }
            // Execute the conversion
            ffmpeg.run(builder);
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
    }

    
    /*
     * Returns a free port (even numbered). (Odd numbered ports might be used by RTPC from an even port).
     */
    private static int acquirePort() 
    {
        for (int port : availablePorts) 
        {
            if (!inUse.contains(port) && port%2 == 0)	// Return only even numbered
            {
                inUse.add(port);
                return port;
            }
        }
        throw new RuntimeException("No available ports");
    }

    
    public synchronized static boolean releasePort(int port) 
    {
        return inUse.remove(port);
    }
    
    
    public static void setProgressCallback(java.util.function.IntConsumer callback)
    {
        progressCallback = callback;
    }

    
    private static void reportProgress(int percent)
    {
        if (progressCallback != null)
        {
            progressCallback.accept(percent);
        }
    }
    
    
    public static void setCancelFlag(AtomicBoolean flag) 
    {
        cancelFlag = flag;
    }
    

    private static boolean isCancelled()
    {
        return cancelFlag != null && cancelFlag.get();
    }
    
}
