package org.jcae.netbeans.viewer3d.actions;

import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jcae.netbeans.viewer3d.actions.AbstractViewAction;
import org.jcae.viewer3d.View;
import org.jcae.viewer3d.ViewBehavior;


public class ClipBoxAction extends AbstractViewAction
{
	private static ImageIcon icon = new ImageIcon(ClipBoxAction.class.getResource("stock_3d-all-attributes.png"));

	/**
	 * 
	 */
	public ClipBoxAction()
	{						
		putValue(Action.NAME, "Create a clip box");
		putValue(Action.SHORT_DESCRIPTION, "Create a clip box");
		putValue(SMALL_ICON, icon);
		setIcon(icon);
	}
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(View view)
	{
		view.setMouseMode(ViewBehavior.CLIP_BOX_MODE);
	}				
}