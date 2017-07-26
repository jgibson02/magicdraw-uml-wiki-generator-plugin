package wikigeneratorplugin;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Author: Kareem Abdol-Hamid kkabdolh
 * Version: 6/29/2017
 *
 * This class is used to generate a popup menu in MagicDraw for the user to
 * interact with, it allows the user to choose which diagrams they want to
 * include in  their SharePoint upload
 */
public class ConfigurationPopupMenu extends JFrame {

    private String url;
    private char driveLetter;
    private Object[] emails;
    private Object[] included;
    private DocumentBuilderFactory dbFactory;
    private DocumentBuilder dBuilder;
    private Document doc;
    private Project project;
    private JTextField urlInputField;
    private DefaultListModel<String> emailListModel;
    private DefaultListModel<DiagramPresentationElement> excludesListModel;
    private DefaultListModel<DiagramPresentationElement> includesListModel;
    private DefaultComboBoxModel<String> driveLetterDropdownModel;
    private JList emailList;
    private JList excludesList;
    private JList includesList;
    private JButton cancelButton;
    private JButton includeSingleButton;
    private JButton includeAllButton;
    private JButton excludeSingleButton;
    private JButton excludeAllButton;
    private JButton okButton;
    private JButton removeButton;
    private JButton addButton;
    private JLabel urlInputFieldLabel;
    private JLabel emailListLabel;
    private JLabel excludesListLabel;
    private JLabel includesListLabel;
    private JPanel rootPanel;
    private JPanel confirmationButtonsPanel;
    private JComboBox driveLetterDropdown;
    private JLabel driveLetterDropdownLabel;
    private JScrollPane excludesScrollPane;
    private JScrollPane includesScrollPane;
    private JComboBox driveNameDropdown;

    //==========================================================================
    // CONSTRUCTOR FUNCTIONS
    //==========================================================================

