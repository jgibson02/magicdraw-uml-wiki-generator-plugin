package wikigeneratorplugin;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.SaveParticipant;
import com.nomagic.magicdraw.core.project.ProjectDescriptor;
import com.nomagic.magicdraw.core.project.ProjectEventListener;
import com.nomagic.magicdraw.export.image.ImageExporter;
import com.nomagic.magicdraw.plugins.Plugin;
import com.nomagic.magicdraw.uml.ExtendedPropertyNames;
import com.nomagic.magicdraw.uml.symbols.DiagramListenerAdapter;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.magicdraw.uml.symbols.shapes.DiagramFrameView;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

public class WikiGeneratorPlugin extends Plugin {
    public static boolean initialized;
    private static final String DESTINATION = "s:\\SiteAssets\\";
    private String fileLoc;

    @Override
    public void init() {
        final HashSet<DiagramPresentationElement> dirtyDiagrams = new HashSet<>();
        initialized = true;
        Application.getInstance().getProjectsManager().addProjectListener(new ProjectEventListener() {
            @Override
            public void projectOpened(Project project) {
                setFileLoc(DESTINATION + project.getName() + "_DIAGRAMS");
                DiagramListenerAdapter adapter = new DiagramListenerAdapter(new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        String propertyName = evt.getPropertyName();
                        switch (propertyName) {
                            case ExtendedPropertyNames.VIEW_UPDATED:
                                DiagramFrameView diagramFrameView = (DiagramFrameView) evt.getNewValue();
                                dirtyDiagrams.add(diagramFrameView.getDiagramPresentationElement());
                                break;
                            case ExtendedPropertyNames.DIAGRAM_DELETED: // Doesn't work, but we should check on save anyway. Will handle on Tuesday
                                String fileName = fileLoc + '\\' + ((DiagramFrameView) evt.getNewValue()).getDiagramPresentationElement().getDiagram().getName() + ".svg";
                                (new File(fileName)).delete();
                                System.out.println("Deleting " + fileName);
                                break;
                        }
                    }
                });
                adapter.install(project);
            }

            @Override
            public void projectClosed(Project project) {

            }

            @Override
            public void projectSaved(Project project, boolean b) {

            }

            @Override
            public void projectActivated(Project project) {

            }

            @Override
            public void projectDeActivated(Project project) {

            }

            @Override
            public void projectReplaced(Project project, Project project1) {

            }

            @Override
            public void projectCreated(Project project) {

            }

            @Override
            public void projectPreClosed(Project project) {

            }

            @Override
            public void projectPreClosedFinal(Project project) {

            }

            @Override
            public void projectPreSaved(Project project, boolean b) {

            }

            @Override
            public void projectPreActivated(Project project) {

            }

            @Override
            public void projectPreDeActivated(Project project) {

            }

            @Override
            public void projectPreReplaced(Project project, Project project1) {

            }

            @Override
            public void projectOpenedFromGUI(Project project) {

            }

            @Override
            public void projectActivatedFromGUI(Project project) {

            }
        });

        class WikiUpdateParticipant implements SaveParticipant {

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
                Collection<DiagramPresentationElement> diagrams = project.getDiagrams();
                for (DiagramPresentationElement dpe : diagrams) {
                    if (new File(getFileLoc() + '\\' + dpe.getDiagram().getName() + ".svg").exists() == false)
                        dirtyDiagrams.add(dpe);
                }
                boolean b = makeNewDirectory(getFileLoc());
                exportDiagrams(dirtyDiagrams, getFileLoc());
                dirtyDiagrams.clear();
            }
        }

        Application.getInstance().addSaveParticipant(new WikiUpdateParticipant());
    }

    @Override
    public boolean close() {
        return true;
    }

    @Override
    public boolean isSupported() {
        return true;
    }

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
        double total = (double) diagrams.size();
        if (total > 0)
            System.out.println("0% Complete");
        Iterator<DiagramPresentationElement> itr = diagrams.iterator();
        while (itr.hasNext()) {
            DiagramPresentationElement dpe = itr.next();
            File img = new File(fileLoc + '\\' + dpe.getDiagram().getName() + ".svg");
            System.out.println("Exporting " + dpe.getDiagram().getName());
            try {
                ImageExporter.export(dpe, ImageExporter.SVG, img);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //dpe.close();
            count++;
            System.out.println((int) Math.round(((count / total) * 100)) + "% " + "Complete");
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
    private boolean createDrive(String fileLoc) {
        return createDrive(fileLoc, " https:\\\\larced.spstg.jsc.nasa.gov\\sites\\EDM\\seemb\\sandbox");
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
        } else {
            System.out.println("Drive Found");
        }
        return success;
    }

    public String getFileLoc() {
        return fileLoc;
    }

    public void setFileLoc(String fileLoc) {
        this.fileLoc = fileLoc;
    }
}
