package wikigeneratorplugin;

import com.nomagic.magicdraw.actions.ActionsConfiguratorsManager;
import com.nomagic.magicdraw.actions.MDAction;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.plugins.Plugin;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;

import java.util.HashSet;

public class WikiGeneratorPlugin extends Plugin {

    @Override
    public void init() {
        ActionsConfiguratorsManager manager = ActionsConfiguratorsManager.getInstance();
        MDAction action = new ToolsMenuAction();
        manager.addMainMenuConfigurator( new ToolsMenuConfigurator( action ) );
        final HashSet<DiagramPresentationElement> dirtyDiagrams = new HashSet<>();
        ProjectListener projListener= new ProjectListener(dirtyDiagrams);
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
