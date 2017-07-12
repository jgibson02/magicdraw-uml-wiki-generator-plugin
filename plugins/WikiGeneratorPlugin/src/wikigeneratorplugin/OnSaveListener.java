package wikigeneratorplugin;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.SaveParticipant;
import com.nomagic.magicdraw.core.project.ProjectDescriptor;
import com.nomagic.magicdraw.export.image.ImageExporter;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.json.*;
import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Author: Kareem Abdol-Hamid kkabdolh
 * Version: 6/27/2017
 * <p>
 * This respoonds anytime a MagicDraw project with this plugin installed is
 * saved. It will ask the user if they want to upload their changes to
 * SharePoint or not. The class uses the changes to the diagrams and the
 * projectconfig.xml to decide which files are uploaded to the SharePoint.
 */
public class OnSaveListener implements SaveParticipant {

    private HashSet<DiagramPresentationElement> dirtyDiagrams;
    private LinkedList<String> includedDiagrams; // List of diagram IDs for diagrams specified as included in the XML
    private String diagramsDirectory; // S:\SitePages\PROJECTNAME\diagrams

    /**
     * Takes an input of changed diagrams and diagram directory
     *
     * @param dirtyDiagrams     the list of dirty diagrams (diagrams that have
     *                          been changed in any way)
     * @param diagramsDirectory the file locatoin of the diagrams on SharePoint
     */
    OnSaveListener(HashSet<DiagramPresentationElement> dirtyDiagrams,
                   String diagramsDirectory) {
        this.dirtyDiagrams = dirtyDiagrams;
        this.diagramsDirectory = diagramsDirectory;
        this.includedDiagrams = new LinkedList<String>();
    }

    /**
     * Intentially empty, no purpose
     */
    @Override
    public boolean isReadyForSave(Project project, ProjectDescriptor projectDescriptor) {
        return true;
    }

