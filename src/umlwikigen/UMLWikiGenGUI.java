package umlwikigen;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileFilter;

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
        JButton chooseFileButton = new JButton("Choose file");
        JLabel fileNameLabel = new JLabel("No file selected");
        JButton submitButton = new JButton("Submit");
        JFileChooser fileChooser = new JFileChooser();
        final MagicDrawUMLImageExtractor imageExtractor = new MagicDrawUMLImageExtractor(currentlySelectedFile);

        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        fileChooser.setFileFilter(new FileNameExtensionFilter("MagicDraw Project Files", "mdzip"));



        // Register button listeners
        chooseFileButton.addActionListener((ActionEvent e) -> {
            int returnVal = fileChooser.showOpenDialog(this);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                // User successfully chose a .mdzip
                currentlySelectedFile = fileChooser.getSelectedFile();
                fileNameLabel.setText(currentlySelectedFile.getName());
                submitButton.setEnabled(true);
            } else {
                currentlySelectedFile = null;
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

        add(chooseFileButton);
        add(fileNameLabel);
        add(submitButton);

        setVisible(true);
        setSize(new Dimension(500, 500));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
