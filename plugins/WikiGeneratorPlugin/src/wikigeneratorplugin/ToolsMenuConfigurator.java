package wikigeneratorplugin;

import com.nomagic.actions.AMConfigurator;
import com.nomagic.actions.ActionsCategory;
import com.nomagic.actions.ActionsManager;
import com.nomagic.magicdraw.actions.ActionsID;
import com.nomagic.magicdraw.actions.MDAction;

/**
 * Author: Kareem Abdol-Hamid kkabdolh
 * Version: 6/29/2017
 *
 * Adds action to the tools menu
 */
public class ToolsMenuConfigurator implements AMConfigurator {
    /**
     * Action will be added to manager.
     */
    private MDAction action;

    /**
     * Creates configurator.
     * @param action action to be added to main menu.
     */
    ToolsMenuConfigurator(MDAction action)
    {
        this.action = action;
    }

    /**
     *  Methods adds action to given manager Tools category
     */
    @Override
    public void configure(ActionsManager manager)
    {
        // searching for Tools action category
        ActionsCategory category = (ActionsCategory) manager.getActionFor
                (ActionsID.TOOLS);
        if( category != null)
        {
            // adding action to found category.
            category.addAction(action);
        }
    }

    /**
     * Set default property
     * @return MEDIUM_PRIORITY
     */
    @Override
    public int getPriority()
    {
        return AMConfigurator.MEDIUM_PRIORITY;
    }
}
