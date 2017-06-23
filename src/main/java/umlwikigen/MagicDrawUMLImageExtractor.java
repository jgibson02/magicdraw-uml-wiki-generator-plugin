package main.java.umlwikigen;

import com.nomagic.magicdraw.commandline.CommandLine;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.project.ProjectDescriptor;
import com.nomagic.magicdraw.core.project.ProjectDescriptorsFactory;
import com.nomagic.magicdraw.core.project.ProjectsManager;
import com.nomagic.magicdraw.export.image.ImageExporter;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;

import java.io.File;
import java.util.Calendar;
import java.util.Collection;

/**
 *
 */
public class MagicDrawUMLImageExtractor extends CommandLine {

    private File projectFile;
    private String wikiPage;

    public MagicDrawUMLImageExtractor(File projectFile) {
        this.projectFile = projectFile;
        this.wikiPage = "";
    }

    /**
     * This function executes when the class is executed and exports all the
     * files to the given folder
     * TODO: ADD USER INPUT
     *
     * @return a 0 if it is a success and a -1 if it failed
     */
    @Override
    protected byte execute() {
        String destination =  "s:\\SiteAssets\\";
        // String projectLocation = "C:\\Users\\jhgibso1\\Desktop\\NIFS " +
        //"MagicDraw\\FUELEAP_NIFS_v75.mdzip";
        // Create file for given project and check if it exists
        // final File file = new File(projectLocation);
        final ProjectDescriptor projectDescriptor = ProjectDescriptorsFactory.createProjectDescriptor(projectFile.toURI());
        if (projectDescriptor == null) {
            System.out.println("Project descriptor was not created for " + projectFile.getAbsolutePath());
            return -1;
        }
        // Get project manager and load project
        final ProjectsManager projectsManager = Application.getInstance().getProjectsManager();
        projectsManager.loadProject(projectDescriptor, true);
        final Project project = projectsManager.getActiveProject();
        // Check if project was loaded
        if (project == null) {
            System.out.println("Project " + projectFile.getAbsolutePath() + " was not loaded.");
            return -1;
        }
        // Create file destination string with
        // [PROJECT_NAME]_DIAGRAMS_[CURRENT_DATE_TIME]
        String fileLoc = destination + project.getName() + "_DIAGRAMS_" +
                getDateTimeString();
        // Check if a new file
        // was created or the project already exists
        if (makeNewDirectory(fileLoc)) {
            // Send a msg to the user and begin exporting diagrams to project
            Collection diagrams = project.getDiagrams();
            // Go through each diagram and export it with the diagrams name + .jpg
            double count = 0;
            double total = (double) diagrams.size();
            System.out.println("0% Complete");
            String wikiPage = "<html><title>"+project.getName()+"</title><body>";
            for (Object d : diagrams) {
                DiagramPresentationElement dpe =
                        (DiagramPresentationElement) d;
                try {
                    File img = new File(fileLoc + '\\' + dpe.getDiagram()
                            .getName() + ".svg");
                    ImageExporter.export(dpe, ImageExporter.SVG, img);
                    wikiPage += "<img src=\"" + img.getAbsoluteFile() + "/><br/>";
                } catch (Exception e) {
                    e.printStackTrace();
                }
                dpe.close();
                count++;
                System.out.println((int) Math.round(((count / total) * 100)) + "% " + "Complete");
            }
            wikiPage += "</body></html>";
        }
        projectsManager.closeProject();

        return 0;
    }

    public String getWikiPage() { return this.wikiPage; }
    public void setProjectFile(File f) { this.projectFile = f; }

    //=======================================================
    // PRIVATE METHODS
    //=======================================================

    /**
     * Creates a diagram for a given project
     *
     *
     * @param fileLoc location of folder being saved to, path is accessible
     * @return true if the project exists already or was successfully created
     */
    private boolean makeNewDirectory(String fileLoc) {
        File theDir = new File(fileLoc);
        boolean result = true;
        // if the directory does not exist, create it
        if (!theDir.exists()) {
            result = false;
            try {
                result = createDrive(fileLoc) && theDir.mkdir();
            } catch (SecurityException se) {
                se.printStackTrace();
            }
        }
        return result;
    }

    /**
     * Creates the current date and time with the format YYYYMMDD'T'HHmmss as
     * a string
     *
     * @return string value of date
     */
    private String getDateTimeString() {
        Calendar cal = Calendar.getInstance();
        String year = "" + cal.get(Calendar.YEAR);
        String month = "" + (cal.get(Calendar.MONTH) + 1);
        String day = "" + cal.get(Calendar.DAY_OF_MONTH);
        String hour = "" + cal.get(Calendar.HOUR_OF_DAY);
        String minute = "" + cal.get(Calendar.MINUTE);
        String second = "" + cal.get(Calendar.SECOND);
        if (cal.get(Calendar.MONTH) < 10) {
            month = "0" + (cal.get(Calendar.MONTH) + 1);
        }
        if (cal.get(Calendar.DAY_OF_MONTH) < 10) {
            day = "0" + cal.get(Calendar.DAY_OF_MONTH);
        }
        if (cal.get(Calendar.HOUR_OF_DAY) < 10) {
            hour = "0" + cal.get(Calendar.HOUR_OF_DAY);
        }
        if (cal.get(Calendar.MINUTE) < 10) {
            minute = "0" + cal.get(Calendar.MINUTE);
        }
        if (cal.get(Calendar.SECOND) < 10) {
            second = "0" + cal.get(Calendar.SECOND);
        }
        return year + month + day + "T" + hour + minute + second;
    }

    /**
     * Creates a network drive connecting sandbox to computer.
     * TODO: Add user input for networkLocation
     *
     * @param fileLoc location of folder being saved to, has drive for first
     *                two characters and proper path to file
     * @return true if drive is created successful
     */
    private boolean createDrive(String fileLoc) {
        return createDrive(fileLoc, " https:\\\\larced.spstg.jsc.nasa" +
                ".gov\\sites\\EDM\\seemb\\sandbox");
    }

    /**
     * Creates a network drive connecting sandbox to computer.
     * TODO: Add user input for networkLocation
     *
     * @param fileLoc location of folder being saved to, has drive for first
     *                two characters and proper path to file
     * @return true if drive is created successful
     */
    private boolean createDrive(String fileLoc, String networkLocation) {
        // Grabs the first two letters of fileLoc
        String driveName = fileLoc.length() < 2 ? fileLoc : fileLoc.substring(0,
                2);
        File drive = new File(driveName);
        boolean success = true;
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
                success = p.exitValue() == 0;
                p.destroy();
                System.out.println("Connection " + (success ? "Successful!" :
                        "Failed."));
            } catch (Exception e) {
                success = false;
                e.printStackTrace();
            }
        }
        else {
            System.out.println("Drive Found");
        }
        return success;
    }
}