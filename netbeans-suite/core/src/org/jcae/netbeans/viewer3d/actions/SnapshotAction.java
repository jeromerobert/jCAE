/*
 * Project Info:  http://jcae.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * (C) Copyright 2008, by EADS France
 */

package org.jcae.netbeans.viewer3d.actions;

import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import org.jcae.vtk.Utils;
import org.jcae.vtk.View;
import org.openide.windows.WindowManager;


public class SnapshotAction extends ViewAction
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

	@Override
	protected boolean asynchronous()
	{
		return false;
	}
	
	
	public void actionPerformed(View view)
	{
			final JFileChooser fc = new JFileChooser();

			fc.setFileFilter(PNG_FILE_FILTER);
			if (fc.showSaveDialog(WindowManager.getDefault().getMainWindow())
					 == JFileChooser.APPROVE_OPTION)
			{	
				try {
				ImageIO.write(Utils.takeScreenshot(view), "png", fc.getSelectedFile());
				} catch(Exception e)
				{
					System.err.println("Exception : " + e.getLocalizedMessage());
				}
			}	
	}
}