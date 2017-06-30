package wikigeneratorplugin;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.Collection;

/**
 * Author: Kareem Abdol-Hamid kkabdolh
 * Version: 6/29/2017
 */
public class ConfigurationPopupMenu {


    private Object[] included;
    private DocumentBuilderFactory dbFactory;
    private DocumentBuilder dBuilder;
    private Document doc;
    private Project project;

    public ConfigurationPopupMenu() {
        try {
            dbFactory = DocumentBuilderFactory.newInstance();
            dBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        project = Application.getInstance().getProject();
        doc = null;
        Collection<DiagramPresentationElement> dpes = Application.getInstance()
                .getProject().getDiagrams();
        included = dpes.toArray();
        generateXML();
    }

    private void generateXML() {
        try {
            // Set up file and doc builder
            File fXmlFile = new File
                    ("resources/" + project.getName() + "config.xml");
            if (!fXmlFile.exists()) {
                createNewXML(fXmlFile);
            } else {
                doc = dBuilder.parse(fXmlFile);
            }
            doc.getDocumentElement().normalize();
            Node includeElement = doc.getElementsByTagName("include").item(0);
            if (includeElement == null) {
                System.out.println("IT'S A NULLPOINTER HERE");
            }
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

            // Pushes changes to file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(fXmlFile.getAbsolutePath());
            transformer.transform(source, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createNewXML(File file) {
        try {
            // Create new XML if the file doesn't already exist
            doc = dBuilder.newDocument();
            Element rootElement = doc.createElement("plugin");
            doc.appendChild(rootElement);

            Element settings = doc.createElement("settings");
            rootElement.appendChild(settings);

            Element include = doc.createElement("include");
            Element colors = doc.createElement("colors");
            settings.appendChild(include);
            settings.appendChild(colors);

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

}
