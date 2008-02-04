package org.jcae.netbeans.viewer3d.actions;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import org.jcae.netbeans.viewer3d.View3D;
import org.jcae.netbeans.viewer3d.View3DManager;
import org.jcae.netbeans.viewer3d.actions.AbstractViewAction;
import org.jcae.viewer3d.ScreenshotListener;
import org.jcae.viewer3d.View;
import org.openide.windows.WindowManager;


public class SnapshotAction extends AbstractViewAction
{
	private static final String LABEL = "Snapshot";
	private static ImageIcon icon = new ImageIcon(SnapshotAction.class.getResource("snapshot.png"));	

	/**
	 * @param view
	 */
	public SnapshotAction()
	{
		putValue(Action.NAME, LABEL);
		putValue(Action.SHORT_DESCRIPTION, LABEL);
		putValue(Action.SMALL_ICON, icon);
		setIcon(icon);
	}

	private static final FileFilter PNG_FILE_FILTER = new FileFilter()
		{
		public boolean accept(File f)
		{
			return f.getName().endsWith(".png");
		}
		public String getDescription()
		{
			return "Portable Network Graphics (*.png)";
		}
		};
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(View view)
	{
		View3D v = View3DManager.getDefault().getSelectedView3D();
		if (v!=null)
		{
			final JFileChooser fc = new JFileChooser();

			if (fc.showSaveDialog(WindowManager.getDefault().getMainWindow())
					 == JFileChooser.APPROVE_OPTION)
			{				
				ScreenshotListener ss = new ScreenshotListener()
					{
						public void shot(BufferedImage snapShot)
						{
							try
							{					
								ImageIO.write(snapShot, "png", fc.getSelectedFile());
							}
							catch (IOException e1)
							{
								e1.printStackTrace();
							}				
						}
					};

				v.getView().takeScreenshot(ss);		    	
			}			
		}
	}		
}