package org.jcae.netbeans.viewer3d.actions;

import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jcae.viewer3d.View;
import org.jcae.viewer3d.ViewBehavior;


public class ClipRectangleAction extends AbstractViewAction
{
	private static ImageIcon icon = new ImageIcon(ClipRectangleAction.class.getResource("stock_crop.png"));
	/**
	 * 
	 */
	public ClipRectangleAction()
	{						
		putValue(Action.NAME, "Create a clip rectangle");
		putValue(Action.SHORT_DESCRIPTION, "Create a clip rectangle");
		putValue(Action.SMALL_ICON, icon);
		setIcon(icon);
	}
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(View view)
	{
		view.setMouseMode(ViewBehavior.CLIP_RECTANGLE_MODE);
	}				
}
