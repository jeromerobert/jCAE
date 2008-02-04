package org.jcae.netbeans.viewer3d.actions;

import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jcae.netbeans.viewer3d.actions.AbstractViewAction;
import org.jcae.viewer3d.View;


public class RemoveModelClipAction extends AbstractViewAction
{
	private static ImageIcon icon = new ImageIcon(RemoveModelClipAction.class.getResource("remove.png"));

	/**
	 * 
	 */
	public RemoveModelClipAction()
	{						
		putValue(Action.NAME, "Remove clip rectangle or clip box");
		putValue(Action.SHORT_DESCRIPTION, "Remove clip rectangle or clip box");
		putValue(Action.SMALL_ICON, icon);
		setIcon(icon);
	}
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(View view)
	{
		view.removeModelClip();
	}
}