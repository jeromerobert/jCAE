/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2003,2004,2005,2006, by EADS CRC
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
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.patch.Mesh2D;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.amibe.validation.*;
import org.jcae.mesh.cad.*;
import java.util.logging.Logger;

/**
 * This class MeshValid allows to load a file, construct the mesh structure and read mesh hypothesis.
 * Then starts meshing operation.
 * This class allows to set all explicit constraints desired by the user, and to set all implicit constraints linked to 
 * mesher requirement.
 * The main idea of mesh generation is to sub-structure the mesh linked to the geometric shape into several sub-meshes 
 * according to specifications and geometry decomposition (see mesh.MeshMesh.initMesh()).
 */
public class MeshValid2D
{
	private static Logger logger=Logger.getLogger(MeshValid2D.class.getName());

	/** 
	 * Reads the file, the algorithm type and the constraint value for meshing
	 * @param brepfilename  the filename of the brep file	 
	 * @param discr  the value of the meshing constraint
	 */
	private static void check(String brepfilename, String xmlDir, float discr, float defl)
	{
		logger.info("Loading " + brepfilename);
		CADShape shape = CADShapeFactory.getFactory().newShape(brepfilename);
		try
		{
			int iFace = 0;
			String numFaceProp = System.getProperty("org.jcae.mesh.Mesher.meshFace");
			if (numFaceProp == null)
			{
				numFaceProp = "0";
				System.setProperty("org.jcae.mesh.Mesher.meshFace", numFaceProp);
			}
			int numFace = Integer.parseInt(numFaceProp);
			CADExplorer expF = CADShapeFactory.getFactory().newExplorer();
			QualityFloat data = new QualityFloat(1000);
			for (expF.init(shape, CADShapeEnum.FACE); expF.more(); expF.next())
			{
				CADFace F = (CADFace) expF.current();
				iFace++;
				if (numFace != 0 && iFace != numFace)
					continue;
				Mesh2D mesh = new Mesh2D(F);
				MeshReader.readObject(mesh, xmlDir, iFace);
				QualityProcedure qproc = new AbsoluteDeflection2D(mesh);
				data.setQualityProcedure(qproc);
				for(Triangle f: mesh.getTriangles())
				{
					if (f.hasAttributes(AbstractHalfEdge.OUTER))
						continue;
					data.compute(f);
				}
			}
			data.split(0.0f, 2.0f * defl, 10);
			data.printLayers();
			String bbfile = brepfilename.substring(0, brepfilename.lastIndexOf('.'))+".bb";
			data.printMeshBB(bbfile);
		}
		catch(Exception ex)
		{
			logger.warning(ex.getMessage());
			ex.printStackTrace();
		}
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
		if (filename.endsWith(".step") || filename.endsWith(".igs"))
		{
			CADShape shape = CADShapeFactory.getFactory().newShape(filename);
			filename = filename.substring(0, filename.lastIndexOf('.')) + ".tmp.brep";
			shape.writeNative(filename);
		}
		String xmlDir = args[1];
		Float discr = new Float(args[2]);
		Float defl = new Float(args[3]);
		check(filename, xmlDir, discr.floatValue(), defl.floatValue());
	}
}
