package frontend;

import backend.Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


public class StartUpWindow extends JFrame 
{

    private JTextField serverInfoField;
    private JButton connectButton;
    private JButton speedTestButton;

    
    public StartUpWindow()
    {
        setTitle("Video Streaming Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 200);
        setLocationRelativeTo(null);

        // Layout
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        // Label + text field for server info
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("Connected Server:"), gbc);

        serverInfoField = new JTextField("Not connected", 20);
        serverInfoField.setEditable(false);
        gbc.gridx = 1;
        panel.add(serverInfoField, gbc);

        // Connect button
        connectButton = new JButton("Connect to a server");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        panel.add(connectButton, gbc);

        // Speed Test button
        speedTestButton = new JButton("Speed Test");
        gbc.gridy = 2;
        panel.add(speedTestButton, gbc);

        add(panel);
    }

    public void setServerInfo(String host, int port) 
    {
        serverInfoField.setText(host + ":" + port);
    }

    public void setConnectAction(ActionListener listener)
    {
        connectButton.addActionListener(listener);
    }

    public void setSpeedTestAction(ActionListener listener)
    {
        speedTestButton.addActionListener(listener);
    }

    // Quick test launcher
    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(() -> {
            StartUpWindow win = new StartUpWindow();
            win.setVisible(true);
        });
    }
}
