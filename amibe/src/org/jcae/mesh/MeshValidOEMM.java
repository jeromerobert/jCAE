/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2006 EADS CRC

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

import java.util.Iterator;
import java.io.File;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.oemm.*;
import org.jcae.mesh.cad.CADShape;
import org.jcae.mesh.cad.CADShapeBuilder;
import org.apache.log4j.Logger;

/**
 * This class illustrates how to perform quality checks.
 */
public class MeshValidOEMM
{
	private static Logger logger=Logger.getLogger(MeshValidOEMM.class);

	private static void check(String brepfilename, String xmlDir, float discr, float defl)
	{
		logger.info("Reading triangle soup");

		CADShapeBuilder factory = CADShapeBuilder.factory;
		CADShape shape = factory.newShape(brepfilename);
		double [] bbox = shape.boundingBox();
		double [] umax = new double[3];
		for (int i = 0; i < 3; i++)
			umax[i] = bbox[i+3];
		String rawMesh = xmlDir+File.separator+"soup";
		final RawOEMM oemm = new RawOEMM(rawMesh, 1, bbox, umax);

		final int split = 10;
		final int maxgroup = 5000;
		int [][] nr = new int[maxgroup][split+1];
		double [] amin = new double[maxgroup];
		int nrgroup = 0;
		for (int i = 0; i < maxgroup; i++)
			amin[i] = 1.0;
		for (Iterator itf = RawStorage.getFacesIterator(oemm); itf.hasNext();)
		{
			double [] xyz = (double []) itf.next();
			Vertex n1 = Vertex.valueOf(xyz[0], xyz[1], xyz[2]);
			Vertex n2 = Vertex.valueOf(xyz[3], xyz[4], xyz[5]);
			Vertex n3 = Vertex.valueOf(xyz[6], xyz[7], xyz[8]);
			double a1 = Math.abs(n1.angle3D(n2, n3));
			double a2 = Math.abs(n2.angle3D(n3, n1));
			double a3 = Math.abs(n3.angle3D(n1, n2));
			int group = (int) xyz[9];
			if (group > nrgroup)
				nrgroup = group;
			if (a2 < a1)
				a1 = a2;
			if (a3 < a1)
				a1 = a3;
			a1 *= 3.0 / Math.PI;
			if (a1 < amin[group])
				amin[group] = a1;
			int index = (int) (a1 * (double) split);
			nr[group][index]++;
		}
		double amintot = amin[1];
		for (int g = 1; g <= nrgroup; g++)
		{
			System.out.println("Group "+g+" Minimum angle: "+(60.0*amin[g]));
			if (amin[g] < amintot)
				amintot = amin[g];
			for (int i = 0; i < split; i++)
				System.out.println("Slice: "+i+" "+nr[g][i]);
			if (g > 1)
				for (int i = 0; i < split; i++)
					nr[1][i] += nr[g][i];
		}
		System.out.println("Minimum angle: "+(60.0*amintot));
		for (int i = 0; i < split; i++)
			System.out.println("Slice: "+i+" "+nr[1][i]);
	}

	/**
	 * main method, reads 2 arguments and calls mesh() method
	 * @param args  an array of String, filename, algorithm type and constraint value
	 */
	public static void main(String args[])
	{
		if (args.length < 2)
		{
			System.out.println("Usage : MeshValid brep directory");
			System.exit(0);
		}
		String filename=args[0];
		String xmlDir = args[1];
		Float discr = new Float(args[2]);
		Float defl = new Float(args[3]);
		check(filename, xmlDir, discr.floatValue(), defl.floatValue());
	}
}
