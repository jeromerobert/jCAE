/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2005, by EADS CRC
    Copyright (C) 2007, by EADS France

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

import org.jcae.mesh.oemm.*;
import org.jcae.mesh.cad.CADShape;
import org.jcae.mesh.cad.CADShapeBuilder;
import org.apache.log4j.Logger;
import java.io.File;

/**
 * This class illustrates how to perform quality checks.
 */
public class MeshOEMMIndex
{
	private static Logger logger=Logger.getLogger(MeshOEMMIndex.class);

	private static void check(String brepfilename, int lmax, int triangles_max, String soupDir, String outDir)
	{
		logger.info("Reading "+brepfilename);
		CADShapeBuilder factory = CADShapeBuilder.factory;
		CADShape shape = factory.newShape(brepfilename);
		double [] bbox = shape.boundingBox();
		final OEMM oemm = new OEMM(lmax);
		oemm.setBoundingBox(bbox);
		String soupFile = soupDir+File.separator+"soup";
		if (!RawStorage.countTriangles(oemm, soupFile))
		{
			// Bounding box was invalid and has been fixed
			// in RawStorage.countTriangles(), we need to
			// count triangles against the new OEMM.
			logger.info("Invalid bounding box has been detected");
			if (!RawStorage.countTriangles(oemm, soupFile))
				throw new RuntimeException("Fatal error... aborting");
		}
		Aggregate.compute(oemm, triangles_max);
		RawStorage.dispatch(oemm, soupFile, "dispatched", "dispatched.data");
		RawStorage.indexOEMM("dispatched", outDir);
		logger.info("End processing");
	}
	
	/**
	 * main method, reads 2 arguments and calls mesh() method
	 * @param args  an array of String, filename, algorithm type and constraint value
	 */
	public static void main(String args[])
	{
		if (args.length < 4)
		{
			System.out.println("Usage: MeshOEMMIndex level_max tri_max outDir brep soupDir");
			System.exit(0);
		}
		Integer lmax = new Integer(args[0]);
		Integer triangles_max = new Integer(args[1]);
		String outDir=args[2];
		String filename=args[3];
		String soupDir=args[4];
		check(filename, lmax.intValue(), triangles_max.intValue(), soupDir, outDir);
	}
}
