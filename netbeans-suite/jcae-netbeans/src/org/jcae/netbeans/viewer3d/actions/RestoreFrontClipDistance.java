package org.jcae.netbeans.viewer3d.actions;

import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jcae.netbeans.viewer3d.actions.AbstractViewAction;
import org.jcae.viewer3d.View;


public class RestoreFrontClipDistance extends AbstractViewAction
{
	private static ImageIcon icon = new ImageIcon(RestoreFrontClipDistance.class.getResource("redo.png"));
	/**
	 * 
	 */
	public RestoreFrontClipDistance()
	{
		putValue(Action.NAME, "Restore front clip distance");
		putValue(Action.SHORT_DESCRIPTION, "Restore front clip distance");
		putValue(Action.SMALL_ICON, icon);
		setIcon(icon);
	}
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(View view)
	{
		view.restoreFrontClipDistance();
		
	}
}