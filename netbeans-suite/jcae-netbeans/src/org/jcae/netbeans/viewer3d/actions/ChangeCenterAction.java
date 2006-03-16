package org.jcae.netbeans.viewer3d.actions;

import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jcae.netbeans.viewer3d.actions.AbstractViewAction;
import org.jcae.viewer3d.View;


public class ChangeCenterAction extends AbstractViewAction
{
	private static ImageIcon icon = new ImageIcon(ChangeCenterAction.class.getResource("reload.png"));
	
	/**
	 *
	 */
	public ChangeCenterAction()
	{
		putValue(Action.NAME, "Rotation center");
		putValue(Action.SHORT_DESCRIPTION, "Set rotation center");
		putValue(Action.SMALL_ICON, icon);
		setIcon(icon);
	}
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(View view)
	{
		view.setChangeRotationCenter(true);
	}
}