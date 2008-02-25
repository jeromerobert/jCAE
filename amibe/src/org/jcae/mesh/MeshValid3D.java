/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2005, by EADS CRC
    Copyright (C) 2007,2008, by EADS France

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

import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshExporter;
import org.jcae.mesh.amibe.validation.*;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * This class illustrates how to perform quality checks.
 */
public class MeshValid3D
{
	private static Logger logger=Logger.getLogger(MeshValid3D.class.getName());

	private static void check(String brepfilename, String xmlDir, float discr, float defl)
	{
		logger.info("Reading 3D mesh");
		Mesh mesh3D = new Mesh();
		try
		{
			MeshReader.readObject3D(mesh3D, xmlDir);
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		//MinLengthFace qproc = new MinLengthFace();
		MinAngleFace qproc = new MinAngleFace();
		QualityFloat data = new QualityFloat(1000);
		data.setQualityProcedure(qproc);
		for (Triangle f: mesh3D.getTriangles())
			data.compute(f);
		data.finish();
		data.setTarget((float) Math.PI/3.0f);
		//data.setTarget((float) discr);
		//data.split(10);
		data.split(0.0f, 1.0f, 10);
		data.printLayers();
		String bbfile = brepfilename.substring(0, brepfilename.lastIndexOf('.'))+".bb";
		data.printMeshBB(bbfile);
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
		String MESHName=filename.substring(0, filename.lastIndexOf('.'))+".mesh";
		new MeshExporter.MESH(xmlDir).write(MESHName);
	}
}
