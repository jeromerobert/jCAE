package org.jcae.netbeans.viewer3d.actions;

import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jcae.netbeans.viewer3d.PositionManager;
import org.jcae.viewer3d.View;


public class GoToAction extends AbstractViewAction
{
	private static ImageIcon icon = new ImageIcon(GoToAction.class.getResource("im-aim.png"));	
	/**
	 * @param view
	 */
	public GoToAction()
	{			
		putValue(Action.NAME, "Go To");
		putValue(Action.SHORT_DESCRIPTION, "Go To");
		putValue(Action.SMALL_ICON, icon);
		setIcon(icon);
	}
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(View view)
	{
		PositionManager.getDefault().goTo(view);	
	}		
}