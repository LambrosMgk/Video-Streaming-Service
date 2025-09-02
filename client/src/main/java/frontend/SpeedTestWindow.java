package frontend;

import java.awt.BorderLayout;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;


public class SpeedTestWindow extends JDialog
{
    private JTextArea logArea;

    public SpeedTestWindow(JFrame parent)
    {
        super(parent, "Speed Test", false);
        setSize(400, 300);
        setLocationRelativeTo(parent);

        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(logArea);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void appendMessage(String msg)
    {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}

