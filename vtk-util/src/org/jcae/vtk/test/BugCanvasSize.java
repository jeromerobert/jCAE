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

package org.jcae.vtk.test;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import org.jcae.vtk.Utils;
import vtk.vtkCanvas;

public class BugCanvasSize
{
	public static void main(String[] args)
	{		
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JSplitPane split = new JSplitPane();
		vtkCanvas canvas = new vtkCanvas()
		{			
			public void setSize(int x, int y)
			{
				super.setSize(x, y);
				System.out.println("Someone has changed my size to "+x+" "+y);
				System.out.println("prefered and min size are now: "+
					getPreferredSize()+" "+getMinimumSize());
			}				
		};
		//Uncomment: Workaround for not resizable vtkCanvas bug.
		//canvas.setMinimumSize(new Dimension(0, 0));
		//canvas.setPreferredSize(new Dimension(0, 0));

		split.add(canvas, JSplitPane.BOTTOM);
		canvas.GetRenderer().AddActor(Utils.createDummyActor());
		frame.add(split);
		frame.setSize(800, 600);
		frame.setVisible(true);
	}
}
