/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2003 Jerome Robert <jeromerobert@users.sourceforge.net>

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

import java.io.File;
import java.net.URI;

import org.apache.log4j.Logger;
import org.jcae.mesh.mesher.algos1d.UniformLength;
import org.jcae.mesh.mesher.ds.*;
import org.jcae.mesh.mesher.InitialTriangulationException;
import org.jcae.mesh.amibe.metrics.*;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.algos2d.BasicMesh;
import org.jcae.mesh.mesher.algos3d.Fuse;
import org.jcae.mesh.xmldata.*;
import org.jcae.mesh.cad.*;

/**
 * This class Mesher allows to load a file, construct the mesh structure and read mesh hypothesis.
 * Then starts meshing operation.
 * This class allows to set all explicit constraints desired by the user, and to set all implicit constraints linked to 
 * mesher requirement.
 * The main idea of mesh generation is to sub-structure the mesh linked to the geometric shape into several sub-meshes 
 * according to specifications and geometry decomposition (see mesh.MeshMesh.initMesh()).
 */
public class Mesher
{
	private static Logger logger=Logger.getLogger(Mesher.class);

	/** 
	 * Reads the file, the algorithm type and the constraint value for meshing
	 * @param brepfilename  the filename of the brep file	 
	 * @param discr  the value of the meshing constraint
	 */
	private static MMesh3D mesh(String brepfilename, String xmlDir, double discr, double defl, double tolerance)
	{
		//  Declare all variables here
		MMesh3D mesh3D = new MMesh3D();
		Metric2D.setLength(discr);
		Metric3D.setLength(discr);
		Metric3D.setDeflection(defl);
		MMesh1D mesh1D;
		//  xmlDir:      absolute path name where XML files are stored
		//  xmlFile:     basename of the main XML file
		//  xmlBrepDir:  path to brep file, relative to xmlDir
		//  brepFile:    basename of the brep file
		
		String brepFile = (new File(brepfilename)).getName();		
		String xmlFile = "jcae1d";
		URI brepDirURI=new File(xmlDir, "dummy").toURI().relativize(new File(brepfilename).getParentFile().toURI());
		String xmlBrepDir = new File(brepDirURI).getPath();
		
		int iFace = 0;
		logger.info("Loading " + brepfilename);
		
		CADShapeBuilder factory = CADShapeBuilder.factory;
		CADShape shape = factory.newShape(brepfilename);
		CADExplorer expF = factory.newExplorer();
		if (System.getProperty("org.jcae.mesh.Mesher.mesh1d", "true").equals("true")) {
			//  Step 1: Compute 1D mesh
			logger.info("1D mesh");
			mesh1D = new MMesh1D(shape);
			mesh1D.setMaxLength(discr);
			new UniformLength(mesh1D).compute();
			//  Store the 1D mesh onto disk
			MMesh1DWriter.writeObject(mesh1D, xmlDir, xmlFile, xmlBrepDir, brepFile);
		}
		if (System.getProperty("org.jcae.mesh.Mesher.mesh2d", "true").equals("true")) {
			//  Step 2: Read the 1D mesh and compute 2D meshes
			mesh1D = MMesh1DReader.readObject(xmlDir, xmlFile);
			shape = mesh1D.getGeometry();
			mesh1D.setMaxLength(discr);
	
			//  Prepare 2D discretization
			mesh1D.duplicateEdges();
			//  Compute node labels shared by all 2D and 3D meshes
			mesh1D.updateNodeLabels();
			
			int nTryMax = 20;
			int numFace = Integer.parseInt(System.getProperty("org.jcae.mesh.Mesher.meshFace", "0"));
			for (expF.init(shape, CADExplorer.FACE); expF.more(); expF.next())
			{
				CADFace F = (CADFace) expF.current();
				iFace++;
				if (numFace != 0 && iFace != numFace)
					continue;
				logger.info("Meshing face " + iFace);
// F.writeNative("face."+iFace+".brep");
				Mesh mesh = new Mesh(F); 
				int nTry = 0;
				while (nTry < nTryMax)
				{
					try
					{
						new BasicMesh(mesh, mesh1D).compute();
						xmlFile = "jcae2d."+iFace;
						MeshWriter.writeObject(mesh, xmlDir, xmlFile, xmlBrepDir, brepFile, iFace);
					}
					catch(Exception ex)
					{
						if (ex instanceof InitialTriangulationException)
						{
							logger.warn("Face "+iFace+" cannot be triangulated, trying again with a larger tolerance...");
							mesh = new Mesh(F);
							mesh.scaleTolerance(10.);
							nTry++;
							continue;
						}
						logger.warn(ex.getMessage());
						ex.printStackTrace();
					}
					break;
				}
				if (nTry == nTryMax)
					logger.error("Face "+iFace+" cannot be triangulated, skipping...");
			}
		}

		if (System.getProperty("org.jcae.mesh.Mesher.mesh3d", "true").equals("true")) {
			// Step 3: Read 2D meshes and compute 3D mesh
			try
			{
				iFace = 0;
				int numFace = Integer.parseInt(System.getProperty("org.jcae.mesh.Mesher.meshFace", "0"));
				for (expF.init(shape, CADExplorer.FACE); expF.more(); expF.next())
				{
					CADFace F = (CADFace) expF.current();
					iFace++;
					if (numFace != 0 && iFace != numFace)
						continue;
					xmlFile = "jcae2d."+iFace;
					SubMesh2D submesh = SubMesh2DReader.readObject(xmlDir, xmlFile, F);
					if (null != submesh)
					{
						logger.info("Importing face "+iFace);
						mesh3D.addSubMesh2D(submesh, true);
					}
				}
				mesh3D.printInfos();
			}
			catch(Exception ex)
			{
				logger.warn(ex.getMessage());
				ex.printStackTrace();
			}
			
			if (tolerance >= 0.0)
				new Fuse(mesh3D, tolerance).compute();
			xmlFile = "jcae3d";
			MMesh3DWriter.writeObject(mesh3D, xmlDir, xmlFile, xmlBrepDir);
		}
		else
		{
			logger.info("Reading 3D mesh");
			xmlFile = "jcae3d";
			mesh3D = MMesh3DReader.readObject(xmlDir, xmlFile);
		}
		return mesh3D;
	}

	/**
	 * main method, reads 2 arguments and calls mesh() method
	 * @param args  an array of String, filename, algorithm type and constraint value
	 * @see #mesh
	 */
	public static void main(String args[])
	{
		if (args.length < 2 || args.length > 4)
		{
			System.out.println("Usage : Mesher filename output_directory edge_length deflection");
			System.exit(0);
		}
		String filename=args[0];
		String unvName=filename.substring(0, filename.lastIndexOf('.'))+".unv";
		if (filename.endsWith(".step") || filename.endsWith(".igs"))
		{
			CADShape shape = CADShapeBuilder.factory.newShape(filename);
			filename = filename.substring(0, filename.lastIndexOf('.')) + ".tmp.brep";
			shape.writeNative(filename);
		}
		String xmlDir = args[1];
		Double discr=new Double(args[2]);
		Double defl=new Double(args[3]);
		Double tolerance=new Double(System.getProperty("org.jcae.mesh.tolerance", "-1.0"));
		MMesh3D mesh3D = mesh(filename, xmlDir, discr.doubleValue(), defl.doubleValue(), tolerance.doubleValue());
		logger.info("Exporting UNV");

		if(Boolean.getBoolean("org.jcae.mesh.unv.nogz"))
			mesh3D.writeUNV(unvName);
		else
			mesh3D.writeUNV(unvName+".gz");

		logger.info("End mesh");
	}
}
