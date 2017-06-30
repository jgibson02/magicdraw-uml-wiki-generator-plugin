package wikigeneratorplugin;

import com.nomagic.actions.NMAction;
import com.nomagic.magicdraw.actions.MDAction;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Author: Kareem Abdol-Hamid kkabdolh
 * Version: 6/29/2017
 */
public class ToolsMenuAction extends MDAction {

    /**
     * Creates action with name "ExampleAction", and with key stroke E +CTRL+SHIFT
     */
    ToolsMenuAction()
    {
        super( "sppluginoptions", "SharePoint Plugin Options",  KeyStroke.getKeyStroke( KeyEvent.VK_E, NMAction.MENU_SHORTCUT_MASK+KeyEvent.SHIFT_MASK), null);
    }

    /**
     */
    public ToolsMenuAction(String id, String name, KeyStroke stroke, String group)
    {
        super(id, name, stroke, group);
    }

    /**
     * Method is called when action should be performed. Showing simple message.
     * @param e event causes action call.
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        new ConfigurationPopupMenu();
    }
}
