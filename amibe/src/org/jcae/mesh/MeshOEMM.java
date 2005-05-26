/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2005
                  Jerome Robert <jeromerobert@users.sourceforge.net>

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

import org.jcae.mesh.oemm.RawOEMM;
import org.jcae.mesh.cad.CADShape;
import org.jcae.mesh.cad.CADShapeBuilder;
import org.apache.log4j.Logger;
import org.jcae.mesh.java3d.Viewer;

import javax.media.j3d.Appearance;
import javax.media.j3d.QuadArray;
import javax.media.j3d.LineArray;
import javax.media.j3d.PointArray;
import javax.media.j3d.Shape3D;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.PointAttributes;

/**
 * This class illustrates how to perform quality checks.
 */
public class MeshOEMM
{
	private static Logger logger=Logger.getLogger(MeshOEMM.class);

	private static void check(String brepfilename, int lmax, int triangles_max, boolean onlyLeaves)
	{
		logger.info("Reading "+brepfilename);
		CADShapeBuilder factory = CADShapeBuilder.factory;
		CADShape shape = factory.newShape(brepfilename);
		double [] bbox = shape.boundingBox();
		double [] umax = new double[3];
		for (int i = 0; i < 3; i++)
			umax[i] = bbox[i+3];
		final RawOEMM oemm = new RawOEMM("rawMesh", lmax, bbox, umax);
		oemm.countTriangles();
		oemm.aggregate(triangles_max, 3);
		oemm.dispatch("dispatched");
		final Viewer view=new Viewer();
		view.addBranchGroup(RawOEMM.loadIntermediate("dispatched").bgOctree(onlyLeaves));
		//view.addBranchGroup(oemm.bgOctree(onlyLeaves));
		view.zoomTo(); 
		view.show();
	}
	
	/**
	 * main method, reads 2 arguments and calls mesh() method
	 * @param args  an array of String, filename, algorithm type and constraint value
	 */
	public static void main(String args[])
	{
		if (args.length < 2)
		{
			System.out.println("Usage : MeshOEMM lmax brep");
			System.exit(0);
		}
		Integer lmax = new Integer(args[0]);
		Integer triangles_max = new Integer(args[1]);
		String filename=args[2];
		check(filename, lmax.intValue(), triangles_max.intValue(), true);
	}
}
