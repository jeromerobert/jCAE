package org.jcae.netbeans.viewer3d.actions;

import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jcae.netbeans.viewer3d.PositionManager;
import org.jcae.netbeans.viewer3d.actions.AbstractViewAction;
import org.jcae.viewer3d.View;


public class SavePositionAction extends AbstractViewAction
{
	private static ImageIcon icon = new ImageIcon(SavePositionAction.class.getResource("attach.png"));	

	/**
	 * @param view
	 */
	public SavePositionAction()
	{			
		putValue(Action.NAME, "Save position");
		putValue(Action.SHORT_DESCRIPTION, "Save position");
		putValue(Action.SMALL_ICON, icon);
		setIcon(icon);
	}
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(View view)
	{
		PositionManager.getDefault().savePosition(view);
	}		
}