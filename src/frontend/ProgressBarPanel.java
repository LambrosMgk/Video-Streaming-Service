package frontend;

import javax.swing.*;
import java.awt.*;
import java.util.regex.*;



public class ProgressBarPanel 
{
    private JTextArea textArea;
    private JProgressBar progressBar;
    private int totalVideoTimeSeconds;

    
    public ProgressBarPanel(int totalVideoTimeSeconds)
    {
    	this.totalVideoTimeSeconds = totalVideoTimeSeconds;
    	createProgressBarPanel();
    }
    
    
    private void createProgressBarPanel()
    {
    	JFrame BarFrame = new JFrame("Download Progress Window");
    	BarFrame.setSize(600, 300);
    	BarFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    	BarFrame.setLocationRelativeTo(null);

    	
        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);

        
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);

        
        BarFrame.add(scrollPane, BorderLayout.CENTER);
        BarFrame.add(progressBar, BorderLayout.SOUTH);
        
        BarFrame.setVisible(true);
    }

    
    public void appendOutput(String line) 
    {
        textArea.append(line + "\n");

        // Try to parse "time=00:01:25.32" from the ffmpeg output
        Pattern pattern = Pattern.compile("time=(\\d+):(\\d+):(\\d+\\.?\\d*)");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find())
        {
            int hours = Integer.parseInt(matcher.group(1));
            int minutes = Integer.parseInt(matcher.group(2));
            double seconds = Double.parseDouble(matcher.group(3));

            double totalSeconds = hours * 3600 + minutes * 60 + seconds;
            int percent = (int) ((totalSeconds / this.totalVideoTimeSeconds) * 100);
            progressBar.setValue(Math.min(percent, 100));
        }
    }
}
