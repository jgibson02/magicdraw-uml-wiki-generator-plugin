package wikigeneratorplugin;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.project.ProjectEventListenerAdapter;
import com.nomagic.magicdraw.export.image.ImageExporter;
import com.nomagic.magicdraw.uml.ExtendedPropertyNames;
import com.nomagic.magicdraw.uml.RepresentationTextCreator;
import com.nomagic.magicdraw.uml.symbols.DiagramListenerAdapter;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.uml2.ext.jmi.helpers.ModelHelper;
import com.nomagic.uml2.ext.jmi.helpers.StereotypesHelper;
import com.nomagic.uml2.ext.magicdraw.mdprofiles.Stereotype;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.json.*;
import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Author: Kareem Abdol-Hamid kkabdolh
 * Version: 6/27/2017
 * <p>
 * This responds anytime a MagicDraw project with this plugin installed is
 * saved. It will ask the user if they want to upload their changes to
 * SharePoint or not. The class uses the changes to the diagrams and the
 * projectconfig.xml to decide which files are uploaded to the SharePoint.
 */
public class ProjectListener extends ProjectEventListenerAdapter {

    private Project project;
    private HashMap<DiagramPresentationElement, Status> dirtyDiagrams;
    private LinkedList<String> includedDiagrams; // List of diagram IDs for diagrams specified as included in the XML
    private String diagramsDirectory; // S:\SitePages\PROJECTNAME\diagrams
    private String spSiteURL;
    private String dsvEmailRecipients;
    private LinkedList<String> removedDiagrams;
    private DateFormat df;
    private String lastModifiedBy;

    /**
     * Initializes collections.
     */
    ProjectListener() {
        this.dirtyDiagrams = new HashMap<>();
        this.includedDiagrams = new LinkedList<>();
        this.removedDiagrams = new LinkedList<>();
        this.spSiteURL = null;
        this.dsvEmailRecipients = null;
        this.project = null;
        this.df = new SimpleDateFormat("MMM d, yyyy HH:mm:ss z");
        this.lastModifiedBy = null;
    }

