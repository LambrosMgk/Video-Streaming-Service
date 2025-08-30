package shared;


import java.io.*;
import java.nio.file.*;



public class FFmpegLoader 
{
	private static final String FFMPEG_FOLDER = "ffmpeg";
	
	
	/**
     * Returns a File pointing to the chosen ffmpeg tool (ffmpeg, ffplay, ffprobe).
     * @param tool one of: "ffmpeg", "ffplay", "ffprobe"
     */
    public static File getFFtoolExecutable(String tool) throws IOException 
    {
    	String exeName = getExecutableName(tool);
    	
    	
        // 1️) Try external folder beside JAR
    	Path jarDir = Paths.get(System.getProperty("user.dir"));
        File externalExe = jarDir.resolve(FFMPEG_FOLDER).resolve(exeName).toFile();
        if (externalExe.exists()) 
        {
            System.out.println("Using external ffplay: " + externalExe.getAbsolutePath());
            return externalExe;
        }

        // 2️) Fallback to bundled resource inside JAR
        String resourcePath = "/" + FFMPEG_FOLDER + "/" + exeName;
        InputStream is = FFmpegLoader.class.getResourceAsStream(resourcePath);
        if (is == null)
        {
            throw new FileNotFoundException(
                "ffplay not found! Checked external folder ./ffmpeg and bundled resource: " + resourcePath
            );
        }

        
        // Copy to temp file so ProcessBuilder can run it
        File tempExe = File.createTempFile(exeName, exeName.endsWith(".exe") ? ".exe" : "");
        tempExe.deleteOnExit();
        Files.copy(is, tempExe.toPath(), StandardCopyOption.REPLACE_EXISTING);
        
        if (!isWindows()) tempExe.setExecutable(true); // make executable on Unix
        	System.out.println("Using bundled ffplay: " + tempExe.getAbsolutePath());
        
        return tempExe;
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
