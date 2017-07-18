package wikigeneratorplugin;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.project.ProjectEventListener;
import com.nomagic.magicdraw.export.image.ImageExporter;
import com.nomagic.magicdraw.uml.ExtendedPropertyNames;
import com.nomagic.magicdraw.uml.symbols.DiagramListenerAdapter;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.magicdraw.uml.symbols.shapes.DiagramFrameView;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Author: Kareem Abdol-Hamid kkabdolh
 * Version: 6/27/2017
 *
 * This responds anytime a MagicDraw project with this plugin installed is
 * saved. It will ask the user if they want to upload their changes to
 * SharePoint or not. The class uses the changes to the diagrams and the
 * projectconfig.xml to decide which files are uploaded to the SharePoint.
 */
public class ProjectListener implements ProjectEventListener {

    private HashSet<DiagramPresentationElement> dirtyDiagrams;
    private LinkedList<String> includedDiagrams; // List of diagram IDs for diagrams specified as included in the XML
    private String diagramsDirectory; // S:\SitePages\PROJECTNAME\diagrams
    private String spSiteURL;

    /**
     * Initializes collections.
     */
    ProjectListener() {
        this.dirtyDiagrams = new HashSet<>();
        this.includedDiagrams = new LinkedList<>();
        this.spSiteURL = null;
    }

