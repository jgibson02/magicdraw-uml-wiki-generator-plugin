package wikigeneratorplugin;

import com.nomagic.actions.NMAction;
import com.nomagic.magicdraw.actions.MDAction;
import com.nomagic.magicdraw.core.Application;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Author: Kareem Abdol-Hamid kkabdolh
 * Version: 6/29/2017
 *
 * Calls a new configuration popup menu and is only active when a project is
 * open
 */
public class ToolsMenuAction extends MDAction {

    /**
     * Creates action with name "SharePoint Plugin Options", and with key
     * stroke E +CTRL+SHIFT
     */
    ToolsMenuAction()
    {
        super( "sppluginoptions", "SharePoint Plugin Options",  KeyStroke.getKeyStroke( KeyEvent.VK_E, NMAction.MENU_SHORTCUT_MASK+KeyEvent.SHIFT_MASK), null);
    }


    /**
     * Calls ConfigurationPopupMenu.
     * @param e event causes action call.
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        new ConfigurationPopupMenu();
    }

    /**
     * Makes action only available when a project has been opened
     */
    @Override
    public void updateState() {
        setEnabled(Application.getInstance()
                .getProject() != null);
    }
}
