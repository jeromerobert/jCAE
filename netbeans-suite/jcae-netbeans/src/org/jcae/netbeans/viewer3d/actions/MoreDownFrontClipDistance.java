package org.jcae.netbeans.viewer3d.actions;

import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jcae.netbeans.viewer3d.actions.AbstractViewAction;
import org.jcae.viewer3d.View;


public class MoreDownFrontClipDistance extends AbstractViewAction
{
	private static ImageIcon icon = new ImageIcon(DownFrontClipDistance.class.getResource("down_more.png"));
	/**
	 * 
	 */
	public MoreDownFrontClipDistance()
	{						
		putValue(Action.NAME, "Decrease front clip distance faster");
		putValue(Action.SHORT_DESCRIPTION, "Decrease front clip distance faster");
		putValue(Action.SMALL_ICON, icon);
		setIcon(icon);
	}
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(View view)
	{
		
		double back = view.getBackClipDistance();
		double current = view.getFrontClipDistance();
		double step = back/20;
		double value = Math.max(current-step,
					back/View.BackClipDistanceFactor*View.FrontClipDistanceFactor);
		view.setFrontClipDistance(value);
	}				
}