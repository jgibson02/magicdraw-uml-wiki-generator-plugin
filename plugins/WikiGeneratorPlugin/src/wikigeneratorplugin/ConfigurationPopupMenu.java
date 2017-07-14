package wikigeneratorplugin;


import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collection;
import java.util.LinkedList;

import static javax.swing.ListSelectionModel.SINGLE_SELECTION;

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

    //==========================================================================
    // CONSTRUCTOR FUNCTIONS
    //==========================================================================

    public ConfigurationPopupMenu() {
        super("Includes/Excludes");

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

        setLayout(new GridLayout(1, 3));
        JList excludesJList = new JList();
        JList includesJList = new JList();
        excludesJList.setSelectionMode(SINGLE_SELECTION);
        includesJList.setSelectionMode(SINGLE_SELECTION);
        excludesJList.setCellRenderer(new DiagramPresentationElementListCellRender());
        includesJList.setCellRenderer(new DiagramPresentationElementListCellRender());
        DefaultListModel<DiagramPresentationElement> excludesListModel = new DefaultListModel<>();
        DefaultListModel<DiagramPresentationElement> includesListModel = new DefaultListModel<>();
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
        excludesJList.setModel(excludesListModel);
        includesJList.setModel(includesListModel);

        // Initialize components for buttons panel
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.setLayout(new GridLayout(6, 1));
        JButton includeSingleButton = new JButton(">");
        JButton includeAllButton = new JButton(">>");
        JButton excludeSingleButton = new JButton("<");
        JButton excludeAllButton = new JButton("<<");
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        includeSingleButton.addActionListener((ActionEvent e) -> {
            DiagramPresentationElement dpe = (DiagramPresentationElement) excludesJList.getSelectedValue();
            includesListModel.addElement(dpe);
            excludesListModel.removeElement(dpe);
        });
        excludeSingleButton.addActionListener((ActionEvent e) -> {
            DiagramPresentationElement dpe = (DiagramPresentationElement) includesJList.getSelectedValue();
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

        // Add buttons to button panel
        buttonsPanel.add(includeSingleButton);
        buttonsPanel.add(includeAllButton);
        buttonsPanel.add(excludeSingleButton);
        buttonsPanel.add(excludeAllButton);
        buttonsPanel.add(okButton);
        buttonsPanel.add(cancelButton);

        this.getRootPane().setBorder(new EmptyBorder(15, 15, 15, 15));
        JScrollPane excludesPane = new JScrollPane(excludesJList);
        JScrollPane includesPane = new JScrollPane(includesJList);
        excludesPane.setBorder(new TitledBorder("Excludes"));
        includesPane.setBorder(new TitledBorder("Includes"));
        this.add(excludesPane);
        this.add(buttonsPanel);
        this.add(includesPane);

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