    /**
     * Before the program saves, gets the list of files that need to be included
     *
     * @param project           the project that's being worked on
     * @param projectDescriptor the ProjectDescriptor of the project being
     *                          worked on
     */
    @Override
    public void doBeforeSave(Project project, ProjectDescriptor projectDescriptor) {
        includedDiagrams.clear();
        try {
            // Parse projectconfig.xml for diagrams that need to be included
            File fXmlFile = new File
                    ("resources/" + project.getName() + "config.xml");
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
    }

    /**
     * Aftre the project is succesfully saved, it will ask if the user wants
     * to edit the SharePoint wiki page based off the changes since the last
     * upload. If yet, it will continue forward and upload all files that
     * have been changed or added (that are in included) and remove all
     * diagrams that are no longer in included or no longer exist
     *
     * @param project           the project that's being worked on
     * @param projectDescriptor the ProjectDescriptor of the project being
     *                          worked on
     */
    @Override
    public void doAfterSave(Project project, ProjectDescriptor projectDescriptor) {

        // Set up and prompt user if they want to upload to SharePoint and if
        // they want to have the page open or not
        JCheckBox openWikiPageCheckbox = new JCheckBox("Open after completion?", true);
        String promptMessage = "Update the wiki page on SharePoint?";
        Object[] params = {promptMessage, openWikiPageCheckbox};
        int dialogResponse = JOptionPane.showConfirmDialog(null, params, "Question", JOptionPane.YES_NO_OPTION);
        boolean openWikiPage = openWikiPageCheckbox.isSelected();

        // If ues, continue with the upload process
        if (dialogResponse == JOptionPane.YES_OPTION) {
            // Get all diagrams and add new ones that are in the project config
            Collection<DiagramPresentationElement> diagrams = project.getDiagrams();
            makeNewDirectory(diagramsDirectory); // Create project diagrams folder if it isn't already created
            for (DiagramPresentationElement dpe : diagrams) {
                // Add to dirtyDiagrams if they are not in the project file
                // and they are in the included diagrams list
                if (!(new File(diagramsDirectory + dpe.getDiagram().getName() +
                        ".svg")).exists() && includedDiagrams.contains(dpe.getDiagram().getID())) {
                    dirtyDiagrams.add(dpe);
                }
            }
            // Iterate over every .svg in project's folder, check if it is in
            // the list of project diagrams and is in the include list, and if
            // not: delete
            File diagramsDirectoryFile = new File(diagramsDirectory);
            System.out.println("diagramsDirectory: ["+diagramsDirectory+"]");
            System.out.println("diagramsDirectoryFile path: ["+diagramsDirectoryFile.getAbsolutePath()+"]");
            File[] existentFiles = diagramsDirectoryFile.listFiles((dir, name) -> name.toLowerCase().endsWith(".svg"));
            for (File f : existentFiles) {
                String diagramNameFromFile = f.getName().replace(".svg", "");
                boolean isInDiagrams = false;
                boolean included = false;
                // TODO: THIS CAN BE FIXED TO BE CLEANER I'M SURE
                for (DiagramPresentationElement dpe : diagrams) {
                    if (dpe.getDiagram().getName().equals(diagramNameFromFile)) {
                        isInDiagrams = true;
                        if(includedDiagrams.contains(dpe.getDiagram().getID())) {
                            included = true;
                        }
                    }
                }

                // Delete if not in included or diagram no longer exists
                if (!isInDiagrams || !included) {
                    Application.getInstance().getGUILog().log("Deleting " + f.getName());
                    System.out.print("Deleting " + f.getName());
                    f.delete();
                }
            }

            // Go to export then clear dirty diagrams list
            exportDiagrams(project, dirtyDiagrams, diagramsDirectory);
            dirtyDiagrams.clear();

            Application.getInstance().getGUILog().log("Constructing JSON data of project.", true);
            System.out.println("Constructing JSON data of project.");

            // Create JSON object of project and diagram data.
            JsonBuilderFactory factory = Json.createBuilderFactory(null);
            JsonObject projectJsonObject;
            JsonObjectBuilder projectObjectBuilder = factory.createObjectBuilder()
                    .add("projectName", project.getName())
                    .add("lastModified", "2013-01-13T12:54:09,186Z")
                    .add("revision", 89)
                    .add("lastUser", "nphojana");
            JsonArrayBuilder diagramArrayBuilder = factory.createArrayBuilder();

            // Why use a document templating engine when you have STRINGS
            String html = "";
            try {
                byte[] encodedHTMLContent = Files.readAllBytes(new File("htmlTemplate.txt").toPath());
                html = new String(encodedHTMLContent, StandardCharsets.US_ASCII);
            } catch (IOException e) {
                System.err.println("Could not read HTML template from file.");
            }

            /**
             * Now that necessary deletions and new updates have been handled, build JSON out of the images in the
             * PROJECTNAME_DIAGRAMS directory.
             */
            for (DiagramPresentationElement dpe : project.getDiagrams()) {
                String diagramName = dpe.getDiagram().getName();
                // Test to see if diagram has an associated file
                File SVGFileLocation = new File(diagramsDirectory + '\\' + diagramName + ".svg");
                if (SVGFileLocation.exists()) {
                    Application.getInstance().getGUILog().log("Adding " + SVGFileLocation.getName() + " to JSON data.", true);
                    System.out.println("Adding " + SVGFileLocation.getName() + " to JSON data.");

                    String url = "diagrams/" + diagramName + ".svg".replace(" ", "%20");
                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd', 'HH:mm");
                    df.setTimeZone(TimeZone.getTimeZone("UTC"));
                    String iso8601LastModified = df.format(new Date(SVGFileLocation.lastModified()));
                    String lastModifiedBy = System.getProperty("user.name");
                    String diagramType = dpe.getDiagramType().getType(), diagramClass = ".undefined";
                    switch (diagramType) {
                        case "SysML Activity Diagram":
                            diagramType = "Activity Diagram"; diagramClass = "act";
                            break;
                        case "SysML Block Definition Diagram":
                            diagramType = "Block Definition Diagram"; diagramClass = "bdd";
                            break;
                        case "SysML Internal Block Diagram":
                            diagramType = "Internal Block Diagram"; diagramClass = "ibd";
                            break;
                        case "SysML Package Diagram":
                            diagramType = "Package Diagram"; diagramClass = "pkg";
                            break;
                        case "SysML Parametric Diagram":
                            diagramType = "Parametric Diagram"; diagramClass = "par";
                            break;
                        case "Requirement Diagram":
                            diagramClass = "req";
                            break;
                        case "SysML Sequence Diagram":
                            diagramType = "Sequence Diagram"; diagramClass = "sd";
                            break;
                        case "SysML State Machine Diagram":
                            diagramType = "State Machine Diagram"; diagramClass = "stm";
                            break;
                        case "SysML Use Case Diagram":
                            diagramType = "Use Case Diagram"; diagramClass = "uc";
                            break;
                        default:
                            diagramType += " (Other)"; diagramClass = "other";
                    }

                    html += "<div class=\"card diagram-card "+diagramClass+"\">" +
                                "<div class=\"card-block\">" +
                                    "<h4 class=\"card-title\">"+diagramName+"</h4>" +
                                    "<p class=\"card-text\">"+diagramType+"</p>" +
                                    "<p class=\"text-muted\"><small>Last updated: "+iso8601LastModified+" by " + lastModifiedBy + "</small></p>" +
                                "</div>" +
                                "<a href=\""+url+"\" data-lightbox=\""+diagramName+"\" data-title=\""+diagramName+"\">" +
                                    "<img class=\"card-img-bottom\" src=\""+url+"\">" +
                                "</a>" +
                            "</div>";

                    diagramArrayBuilder.add(factory.createObjectBuilder()
                            .add("name", diagramName)
                            .add("lastModified", iso8601LastModified)
                            .add("lastModifiedBy", lastModifiedBy)
                            .add("url", url) // Sort of URL encode the svg path
                            .build()
                    );
                } else {
                    System.out.println(SVGFileLocation.getAbsolutePath() + " does not exist.");
                }
            }

            html += "<h2 class=\"no-results-message\">No results found.</h2></div></div></div></body></html>";

            File htmlFile = new File("S:\\SitePages\\"+project.getName()+"\\"+project.getName()+"_diagrams.html");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(htmlFile))) {
                writer.write(html);
                if (openWikiPage)
                    Desktop.getDesktop().browse(URI.create("https://larced.spstg.jsc.nasa.gov/sites/EDM/seemb/sandbox/SitePages/"+project.getName()+"/"+project.getName()+"_diagrams.html"));
            } catch (IOException e) {
                System.out.println("Error writing HTML to SharePoint:");
                e.printStackTrace();
                Application.getInstance().getGUILog().showMessage("Error writing HTML to SharePoint:\n" + e.getMessage());
            }

            projectObjectBuilder.add("diagrams", diagramArrayBuilder.build());
            projectJsonObject = projectObjectBuilder.build();
            File jsonLocation = new File("s:\\SitePages\\"+project.getName()+"\\" + project.getName()+".json");
            Application.getInstance().getGUILog().log("Writing project JSON to: " + jsonLocation.getAbsolutePath());
            System.out.println("Writing project JSON to: " + jsonLocation.getAbsolutePath());
            // Writes assembled JSON to disk.
            try (JsonWriter writer = Json.createWriterFactory(null).createWriter(new FileOutputStream(jsonLocation))) {
                writer.writeObject(projectJsonObject);
            } catch (FileNotFoundException e) {
                Application.getInstance().getGUILog().showMessage(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    //===================================================
    // Private Methods
    //===================================================


    private void exportDiagrams(Project project, Collection<DiagramPresentationElement> dirtyDiagrams, String fileLoc) {
        int count = 0;
        int total = dirtyDiagrams.size();
        if (total == 0) {
            System.out.println("Nothing to export.");
            Application.getInstance().getGUILog().log("Nothing to export.", true);
        } else if (total > 0) {
            System.out.println("Diagrams to export: " + total);
            System.out.println("0% Complete");
            Application.getInstance().getGUILog().log("Diagrams to export: " + total, true);
            Application.getInstance().getGUILog().log("0% Complete", true);
        }

        for (DiagramPresentationElement dpe : dirtyDiagrams) {
            if (dpe != null) {
                count++;
                File SVGFileLocation = new File(fileLoc + '\\' + dpe.getDiagram().getName() + ".svg");
                Application.getInstance().getGUILog().log("Exporting " + dpe.getDiagram().getName() + ".svg to " + SVGFileLocation.getAbsolutePath() + " (" + count + "/" + total + ")", true);
                System.out.println("Exporting " + dpe.getDiagram().getName() + ".svg to " + SVGFileLocation.getAbsolutePath() + " (" + count + "/" + total + ")");

                /**
                 * Due to limitations of the MagicDraw ImageExporter API, the SVG representation of the diagram must be
                 * written to disk in order to be read as an SVG DOM.
                 */
                try {
                    ImageExporter.export(dpe, ImageExporter.SVG, SVGFileLocation);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } // End if-statement
            Application.getInstance().getGUILog().log((int) Math.round((((double) count / (double) total) * 100)) + "% Complete", true);
            System.out.println((int) Math.round(((count / (double) total) * 100)) + "% " + "Complete");
        } // End diagrams loop
    }

    /**
     * Creates a diagram for a given project
     *
     * @param fileLoc location of folder being saved to, path is accessible
     */
    private void makeNewDirectory(String fileLoc) {
        File theDir = new File(fileLoc);
        // if the directory does not exist, create it
        if (!theDir.exists()) {
            try {
                createDrive(fileLoc);
                theDir.mkdir();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Creates a network drive connecting sandbox to computer.
     * TODO: Add user input for networkLocation
     *
     * @param fileLoc location of folder being saved to, has drive for first
     *                two characters and proper path to file
     */
    private void createDrive(String fileLoc) {
        createDrive(fileLoc, "https:\\\\larced.spstg.jsc.nasa.gov\\sites\\EDM\\seemb\\sandbox");
    }

    /**
     * Creates a network drive connecting sandbox to computer.
     * TODO: Add user input for networkLocation
     *
     * @param fileLoc location of folder being saved to, has drive for first
     *                two characters and proper path to file
     */
    private void createDrive(String fileLoc, String networkLocation) {
        // Grabs the first two letters of diagramsDirectory
        String driveName = fileLoc.length() < 2 ? fileLoc : fileLoc.substring(0,
                2);
        File drive = new File(driveName);
        // If the given drive doesn't already exist, if it does just continue
        if (!drive.exists()) {
            // Try connecting it to the given networkLocation
            try {
                String command = "c:\\windows\\system32\\net.exe use " +
                        drive + networkLocation;
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
        } else {
            System.out.println("Drive Found");
        }
    }
}