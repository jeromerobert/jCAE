package org.jcae.netbeans.viewer3d.actions;

import javax.swing.Action;
import javax.swing.ImageIcon;
import org.jcae.netbeans.viewer3d.actions.AbstractViewAction;
import org.jcae.viewer3d.View;


public class UpFrontClipDistance extends AbstractViewAction
{
	private static ImageIcon icon = new ImageIcon(UpFrontClipDistance.class.getResource("up.png"));
	/**
	 * 
	 */
	public UpFrontClipDistance()
	{
		putValue(Action.NAME, "Increase front clip distance");
		putValue(Action.SHORT_DESCRIPTION, "Increase front clip distance");
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
		double step = back/200;
		double value = Math.min(current+step,back);
		view.setFrontClipDistance(value);
	}				
}