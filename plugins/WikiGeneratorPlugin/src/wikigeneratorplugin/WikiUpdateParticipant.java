package wikigeneratorplugin;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.GUILog;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.SaveParticipant;
import com.nomagic.magicdraw.core.project.ProjectDescriptor;
import com.nomagic.magicdraw.export.image.ImageExporter;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

/**
 * Author: Kareem Abdol-Hamid kkabdolh
 * Version: 6/27/2017
 */
public class WikiUpdateParticipant implements SaveParticipant {

    private HashSet<DiagramPresentationElement> dirtyDiagrams;
    private String fileLoc;

    WikiUpdateParticipant(HashSet<DiagramPresentationElement> dirtyDiagrams,
                          String fileLoc) {
        this.dirtyDiagrams = dirtyDiagrams;
        this.fileLoc = fileLoc;
    }

    @Override
    public boolean isReadyForSave(Project project, ProjectDescriptor projectDescriptor) {
        return true;
    }

    @Override
    public void doBeforeSave(Project project, ProjectDescriptor projectDescriptor) {
        // Do nothing
    }

    @Override
    public void doAfterSave(Project project, ProjectDescriptor projectDescriptor) {
        if (Application.getInstance().getGUILog().showQuestion("Would you like to update the wiki page on SharePoint?")) {
            Collection<DiagramPresentationElement> diagrams = project.getDiagrams();
            for (DiagramPresentationElement dpe : diagrams)
                if (new File(fileLoc + '\\' + dpe.getDiagram().getName() + ".svg").exists() == false)
                    dirtyDiagrams.add(dpe);
            makeNewDirectory(fileLoc); // Create project_diagrams folder if it isn't already created

            // Iterate over every .svg in project's folder, check if it is in the list of project diagrams, and if not: delete
            File siteAssetsDirectory = new File(fileLoc);
            File[] existentFiles = siteAssetsDirectory.listFiles((dir, name) -> {
                return name.toLowerCase().endsWith(".svg");
            });
            for (File f : existentFiles) {
                String diagramNameFromFile = f.getName().replace(".svg", "");
                boolean isInDiagrams = false;
                for (DiagramPresentationElement dpe : diagrams) {
                    if (dpe.getDiagram().getName().equals(diagramNameFromFile))
                        isInDiagrams = true;
                }
                if (isInDiagrams == false)
                    Application.getInstance().getGUILog().log("Deleting " + f.getName(), true);
                    f.delete();
            }

            exportDiagrams(dirtyDiagrams, fileLoc);
            dirtyDiagrams.clear();
        }
    }

    /**
     * Creates a diagram for a given project
     *
     * @param fileLoc location of folder being saved to, path is accessible
     * @return true if the project exists already or was successfully created
     */
    private boolean makeNewDirectory(String fileLoc) {
        File theDir = new File(fileLoc);
        boolean result = true;
        // if the directory does not exist, create it
        if (theDir.exists() == false) {
            result = false;
            try {
                createDrive(fileLoc);
                theDir.mkdir();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    private void exportDiagrams(Collection<DiagramPresentationElement> diagrams, String fileLoc) {
        double count = 0;
        int total = diagrams.size();
        if (total == 0) {
            System.out.println("Nothing to export.");
            Application.getInstance().getGUILog().log("Nothing to export.", true);
        } else if (total > 0) {
            System.out.println("Diagrams to export: " + total);
            System.out.println("0% Complete");
            Application.getInstance().getGUILog().log("Diagrams to export: " + total, true);
            Application.getInstance().getGUILog().log("0% Complete", true);
        }
        for (DiagramPresentationElement dpe : diagrams) {
            if (dpe != null) {
                File img = new File(fileLoc + '\\' + dpe.getDiagram().getName() + ".svg");
                Application.getInstance().getGUILog().log("Exporting " + dpe.getDiagram().getName() + ".svg (" + count + "/" + total + ")", true);
                System.out.println("Exporting " + dpe.getDiagram().getName() + ".svg (" + count + "/" + total + ")");
                try {
                    ImageExporter.export(dpe, ImageExporter.SVG, img);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            count++;
            Application.getInstance().getGUILog().log((int) Math.round(((count / (double) total) * 100)) + "% " + "Complete", true);
            System.out.println((int) Math.round(((count / (double) total) * 100)) + "% " + "Complete");
        }
    }

    /**
     * Creates a network drive connecting sandbox to computer.
     * TODO: Add user input for networkLocation
     *
     * @param fileLoc location of folder being saved to, has drive for first
     *                two characters and proper path to file
     * @return true if drive is created successful
     */
    private void createDrive(String fileLoc) {
        createDrive(fileLoc, " https:\\\\larced.spstg.jsc.nasa.gov\\sites\\EDM\\seemb\\sandbox");
    }

    /**
     * Creates a network drive connecting sandbox to computer.
     * TODO: Add user input for networkLocation
     *
     * @param fileLoc location of folder being saved to, has drive for first
     *                two characters and proper path to file
     * @return true if drive is created successful
     */
    private void createDrive(String fileLoc, String networkLocation) {
        // Grabs the first two letters of fileLoc
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