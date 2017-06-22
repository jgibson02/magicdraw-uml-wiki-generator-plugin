package umlwikigen;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Created by jhgibso1 on 6/22/2017.
 */
public class UMLWikiGenGUI {

    public UMLWikiGenGUI() {
        JFrame frame = new JFrame("UML Wiki Generator");
        frame.add(new JFileChooser());
        JButton submitButton = new JButton("Submit");
        submitButton.addActionListener((ActionEvent e) -> {
           new MagicDrawUMLImageExtractor();
        });
        frame.add(submitButton);
    }
}
