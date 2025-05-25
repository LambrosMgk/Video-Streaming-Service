package backend;


import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.model.SpeedTestError;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.SpeedTestReport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import frontend.ChooseVideoFormat;
import frontend.ProgressBarPanel;


public class Client 
{
    private static final String SERVER_HOST = "localhost";
    private static int SERVER_PORT = 1111;
    private static final int UDP_BUFFER_SIZE = 65536;		// 64KB
    private static final int SPEED_TEST_DURATION = 5000;	// in milliseconds
    private static final int SPEED_TEST_INTERVAL = 1000;	// in milliseconds
    private static final String VIDEO_FOLDER = "resources/Downloads";
    private static final String FFPLAY_PATH = "resources/ffmpeg/bin/ffplay.exe";
    private static final String FFMPEG_PATH = "resources/ffmpeg/bin/ffmpeg.exe";
    
    private static int STREAM_PORT;
    private static Socket socket;
    private static Logger log = LogManager.getLogger(Client.class);		// Available modes : debug.fatal.error.warn.info

    private static double downloadSpeedMbps = 0.0;
    public static String videoChoice = null;
    public static String transferProtocolChoice = null;
    public static String TransferMethodChoice = null;
    private static ProgressBarPanel progressBarGui = null;
    
    
    
    public static void main(String[] args) 
    {
    	log.info("Speed Testing for " + SPEED_TEST_DURATION/1000 + " seconds...");
        runJSpeedTest();
        
        // Wait for the speed test to finish.
        try 
        {
            Thread.sleep(SPEED_TEST_DURATION);
        } 
        catch (InterruptedException e) 
        {
            e.printStackTrace();
        }
        log.debug("Download test time is up.");


        
        try
        {
        	// Connect to the load balancing server.
        	socket = new Socket(SERVER_HOST, SERVER_PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            
            // Get the actual server port from the balancer.
            out.println("Hello");
            String message = in.readLine();
	        if(message != null && message.contains("STREAM_SERVER:") == true)
	        {
	        	String[] parts = message.split(":");
	        	SERVER_PORT = Integer.parseInt(parts[1]);
	        	log.info("Load balancing server said to go to port:" + SERVER_PORT);
	        }
	        else
	        {
	        	log.error("Can't connect to the load balancing server. Message: " + message);
	        	log.error("Exiting...");
	        	return;
	        }
	        socket.close();
	        
	        
	        socket = new Socket(SERVER_HOST, SERVER_PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            
            
            // Call GUI to select the desired video format.
	        String selectedFormat = ChooseVideoFormat.showFormatDialog();
	        if (selectedFormat == null) 
	        {
	        	log.error("User cancelled format selection.");
	        	return;
	        }
	        log.debug("Selected " + selectedFormat + " format.");
	        
            // 1# Send formatted information to the server so we can receive a list of videos.
            out.println(downloadSpeedMbps + ":" + selectedFormat);

            
            String line;
            StringBuilder result = new StringBuilder("Available videos:\n");
            while (!(line = in.readLine()).equals("END")) 
            {
                result.append(line).append("\n");
            }
            List<String> videoList = Arrays.asList(result.toString().split("\n"));
            
            
            // Call the GUI selector
            ChooseVideoFormat.showVideoAndProtocolSelection(videoList);
            if(videoChoice == null)
            {
            	log.error("User cancelled video selection.");
            	socket.close();
                return;
            }
            log.info("Chose video: " + videoChoice + ", with Transfer Protocol Choice: " + transferProtocolChoice);
            
            
            // 2# Send your choices to the server.
            out.println(videoChoice + ":" + transferProtocolChoice + ":" + TransferMethodChoice);
            
            
            // 3# Get the port you need to listen the video duration and bitrate. (PORT:XXXX,DURATION=XX.XX,BITRATE=XX.XX)
            STREAM_PORT = -1;
            if (!(line = in.readLine()).contains("PORT:")) 
			{
			    log.error("Server send unknown message when expecting streaming port: " + line);
			    socket.close();
			    return;
			}
            String[] server_args = line.split(",");
            STREAM_PORT = Integer.parseInt(server_args[0].replaceAll("PORT:", ""));
            
            // Try to parse "DURATION=60.12"
            double totalSeconds = 0;
            Pattern pattern = Pattern.compile("DURATION=(\\d+\\.?\\d*)");
            Matcher matcher = pattern.matcher(server_args[1]);
            if (matcher.find())
            {
                double seconds = Double.parseDouble(matcher.group(1));

                totalSeconds = seconds;
            }
            else
            {
            	log.error("Unformatted message from server, expected <PORT:XXXX,DURATION=XX.XX,BITRATE=XX.XX> format but received: " + line);
            	socket.close();
            	return;
            }
            
            int videoSize = Integer.parseInt(server_args[3].replace("SIZE=", ""));
            
            // Try to parse "BITRATE="
            int bitRate;
            if(server_args[2].replace("BITRATE=", "").equals("N/A")) // Probably because selectedFormat.equals(".mkv")
            {
            	bitRate = (int) ((videoSize * 8) / totalSeconds);
            } 
            else 
            {        	
            	bitRate = Integer.parseInt(server_args[2].replace("BITRATE=", ""));
            }
            double videoBitrateMbps = bitRate / 1000000.0;
            double videoSizeMb = videoBitrateMbps * totalSeconds;
            double expectedDownloadTime = videoSizeMb / downloadSpeedMbps;
			log.debug("Server said to listen to port " + STREAM_PORT + " for streaming...");
			log.debug("Server said the duration of the video is " + totalSeconds + " with size of " + videoSize + " bytes and the bit rate is " + bitRate);
            
			log.info("Calculated Video bitrate Mbps: " + videoBitrateMbps + ", for a total size of: " + videoSizeMb/8 + "MB (" + videoSizeMb + " Mbits)");
			log.info("Expected download time is: " + expectedDownloadTime + " seconds");
            
			
            if (TransferMethodChoice.equals("download"))
            {
            	// Initialize the progress bar gui.
                progressBarGui = new ProgressBarPanel((int)totalSeconds);
                //SwingUtilities.invokeLater(() -> progressBarGui.setVisible(true));

                
                // Receive the streaming video and save it in the downloads folder.
                File folder = new File(VIDEO_FOLDER);
                downloadVideo(in, transferProtocolChoice, folder.getAbsolutePath() + "\\" + videoChoice, expectedDownloadTime);                
            }
            else if (TransferMethodChoice.equals("stream"))
            {
            	streamVideo(in, transferProtocolChoice);
            }
            else
            {
            	log.error("Undefined transfer method. Exiting...");
            }
            
            log.info("Video streaming is complete. Closing connection with the server...");
            socket.close();
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
    }

    
    /*
     * Runs a speed test using the JSpeedTest library, this code is actually a demo from their github page.
     */
    private static void runJSpeedTest() 
    {
    	SpeedTestSocket speedTestSocket = new SpeedTestSocket();

    	// add a listener to wait for speedtest completion and progress
    	speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

    	    @Override
    	    public void onCompletion(SpeedTestReport report) 
    	    {
    	        // called when download/upload is complete
    	        log.info("[COMPLETED] rate in octet/s : " + report.getTransferRateOctet());
    	        log.info("[COMPLETED] rate in bit/s   : " + report.getTransferRateBit());
    	        
    	        downloadSpeedMbps = report.getTransferRateBit().doubleValue() / 1000000.0;
    	        log.info("Download speed Mbps: " + downloadSpeedMbps);
    	    }

    	    @Override
    	    public void onError(SpeedTestError speedTestError, String errorMessage) 
    	    {
    	        // called when a download/upload error occur
    	    	log.error("Speed test error: " + errorMessage + ", " + speedTestError.toString());
    	    }

    	    @Override
    	    public void onProgress(float percent, SpeedTestReport report) 
    	    {
    	        // called to notify download/upload progress
    	    	log.debug("[PROGRESS] : " + percent + "%");
    	    }
    	});

    	// Do a 5 seconds test.
        speedTestSocket.startFixedDownload("http://speedtest.tele2.net/100MB.zip", SPEED_TEST_DURATION, SPEED_TEST_INTERVAL);
    }
    
    
    /*
     * Streams a video from the server.
     */
    private static void streamVideo(BufferedReader in, String protocol)
    {
    	ProcessBuilder builder;

    	switch (protocol)
    	{
        case "UDP":
            builder = new ProcessBuilder(
            		FFPLAY_PATH,
            		"-buffer_size", "" + UDP_BUFFER_SIZE,
                    "-i", "udp://" + SERVER_HOST + ":" + STREAM_PORT
                    );
            break;

        case "TCP":
            builder = new ProcessBuilder(
            		FFPLAY_PATH,
                    "-i", "tcp://" + SERVER_HOST + ":" + STREAM_PORT
                    );
            break;

        case "RTP/UDP":
        	String line;
        	String sdp_path = "resources/stream_" + STREAM_PORT + ".sdp";		// Used if the protocol is RTP
            try 
            {
				if (!(line = in.readLine()).equals("SDP READY")) 
				{
				    log.error("Server send unknown message when expecting .spd confirmation: " + line);
				    return;
				}
				log.debug("Looking for .sdp file...");
			} 
            catch (IOException e)
            {
				e.printStackTrace();
			}
            
            builder = new ProcessBuilder(
            		FFPLAY_PATH,
            		"-protocol_whitelist", "file,udp,rtp",
                    "-i", sdp_path
                    );
            break;

        default:
            log.error("Unknown/Unsupported protocol: " + protocol);
            return;
    	}
    	

        builder.redirectErrorStream(true);

        try
        {
            Process process = builder.start();
            log.info("Starting stream via " + protocol + " on port " + STREAM_PORT);
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) 
            {
                log.info("[STREAMING PROGRESS] " + line);
            }
            
            process.waitFor();
        } 
        catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }
    }
    
    
    /*
     * Downloads a video from the server. Note that only TCP works because its made that way, UDP and RTP
     * are fire-and-forget protocols and they won't capture the video with 100% precision. I've implemented
     * these protocols though and i think of them more like "stream capturing" than downloading.
     */
    private static void downloadVideo(BufferedReader in, String protocol, String downloadFilePath, double expectedDownloadTime)
    {
    	ProcessBuilder builder;

    	switch (protocol)
    	{
        case "UDP":
            builder = new ProcessBuilder(FFMPEG_PATH,
            		"-y", // Overwrite output file if it exists
                    "-i", "udp://" + SERVER_HOST + ":" + STREAM_PORT,
                    "-c", "copy",
                    downloadFilePath);
            log.warn("Downloading is not possible with UDP because of the nature of the protocol."
            		+ "However it will do a stream capture and save whatever packets it receives!");
            break;

        case "TCP":
            builder = new ProcessBuilder(FFMPEG_PATH,
            		"-y", // Overwrite output file if it exists
                    "-i", "tcp://" + SERVER_HOST + ":" + STREAM_PORT,
                    "-c", "copy",
                    downloadFilePath);
            break;

        case "RTP/UDP":
        	String line;
        	String sdp_path = "resources/stream_" + STREAM_PORT + ".sdp";		// Used if the protocol is RTP
            try 
            {
				if (!(line = in.readLine()).equals("SDP READY")) 
				{
				    log.error("Server send unknown message when expecting .spd confirmation: " + line);
				    return;
				}
				log.debug("Got SDP READY. Looking for .sdp file...");
			} 
            catch (IOException e)
            {
				e.printStackTrace();
			}
            
            builder = new ProcessBuilder(
            		FFMPEG_PATH,
            		"-y", // Overwrite output file if it exists
            		"-protocol_whitelist", "file,udp,rtp",
                    "-i", sdp_path,
                    "-c", "copy",
                    downloadFilePath
                    );
            log.warn("Downloading is not possible with RTP/UDP because of the nature of the protocol."
            		+ "However it will do a stream capture and save whatever packets it receives!");
            break;

        default:
            log.error("Unknown/Unsupported protocol: " + protocol);
            return;
    	}
    	

        builder.redirectErrorStream(true);

        try
        {
            Process process = builder.start();
            log.info("Saving stream via " + protocol + " on port " + STREAM_PORT + " to file: " + downloadFilePath);
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            long startTime = System.currentTimeMillis();
            long timeoutBufferMs = 3000;		// 3 second buffer
            double maxAllowedTime = startTime + expectedDownloadTime*1000 + timeoutBufferMs;
            while ((line = reader.readLine()) != null)
            {
            	long curr_time = System.currentTimeMillis();
            	
                if (line.contains("frame=") || line.contains("bitrate=")) 
                {
                	progressBarGui.appendOutput(line);
                }
                log.debug("[VIDEO DOWNLOAD PROGRESS] " + line);
                
                // Check if we've gone over the expected download time. (If using UDP or RTP it's most likely to happen because we lost packets)
                if (curr_time >= maxAllowedTime) 
                {
                    log.warn("Stream duration exceeded expected video duration. Exiting stream capture loop...");
                    break;
                }
                
                // Check if server disconnected.
            	if(socket.isClosed() == true)
            	{
            		process.destroy();	// Stop the builder from running the ffmpeg.
            		log.warn("Server disconnected. Stopping download...");
            		break;
            	}
            }
            
            process.waitFor();
            log.info("Video is saved in the downloads folder!");
        } 
        catch (IOException | InterruptedException e)
        {
            e.printStackTrace();
        }
    }
}