    @Override
    public void projectOpened(final Project project) {
        this.project = project;
        this.diagramsDirectory = "s:\\SitePages\\" + project.getName() + "\\diagrams\\";
        Application.getInstance().getProjectsManager().addProjectListener(this);
        DiagramListenerAdapter adapter = new DiagramListenerAdapter(evt -> {
            String propertyName = evt.getPropertyName();
            if (propertyName.equals(ExtendedPropertyNames.BOUNDS)) {
                dirtyDiagrams.put(project.getActiveDiagram(), Status.UPDATED);

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
     *
     * @param project: the MagicDraw project currently loaded
     * @param b:       true if the project was committed to a teamwork server
     */
    @Override
    public void projectSaved(Project project, boolean b) {
        includedDiagrams.clear();

        // Set up and prompt user if they want to upload to SharePoint and if
        // they want to have the page open or not
        JCheckBox openWikiPageCheckbox = new JCheckBox("Open after completion?", true);
        JCheckBox emailCheckbox = new JCheckBox("Send email to your email list?", false);
        emailCheckbox.setEnabled(false); // Disable checkbox if user has no email recipients
        String promptMessage = "Update the wiki page on SharePoint?";
        Object[] params = {promptMessage, openWikiPageCheckbox, emailCheckbox};

        boolean xmlExists = true;
        try {
            // Parse projectconfig.xml for diagrams that need to be included
            File fXmlFile = new File
                    ("resources/config/" + project.getName() + "config.xml");
            if (fXmlFile.exists() == false) {
                xmlExists = false;
            } else {
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

                this.spSiteURL = doc.getElementsByTagName("url").item(0).getTextContent();
                NodeList emailNodes = doc.getElementsByTagName("emails");
                if (emailNodes.getLength() > 0) {
                    this.dsvEmailRecipients = emailNodes.item(0).getTextContent();
                    if (this.dsvEmailRecipients.length() > 0) {
                        emailCheckbox.setEnabled(true);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        int dialogResponse = JOptionPane.showConfirmDialog(null, params, "Question", JOptionPane.YES_NO_OPTION);
        boolean openWikiPage = openWikiPageCheckbox.isSelected();
        boolean sendEmail = emailCheckbox.isSelected();

        // If yes, continue with the upload process
        if (dialogResponse == JOptionPane.YES_OPTION) {
            if (xmlExists == false) {
                Application.getInstance().getGUILog().showError("No project config found for this project.\nPlease" +
                        " go to Tools -> SharePoint Plugin Options and configure options before continuing.");
                return;
            }
            // Get all diagrams and add new ones that are in the project config
            Collection<DiagramPresentationElement> diagrams = project.getDiagrams();
            new File(diagramsDirectory).mkdirs(); // Create project diagrams folder if it isn't already created
            for (DiagramPresentationElement dpe : diagrams) {
                // Add to dirtyDiagrams if they are not in the project file
                // and they are in the included diagrams list
                if (!(new File(diagramsDirectory + dpe.getName() +
                        ".svg")).exists() && includedDiagrams.contains(dpe.getID())) {
                    dirtyDiagrams.put(dpe, Status.CREATED);
                }
            }
            //==========================================================================================================
            //  Iterate over every .svg in project's folder, check if it is in the list of project diagrams and is in
            //  the include list, and if not: delete.
            //==========================================================================================================
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
                        if (dpe.getName().equals(diagramNameFromFile)) {
                            isInDiagrams = true;
                            if (includedDiagrams.contains(dpe.getID())) {
                                included = true;
                            }
                        }
                    }

                    // Delete if not in included or diagram no longer exists
                    if (!isInDiagrams || !included) {
                        if (f.delete()) { // if deletion succeeds
                            Application.getInstance().getGUILog().log
                                    ("Removing" + f.getName());
                            removedDiagrams.add(f.getName());
                            System.out.print("Removing " + f.getName());
                        }
                    }
                }
            } else {
                if (dirtyDiagrams.isEmpty())
                    System.out.println("No diagrams to update.");
                else
                    System.out.println("There are no diagrams in the folder to delete.");
            }
            //==========================================================================================================

            // Go to export then clear dirty diagrams list
            exportDiagrams(dirtyDiagrams, diagramsDirectory);

            // Create JSON object of project and diagram data.
            JsonBuilderFactory factory = Json.createBuilderFactory(null);
            JsonObject projectJsonObject;
            JsonObjectBuilder projectObjectBuilder = factory.createObjectBuilder()
                    .add("projectName", project.getName());

            //==========================================================================================================
            //  Setting up diagram types structure
            //==========================================================================================================
            JsonArrayBuilder diagramTypesArrayBuilder = factory.createArrayBuilder();

            JsonObjectBuilder actObjectBuilder = factory.createObjectBuilder();
            actObjectBuilder.add("title", "Process Flow/Flow Chart (act)");
            JsonArrayBuilder actArrayBuilder = factory.createArrayBuilder();

            JsonObjectBuilder bddObjectBuilder = factory.createObjectBuilder();
            bddObjectBuilder.add("title", "Architecture/Decomposition (bdd)");
            JsonArrayBuilder bddArrayBuilder = factory.createArrayBuilder();

            JsonObjectBuilder ibdObjectBuilder = factory.createObjectBuilder();
            ibdObjectBuilder.add("title", "Interface (ibd)");
            JsonArrayBuilder ibdArrayBuilder = factory.createArrayBuilder();

            JsonObjectBuilder pkgObjectBuilder = factory.createObjectBuilder();
            pkgObjectBuilder.add("title", "Doc Tree/Organization (pkg)");
            JsonArrayBuilder pkgArrayBuilder = factory.createArrayBuilder();

            JsonObjectBuilder parObjectBuilder = factory.createObjectBuilder();
            parObjectBuilder.add("title", "Parametric (par)");
            JsonArrayBuilder parArrayBuilder = factory.createArrayBuilder();

            JsonObjectBuilder reqObjectBuilder = factory.createObjectBuilder();
            reqObjectBuilder.add("title", "Requirement (req)");
            JsonArrayBuilder reqArrayBuilder = factory.createArrayBuilder();

            JsonObjectBuilder sdstmObjectBuilder = factory.createObjectBuilder();
            sdstmObjectBuilder.add("title", "Interaction/System Behavior (sd, stm)");
            JsonArrayBuilder sdstmArrayBuilder = factory.createArrayBuilder();

            JsonObjectBuilder ucObjectBuilder = factory.createObjectBuilder();
            ucObjectBuilder.add("title", "Stakeholder Analysis (uc)");
            JsonArrayBuilder ucArrayBuilder = factory.createArrayBuilder();

            JsonObjectBuilder otherObjectBuilder = factory.createObjectBuilder();
            otherObjectBuilder.add("title", "Other");
            JsonArrayBuilder otherArrayBuilder = factory.createArrayBuilder();
            //==========================================================================================================


            //==========================================================================================================
            //  Now that necessary deletions and new updates have been handled, build JSON out of the images in the
            //  PROJECTNAME_DIAGRAMS directory.
            //==========================================================================================================
            for (DiagramPresentationElement dpe : project.getDiagrams()) {
                String diagramName = RepresentationTextCreator.getFullUMLName
                        (dpe.getDiagram());

                // Test to see if diagram has an associated file
                File SVGFileLocation = new File(diagramsDirectory + '\\' +
                        diagramName.replace("::","_") + ".svg");
                if (SVGFileLocation.exists()) {
                    String url = "diagrams/" + diagramName + ".svg".replace(" ", "%20"); // Kinda URL-encode the svg path
                    String formattedLastModified = df.format(new Date(SVGFileLocation.lastModified()));
                    this.lastModifiedBy = System.getProperty("user.name");
                    String diagramType = dpe.getDiagramType().getType();
                    String modelComment = ModelHelper.getComment(dpe.getDiagram());
                    String documentation;
                    if (modelComment != null) {
                         documentation = modelComment;
                         System.out.println("Documentation for "+diagramName+": " + documentation);
                    } else {
                        documentation = "";
                    }

                    JsonObjectBuilder diagramObjectBuilder = factory.createObjectBuilder()
                            .add("title", diagramName)
                            .add("subtitle", formattedLastModified + " by " + lastModifiedBy)
                            .add("url", url)
                            .add("comments", "")
                            .add("documentation", documentation);

                    switch (diagramType) {
                        case "SysML Activity Diagram":
                            diagramType = "Process Flow/Flow Chart (Activity Diagram)";
                            diagramObjectBuilder.add("type", diagramType);
                            actArrayBuilder.add(diagramObjectBuilder.build());
                            break;
                        case "SysML Block Definition Diagram":
                            diagramType = "Architecture/Decomposition (Block Definition Diagram)";
                            diagramObjectBuilder.add("type", diagramType);
                            bddArrayBuilder.add(diagramObjectBuilder.build());
                            break;
                        case "SysML Internal Block Diagram":
                            diagramType = "Interface (Internal Block Diagram)";
                            diagramObjectBuilder.add("type", diagramType);
                            ibdArrayBuilder.add(diagramObjectBuilder.build());
                            break;
                        case "SysML Package Diagram":
                            diagramType = "Doc Tree/Organization (Package Diagram)";
                            diagramObjectBuilder.add("type", diagramType);
                            pkgArrayBuilder.add(diagramObjectBuilder.build());
                            break;
                        case "SysML Parametric Diagram":
                            diagramType = "Parametric Diagram";
                            diagramObjectBuilder.add("type", diagramType);
                            parArrayBuilder.add(diagramObjectBuilder.build());
                            break;
                        case "Requirement Diagram":
                            diagramObjectBuilder.add("type", diagramType);
                            reqArrayBuilder.add(diagramObjectBuilder.build());
                            break;
                        case "SysML Sequence Diagram":
                            diagramType = "Interaction/System Behavior (Sequence Diagram)";
                            diagramObjectBuilder.add("type", diagramType);
                            sdstmArrayBuilder.add(diagramObjectBuilder.build());
                            break;
                        case "SysML State Machine Diagram":
                            diagramType = "Interaction/System Behavior (State Machine Diagram)";
                            diagramObjectBuilder.add("type", diagramType);
                            sdstmArrayBuilder.add(diagramObjectBuilder.build());
                            break;
                        case "SysML Use Case Diagram":
                            diagramType = "Stakeholder Analysis (Use Case Diagram)";
                            diagramObjectBuilder.add("type", diagramType);
                            ucArrayBuilder.add(diagramObjectBuilder.build());
                            break;
                        default:
                            diagramType += " (Other)";
                            diagramObjectBuilder.add("type", diagramType);
                            otherArrayBuilder.add(diagramObjectBuilder.build());
                    } // switch (diagramType)
                } else {
                    System.out.println(SVGFileLocation.getAbsolutePath() + " does not exist.");
                } // if (SVGFileLocation.exists())
            } // for (dpe)
            //==========================================================================================================


            //==========================================================================================================
            //  Copies webpage dependencies to project's SharePoint folder if they don't exist.
            //==========================================================================================================
            try {
                File cssDest = new File("S:/SitePages/" + project.getName() + "/css");
                if (!cssDest.exists())
                    FileUtils.copyDirectory(new File("resources/css"), cssDest);

                File imagesDest = new File("S:/SitePages/" + project.getName() + "/images");
                if (!imagesDest.exists())
                    FileUtils.copyDirectory(new File("resources/images"), imagesDest);

                File jsDest = new File("S:/SitePages/" + project.getName() + "/js");
                if (!jsDest.exists())
                    FileUtils.copyDirectory(new File("resources/js"), jsDest);

                File htmlDest = new File("S:/SitePages/" + project.getName() +
                        "/index.html");
                if (!htmlDest.exists()) {
                    FileUtils.copyFile(new File("resources/index.html"),
                            htmlDest);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            //==========================================================================================================

            StringBuilder changelog = constructChangelog();

            //==========================================================================================================
            //  Opens a new email addressed to the user's email recipients list with the changelog as the body.
            //==========================================================================================================
            if (sendEmail) {
                try {
                    String email = "mailto:" + dsvEmailRecipients + "?subject=" + project
                            .getName() + " - Changelog&body=" + changelog.toString();
                    System.out.println("THIS IS THE EMAIL" + email.replace
                            (" ", "%20").replace("\n", "%0A"));
                    Desktop.getDesktop().browse(URI.create(email.replace(" ", "%20").replace("\n", "%0A")));
                } catch (IOException e) {
                    Application.getInstance().getGUILog().showError("Could not create email for recipients: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            //==========================================================================================================

            //==========================================================================================================
            //  Constructs this session's changes and adds it to the on-disk changelog.
            //==========================================================================================================
            // Update changelog on disk
            File changelogFile = new File("S:/SitePages/" + project.getName()
                    + "/changelog.txt");
            if (changelogFile.exists()) {
                try {
                    changelog.append(FileUtils.readFileToString(changelogFile, StandardCharsets.US_ASCII));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                FileUtils.writeStringToFile(changelogFile, changelog.toString(), StandardCharsets.US_ASCII);
            } catch (IOException e) {
                e.printStackTrace();
            }

            //==========================================================================================================

            //==========================================================================================================
            //  Builds each diagram type object, and the rest of the project's JSON, then writes to disk.
            //==========================================================================================================
            actObjectBuilder.add("children", actArrayBuilder.build());
            bddObjectBuilder.add("children", bddArrayBuilder.build());
            ibdObjectBuilder.add("children", ibdArrayBuilder.build());
            pkgObjectBuilder.add("children", pkgArrayBuilder.build());
            parObjectBuilder.add("children", parArrayBuilder.build());
            reqObjectBuilder.add("children", reqArrayBuilder.build());
            sdstmObjectBuilder.add("children", sdstmArrayBuilder.build());
            ucObjectBuilder.add("children", ucArrayBuilder.build());
            otherObjectBuilder.add("children", otherArrayBuilder.build());

            diagramTypesArrayBuilder.add(actObjectBuilder.build());
            diagramTypesArrayBuilder.add(bddObjectBuilder.build());
            diagramTypesArrayBuilder.add(ibdObjectBuilder.build());
            diagramTypesArrayBuilder.add(pkgObjectBuilder.build());
            diagramTypesArrayBuilder.add(parObjectBuilder.build());
            diagramTypesArrayBuilder.add(reqObjectBuilder.build());
            diagramTypesArrayBuilder.add(sdstmObjectBuilder.build());
            diagramTypesArrayBuilder.add(ucObjectBuilder.build());
            diagramTypesArrayBuilder.add(otherObjectBuilder.build());
            projectObjectBuilder.add("diagrams", diagramTypesArrayBuilder.build());
            projectJsonObject = projectObjectBuilder.build();

            // Writes assembled JSON to disk.
            File jsonLocation = new File("S:/SitePages/" + project.getName() + "/js/data.txt");
            try {
                if (!jsonLocation.exists()) {
                    jsonLocation.createNewFile();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            Application.getInstance().getGUILog().log("Writing project JSON to: " + jsonLocation.getAbsolutePath());
            System.out.println("Writing project JSON to: " + jsonLocation.getAbsolutePath());
            try (JsonWriter writer = Json.createWriterFactory(null).createWriter(new FileOutputStream(jsonLocation))) {
                writer.writeObject(projectJsonObject);
            } catch (FileNotFoundException e) {
                Application.getInstance().getGUILog().showMessage(e.getMessage());
                e.printStackTrace();
            }
            //==========================================================================================================


            //==========================================================================================================
            //  Opens the wikipage in the user's default browser.
            //==========================================================================================================
            if (openWikiPage) {
                try {
                    Desktop.getDesktop().browse(URI.create(spSiteURL + "/SitePages/" + project.getName() + "/index.html"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //==========================================================================================================

        } // if (dialogResponse == JOptionPane.YES_OPTION)
        dirtyDiagrams.clear();
        removedDiagrams.clear();
    }


    //==================================================================================================================
    // Private Methods
    //==================================================================================================================
    private void exportDiagrams(HashMap<DiagramPresentationElement, Status> dirtyDiagrams, String fileLoc) {
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
            for (DiagramPresentationElement dpe : dirtyDiagrams.keySet()) {
                if (dpe != null) {
                    count++;
                    String diagramName =  RepresentationTextCreator.getFullUMLName
                            (dpe.getDiagram()).replace("::","_");
                    File SVGFileLocation = new File(fileLoc + '\\' + diagramName + ".svg");

                    Application.getInstance().getGUILog().log("Exporting " + diagramName + ".svg to "
                            + SVGFileLocation.getAbsolutePath() + " (" + count + "/" + total + ")", true);
                    System.out.println("Exporting " + diagramName + ".svg to "
                            + SVGFileLocation.getAbsolutePath() + " (" + count + "/" + total + ")");

                    // This isn't actually multithreaded, MagicDraw is not thread-safe.
                    new Thread(() -> {
                        try {
                            ImageExporter.export(dpe, ImageExporter.SVG, SVGFileLocation);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();
                } // End if-statement
                Application.getInstance().getGUILog().log((int) Math.round((((double) count / (double) total) * 100)) + "% Complete", true);
                System.out.println((int) Math.round(((count / (double) total) * 100)) + "% " + "Complete");
            } // End diagrams loop
            System.out.println("===============================");
        }
    }

    private StringBuilder constructChangelog() {
        StringBuilder changelog = new StringBuilder();
        // Add the timestamp
        changelog.append(df.format(new Date()) + '\n');
        // Add the user
        changelog.append(this.lastModifiedBy + '\n');

        ArrayList<String> created = new ArrayList<>();
        ArrayList<String> updated = new ArrayList<>();

        // Add altered diagrams into changelog, categorized by CHANGED, UPDATED, and REMOVED
        for (DiagramPresentationElement dpe : dirtyDiagrams.keySet()) {
            System.out.println(dirtyDiagrams.get(dpe).getString());
            if (includedDiagrams.contains(dpe.getID())) {
                switch (dirtyDiagrams.get(dpe)) {
                    case CREATED:
                        created.add("  - " + dpe.getName());
                        break;
                    case UPDATED:
                        updated.add("  - " + dpe.getName());
                        break;
                }
            }
        }
        if (created.size() > 0) {
            changelog.append("CREATED:\n");
            for (String s : created) {
                changelog.append(s + '\n');
            }
        }
        if (updated.size() > 0) {
            changelog.append("UPDATED:\n");
            for (String s : updated) {
                changelog.append(s + '\n');
            }
        }
        if (removedDiagrams.size() > 0) {
            changelog.append("REMOVED:\n");
            for (String removed : removedDiagrams) {
                changelog.append("  - " + removed.replace(".svg", "") + "\n");
            }
        }

        // Leave message if no changes were made
        if (created.size() == 0 && updated.size() == 0 && removedDiagrams.size() == 0) {
            changelog.append("No changes have been made.\n");
        }

        // Finalize changes block
        changelog.append("\n");
        System.out.println(changelog.toString());

        return changelog;
    }
    //==================================================================================================================

}
