package wikigeneratorplugin;

import com.nomagic.actions.AMConfigurator;
import com.nomagic.actions.ActionsCategory;
import com.nomagic.actions.ActionsManager;
import com.nomagic.magicdraw.actions.ActionsID;
import com.nomagic.magicdraw.actions.MDAction;

/**
 * Author: Kareem Abdol-Hamid kkabdolh
 * Version: 6/29/2017
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
    public ToolsMenuConfigurator(MDAction action)
    {
        this.action = action;
    }

    /**
     * @see com.nomagic.actions.AMConfigurator#configure(ActionsManager)
     *  Methods adds action to given manager Help category
     */
    @Override
    public void configure(ActionsManager manager)
    {
        // searching for Help action category
        ActionsCategory category = (ActionsCategory)manager.getActionFor
                (ActionsID.TOOLS);
        if( category != null)
        {
            // adding action to found category.
            category.addAction(action);
        }
    }
    @Override
    public int getPriority()
    {
        return AMConfigurator.MEDIUM_PRIORITY;
    }
}
