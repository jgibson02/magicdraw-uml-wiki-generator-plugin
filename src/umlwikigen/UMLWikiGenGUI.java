package umlwikigen;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.FileFilter;

/**
 * Created by jhgibso1 on 6/22/2017.
 */
public class UMLWikiGenGUI {

    public UMLWikiGenGUI() {
        JFrame frame = new JFrame("UML Wiki Generator");
        frame.setLayout(new BoxLayout(frame, BoxLayout.Y_AXIS));
        JButton chooseFileButton = new JButton("Choose file");
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("MagicDraw Project Files", "mdzip"));
        chooseFileButton.addActionListener((ActionEvent e) -> {
            
        });
        frame.add(chooseFileButton);
        frame.add(new JLabel)
        JButton submitButton = new JButton("Submit");
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
            new MagicDrawUMLImageExtractor(fileChooser.getSelectedFile()).launch(defaultArgs);
        });
        frame.add(submitButton);
        frame.setVisible(true);
        frame.setSize(new Dimension(500, 500));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
