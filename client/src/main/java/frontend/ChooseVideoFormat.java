package frontend;


import javax.swing.*;

import backend.Client;

import java.awt.*;
import java.util.List;


public class ChooseVideoFormat 
{
	
	public static String showFormatDialog() 
	{
	    JFrame frame = new JFrame();
	    //frame.setAlwaysOnTop(true);  // Keeps dialog above others
	    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	    //frame.setUndecorated(true);  // No window chrome
	    frame.setVisible(true);

	    String[] options = {".mp4", ".avi", ".mkv"};
	    String answer = (String) JOptionPane.showInputDialog(
	            frame,
	            "Choose your preferred format:",
	            "Format Selection",
	            JOptionPane.PLAIN_MESSAGE,
	            null,
	            options,
	            options[0]
	    );

	    frame.dispose();  // Close the hidden parent frame after use
	    return answer;
	}
	
	
	public static void showVideoAndProtocolSelection(List<String> videoNames) 
	{
	    JFrame frame = new JFrame();
	    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	    frame.setVisible(true);

	    JComboBox<String> videoBox = new JComboBox<>(videoNames.toArray(new String[0]));

	    String[] protocols = {"Auto", "UDP", "TCP", "RTP/UDP"};
	    JComboBox<String> protocolBox = new JComboBox<>(protocols);
	    String[] transferMethods = {"Stream", "Download"};
	    JComboBox<String> transferMethodBox = new JComboBox<>(transferMethods);

	    JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
	    panel.add(new JLabel("<Select video>"));
	    panel.add(videoBox);
	    panel.add(new JLabel("<Select protocol>"));
	    panel.add(protocolBox);
	    panel.add(new JLabel("<Select transfer method>"));
	    panel.add(transferMethodBox);

	    int result = JOptionPane.showConfirmDialog(frame, panel, "Video & Protocol Selection",
	            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

	    frame.dispose();

	    if (result == JOptionPane.OK_OPTION) {
	        String selectedVideo = (String) videoBox.getSelectedItem();
	        String selectedProtocol = (String) protocolBox.getSelectedItem();
	        String selectedTransferMethod = (String) transferMethodBox.getSelectedItem();

	        // Auto-select protocol if "Auto" was chosen
	        if ("Auto".equals(selectedProtocol) && selectedVideo != null) {
	            if (selectedVideo.contains("1080p") || selectedVideo.contains("720p")) {
	                selectedProtocol = "RTP/UDP";
	            } else if (selectedVideo.contains("480p") || selectedVideo.contains("360p")) {
	                selectedProtocol = "UDP";
	            } else {
	                selectedProtocol = "TCP";
	            }
	        }

	        Client.videoChoice = selectedVideo;
	        Client.transferProtocolChoice = selectedProtocol;
	        Client.TransferMethodChoice = selectedTransferMethod.toLowerCase();
	    }
	}

}
