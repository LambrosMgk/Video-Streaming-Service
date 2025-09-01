package shared;


import java.io.*;
import java.nio.file.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;



public class FFmpegLoader 
{
	private static final String FFMPEG_FOLDER = "ffmpeg";
	private static final Logger log = LogManager.getLogger(FFmpegLoader.class);	// Available modes : debug.fatal.error.warn.info
	
	
	/**
     * Returns a File pointing to the chosen ffmpeg tool (ffmpeg, ffplay, ffprobe).
     * @param tool one of: "ffmpeg", "ffplay", "ffprobe"
     */
    public static File getFFtoolExecutable(String tool) throws IOException 
    {
    	String exeName = getExecutableName(tool);
    	
    	
        // 1️) Try external folder beside JAR
    	Path jarDir = Paths.get(System.getProperty("user.dir"));	// Directory where the JAR was launched
    	Path externalPath = jarDir.resolve(FFMPEG_FOLDER).resolve(exeName);
        File externalExe = externalPath.toFile();
        if (externalExe.exists()) 
        {
            log.info("Using external ffplay: " + externalExe.getAbsolutePath());
            return externalExe;
        }
        else
        {
        	log.warn("External folder of ffmpeg not found. Fallback to bundled resources...");
        }
        

        // 2️) Fallback to bundled resource inside JAR (extract bundled resource to ./ffmpeg/)
        String resourcePath = "/" + FFMPEG_FOLDER + "/bin/" + exeName;
        try (InputStream is = FFmpegLoader.class.getResourceAsStream(resourcePath)) 
        {
            if (is == null)
            {
                throw new FileNotFoundException(
                    tool + " not found! Checked external folder ./ffmpeg and bundled resource: " + resourcePath
                );
            }
            
            // Ensure ./ffmpeg folder exists
            Files.createDirectories(externalPath.getParent());
            
            // Copy bundled binary to ./ffmpeg/<exeName>
            Files.copy(is, externalPath, StandardCopyOption.REPLACE_EXISTING);
            
            // make executable on Unix
            if (!isWindows())
            {
                externalExe.setExecutable(true); 
            }
            
            log.info("Extracted bundled " + tool + " to: " + externalExe.getAbsolutePath());
            return externalExe;
        }
    }
    
    private static String getExecutableName(String tool)
    {
        if (!tool.equals("ffmpeg") && !tool.equals("ffplay") && !tool.equals("ffprobe"))
        {
            throw new IllegalArgumentException("Unknown tool: " + tool);
        }
        return isWindows() ? tool + ".exe" : tool;
    }

    private static boolean isWindows()
    {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("win");
    }
    
    
    /**
     * 
     * @param fileName
     * @return The absolute path of "downloads" and the given "fileName"
     *
     */
    public static Path getVideoSavePath(String fileName) throws IOException
    {
        // Directory where the JAR was launched
        Path jarDir = Paths.get(System.getProperty("user.dir"));

        // Create (if needed) a "downloads" folder next to the JAR
        Path saveDir = jarDir.resolve("downloads");
        Files.createDirectories(saveDir);

        // Return full path to requested filename
        return saveDir.resolve(fileName).toAbsolutePath();
    }
    
    
    public static void main(String[] args) throws IOException 
    {
    	File ffmpeg = FFmpegLoader.getFFtoolExecutable("ffmpeg");
    	
        ProcessBuilder pb = new ProcessBuilder(ffmpeg.getAbsolutePath(), "-version");
        pb.inheritIO().start();
    }
}
