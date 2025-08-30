package backend;

import java.util.ArrayList;

public class VideoInfo
{
	private String name;
    private ArrayList<String> extension;
    private ArrayList<String> quality;
    private int BestQualityIndex;		// This index is for both arrays because qualities must be tied to a format (arrays are used in pairs) e.g. We could have 1080p with .mp4 (but not .mkv)

    
    
    private void init()
    {
    	this.extension = new ArrayList<String>();
    	this.quality = new ArrayList<String>();
    }
    
    public VideoInfo(String name, String extension, String quality, int BestQualityIndex) 
    {
    	init();
        this.setName(name);
        this.addExtension(extension);
        this.addQuality(quality);
        this.setBestQualityIndex(BestQualityIndex);
    }
    
    public VideoInfo(String name, ArrayList<String> extension, ArrayList<String> quality, int BestQualityIndex) 
    {
    	init();
        this.setName(name);
        this.setExtension(extension);
        this.setQuality(quality);
        this.setBestQualityIndex(BestQualityIndex);
    }
    
    

    public String getName(){return name;}
    public void setName(String name){this.name = name;}


	public ArrayList<String> getExtension(){return this.extension;}
	public void setExtension(ArrayList<String> extension){this.extension = extension;}
	public void addExtension(String extension) {this.extension.add(extension);}


	public ArrayList<String> getQuality(){return this.quality;}
	public void setQuality(ArrayList<String> quality){this.quality = quality;}
	public void addQuality(String quality) {this.quality.add(quality);}


	public int getBestQualityIndex(){return BestQualityIndex;}
	public void setBestQualityIndex(int BestQualityIndex){this.BestQualityIndex = BestQualityIndex;}

	
	public String toString() 
    {
        return getName() + getExtension() + " (" + getQuality() + ")";
    }
}
