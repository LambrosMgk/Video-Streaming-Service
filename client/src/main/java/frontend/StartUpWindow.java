package frontend;



import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;


public class StartUpWindow extends JFrame 
{

	private JTextField serverIpField;
    private JTextField serverPortField;
    private JTextField serverInfoField;
    
    private JButton connectButton;
    private JButton speedTestButton;

    
    public StartUpWindow()
    {
        setTitle("Video Streaming Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 300);
        setLocationRelativeTo(null);

        // Layout
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- Server IP ---
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Server IP:"), gbc);

        serverIpField = new JTextField("127.0.0.1", 15);
        gbc.gridx = 1;
        panel.add(serverIpField, gbc);

        // --- Server Port ---
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Server Port:"), gbc);

        serverPortField = new JTextField("1111", 15);
        gbc.gridx = 1;
        panel.add(serverPortField, gbc);

        // --- Connection status field ---
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Connected Server:"), gbc);

        serverInfoField = new JTextField("Not connected", 20);
        serverInfoField.setEditable(false);
        gbc.gridx = 1;
        panel.add(serverInfoField, gbc);

        // --- Connect button ---
        connectButton = new JButton("Connect to Server");
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        panel.add(connectButton, gbc);

        // --- Speed Test button ---
        speedTestButton = new JButton("Speed Test");
        gbc.gridy = 4;
        panel.add(speedTestButton, gbc);

        add(panel);
    }

    
    public String getServerIp()
    {
        return serverIpField.getText().trim();
    }

    public int getServerPort()
    {
        try {
            return Integer.parseInt(serverPortField.getText().trim());
        } catch (NumberFormatException e) {
            return -1; // invalid port
        }
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
