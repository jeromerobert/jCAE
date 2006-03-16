package org.jcae.netbeans.viewer3d.actions;

import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jcae.netbeans.viewer3d.actions.AbstractViewAction;
import org.jcae.viewer3d.View;


public abstract class ViewAction extends AbstractViewAction
{
	private byte orientation;

	public ViewAction(String label, String icon, byte orientation)
	{			
		label=label+" view";
		putValue(Action.NAME, label);
		putValue(Action.SHORT_DESCRIPTION, label);
		putValue(SMALL_ICON, new ImageIcon(ViewAction.class.getResource(icon)));
		setIcon(new ImageIcon(ViewAction.class.getResource(icon)));
		this.orientation=orientation;
	}
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(View view)
	{
		view.setOrientation(orientation);				
	}
}