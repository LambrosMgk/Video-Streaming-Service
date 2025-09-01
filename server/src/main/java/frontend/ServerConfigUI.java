package frontend;

import backend.NetUtil;
import backend.Server;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerConfigUI extends JFrame
{

	private JTextField serverIpField;
    private JTextField portField;
    private JTextField folderField;
    private JButton browseButton;
    private JRadioButton convertYes;
    private JRadioButton convertNo;
    private JButton startButton;
    private JProgressBar progressBar;
    private JButton cancelButton;
    
    private String default_server_port = "8080";
    private AtomicBoolean cancelRequested = new AtomicBoolean(false);

    
    public ServerConfigUI()
    {
        setTitle("Server Configuration");
        setSize(550, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI()
    {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Server IP
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Server IP:"), gbc);
        
        gbc.gridx = 1;
        serverIpField = new JTextField("Unknown", 15);
        serverIpField.setEditable(false);
        panel.add(serverIpField, gbc);
        
        
        // Server Port
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Server Port:"), gbc);

        gbc.gridx = 1;
        portField = new JTextField(default_server_port, 10);
        panel.add(portField, gbc);

        
        // Folder Selection
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Videos Folder:"), gbc);

        gbc.gridx = 1;
        folderField = new JTextField(20);
        panel.add(folderField, gbc);

        gbc.gridx = 2;
        browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> chooseFolder());
        panel.add(browseButton, gbc);

        
        // Convert videos option
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("Convert videos at start?"), gbc);

        gbc.gridx = 1;
        convertYes = new JRadioButton("Yes");
        convertNo = new JRadioButton("No", true);
        ButtonGroup group = new ButtonGroup();
        group.add(convertYes);
        group.add(convertNo);

        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        radioPanel.add(convertYes);
        radioPanel.add(convertNo);
        panel.add(radioPanel, gbc);

        
        // Start Button
        gbc.gridx = 1;
        gbc.gridy = 4;
        startButton = new JButton("Start Server");
        startButton.addActionListener(e -> startServer());
        panel.add(startButton, gbc);

        
        // Progress Bar
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        panel.add(progressBar, gbc);

        
        // Cancel Button
        gbc.gridx = 2;
        cancelButton = new JButton("Cancel");
        cancelButton.setEnabled(false);
        cancelButton.addActionListener(e -> cancelRequested.set(true));
        panel.add(cancelButton, gbc);

        add(panel);
    }

    
    private void chooseFolder()
    {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
        {
            folderField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    
    private void startServer()
    {
        String portText = portField.getText().trim();
        String folderPath = folderField.getText().trim();
        int port;

        
        // Validate port
        try
        {
            port = Integer.parseInt(portText);
            if (port < 1024 || port > 65535)
            {
                throw new NumberFormatException("Invalid range");
            }
        }
        catch (NumberFormatException ex)
        {
            JOptionPane.showMessageDialog(this, "Please enter a valid port (1024-65535).",
                    "Invalid Port", JOptionPane.ERROR_MESSAGE);
            return;
        }

        
        // Validate folder
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory())
        {
            JOptionPane.showMessageDialog(this, "Please select a valid folder.",
                    "Invalid Folder", JOptionPane.ERROR_MESSAGE);
            return;
        }


        String convertFlag = convertYes.isSelected() ? "true" : "false";
        // Disable inputs
        portField.setEnabled(false);
        folderField.setEnabled(false);
        browseButton.setEnabled(false);
        convertYes.setEnabled(false);
        convertNo.setEnabled(false);
        startButton.setEnabled(false);

        // Reset cancel flag
        cancelRequested.set(false);

       
        // Show progress bar if conversion is needed
        if (convertFlag.equals("true"))
        {
            progressBar.setVisible(true);
            progressBar.setValue(0);
            cancelButton.setEnabled(true);
        }

        
        // Background task
        SwingWorker<Void, Integer> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try
                {
                    Server.setProgressCallback(percent -> publish(percent));
                    Server.setCancelFlag(cancelRequested);
                    String serverIP = NetUtil.getLocalIPv4();
                    
                    setServerIp(serverIP);
                    Server.Start(serverIP, port, folderPath, convertYes.isSelected());
                } 
                catch (Exception e)
                {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(ServerConfigUI.this,
                                    "Error starting server: " + e.getMessage(),
                                    "Server Error", JOptionPane.ERROR_MESSAGE));
                }
                return null;
            }

            @Override
            protected void process(java.util.List<Integer> chunks) {
                if (!chunks.isEmpty()) {
                    int latest = chunks.get(chunks.size() - 1);
                    progressBar.setValue(latest);
                }
            }

            @Override
            protected void done() {
                cancelButton.setEnabled(false);
                if (cancelRequested.get()) {
                    JOptionPane.showMessageDialog(ServerConfigUI.this,
                            "Conversion was canceled.",
                            "Canceled", JOptionPane.WARNING_MESSAGE);
                } else {
                    progressBar.setValue(100);
                    JOptionPane.showMessageDialog(ServerConfigUI.this,
                            "Server is running!",
                            "Server Started", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        };

        worker.execute();
    }
    
    
    public void setServerIp(String ip)
    {
        SwingUtilities.invokeLater(() -> serverIpField.setText(ip));
    }

    
    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(() -> new ServerConfigUI().setVisible(true));
    }
}
