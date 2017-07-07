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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Author: Kareem Abdol-Hamid kkabdolh
 * Version: 6/27/2017
 */
public class OnSaveListener implements SaveParticipant {

    private HashSet<DiagramPresentationElement> dirtyDiagrams;
    private LinkedList<String> includedDiagrams;
    private String diagramsDirectory;

    OnSaveListener(HashSet<DiagramPresentationElement> dirtyDiagrams,
                   String diagramsDirectory) {
        this.dirtyDiagrams = dirtyDiagrams;
        this.diagramsDirectory = diagramsDirectory;
        this.includedDiagrams = new LinkedList<String>();
    }

    @Override
    public boolean isReadyForSave(Project project, ProjectDescriptor projectDescriptor) {
        return true;
    }

    @Override
    public void doBeforeSave(Project project, ProjectDescriptor projectDescriptor) {
        includedDiagrams.clear();
        try {
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

    @Override
    public void doAfterSave(Project project, ProjectDescriptor projectDescriptor) {
        if (Application.getInstance().getGUILog().showQuestion("Update the Wiki page on SharePoint?")) {
            Collection<DiagramPresentationElement> diagrams = project.getDiagrams();
            for (DiagramPresentationElement dpe : diagrams) {
                if (!new File(diagramsDirectory + '\\' + dpe.getDiagram().getName() + "" +
                        ".svg").exists() && includedDiagrams.contains(dpe.getDiagram().getID())) {
                    dirtyDiagrams.add(dpe);
                }
            }
            makeNewDirectory(diagramsDirectory); // Create project_diagrams folder if it isn't already created
            // Iterate over every .svg in project's folder, check if it is in the list of project diagrams, and if not: delete
            File siteAssetsDirectory = new File(diagramsDirectory);
            File[] existentFiles = siteAssetsDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".svg"));
            for (File f : existentFiles) {
                String diagramNameFromFile = f.getName().replace(".svg", "");
                boolean isInDiagrams = false;
                boolean included = false;
                for (DiagramPresentationElement dpe : diagrams) {
                    if (dpe.getDiagram().getName().equals(diagramNameFromFile)) {
                        isInDiagrams = true;
                        if(includedDiagrams.contains(dpe.getDiagram().getID())) {
                            included = true;
                        }
                    }
                }

                if (!isInDiagrams || !included) {
                    Application.getInstance().getGUILog().log("Deleting " + f.getName());
                    System.out.print("Deleting " + f.getName());
                    f.delete();
                }
            }
            exportDiagrams(project, dirtyDiagrams, diagramsDirectory);
            dirtyDiagrams.clear();
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

        Application.getInstance().getGUILog().log("Constructing JSON data of project.", true);
        System.out.println("Constructing JSON data of project.");

        // Why use a document templating engine when you have STRINGS
        String html = "<html> <head> <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">  <!-- CSS --> <link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0-alpha.6/css/bootstrap.min.css\" integrity=\"sha384-rwoIResjU2yc3z8GV/NPeZWAv56rSmLldC3R/AZzGRnGxQQKnKkoFVhFQhNUwEyJ\" crossorigin=\"anonymous\"> <link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css\"> <link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/lightbox2/2.9.0/css/lightbox.min.css\"> <link rel=\"stylesheet\" href=\"styles.css\">  <!-- JavaScript --> <script src=\"https://cdnjs.cloudflare.com/ajax/libs/jquery/3.2.1/jquery.min.js\"></script> <script src=\"https://cdnjs.cloudflare.com/ajax/libs/tether/1.4.0/js/tether.min.js\"></script> <script src=\"https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0-alpha.6/js/bootstrap.min.js\"></script> <script src=\"https://cdnjs.cloudflare.com/ajax/libs/lightbox2/2.9.0/js/lightbox-plus-jquery.min.js\"></script> <script src=\"scripts.js\"></script> </head> <body> <div class=\"wrapper card col-md-8 offset-md-2\"> <div class=\"navbar-form row\"> <select class=\"form-control\" id=\"project-select\"> <option>Project</option> <option>FUELEAP</option> <option>TestingProj</option> </select> <div class=\"dropdown btn-group\"> <button id=\"filter-list-dropdown\" type=\"button\" class=\"btn btn-secondary dropdown-toggle\" data-toggle=\"dropdown\" aria-haspopup=\"true\" aria-expanded=\"false\">Filter By Type</button> <div class=\"dropdown-menu\" aria-labelledby=\"filter-list-dropdown\"> <table id=\"filter-list\" class=\"dropdown-item\"> <tr> <td>Activity Diagram</td> <td><input type=\"checkbox\" name=\"act-check\" checked></input> </tr> <tr> <td>Block Definition Diagram</td> <td><input type=\"checkbox\" name=\"bdd-check\" checked></input> </tr> <tr> <td>Internal Block Diagram</td> <td><input type=\"checkbox\" name=\"ibd-check\" checked></input> </tr> <tr> <td>Package Diagram</td> <td><input type=\"checkbox\" name=\"pkg-check\" checked></input></td></tr><tr><td>Parametric Diagram</td> <td><input type=\"checkbox\" name=\"par-check\" checked></input></td></tr><tr><td>Requirement Diagram</td> <td><input type=\"checkbox\" name=\"req-check\" checked></input></td></tr><tr><td>Sequence Diagram</td><td><input type=\"checkbox\" name=\"sd-check\" checked></input></td></tr><tr><td>State Machine Diagram</td><td><input type=\"checkbox\" name=\"stm-check\" checked></input></td></tr><tr><td>Use Case Diagram</td><td><input type=\"checkbox\" name=\"uc-check\" checked></input></td></tr> <tr><td>Non-SysML Diagram</td><td><input type=\"checkbox\" name=\"non-sysml-check\" checked></input></td></tr></table> </div> </div> <!-- Search Bar --> <div class=\"input-group add-on\"> <div class=\"input-group-btn\" id=\"search-btn\"> <button class=\"btn btn-default\"><i class=\"fa fa-search\"></i></button> </div> <input class=\"form-control\" placeholder=\"Search\" name=\"srch-term\" id=\"srch-term\" type=\"text\"> </div> <!-- / Search Bar --> </div> <div id=\"list-container\">";

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

                String url = "https://larced.spstg.jsc.nasa.gov/sites/EDM/seemb/sandbox/SiteAssets/" + project.getName() + "_DIAGRAMS/" + diagramName + ".svg".replace(" ", "%20");
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");
                df.setTimeZone(TimeZone.getTimeZone("UTC"));
                String iso8601LastModified = df.format(new Date(SVGFileLocation.lastModified()));
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
                        diagramClass = "non-sysml";
                }

                html += "<div class=\"card diagram-card "+diagramClass+"\"><div class=\"card-block\"><h4 class=\"card-title\">"+diagramName+"</h4><p class=\"card-text\">"+diagramType+"</p><p class=\"text-muted\"><small>Last updated: "+iso8601LastModified+"</small></p></div><a href=\""+url+"\" data-lightbox=\""+diagramName+"\" data-title=\""+diagramName+"\"><img class=\"card-img-bottom\" src=\""+url+"\"></a></div>";

                /*
                diagramArrayBuilder.add(factory.createObjectBuilder()
                        .add("name", diagramName)
                        .add("lastModified", iso8601LastModified)
                        .add("lastModifiedBy", System.getProperty("user.name"))
                        .add("url", url.replace(" ", "%20")) // Sort of URL encode the svg path
                        .build()
                );
                */
            } else {
                System.out.println(SVGFileLocation.getAbsolutePath() + " does not exist.");
            }
        }

        html += "<h2 class=\"no-results-message\">No results found.</h2></div></div></div></body></html>";

        File htmlFile = new File("S:\\SitePages\\"+project.getName()+"_WIKI.html");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(htmlFile))) {
            writer.write(html);
            Desktop.getDesktop().browse(URI.create("https://larced.spstg.jsc.nasa.gov/sites/EDM/seemb/sandbox/SitePages/"+project.getName()+"_WIKI.html"));
        } catch (IOException e) {

        }

        /*
        projectObjectBuilder.add("diagrams", diagramArrayBuilder.build());
        projectJsonObject = projectObjectBuilder.build();
        File jsonLocation = new File("s:\\SiteAssets\\" + project.getName() + ".json");
        Application.getInstance().getGUILog().log("Writing project JSON to: " + jsonLocation.getAbsolutePath());
        System.out.println("Writing project JSON to: " + jsonLocation.getAbsolutePath());
        // Writes assembled JSON to disk.
        try (JsonWriter writer = Json.createWriterFactory(null).createWriter(new FileOutputStream(jsonLocation))) {
            writer.write(projectJsonObject);
        } catch (FileNotFoundException e) {
            Application.getInstance().getGUILog().showMessage(e.getMessage());
            e.printStackTrace();
        }
        */
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