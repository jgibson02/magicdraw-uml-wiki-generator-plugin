package wikigeneratorplugin;

import com.nomagic.magicdraw.actions.ActionsConfiguratorsManager;
import com.nomagic.magicdraw.actions.MDAction;
import com.nomagic.magicdraw.core.Application;
import com.nomagic.magicdraw.plugins.Plugin;
import com.nomagic.magicdraw.uml.symbols.DiagramPresentationElement;

import java.util.HashSet;

/**
 * Author: Kareem Abdol-Hamid kkabdolh
 * Version: 6/29/2017
 *
 *  Main plugin class, this runs when MagicDraw is initially started up. Adds
 * a new menu option to Tools for configuring settings of the plugin and sets
 * up a new project listener that will activate when the project is saved and
 * when a diagram is edited.
 */
public class WikiGeneratorPlugin extends Plugin {

    /**
     * When the project originally initiates and adds a menu option to Tools
     * and a projListener to do the functionality of the plugin
     * @see ProjectListener for more details on what the plugin does
     */
    @Override
    public void init() {
        ActionsConfiguratorsManager manager = ActionsConfiguratorsManager.getInstance();
        MDAction action = new ToolsMenuAction();
        manager.addMainMenuConfigurator(new ToolsMenuConfigurator(action));
        ProjectListener projListener= new ProjectListener();
        Application.getInstance().getProjectsManager().addProjectListener(projListener);
    }

    /**
     * Does not need to check
     * @return true always
     */
    @Override
    public boolean close() {
        return true;
    }

    /**
     * No requirements for being supported
     * @return true always
     */
    @Override
    public boolean isSupported() {
        return true;
    }
}
