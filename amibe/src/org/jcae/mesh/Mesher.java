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
import org.jcae.mesh.mesher.algos2d.InnerRefine;
import org.jcae.mesh.mesher.algos2d.TargetSize;
import org.jcae.mesh.mesher.algos2d.SmoothNodes;
import org.jcae.mesh.mesher.ds.*;
import org.jcae.mesh.mesher.metrics.*;
import org.jcae.mesh.mesher.InitialTriangulationException;
import org.jcae.mesh.xmldata.*;
import org.jcae.mesh.cad.*;
import org.jcae.opencascade.jni.*;

/**
 * This class MeshGen allows to load a file, construct the mesh structure and read mesh hypothesis.
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
	public static void load(String brepfilename, double discr, String xmlDir)
	{
		//  Declare all variables here
		MMesh3D mesh3D = new MMesh3D();
		Metric3D.setLength(discr);
		Metric2D.setLength(discr);
		MMesh1D mesh1D;
		//     xmlDir:      absolute path name where XML files are stored
		//     xmlFile:     basename of the main XML file
		//     xmlBrepDir:  path to brep file, relative to xmlDir
		//     brepFile:    basename of the brep file
		
		String brepFile = (new File(brepfilename)).getName();		
		String xmlFile = "jcae1d";
		URI brepDirURI=new File(xmlDir, "dummy").toURI().relativize(new File(brepfilename).getParentFile().toURI());
		String xmlBrepDir = new File(brepDirURI).getPath();
		
		int iFace = 0;
		logger.info("Loading " + brepfilename);
		boolean testWrite2D = true;
		
		CADShapeBuilder factory = CADShapeBuilder.factory;
		CADShape shape = factory.newShape(brepfilename);
		CADExplorer expF = factory.newExplorer();
		
		//  Step 1: Compute 1D mesh
		mesh1D = new MMesh1D(shape);
		mesh1D.setMaxLength(discr);
		new UniformLength(mesh1D).compute();
		//  Store the 1D mesh onto disk
		MMesh1DWriter.writeObject(mesh1D, xmlDir, xmlFile, xmlBrepDir, brepFile);
		
		//  Step 2: Read the 1D mesh and compute 2D meshes
		mesh1D = MMesh1DReader.readObject(xmlDir, xmlFile);
		shape = mesh1D.getGeometry();
		mesh1D.setMaxLength(discr);

		//  Prepare 2D discretization
		mesh1D.duplicateEdges();
		//  Compute node labels shared by all 2D and 3D meshes
		mesh1D.updateNodeLabels();
		
		int nTryMax = 20;
		for (expF.init(shape, CADExplorer.FACE); expF.more(); expF.next())
		{
			CADFace F = (CADFace) expF.current();
			iFace++;
			logger.info("Meshing face " + iFace);
			SubMesh2D submesh = new SubMesh2D(F);
			int nTry = 0;
			while (nTry < nTryMax)
			{
				try
				{
					submesh.pushCompGeom(2);
					submesh.init(mesh1D);
				
					//  Those calls to InnerRefine are only needed for spheres:
					//new InnerRefine(submesh).compute();
					//new InnerRefine(submesh).compute();
					submesh.popCompGeom(2);
					
					submesh.pushCompGeom(3);
					new TargetSize(submesh).compute();				
					new SmoothNodes(submesh, 20).compute();
					if (testWrite2D)
					{
						xmlFile = "jcae2d."+iFace;
						SubMesh2DWriter.writeObject(submesh, xmlDir, xmlFile, xmlBrepDir, brepFile, iFace);
					}
					else
					{
						mesh3D.addSubMesh2D(submesh, false);
						mesh3D.printInfos();
					}
					submesh.popCompGeom(3);
				}
				catch(Exception ex)
				{
					if (ex instanceof InitialTriangulationException)
					{
						logger.warn("Face "+iFace+" cannot be triangulated, trying again with a larger tolerance...");
						submesh = new SubMesh2D(F);
						submesh.scaleTolerance(10.);
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

		// Step 3: Read 2D meshes and compute 3D mesh
		if (testWrite2D)
		{
			try
			{
				iFace = 0;
				for (expF.init(shape, CADExplorer.FACE); expF.more(); expF.next())
				{
					CADFace F = (CADFace) expF.current();
					iFace++;
					xmlFile = "jcae2d."+iFace;
					SubMesh2D submesh = SubMesh2DReader.readObject(xmlDir, xmlFile, F);
					if (null != submesh)
					{
						logger.debug("Loading face "+iFace);
						submesh.pushCompGeom(3);
						mesh3D.addSubMesh2D(submesh, true);
						submesh.popCompGeom(3);					
					}
				}
				mesh3D.printInfos();
			}
			catch(Exception ex)
			{
				logger.warn(ex.getMessage());
				ex.printStackTrace();
			}
		}
		
		xmlFile = "jcae3d";
		MMesh3DWriter.writeObject(mesh3D, xmlDir, xmlFile, xmlBrepDir);
		String unvName=brepfilename.substring(0, brepfilename.lastIndexOf('.'))+".unv";
		mesh3D.writeUNV(unvName);
		
	}

	/**
	 * main method, reads 2 arguments and calls mesh.MeshGen.load() method
	 * @param args  an array of String, filename, algorithm type and constraint value
	 * @see #load
	 */
	public static void main(String args[])
	{
		if (args.length!=2&&args.length!=3)
		{
			System.out.println("Usage : MeshGen filename size [output directory]");
			System.exit(0);
		}
		else
		{
			String filename=args[0];
			if (filename.endsWith(".step") || filename.endsWith(".igs"))
				filename=MeshGen.convertToBRep(filename);
			Double discr=new Double(args[1]);
			String xmlDir;
			if(args.length==3)
			{
				xmlDir=args[2];
			}
			else
			{
				File tmpDir=new File("/tmp");
				if(!tmpDir.exists())
				{
					tmpDir=new File(System.getProperty("user.home"), ".jcae.tmp");			
					if(!tmpDir.exists()) tmpDir.mkdirs();
				}
				xmlDir = (new File(tmpDir, filename+".jcae")).getAbsolutePath();
			}
			load(filename, discr.doubleValue(), xmlDir);	
		}
	}
}
