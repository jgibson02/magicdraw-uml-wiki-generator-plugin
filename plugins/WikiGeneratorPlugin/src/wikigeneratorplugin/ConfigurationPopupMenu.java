package wikigeneratorplugin;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
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
 * <p>
 * This class is used to generate a popup menu in MagicDraw for the user to
 * interact with, it allows the user to choose which diagrams they want to
 * include in  their SharePoint upload
 */
public class ConfigurationPopupMenu extends JFrame {

    //=== Current Working Project ===
    private Project project;

    //==== User inputs ======
    private String url;
    private char driveLetter;
    private Object[] emails;
    private Object[] included;

    //======== Document Variables ========
    private DocumentBuilderFactory dbFactory;
    private DocumentBuilder dBuilder;
    private Document doc;

    //============ Model Variables ===============
    private DefaultListModel<String> emailListModel;
    private DefaultListModel<DiagramPresentationElement> excludesListModel;
    private DefaultListModel<DiagramPresentationElement> includesListModel;
    private DefaultComboBoxModel<String> driveLetterDropdownModel;

    //======= Swing Variables =========
    private JTextField urlInputField;
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
    // CONSTRUCTOR FUNCTION
    //==========================================================================

    ConfigurationPopupMenu() {
        super("SharePoint Plugin Options");

        $$$setupUI$$$();

        project = Application.getInstance().getProject();
        doc = null;
        driveLetter = 'S';
        DiagramPresentationElement[] dpes = new DiagramPresentationElement[0];
        if (project != null) {
            Collection<DiagramPresentationElement> dpesCollection = project.getDiagrams();
            dpes = dpesCollection.toArray(new DiagramPresentationElement[dpesCollection.size()]);
        }
        included = dpes;

        File fXmlFile = new File("resources/config/" + project.getName() + ".xml");

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
                if (s.equals(dpe.getID())) {
                    isInIncludedDiagrams = true;
                    break; // already found, skip rest
                }
            }
            if (isInIncludedDiagrams) {
                includedDiagramsList.append(dpe.getName() + "\n");
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
            File configDirectory = new File("resources/config/");
            configDirectory.mkdirs();
            File fXmlFile = new File("resources/config/" + project.getName() + ".xml");
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

    /**
     * Updates the list of included elements by first clearing previous
     * diagramIDs then going through list of included diagrams and adding them
     *
     * @param includeElement the element that new diagramIDs are appended to
     */
    private void updateIncludes(Node includeElement) {
        // Removes all previous diagramIDs
        doc.getElementsByTagName("include").item(0).setTextContent("");

        // Adds new ones from selected list
        for (Object dpeObj : included) {
            if (dpeObj instanceof DiagramPresentationElement) {
                DiagramPresentationElement dpe =
                        (DiagramPresentationElement) dpeObj;
                Element diagramID = doc.createElement("diagramID");
                diagramID.appendChild(doc.createTextNode(dpe.getID()));
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
     * <include></include>
     * <colors></colors>
     * </settings>
     * </plugin>
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

            settings.appendChild(url);
            settings.appendChild(driveLetter);
            settings.appendChild(emails);
            settings.appendChild(include);

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

    /**
     * Creates a network drive connecting sandbox to computer.
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

    /**
     * Deletes old network drive when a new one is selected
     *
     * @param driveToDelete old network drive to be deleted
     */
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

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        rootPanel = new JPanel();
        rootPanel.setLayout(new GridLayoutManager(15, 5, new Insets(10, 15, 10, 15), -1, -1));
        rootPanel.setBackground(new Color(-789517));
        urlInputField = new JTextField();
        urlInputField.setText("");
        rootPanel.add(urlInputField, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(248, 24), null, 0, false));
        emailListLabel = new JLabel();
        emailListLabel.setText("Email Recipients");
        rootPanel.add(emailListLabel, new GridConstraints(3, 0, 1, 5, GridConstraints.ANCHOR_SOUTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(248, 16), null, 0, false));
        emailList = new JList();
        final DefaultListModel defaultListModel1 = new DefaultListModel();
        defaultListModel1.addElement("bob.b.baker@nasa.gov");
        defaultListModel1.addElement("cyndi.s.scurci@nasa.gov");
        defaultListModel1.addElement("james.e.price@nasa.gov");
        defaultListModel1.addElement("gordon.m.cole@nasa.gov");
        defaultListModel1.addElement("agent.cooper@fbi.gov");
        emailList.setModel(defaultListModel1);
        emailList.setSelectionMode(0);
        emailList.putClientProperty("List.isFileList", Boolean.FALSE);
        rootPanel.add(emailList, new GridConstraints(4, 0, 2, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(248, 50), null, 0, false));
        excludesListLabel = new JLabel();
        excludesListLabel.setText("Excludes");
        rootPanel.add(excludesListLabel, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        includesListLabel = new JLabel();
        includesListLabel.setText("Includes");
        rootPanel.add(includesListLabel, new GridConstraints(7, 2, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        rootPanel.add(separator1, new GridConstraints(6, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JSeparator separator2 = new JSeparator();
        rootPanel.add(separator2, new GridConstraints(2, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        addButton = new JButton();
        addButton.setHorizontalAlignment(0);
        addButton.setText("Add");
        rootPanel.add(addButton, new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(104, 32), null, 0, false));
        removeButton = new JButton();
        removeButton.setHorizontalAlignment(0);
        removeButton.setText("Remove");
        rootPanel.add(removeButton, new GridConstraints(5, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, 1, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(104, 32), null, 0, false));
        final Spacer spacer1 = new Spacer();
        rootPanel.add(spacer1, new GridConstraints(4, 3, 2, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        excludeAllButton = new JButton();
        excludeAllButton.setText("<<");
        rootPanel.add(excludeAllButton, new GridConstraints(12, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        excludeSingleButton = new JButton();
        excludeSingleButton.setText("<");
        rootPanel.add(excludeSingleButton, new GridConstraints(11, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        includeAllButton = new JButton();
        includeAllButton.setText(">>");
        rootPanel.add(includeAllButton, new GridConstraints(10, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        includeSingleButton = new JButton();
        includeSingleButton.setBorderPainted(true);
        includeSingleButton.setContentAreaFilled(true);
        includeSingleButton.setDoubleBuffered(false);
        includeSingleButton.setEnabled(true);
        includeSingleButton.setText(">");
        rootPanel.add(includeSingleButton, new GridConstraints(9, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        confirmationButtonsPanel = new JPanel();
        confirmationButtonsPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        confirmationButtonsPanel.setBackground(new Color(-789517));
        rootPanel.add(confirmationButtonsPanel, new GridConstraints(14, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        okButton = new JButton();
        okButton.setText("OK");
        confirmationButtonsPanel.add(okButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cancelButton = new JButton();
        cancelButton.setText("Cancel");
        confirmationButtonsPanel.add(cancelButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        excludesScrollPane = new JScrollPane();
        rootPanel.add(excludesScrollPane, new GridConstraints(8, 0, 6, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        excludesList = new JList();
        excludesList.setDoubleBuffered(false);
        excludesScrollPane.setViewportView(excludesList);
        includesScrollPane = new JScrollPane();
        rootPanel.add(includesScrollPane, new GridConstraints(8, 2, 6, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        includesList = new JList();
        final DefaultListModel defaultListModel2 = new DefaultListModel();
        includesList.setModel(defaultListModel2);
        includesList.setSelectionMode(0);
        includesScrollPane.setViewportView(includesList);
        driveLetterDropdown = new JComboBox();
        rootPanel.add(driveLetterDropdown, new GridConstraints(1, 3, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        driveLetterDropdownLabel = new JLabel();
        driveLetterDropdownLabel.setHorizontalTextPosition(2);
        driveLetterDropdownLabel.setText("Drive Letter");
        rootPanel.add(driveLetterDropdownLabel, new GridConstraints(0, 3, 1, 2, GridConstraints.ANCHOR_SOUTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        urlInputFieldLabel = new JLabel();
        urlInputFieldLabel.setText("SharePoint Site URL");
        urlInputFieldLabel.setVerticalAlignment(3);
        urlInputFieldLabel.setVerticalTextPosition(3);
        rootPanel.add(urlInputFieldLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_SOUTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(248, 16), null, 0, false));
        excludesListLabel.setLabelFor(excludesScrollPane);
        includesListLabel.setLabelFor(includesScrollPane);
        driveLetterDropdownLabel.setLabelFor(driveLetterDropdown);
        urlInputFieldLabel.setLabelFor(urlInputField);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

    private class DiagramPresentationElementListCellRender extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value != null) {
                value = ((DiagramPresentationElement) value).getName();
            }
            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }
    }
}
