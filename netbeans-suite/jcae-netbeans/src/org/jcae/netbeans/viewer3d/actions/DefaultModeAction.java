package org.jcae.netbeans.viewer3d.actions;

import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jcae.netbeans.viewer3d.actions.AbstractViewAction;
import org.jcae.viewer3d.View;
import org.jcae.viewer3d.ViewBehavior;


public class DefaultModeAction extends AbstractViewAction
{
	private static ImageIcon icon = new ImageIcon(DefaultModeAction.class.getResource("stock_draw-selection.png"));
	/**
	 * 
	 */
	public DefaultModeAction()
	{						
		putValue(Action.NAME, "Restore default viewer mode");
		putValue(Action.SHORT_DESCRIPTION, "Restore default viewer mode");
		putValue(Action.SMALL_ICON, icon);
		setIcon(icon);
	}
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(View view)
	{
		view.setMouseMode(ViewBehavior.DEFAULT_MODE);
	}				
}