    ConfigurationPopupMenu() {
        super("SharePoint Plugin Options");

        project = Application.getInstance().getProject();
        doc = null;
        driveLetter = 'S';
        DiagramPresentationElement[] dpes = new DiagramPresentationElement[0];
        if (project != null) {
            Collection<DiagramPresentationElement> dpesCollection = project.getDiagrams();
            dpes = dpesCollection.toArray(new DiagramPresentationElement[dpesCollection.size()]);
        }
        included = dpes;

        File fXmlFile = new File("resources/" + project.getName() + "config.xml");

        // Retrieve user settings from XML file
        LinkedList<String> includedDiagrams = new LinkedList<String>();
        excludesListModel = new DefaultListModel<>();
        includesListModel = new DefaultListModel<>();
        emailListModel = new DefaultListModel<>();
        driveLetterDropdownModel = new DefaultComboBoxModel<>();

        try {
            dbFactory = DocumentBuilderFactory.newInstance();
            dBuilder = dbFactory.newDocumentBuilder();
            this.doc = dBuilder.parse(fXmlFile);
            this.doc.getDocumentElement().normalize();
        } catch (Exception e) {
            System.err.println("Error parsing plugin configuration XML file.");
            e.printStackTrace();
        }

        if (fXmlFile.exists()) {
            try {
                // Retrieve SharePoint site URL from XML file
                String spSiteURL = doc.getElementsByTagName("url").item(0).getTextContent(); // There should only be 1 <url> element
                urlInputField.setText(spSiteURL);
            } catch (Exception e) {
                System.err.println("Error retrieving SharePoint site URL from plugin configuration XML.");
                Application.getInstance().getGUILog().showError("Error retrieving SharePoint site URL from plugin configuration XML.");
                e.printStackTrace();
            }

            try {
                // Retrieve network drive letter from XML file
                this.driveLetter = doc.getElementsByTagName("driveLetter").item(0).getTextContent().charAt(0);
            } catch (Exception e) {
                System.err.println("Error retrieving network drive letter from plugin configuration XML.");
                Application.getInstance().getGUILog().showError("Error retrieving network drive letter from plugin configuration XML.");
                e.printStackTrace();
            }

            try {
                // Retrieve list of email recipients
                NodeList emailNodeList = doc.getElementsByTagName("emails");
                boolean hasEmails = emailNodeList.getLength() > 0;
                System.out.println("\n======== Email Recipients ========");
                if (hasEmails) {
                    String dsvEmailList = emailNodeList.item(0).getTextContent(); // There should only be 1 <emails> element
                    this.emails = dsvEmailList.split(";");
                    for (Object email : emails) {
                        System.out.println((String) email);
                        emailListModel.addElement((String) email);
                    }
                } else {
                    System.out.println("No emails found.");
                }
                System.out.println("==================================");
            } catch (Exception e) {
                System.err.println("Error retrieving email recipients list from plugin configuration XML.");
                Application.getInstance().getGUILog().showError("Error retrieving email recipients list from plugin configuration XML.");
                e.printStackTrace();
            }

            try {
                // Retrieve list of included and excluded diagrams' IDs
                NodeList diagramNodeList = doc.getElementsByTagName("diagramID");
                for (int i = 0; i < diagramNodeList.getLength(); i++) {
                    Node diagramNode = diagramNodeList.item(i);
                    if (diagramNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element diagramElement = (Element) diagramNode;
                        includedDiagrams.add(diagramElement.getTextContent());
                    }
                }
            } catch (Exception e) {
                System.err.println("Error retrieving included/excluded diagrams list from plugin configuration XML.");
                Application.getInstance().getGUILog().showError("Error retrieving included/excluded diagrams list from plugin configuration XML.");
                e.printStackTrace();
            }
        }

        // Sets the dropdown options for drive letter to be all unused drive letters, and drive letters currently mapped to network drives
        FileSystemView fsv = FileSystemView.getFileSystemView();
        ArrayList<File> roots = new ArrayList<>(Arrays.asList(File.listRoots()));
        for (char i = 'A'; i <= 'Z'; i++) {
            File rootDrive = new File(String.valueOf(i) + ":\\");
            if (!roots.contains(rootDrive) || (roots.contains(rootDrive) && fsv.getSystemTypeDescription(rootDrive).equals("Network Drive"))) {
                driveLetterDropdownModel.addElement(rootDrive.toString());
            }
        }

        includesList.setCellRenderer(new DiagramPresentationElementListCellRender());
        excludesList.setCellRenderer(new DiagramPresentationElementListCellRender());

        StringBuilder includedDiagramsList = new StringBuilder("\n======== Included Diagrams ========\n");
        for (DiagramPresentationElement dpe : dpes) {
            boolean isInIncludedDiagrams = false;
            for (String s : includedDiagrams) {
                if (s.equals(dpe.getDiagram().getID())) {
                    isInIncludedDiagrams = true;
                    break; // already found, skip rest
                }
            }
            if (isInIncludedDiagrams) {
                includedDiagramsList.append(dpe.getDiagram().getName() + "\n");
                includesListModel.addElement(dpe);
            } else {
                excludesListModel.addElement(dpe);
            }
        }
        System.out.println(includedDiagramsList.toString() + "===================================");
        includesList.setModel(includesListModel);
        excludesList.setModel(excludesListModel);
        emailList.setModel(emailListModel);
        driveLetterDropdown.setModel(driveLetterDropdownModel);
        driveLetterDropdown.setSelectedItem(String.valueOf(this.driveLetter) + ":\\");

        addButton.addActionListener((ActionEvent e) -> {
            String newEmail = JOptionPane.showInputDialog("Email address of new recipient:");
            if (newEmail != null) {
                emailListModel.addElement(newEmail);
            }
        });
        removeButton.addActionListener((ActionEvent e) -> {
            emailListModel.remove(emailList.getSelectedIndex());
        });

        includeSingleButton.addActionListener((ActionEvent e) -> {
            DiagramPresentationElement dpe = (DiagramPresentationElement) excludesList.getSelectedValue();
            includesListModel.addElement(dpe);
            excludesListModel.removeElement(dpe);
        });
        excludeSingleButton.addActionListener((ActionEvent e) -> {
            DiagramPresentationElement dpe = (DiagramPresentationElement) includesList.getSelectedValue();
            excludesListModel.addElement(dpe);
            includesListModel.removeElement(dpe);
        });
        includeAllButton.addActionListener((ActionEvent e) -> {
            for (Object o : excludesListModel.toArray()) {
                includesListModel.addElement((DiagramPresentationElement) o);
                excludesListModel.removeElement(o);
            }
        });
        excludeAllButton.addActionListener((ActionEvent e) -> {
            for (Object o : includesListModel.toArray()) {
                excludesListModel.addElement((DiagramPresentationElement) o);
                includesListModel.removeElement(o);
            }
        });

        okButton.addActionListener((ActionEvent e) -> {
            included = includesListModel.toArray();
            emails = emailListModel.toArray();

            // Check for changes to URL or drive letter
            String currentDriveLetter = ((String) driveLetterDropdown.getSelectedItem()).substring(0, 1);
            if (!currentDriveLetter.equals(String.valueOf(this.driveLetter))) {
                deleteNetworkDrive(new File(String.valueOf(this.driveLetter) + ":"));
            }
            this.driveLetter = currentDriveLetter.charAt(0);
            String currentURL = urlInputField.getText();
            if (!currentURL.equals(this.url)) {
                createDrive(this.driveLetter, currentURL);
            }
            this.url = currentURL;

            generateXML();
        });
        cancelButton.addActionListener((ActionEvent e) -> this.dispose());

        // Nimbus Look-And-Feel
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // If Nimbus is not available, you can set the GUI to another look and feel.
        }

