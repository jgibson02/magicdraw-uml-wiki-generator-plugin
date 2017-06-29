package wikigeneratorplugin;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.core.Project;
import com.nomagic.magicdraw.core.project.ProjectEventListener;
import com.nomagic.magicdraw.uml.ExtendedPropertyNames;
import com.nomagic.magicdraw.uml.symbols.DiagramListenerAdapter;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;
import com.nomagic.magicdraw.uml.symbols.shapes.DiagramFrameView;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;

/**
 * Author: Kareem Abdol-Hamid kkabdolh
 * Version: 6/27/2017
 */
public class ProjectListener implements ProjectEventListener {

    private static final String DESTINATION = "s:\\SiteAssets\\";
    private HashSet<DiagramPresentationElement> dirtyDiagrams;
    private String fileLoc;

    ProjectListener(HashSet<DiagramPresentationElement> dirtyDiagrams) {
        this.dirtyDiagrams = dirtyDiagrams;
    }

    @Override
    public void projectOpened(final Project project) {
        fileLoc = DESTINATION + project.getName() + "_DIAGRAMS";
        Application.getInstance().addSaveParticipant(new
                OnSaveListener(dirtyDiagrams, fileLoc));
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
    //============================================================
    // Unused Methods
    //============================================================
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
}
