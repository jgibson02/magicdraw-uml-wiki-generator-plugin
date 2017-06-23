package main.java.umlwikigen;

import main.java.umlwikigen.messageconsole.MessageConsole;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * Created by jhgibso1 on 6/22/2017.
 */
public class UMLWikiGenGUI extends JFrame {

    private File currentlySelectedFile;

    public UMLWikiGenGUI() {
        // Initialize class fields
        this.currentlySelectedFile = null;

        // Declare GUI elements
        this.setTitle("UML Wiki Generator");
        JPanel panel = new JPanel();
        add(panel);
        JButton chooseFileButton = new JButton("Choose file");
        JLabel fileNameLabel = new JLabel("No file selected");
        JButton submitButton = new JButton("Submit");
        JFileChooser fileChooser = new JFileChooser();
        JTextArea textComponent = new JTextArea();
        MessageConsole mc = new MessageConsole(textComponent);
        mc.redirectOut();
        mc.setMessageLines(100);
        final MagicDrawUMLImageExtractor imageExtractor = new MagicDrawUMLImageExtractor(currentlySelectedFile);

        submitButton.setEnabled(false); // Set submit button to disabled by default, enables when user selects file
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        fileChooser.setFileFilter(new FileNameExtensionFilter("MagicDraw Project Files", "mdzip"));

        // Register button listeners
        chooseFileButton.addActionListener((ActionEvent e) -> {
            int returnVal = fileChooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                // User successfully chose a .mdzip
                this.currentlySelectedFile = fileChooser.getSelectedFile();
                fileNameLabel.setText(currentlySelectedFile.getName());
                submitButton.setEnabled(true);
            } else {
                this.currentlySelectedFile = null;
                fileNameLabel.setText("No file selected");
                submitButton.setEnabled(false);
            }
        });
        submitButton.addActionListener((ActionEvent e) -> {
            String[] defaultArgs = {
                    "-verbose",
                    "-Xmx1200M",
                    "-Xss1024K",
                    "-DLOCALCONFIG=true",
                    "-Dinstall.root=\"C:\\Program Files\\MagicDraw\"",
                    "-DFL_SERVER_ADDRESS=ed-svn",
                    "-DFL_SERVER_PORT=1101", "-DFL_EDITION=Architect"
            };
            imageExtractor.setProjectFile(currentlySelectedFile);
            imageExtractor.launch(defaultArgs);
        });

        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        chooseFileButton.setAlignmentX(CENTER_ALIGNMENT);
        fileNameLabel.setAlignmentX(CENTER_ALIGNMENT);
        submitButton.setAlignmentX(CENTER_ALIGNMENT);
        chooseFileButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 24));
        fileNameLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 24));
        submitButton.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 24));
        panel.add(chooseFileButton);
        panel.add(Box.createVerticalStrut(10));
        panel.add(fileNameLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(submitButton);
        panel.add(Box.createVerticalStrut(30));
        panel.add(new JScrollPane(textComponent));

        add(panel);
        setVisible(true);
        setSize(900, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