        this.add(rootPanel);
        this.getRootPane().setDefaultButton(okButton);
        this.setLocationRelativeTo(null);
        this.pack();
        this.setVisible(true);
    }

    //==========================================================================
    // PRIVATE FUNCTIONS
    //==========================================================================

    /**
     * Generate an XML file either creating a new one or editing an old one,
     * adding new diagramIDs for the newly included diagramsIDs
     */
    private void generateXML() {
        try {
            // Set up file and doc builder
            File fXmlFile = new File("resources/" + project.getName() + "config.xml");
            // If the file doesn't already exist, create it
            if (!fXmlFile.exists()) {
                createNewXML(fXmlFile);
            } else { // otherwise create a new doc from it
                doc = dBuilder.parse(fXmlFile);
            }
            // parse doc from the include element
            doc.getDocumentElement().normalize();
            Node includeElement = doc.getElementsByTagName("include").item(0);
            Node emailElement = doc.getElementsByTagName("emails").item(0);
            Node urlElement = doc.getElementsByTagName("url").item(0);
            Node driveLetterElement = doc.getElementsByTagName("driveLetter").item(0);
            if (includeElement == null) {
                System.out.println("Includes list from user's project configuration XML does not exist. Check plugin's resources directory.");
            } else {
                updateIncludes(includeElement);
                StringBuilder emailStringBuilder = new StringBuilder("");
                for (Object email : emails) {
                    emailStringBuilder.append(email.toString());
                    emailStringBuilder.append(";");
                }
                emailElement.setTextContent(emailStringBuilder.toString());
                urlElement.setTextContent(this.url);
                driveLetterElement.setTextContent(String.valueOf(this.driveLetter));
            }

            // Pushes changes to file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(fXmlFile.getAbsolutePath());
            transformer.transform(source, result);

            // Exit window
            this.dispose();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateIncludes(Node includeElement) {
        // Removes all previous diagramIDs
        doc.getElementsByTagName("include").item(0).setTextContent("");

        // Adds new ones from selected list
        for (Object dpeObj : included) {
            if (dpeObj instanceof DiagramPresentationElement) {
                DiagramPresentationElement dpe =
                        (DiagramPresentationElement) dpeObj;
                Element diagramID = doc.createElement("diagramID");
                diagramID.appendChild(doc.createTextNode(dpe.getDiagram().getID()));
                if (includeElement.getNodeType() == Node.ELEMENT_NODE) {
                    includeElement.appendChild(diagramID);
                }
            }
        }
    }


    /**
     * Creates a new XML file that has the basic structure of a project XML
     * for this plugin.
     * Example:
     * <plugin>
     * <settings>
     * <include>
     * <p>
     * </include>
     * <colors>
     * <p>
     * </colors>
     * </settings>
     * </plugin>
     * <p>
     * This basic outline is generated with the name of the file given
     *
     * @param file file that will be used to generate
     */
    private void createNewXML(File file) {
        try {
            // Create new XML if the file doesn't already exist and make
            // plugin the root
            doc = dBuilder.newDocument();
            Element rootElement = doc.createElement("plugin");
            doc.appendChild(rootElement);

            // Add a settings element under plugin root element
            Element settings = doc.createElement("settings");
            rootElement.appendChild(settings);

            // Add include and colors element under settings element
            Element url = doc.createElement("url");
            Element driveLetter = doc.createElement("driveLetter");
            Element emails = doc.createElement("emails");
            Element include = doc.createElement("include");
            Element colors = doc.createElement("colors");

            settings.appendChild(url);
            settings.appendChild(driveLetter);
            settings.appendChild(emails);
            settings.appendChild(include);
            settings.appendChild(colors);

            // Save the doc as an xml with the given file parameter
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            if (file.createNewFile()) {
                StreamResult result = new StreamResult(file.getAbsolutePath());
                transformer.transform(source, result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createDrive(String networkLocation) {
        createDrive('S', networkLocation);
    }

    /**
     * Creates a network drive connecting sandbox to computer.
     * TODO: Add user input for networkLocation
     *
     * @param driveLetter location of folder being saved to, has drive for first
     *                    two characters and proper path to file
     */
    private static void createDrive(char driveLetter, String networkLocation) {
        // Grabs the first two letters of diagramsDirectory
        File drive = new File(String.valueOf(driveLetter) + ":");
        // If the given drive doesn't already exist, if it does just continue
        if (drive.exists()) {
            deleteNetworkDrive(drive);
        }
        // Try connecting it to the given networkLocation
        try {
            String command = System.getenv("SystemDrive") + "\\windows\\system32\\net.exe use " +
                    drive + " " + networkLocation;
            System.out.println(command);
            // Create a process for connecting and wait for it to finish
            Process p = Runtime.getRuntime().exec(command);
            System.out.println("Connecting new network drive...");
            p.waitFor();
            boolean success = p.exitValue() == 0;
            p.destroy();
            System.out.println("Connection " + (success ? "Successful!" :
                    "Failed."));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void deleteNetworkDrive(File driveToDelete) {
        try {
            String command = System.getenv("SystemDrive") + "\\windows\\system32\\net.exe use " +
                    driveToDelete + " /delete";
            // Create a process for connecting and wait for it to finish
            Process p = Runtime.getRuntime().exec(command);
            System.out.println("Connecting new network drive...");
            p.waitFor();
            boolean success = p.exitValue() == 0;
            p.destroy();
            System.out.println("Deletion " + (success ? "Successful!" :
                    "Failed."));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class DiagramPresentationElementListCellRender extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value != null) {
                value = ((DiagramPresentationElement) value).getDiagram().getName();
            }
            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }
    }
}
