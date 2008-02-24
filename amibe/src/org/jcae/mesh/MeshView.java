/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2003,2004,2005, by EADS CRC

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */


package org.jcae.mesh;

import javax.media.j3d.BranchGroup;
import org.jcae.mesh.java3d.*;
import java.util.logging.Logger;

/**
 * This class MeshView allows to view a generated mesh.
 */
public class MeshView
{
	private static Logger logger=Logger.getLogger(MeshView.class.getName());

	public static void main(String args[])
	{
		if (args.length != 1)
		{
			System.out.println("Usage : MeshView directory");
			System.exit(0);
		}
		String xmlDir = args[0];
		try
		{
			String xmlFile = "jcae3d";
			ComputeEdgesConnectivity computeEdgesConnectivity =
				new ComputeEdgesConnectivity(xmlDir, xmlFile);
			computeEdgesConnectivity.compute();
			
			XMLBranchGroup xbg=new XMLBranchGroup(xmlDir, xmlFile);
			xbg.parseXML();
			Viewer v=new Viewer();
			BranchGroup bg=xbg.getBranchGroup(0);
			logger.info("Bounding sphere of the mesh: "+bg.getBounds());
			v.addBranchGroup(bg);
			bg=xbg.getBranchGroup("FreeEdges");
			v.addBranchGroup(bg);
			bg=xbg.getBranchGroup("MultiEdges");
			v.addBranchGroup(bg);
			v.setVisible(true);
			v.zoomTo();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
