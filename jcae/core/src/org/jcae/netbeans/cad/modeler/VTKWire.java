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
 * (C) Copyright 2009, by EADS France
 */

package org.jcae.netbeans.cad.modeler;

import javax.swing.SwingUtilities;
import org.jcae.vtk.Utils;
import vtk.vtkActor;
import vtk.vtkPanel;
import vtk.vtkPolyData;
import vtk.vtkPolyDataMapper;

/**
 *
 * @author Jerome Robert
 */
public class VTKWire {
	private vtkActor actor;
	private vtkPanel panel;
	public VTKWire(vtkPanel panel)
	{
		this.panel = panel;
	}
	
	public void hide()
	{
		if(actor != null)
		{
			panel.lock();
			panel.GetRenderer().RemoveActor(actor);
			actor = null;
			unlockAndRender();
		}
	}


	private static int[] createPolylineIndex(int nbEdges)
	{
		int[] indicies = new int[3*nbEdges];
		for(int i=0; i<nbEdges; i++)
		{
			indicies[3*i+0]=2;
			indicies[3*i+1]=i;
			indicies[3*i+2]=i+1;
		}
		return indicies;
	}

	private void unlockAndRender()
	{
		if(panel.isWindowSet())
			panel.GetRenderWindow().Render();
		panel.unlock();
		if(!panel.isWindowSet())
		{
			if(!SwingUtilities.isEventDispatchThread())
				throw new IllegalStateException();
			panel.Render();
		}
	}

	public void showWire(double[] coords) {
		vtkPolyData vpd;
		if(coords.length > 0)
		{
			panel.lock();
			if(actor == null)
			{
				vtkPolyDataMapper vdm = new vtkPolyDataMapper();
				actor = new vtkActor();
				actor.SetMapper(vdm);
				vpd = new vtkPolyData();
				vdm.SetInput(vpd);
				panel.GetRenderer().AddActor(actor);
			} else
				vpd = ((vtkPolyDataMapper)actor.GetMapper()).GetInput();

			vpd.SetPoints(Utils.createPoints(coords));
			int nbEdges = coords.length/3-1;
			vpd.SetLines(Utils.createCells(nbEdges, createPolylineIndex(nbEdges)));
			unlockAndRender();
		}
	}  
}