    @Override
    public void projectOpened(final Project project) {
        this.diagramsDirectory = "s:\\SitePages\\" + project.getName() + "\\diagrams\\";
        Application.getInstance().getProjectsManager().addProjectListener(this);
        DiagramListenerAdapter adapter = new DiagramListenerAdapter(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                String propertyName = evt.getPropertyName();
                if(propertyName.equals(ExtendedPropertyNames.VIEW_UPDATED)) {
                    DiagramFrameView diagramFrameView = (DiagramFrameView) evt.getNewValue();
                    dirtyDiagrams.add(diagramFrameView.getDiagramPresentationElement());
                }
            }
        });
        adapter.install(project);
    }

    /**
     * After the project is successfully saved, it will ask if the user wants
     * to edit the SharePoint wiki page based off the changes since the last
     * upload. If yet, it will continue forward and upload all files that
     * have been changed or added (that are in included) and remove all
     * diagrams that are no longer in included or no longer exist.
     * @param project: the MagicDraw project currently loaded
     * @param b: true if the project was committed to a teamwork server
     */
    @Override
    public void projectSaved(Project project, boolean b) {
        includedDiagrams.clear();
        try {
            // Parse projectconfig.xml for diagrams that need to be included
            File fXmlFile = new File
                    ("resources/" + project.getName() + "config.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();

            NodeList diagramNodeList = doc.getElementsByTagName("diagramID");
            for (int temp = 0; temp < diagramNodeList.getLength(); temp++) {
                Node diagramNode = diagramNodeList.item(temp);
                if (diagramNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element diagramElement = (Element) diagramNode;
                    includedDiagrams.add(diagramElement.getTextContent());
                }
            }

            this.spSiteURL = doc.getElementById("url").getTextContent();

        } catch (Exception e) {
            e.printStackTrace();
            Application.getInstance().getGUILog().showError("Error Occurred");
        }

        // Set up and prompt user if they want to upload to SharePoint and if
        // they want to have the page open or not
        JCheckBox openWikiPageCheckbox = new JCheckBox("Open after completion?", true);
        String promptMessage = "Update the wiki page on SharePoint?";
        Object[] params = {promptMessage, openWikiPageCheckbox};
        int dialogResponse = JOptionPane.showConfirmDialog(null, params, "Question", JOptionPane.YES_NO_OPTION);
        boolean openWikiPage = openWikiPageCheckbox.isSelected();

        // If yes, continue with the upload process
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
            System.out.println("Diagrams directory: [" + diagramsDirectory + "]");
            File[] existentFiles = diagramsDirectoryFile.listFiles((dir, name) -> name.toLowerCase().endsWith(".svg"));
            if (existentFiles != null) {
                for (File f : existentFiles) {
                    String diagramNameFromFile = f.getName().replace(".svg", "");
                    boolean isInDiagrams = false;
                    boolean included = false;
                    // TODO: THIS CAN BE FIXED TO BE CLEANER I'M SURE
                    for (DiagramPresentationElement dpe : diagrams) {
                        if (dpe.getDiagram().getName().equals(diagramNameFromFile)) {
                            isInDiagrams = true;
                            if (includedDiagrams.contains(dpe.getDiagram().getID())) {
                                included = true;
                            }
                        }
                    }

                    // Delete if not in included or diagram no longer exists
                    if (!isInDiagrams || !included) {
                        if (f.delete()) { // if deletion succeeds
                            Application.getInstance().getGUILog().log("Deleting " + f.getName());
                            System.out.print("Deleting " + f.getName());
                        }
                    }
                }
            } else {
                if (dirtyDiagrams.isEmpty())
                    System.out.println("No diagrams to update.");
                else
                    System.out.println("There are no diagrams in the folder to delete.");
            }

            // Go to export then clear dirty diagrams list
            exportDiagrams(project, dirtyDiagrams, diagramsDirectory);
            dirtyDiagrams.clear();

            /*
            // Create JSON object of project and diagram data.
            JsonBuilderFactory factory = Json.createBuilderFactory(null);
            JsonObject projectJsonObject;
            JsonObjectBuilder projectObjectBuilder = factory.createObjectBuilder()
                    .add("projectName", project.getName())
                    .add("lastModified", "2013-01-13T12:54:09,186Z")
                    .add("revision", 89)
                    .add("lastUser", "nphojana");
            JsonArrayBuilder diagramArrayBuilder = factory.createArrayBuilder();
            */

            // Why use a templating engine when you have STRINGS
            String html = "";
            try {
                byte[] encodedHTMLContent = Files.readAllBytes(new File("htmlTemplate.txt").toPath());
                html = new String(encodedHTMLContent, StandardCharsets.US_ASCII);
            } catch (IOException e) {
                System.err.println("Could not read HTML template from file.");
            }
            StringBuilder htmlBuilder = new StringBuilder(html);

            /*
             * Now that necessary deletions and new updates have been handled, build JSON out of the images in the
             * PROJECTNAME_DIAGRAMS directory.
             */
            for (DiagramPresentationElement dpe : project.getDiagrams()) {
                String diagramName = dpe.getDiagram().getName();
                // Test to see if diagram has an associated file
                File SVGFileLocation = new File(diagramsDirectory + '\\' + diagramName + ".svg");
                if (SVGFileLocation.exists()) {
                    String url = "diagrams/" + diagramName + ".svg".replace(" ", "%20");
                    DateFormat df = new SimpleDateFormat("yyyy-MM-dd', 'HH:mm' UTC'");
                    df.setTimeZone(TimeZone.getTimeZone("UTC"));
                    String iso8601LastModified = df.format(new Date(SVGFileLocation.lastModified()));
                    String lastModifiedBy = System.getProperty("user.name");
                    String diagramType = dpe.getDiagramType().getType(), diagramClass = ".undefined";
                    switch (diagramType) {
                        case "SysML Activity Diagram":
                            diagramType = "Process Flow/Flow Chart (Activity Diagram)";
                            diagramClass = "act";
                            break;
                        case "SysML Block Definition Diagram":
                            diagramType = "Architecture/Decomposition (Block Definition Diagram)";
                            diagramClass = "bdd";
                            break;
                        case "SysML Internal Block Diagram":
                            diagramType = "Interface (Internal Block Diagram)";
                            diagramClass = "ibd";
                            break;
                        case "SysML Package Diagram":
                            diagramType = "Doc Tree/Organization (Package Diagram)";
                            diagramClass = "pkg";
                            break;
                        case "SysML Parametric Diagram":
                            diagramType = "Parametric Diagram";
                            diagramClass = "par";
                            break;
                        case "Requirement Diagram":
                            diagramClass = "req";
                            break;
                        case "SysML Sequence Diagram":
                            diagramType = "Interaction/System Behavior (Sequence Diagram)";
                            diagramClass = "sd";
                            break;
                        case "SysML State Machine Diagram":
                            diagramType = "Interaction/System Behavior (State Machine Diagram)";
                            diagramClass = "stm";
                            break;
                        case "SysML Use Case Diagram":
                            diagramType = "Stakeholder Analysis (Use Case Diagram)";
                            diagramClass = "uc";
                            break;
                        default:
                            diagramType += " (Other)";
                            diagramClass = "other";
                    }

                    htmlBuilder.append("<div class=\"card diagram-card " + diagramClass + "\">" +
                            "<div class=\"card-block\">" +
                            "<h4 class=\"card-title\">" + diagramName + "</h4>" +
                            "<p class=\"card-text\">" + diagramType + "</p>" +
                            "<p class=\"text-muted\"><small>Last updated: " + iso8601LastModified + " by " + lastModifiedBy + "</small></p>" +
                            "</div>" +
                            "<a href=\"" + url + "\" data-lightbox=\"" + diagramName + "\" data-title=\"" + diagramName + "\">" +
                            "<img class=\"card-img-bottom\" src=\"" + url + "\">" +
                            "</a>" +
                            "</div>");

                    /*
                    diagramArrayBuilder.add(factory.createObjectBuilder()
                            .add("name", diagramName)
                            .add("lastModified", iso8601LastModified)
                            .add("lastModifiedBy", lastModifiedBy)
                            .add("url", url) // Sort of URL encode the svg path
                            .build()
                    );
                    */
                } else {
                    System.out.println(SVGFileLocation.getAbsolutePath() + " does not exist.");
                }
            }

            htmlBuilder.append("<h2 class=\"no-results-message\">No results found.</h2></div></div></div></body></html>");

            File htmlFile = new File("S:\\SitePages\\" + project.getName() + "\\" + project.getName() + "_diagrams.html");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(htmlFile))) {
                writer.write(htmlBuilder.toString().replace("{{PROJECT_NAME}}", project.getName()));
                if (openWikiPage)
                    Desktop.getDesktop().browse(URI.create(spSiteURL + "/SitePages/" + project.getName() + "/" + project.getName() + "_diagrams.html"));
            } catch (IOException e) {
                System.out.println("Error writing HTML to SharePoint:");
                e.printStackTrace();
                Application.getInstance().getGUILog().showMessage("Error writing HTML to SharePoint:\n" + e.getMessage());
            }

            /*
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
            */
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
            System.out.println("======== Beginning Diagrams Export ========");
            System.out.println("Diagrams to export: " + total);
            System.out.println("0% Complete");
            Application.getInstance().getGUILog().log("Diagrams to export: " + total, true);
            Application.getInstance().getGUILog().log("0% Complete", true);
            for (DiagramPresentationElement dpe : dirtyDiagrams) {
                if (dpe != null) {
                    count++;
                    File SVGFileLocation = new File(fileLoc + '\\' + dpe.getDiagram().getName() + ".svg");
                    Application.getInstance().getGUILog().log("Exporting " + dpe.getDiagram().getName() + ".svg to " + SVGFileLocation.getAbsolutePath() + " (" + count + "/" + total + ")", true);
                    System.out.println("Exporting " + dpe.getDiagram().getName() + ".svg to " + SVGFileLocation.getAbsolutePath() + " (" + count + "/" + total + ")");

                    // This isn't actually multithreaded, MagicDraw is not thread-safe.
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                ImageExporter.export(dpe, ImageExporter.SVG, SVGFileLocation);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                } // End if-statement
                Application.getInstance().getGUILog().log((int) Math.round((((double) count / (double) total) * 100)) + "% Complete", true);
                System.out.println((int) Math.round(((count / (double) total) * 100)) + "% " + "Complete");
            } // End diagrams loop
            System.out.println("======== Beginning Diagrams Export ========");
        }
    }

    /**
     * Creates the full path for a given project diagrams destination. Will also attempt to initialize the drive if it
     * has not already been initialized.
     *
     * @param fileLoc location of folder being saved to, path is accessible
     */
    private void makeNewDirectory(String fileLoc) {
        File theDir = new File(fileLoc);
        // if the directory does not exist, create it
        if (!theDir.exists()) {
            try {
                createDrive(fileLoc);
                theDir.mkdirs();
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
        createDrive(fileLoc, this.spSiteURL);
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

    //============================================================
    // Unused Abstract Methods
    //============================================================
    @Override
    public void projectClosed(Project project) {}
    @Override
    public void projectActivated(Project project) {}
    @Override
    public void projectDeActivated(Project project) {}
    @Override
    public void projectReplaced(Project project, Project project1) {}
    @Override
    public void projectCreated(Project project) {}
    @Override
    public void projectPreClosed(Project project) {}
    @Override
    public void projectPreClosedFinal(Project project) {}
    @Override
    public void projectPreSaved(Project project, boolean b) {}
    @Override
    public void projectPreActivated(Project project) {}
    @Override
    public void projectPreDeActivated(Project project) {}
    @Override
    public void projectPreReplaced(Project project, Project project1) {}
    @Override
    public void projectOpenedFromGUI(Project project) {}
    @Override
    public void projectActivatedFromGUI(Project project) {}
}
