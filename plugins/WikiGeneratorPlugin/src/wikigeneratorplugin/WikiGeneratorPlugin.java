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
    private String fileLoc;

    @Override
    public void init() {
        final HashSet<DiagramPresentationElement> dirtyDiagrams = new HashSet<>();
        WikiGenProjectListener projListener= new WikiGenProjectListener(dirtyDiagrams);
        Application.getInstance().getProjectsManager().addProjectListener(projListener);
    }

    @Override
    public boolean close() {
        return true;
    }

    @Override
    public boolean isSupported() {
        return true;
    }
}
