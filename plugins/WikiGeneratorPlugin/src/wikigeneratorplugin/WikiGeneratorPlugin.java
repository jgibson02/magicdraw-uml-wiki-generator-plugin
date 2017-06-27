package wikigeneratorplugin;

import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.plugins.Plugin;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;

import java.util.HashSet;

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
