package wikigeneratorplugin;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
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
    private JLabel includesListLabel;
    private JLabel excludesListLabel;
    private JPanel rootPanel;
    private JPanel confirmationButtonsPanel;
    private JComboBox driveLetterDropdown;

    //==========================================================================
    // CONSTRUCTOR FUNCTIONS
    //==========================================================================

    public ConfigurationPopupMenu() {
        super("SharePoint Plugin Options");

        project = Application.getInstance().getProject();
        doc = null;
        Collection<DiagramPresentationElement> dpesCollection = Application.getInstance().getProject().getDiagrams();
        DiagramPresentationElement[] dpes = dpesCollection.toArray(new DiagramPresentationElement[dpesCollection.size()]);
        included = dpes;

        File fXmlFile = new File("resources/" + project.getName() + "config.xml");

        // Retrieve user settings from XML file
        LinkedList<String> includedDiagrams = new LinkedList<String>();
        excludesListModel = new DefaultListModel<>();
        includesListModel = new DefaultListModel<>();
        emailListModel = new DefaultListModel<>();

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
                // Update URL input field with value from XML
                String spSiteURL = doc.getElementsByTagName("url").item(0).getTextContent(); // There should only be 1 <url> element
                urlInputField.setText(spSiteURL);
            } catch(Exception e) {
                System.err.println("Error retrieving SharePoint site URL from plugin configuration XML.");
                Application.getInstance().getGUILog().showError("Error retrieving SharePoint site URL from plugin configuration XML.");
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
                System.out.println("\n==================================");
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

        includesList.setCellRenderer(new DiagramPresentationElementListCellRender());
        excludesList.setCellRenderer(new DiagramPresentationElementListCellRender());

        StringBuilder includedDiagramsList = new StringBuilder("\n======== Included Diagrams ========\n");
        for (DiagramPresentationElement dpe : dpes) {
            boolean isInIncludedDiagrams = false;
            for (String s : includedDiagrams) {
                if (s.equals(dpe.getDiagram().getID()))
                    isInIncludedDiagrams = true;
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
            String currentURL = urlInputField.getText();
            if (currentURL.equals(this.url) == false) {
                createDrive(currentURL);
            }
            this.url = currentURL;
            generateXML();
        });
        cancelButton.addActionListener((ActionEvent e) -> this.dispose());

        this.add(rootPanel);
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
    
    private void updateIncludes(Node includeElement){
        // Removes all previous diagramIDs
        if (includeElement.getNodeType() == Node.ELEMENT_NODE) {
            NodeList nList = doc.getElementsByTagName("diagramID");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    includeElement.removeChild(nNode);
                }
            }
        }
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
     *  <plugin>
     *      <settings>
     *          <include>
     *
     *          </include>
     *          <colors>
     *
     *          </colors>
     *      </settings>
     *  </plugin>
     *
     *  This basic outline is generated with the name of the file given
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
            Element include = doc.createElement("include");
            Element colors = doc.createElement("colors");
            Element emails = doc.createElement("emails");
            Element url = doc.createElement("url");
            settings.appendChild(url);
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
        createDrive("S", networkLocation);
    }

    /**
     * Creates a network drive connecting sandbox to computer.
     * TODO: Add user input for networkLocation
     *
     * @param driveLetter location of folder being saved to, has drive for first
     *                two characters and proper path to file
     */
    private static void createDrive(String driveLetter, String networkLocation) {
        // Grabs the first two letters of diagramsDirectory
        File drive = new File(driveLetter + ":");
        // If the given drive doesn't already exist, if it does just continue
        if (drive.exists()) {
            System.out.println("Drive Found");
            try {
                String command = "c:\\windows\\system32\\net.exe use " +
                        drive + " /delete";
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
        // Try connecting it to the given networkLocation
        try {
            String command = "c:\\windows\\system32\\net.exe use " +
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
