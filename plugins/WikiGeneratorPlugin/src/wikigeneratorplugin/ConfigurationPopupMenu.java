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

        // Retrieve list of included diagram names from XML file
        LinkedList<String> includedDiagrams = new LinkedList<String>();
        try {
            File fXmlFile = new File("resources/" + project.getName() + "config.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();

            NodeList nList = doc.getElementsByTagName("diagramID");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    includedDiagrams.add(eElement.getTextContent());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Application.getInstance().getGUILog().showError("Error Occurred");
        }

        try {
            dbFactory = DocumentBuilderFactory.newInstance();
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        includesList.setCellRenderer(new DiagramPresentationElementListCellRender());
        excludesList.setCellRenderer(new DiagramPresentationElementListCellRender());
        excludesListModel = new DefaultListModel<>();
        includesListModel = new DefaultListModel<>();
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
        includesList.setModel(excludesListModel);
        excludesList.setModel(includesListModel);

        emailListModel = new DefaultListModel<>();
        emailList.setModel(emailListModel);

        addButton.addActionListener((ActionEvent e) -> {
            String newEmail = JOptionPane.showInputDialog("Email address of new recipient:");
            if (newEmail != null) {
                emailListModel.addElement(newEmail);
            }
        });
        removeButton.addActionListener((ActionEvent e) -> {
            emailListModel.removeElement(emailList.getSelectedIndex());
        });

        includeSingleButton.addActionListener((ActionEvent e) -> {
            DiagramPresentationElement dpe = (DiagramPresentationElement) includesList.getSelectedValue();
            includesListModel.addElement(dpe);
            excludesListModel.removeElement(dpe);
        });
        excludeSingleButton.addActionListener((ActionEvent e) -> {
            DiagramPresentationElement dpe = (DiagramPresentationElement) excludesList.getSelectedValue();
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
            generateXML();
        });
        cancelButton.addActionListener((ActionEvent e) -> {
            this.dispose();
        });

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
        // TODO: Include the information from the URL inputfield and the email list in the XML.
        // The inputfield has the sysmldiagrams subsite URL as its default unless there is a value in the XML file to
        // overwrite it. So, you can always depend on just grabbing whatever's in the inputfield as the new URL for the
        // XML file. We'll have to put in a check at some point if it's an invalid URL.
        // As for the email list, you can get an array of its Strings with emailListModel.toArray(). Add those to the XML
        // under a single node, and concatenate them into one string separated by semicolons.
        // Once those're written to the XML, I can get to work on reading in their values throughout the plugin.

        try {
            // Set up file and doc builder
            File fXmlFile = new File
                    ("resources/" + project.getName() + "config.xml");
            // If the file doesn't already exist, create it
            if (!fXmlFile.exists()) {
                createNewXML(fXmlFile);
            } else { // otherwise create a new doc from it
                doc = dBuilder.parse(fXmlFile);
            }
            // parse doc from the include element
            doc.getDocumentElement().normalize();
            Node includeElement = doc.getElementsByTagName("include").item(0);
            if (includeElement == null) {
                System.out.println("Includes list from user's project configuration XML does not exist. Check plugin's resources directory.");
            } else {
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
