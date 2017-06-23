package main.java.umlwikigen;

import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.FileSystemNotFoundException;

/**
 * Author: Kareem Abdol-Hamid kkabdolh
 * Version: 6/23/2017
 */
public class WikiPageGenerator {
    static String generateWikiString(String imgDest, File project) {
        String wikiPageHead =
                    "<title>"+removeFileExtension(project)+"</title>" +
                    "<script src=\"http://ariutta.github.io/svg-pan-zoom/dist/svg-pan-zoom.min.js\"></script>" +
                    /*"<style>" +
                        "svg {" +
                            "max-width: 75vw;" +
                        "}" +
                    "</style>" +*/
                    "<script>window.onload = function() {";
        String wikiPageBody = "";
        File dir = new File(imgDest);
        File[] dirList = dir.listFiles();
        if (dirList != null) {
            for (File img : dirList) {
                String diagramName = removeFileExtension(img);
                wikiPageHead += "var "+diagramName+" = svgPanZoom('#"+diagramName+"');";

                String output = "";
                // Get string representation of SVG file's DOM
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = null;
                try {
                    dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.parse(img);
                    TransformerFactory tf = TransformerFactory.newInstance();
                    Transformer transformer = tf.newTransformer();
                    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                    StringWriter writer = new StringWriter();
                    transformer.transform(new DOMSource(doc), new StreamResult(writer));
                    output = writer.getBuffer().toString().replaceAll("\n|\r", "");
                    output = output.substring(0, 5) + "id=\"" + diagramName + "\" " + output.substring(5); // Insert ID
                } catch (Exception e) {
                    e.printStackTrace();
                }

                wikiPageBody += "<h2>"+removeFileExtension(img)+"</h2>"+output+"<br/>";
            }
        }
        else {
            throw new FileSystemNotFoundException();
        }
        return "<html><head>" + wikiPageHead + "};</script></head><body>" + wikiPageBody + "</body></html>";
    }

    static String removeFileExtension(File f) {
        String fileName = f.getName();
        int extensionLength = fileName.substring(fileName.indexOf(".")).length();
        return f.getName().substring(0, f.getName().length() - extensionLength);
    }
}
