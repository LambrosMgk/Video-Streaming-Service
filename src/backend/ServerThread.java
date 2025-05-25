package backend;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import classes.VideoInfo;


public class ServerThread extends Thread 
{
	private final Logger log = LogManager.getLogger(ServerThread.class);
    
	private int STREAM_PORT;
	private Socket clientSocket;
	private String clientIP = "localhost";	// Could be changed to stream outside of localhost
	private ArrayList<VideoInfo> availableVideos;
	
	private static final String FFMPEG_PATH = "resources/ffmpeg/bin/ffmpeg.exe";
	private static final String FFPROBE_PATH = "resources/ffmpeg/bin/ffprobe.exe";
    private static final String VIDEO_FOLDER = "src/Videos";
    //private static final String[] EXTENSIONS = {".avi", ".mp4", ".mkv"};
    private static final String[] QUALITIES = {"240p", "360p", "480p", "720p", "1080p"};
    private static final Double[] RECOMMENDED_Kbps = {400.0, 750.0, 1000.0, 2500.0, 4500.0};
 
    
    // Constructor.
    public ServerThread(Socket clientSocket, int stream_port, ArrayList<VideoInfo> availVideos) 
    {
        this.clientSocket = clientSocket;
        this.STREAM_PORT = stream_port;
        this.availableVideos = availVideos;
    }
 
    
    // Thread execution.
    public void run()
    {
    	try
    	{
    		log.info("Server thread started at port " + STREAM_PORT);
	    	// Initialize communication wrappers for the new socket.
	        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
	        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
	
	        
	        // 1# Expected message from Client with format "<speed_Mbps>:<format>"
	        String clientMessage = in.readLine();
	        if(clientMessage == null)
	        {
	        	log.error("Client didn't send it's download speed and desired format. Exiting...");
	        	clientSocket.close();
	        	Server.releasePort(STREAM_PORT);	// Signal the server that you are done using this port.
	        	return;		// Terminate this thread.
	        }
	        // Handle client message.
	        String[] parts = clientMessage.split(":");
	        double speedMbps = Double.parseDouble(parts[0]);
	        double speedKbps = speedMbps * 1000;		// Convert to kbps so you can filter out videos based on the "Youtube resolutions and bitrates" picture from the pdf.
	        String requestedFormat = parts[1];
	        
	        log.debug("Client has download speed Mbps: " + speedMbps + ", Kbps: " + speedKbps + " and requests videos with format: " + requestedFormat);
	        
	        
	        // 2# Filter out the videos and send the list to the client.
	        log.info("Sendind a list of available videos to the client.");
	        List<String> filtered = filterVideosBySpeedAndFormat(speedKbps, requestedFormat);
	        for (String info : filtered)
	        {
	            out.println(info);
	        }
	        out.println("END");		// Let the client know the list has ended.
	        
	        
	        // 3# Wait for the client's choice. (video choice name : transfer protocol choice : transfer method)
	        clientMessage = in.readLine();
	        if(clientMessage == null)		// Check if the Client "dies" (disconnects)
	        {
	        	log.debug("Client didn't send a message, probably disconnected.");
	        	Server.releasePort(STREAM_PORT);	// Signal the server that you are done using this port.
		        log.debug("Released port " + STREAM_PORT);
		        return;
	        }
	        parts = clientMessage.split(":");
	        String videoChoice = parts[0];
	        String protocolChoice = parts[1];
	        String transferMethod = parts[2];
	        log.debug("Client chose to <" + transferMethod + "> the video " + videoChoice + ", with Transfer Protocol: " + protocolChoice);
	        
	        
	        // 4# Send the port to which you'll stream the video and the video duration and bitrate (PORT:XXXX,DURATION=XX.XX,BITRATE=XX.XX).
	        String vidDuration = getVideoDuration(videoChoice);
	        String vidBitRate = getVideoBitrate(videoChoice);
	        String vidSize = getVideoSize(videoChoice);
	        out.println		("PORT:" + STREAM_PORT + ",DURATION=" + vidDuration + ",BITRATE=" + vidBitRate + ",SIZE=" + vidSize);
	        log.debug("Sent \"PORT:" + STREAM_PORT + ",DURATION=" + vidDuration + ",BITRATE=" + vidBitRate + ",SIZE=" + vidSize + "\" to the client.");
	        
	        
	        
	        if (transferMethod.equals("download") || transferMethod.equals("stream"))
	        {
	        	uploadVideoToClient(out, videoChoice, vidDuration, protocolChoice, transferMethod);
	        }
	        else
	        {
	        	log.error("Undefined transfer method. Exiting...");
	        }
	        log.info("Streaming has finished and the client socket " + STREAM_PORT + " is now closing. Server thread exiting...");
	        clientSocket.close();
	        Server.releasePort(STREAM_PORT);	// Signal the server that you are done using this port.
	        
	        
	    }
	    catch (IOException | NullPointerException e)
	    {
	        e.printStackTrace();
	        Server.releasePort(STREAM_PORT);	// Signal the server that you are done using this port.
	        log.debug("Released port " + STREAM_PORT);
	        try
	        {
				clientSocket.close();
			} 
	        catch (IOException e1)
	        {	
				e1.printStackTrace();
			}
	    }
    }
    
    
    private List<String> filterVideosBySpeedAndFormat(double speedKbps, String format)
    {
    	List<String> result_vid_names = new ArrayList<>();
    	Map<String, Double> qualityThreshold = new HashMap<>();

    	for(int i = 0; i < QUALITIES.length; i++)
    	{    		
    		qualityThreshold.put(QUALITIES[i], RECOMMENDED_Kbps[i]);	// e.g. qualityThreshold.put("1080p", 4500.0);
    	}
    	
    	
        for (VideoInfo vid : this.availableVideos) 
        {
        	ArrayList<String> vidExtensions = vid.getExtension();
        	ArrayList<String> vidQualities = vid.getQuality();
        	for(int i = 0; i < vidExtensions.size(); i++)
        	{
        		String quality = vidQualities.get(i);
        		if (vidExtensions.get(i).equals(format) && speedKbps >= qualityThreshold.get(quality))
                {
                	result_vid_names.add(vid.getName() + "-" + vidQualities.get(i) + vidExtensions.get(i));
                }
        	}
        }
        
        return result_vid_names;
    }
    
    
    /*
     * Check if the given video file name exists in the videos folder and return a "File" reference.
     */
    private File findVideo(String videoName)
    {
    	File folder = new File(VIDEO_FOLDER);
        File[] files = folder.listFiles();
    	File videoFile = null;
    	
    	for(int i = 0; i < files.length; i++)
    	{
    		if(files[i].getName().equals(videoName))
    		{
    			videoFile = files[i];
    			break;
    		}
    	}
    	
    	return videoFile;
    }
    
    
    /*
     * Returns the video duration in seconds using FFProbe.
     */
    private String getVideoDuration(String videoName)
    {
    	File video = findVideo(videoName);
    	if(video == null)
    	{
    		log.error("Couldn't find the video <" + videoName + "> to get its metadata...");
    		return null;
    	}
    	
    	ProcessBuilder builder = new ProcessBuilder(
    			FFPROBE_PATH, "-v", "error",
    		    "-show_entries", "format=duration",
    		    "-of", "default=noprint_wrappers=1:nokey=1",
    		    video.getAbsolutePath()
    		);

    	
		Process process;
		String duration = null;
		builder.redirectErrorStream(true); 		// Merge stderr with stdout
		try 
		{
			process = builder.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			duration = reader.readLine();
			log.debug("Video duration in seconds: " + duration);
			process.waitFor();
		} 
		catch 
		(IOException | InterruptedException e) 
		{
			e.printStackTrace();
		}
		
		
		return duration;
    }
    
    
    /*
     * Returns the video bit rate using FFProbe.
     */
    private String getVideoBitrate(String videoName)
    {
    	File video = findVideo(videoName);
    	if(video == null)
    	{
    		log.error("Couldn't find the video <" + videoName + "> to get its metadata...");
    		return null;
    	}
    	
    	ProcessBuilder builder = new ProcessBuilder(
    			FFPROBE_PATH, "-v", "error",
    			"-select_streams", "v:0",
    		    "-show_entries", "stream=bit_rate",
    		    "-of", "default=noprint_wrappers=1:nokey=1",
    		    video.getAbsolutePath()
    		);

    	
		Process process;
		String bitRate = null;
		builder.redirectErrorStream(true); 		// Merge stderr with stdout
		try 
		{
			process = builder.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			bitRate = reader.readLine();
			log.debug("Video bit rate: " + bitRate);
			process.waitFor();
		} 
		catch 
		(IOException | InterruptedException e) 
		{
			e.printStackTrace();
		}
		
		
		return bitRate;
    }
    
    
    /*
     * Returns the video size in bytes using FFProbe.
     */
    private String getVideoSize(String videoName)
    {
    	File video = findVideo(videoName);
    	if(video == null)
    	{
    		log.error("Couldn't find the video <" + videoName + "> to get its metadata...");
    		return null;
    	}
    	
    	ProcessBuilder builder = new ProcessBuilder(
    			FFPROBE_PATH, "-v", "error",
    		    "-show_entries", "format=size",
    		    "-of", "default=noprint_wrappers=1:nokey=1",
    		    video.getAbsolutePath()
    		);

    	
		Process process;
		String size = null;
		builder.redirectErrorStream(true); 		// Merge stderr with stdout
		try 
		{
			process = builder.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			size = reader.readLine();
			log.debug("Video size in bytes: " + size);
			process.waitFor();
		} 
		catch 
		(IOException | InterruptedException e) 
		{
			e.printStackTrace();
		}
		
		
		return size;
    }
    
    
    /*
     * Sends a video to the client.
     */
    private void uploadVideoToClient(PrintWriter out, String videoName, String vidDuration, String clientProtocol, String transferMethod)
    {
    	File videoFile = findVideo(videoName);
    	if(videoFile == null)
    	{
    		log.error("Upload error: Couldn't find the requested video file in the \"Videos\" folder...");
    		return;
    	}
    	
    	
    	// Check if the protocol is correct
        ProcessBuilder builder;
        String destination;
        String inputFilePath = videoFile.getAbsolutePath();
        String sdp_path = "resources/stream_" + this.STREAM_PORT + ".sdp";		// Used if the protocol is RTP
        switch (clientProtocol) 
        {
            case "UDP":
                destination = "udp://" + this.clientIP + ":" + this.STREAM_PORT;
                builder = new ProcessBuilder(
                		FFMPEG_PATH, "-i", inputFilePath,
                        "-c:a", "aac",
                        "-f", "mpegts",
                        destination
                );
                if(transferMethod.equals("download"))
                	builder.command().add("-re");		// Stream in real time.
                break;
            case "TCP":
                destination = "tcp://" + this.clientIP + ":" + this.STREAM_PORT + "?listen";
                builder = new ProcessBuilder(
                		FFMPEG_PATH, "-i", inputFilePath,
                        "-c:a", "aac",
                        "-f", "mpegts",
                        destination
                );
                if(transferMethod.equals("download"))
                	builder.command().add("-re");
                break;
            case "RTP/UDP":
                destination = "rtp://" + this.clientIP + ":" + this.STREAM_PORT;
                builder = new ProcessBuilder(
                		FFMPEG_PATH, "-i", inputFilePath,
                		"-t", vidDuration,
                		"-an",                      // Disable audio to avoid multi-stream issues. Can't create the .sdp file otherwise.
                        "-c:a", "aac",
                        "-f", "rtp", "-sdp_file", sdp_path,
                        destination
                );
                if(transferMethod.equals("download"))
                	builder.command().add("-re");
                break;
            default:
            	log.error("Can't send video to client. Unknown protocol...");
            	Server.releasePort(STREAM_PORT);	// Signal the server that you are done using this port.
                return;
        }
        // Note: The -re option means that the stream will be streamed in real-time, it slows it down to simulate a live streaming
        builder.redirectErrorStream(true);		// Merge stderr with stdout
        
        
        
        // Start streaming...
        try
        {
        	boolean flag = true;
        	Process process = builder.start();
        	
        	log.info("Streaming via " + clientProtocol + " to " + this.clientIP + ":" + this.STREAM_PORT);
        	BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            
            while ((line = reader.readLine()) != null)
            {
            	log.debug("[STREAM PROGRESS] " + line);
            	
            	// Check if client disconnected.
            	if(clientSocket.isClosed() == true)
            	{
            		process.destroy();	// Stop the builder from running the ffmpeg.
            		log.warn("Client disconnected while streaming.");
            		Server.releasePort(STREAM_PORT);	// Signal the server that you are done using this port.
            		log.debug("Released port " + STREAM_PORT);
            		break;
            	}
            	
            	
            	if(flag == true && clientProtocol.equals("RTP/UDP") && (line.contains("frame=") || line.contains("bitrate=")))
            	{
            		out.println("SDP READY");		// Signal the client that the .sdp file is ready to read.
            		log.debug("Send signal to the client that the .sdp file is ready.");
            		flag = false;
            	}
            }
            
            process.waitFor();
            this.clientSocket.close();
		} 
        catch (IOException | InterruptedException e) 
        {
			e.printStackTrace();
			Server.releasePort(STREAM_PORT);	// Signal the server that you are done using this port.
		}
    }
}
