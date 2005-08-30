/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2003,2004,2005
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

import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.File;
import java.util.Properties;
import java.util.Stack;
import java.util.Date;
import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;
import org.jcae.mesh.amibe.InitialTriangulationException;
import org.jcae.mesh.amibe.InvalidFaceException;
import org.jcae.mesh.amibe.metrics.*;
import org.jcae.mesh.mesher.ds.MMesh1D;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.mesher.algos1d.*;
import org.jcae.mesh.amibe.algos2d.*;
import org.jcae.mesh.amibe.algos3d.Fuse;
import org.jcae.mesh.xmldata.*;
import org.jcae.mesh.cad.*;
import gnu.trove.TIntArrayList;

/**
 * Main class to mesh a surface.
 * This Mesher class takes as input a file name containing the CAD
 * surface, mesh hypothesis (length and deflection), computes a mesh
 * according to these hypothesis and store it onto disk.
 *
 * This class allows to set all explicit constraints desired by the
 * user, and to set all implicit constraints linked to mesher
 * requirement.  The main idea of mesh generation is to sub-structure
 * the mesh linked to the geometric shape into several sub-meshes
 * according to specifications and geometry decomposition (see
 * mesh.MeshMesh.initMesh()).
 */
public class Mesher
{
	private static Logger logger=Logger.getLogger(Mesher.class);

	/** 
	 * Mesh a CAD surface.
	 *
	 * @param brepfilename  the filename of the brep file	 
	 * @param xmlDir  directory where output files are stored
	 * @param discr  length constraint
	 * @param defl   deflection constraint
	 */
	private static TIntArrayList mesh(String brepfilename, String unvName, String xmlDir, double discr, double defl)
	{
		//  Process all needed properties
		String relDeflProp = System.getProperty("org.jcae.mesh.amibe.ds.Metric3D.relativeDeflection");
		if (relDeflProp == null)
		{
			relDeflProp = "true";
			System.setProperty("org.jcae.mesh.amibe.ds.Metric3D.relativeDeflection", relDeflProp);
		}
		String numFaceProp = System.getProperty("org.jcae.mesh.Mesher.meshFace");
		if (numFaceProp == null)
		{
			numFaceProp = "0";
			System.setProperty("org.jcae.mesh.Mesher.meshFace", numFaceProp);
		}
		String minFaceProp = System.getProperty("org.jcae.mesh.Mesher.minFace");
		if (minFaceProp == null)
		{
			minFaceProp = "0";
			System.setProperty("org.jcae.mesh.Mesher.minFace", minFaceProp);
		}
		String maxFaceProp = System.getProperty("org.jcae.mesh.Mesher.maxFace");
		if (maxFaceProp == null)
		{
			maxFaceProp = "0";
			System.setProperty("org.jcae.mesh.Mesher.maxFace", maxFaceProp);
		}
		String processMesh1dProp = System.getProperty("org.jcae.mesh.Mesher.mesh1d");
		if (processMesh1dProp == null)
		{
			processMesh1dProp = "true";
			System.setProperty("org.jcae.mesh.Mesher.mesh1d", processMesh1dProp);
		}
		String processMesh2dProp = System.getProperty("org.jcae.mesh.Mesher.mesh2d");
		if (processMesh2dProp == null)
		{
			processMesh2dProp = "true";
			System.setProperty("org.jcae.mesh.Mesher.mesh2d", processMesh2dProp);
		}
		String processMesh3dProp = System.getProperty("org.jcae.mesh.Mesher.mesh3d");
		if (processMesh3dProp == null)
		{
			processMesh3dProp = "true";
			System.setProperty("org.jcae.mesh.Mesher.mesh3d", processMesh3dProp);
		}
		String exportTriangleSoupProp = System.getProperty("org.jcae.mesh.Mesher.triangleSoup");
		if (exportTriangleSoupProp == null)
		{
			exportTriangleSoupProp = "false";
			System.setProperty("org.jcae.mesh.Mesher.triangleSoup", exportTriangleSoupProp);
		}
		String writeNormalsProp = System.getProperty("org.jcae.mesh.Mesher.writeNormals");
		if (writeNormalsProp == null)
		{
			writeNormalsProp = "false";
			System.setProperty("org.jcae.mesh.Mesher.writeNormals", writeNormalsProp);
		}
		String exportUNVProp = System.getProperty("org.jcae.mesh.exportUNV");
		if (exportUNVProp == null)
		{
			exportUNVProp = "false";
			System.setProperty("org.jcae.mesh.exportUNV", exportUNVProp);
		}
		String exportMESHProp = System.getProperty("org.jcae.mesh.exportMESH");
		if (exportMESHProp == null)
		{
			exportMESHProp = "false";
			System.setProperty("org.jcae.mesh.exportMESH", exportMESHProp);
		}
		String quadranglesProp = System.getProperty("org.jcae.mesh.Mesher.quadrangles");
		if (quadranglesProp == null)
		{
			quadranglesProp = "false";
			System.setProperty("org.jcae.mesh.Mesher.quadrangles", quadranglesProp);
		}

		//  Declare all variables here
		//  xmlDir:      absolute path name where XML files are stored
		//  xmlFile:     basename of the main XML file
		//  xmlBrepDir:  path to brep file, relative to xmlDir
		//  brepFile:    basename of the brep file
		
		String brepFile = (new File(brepfilename)).getName();		
		String xmlFile = "jcae1d";
		MMesh1D mesh1D;
		TIntArrayList badGroups = new TIntArrayList();
						
		String xmlBrepDir = relativize(new File(brepfilename).getAbsoluteFile().getParentFile(),
			new File(xmlDir).getAbsoluteFile()).getPath();
		
		logger.info("Loading " + brepfilename);
		
		CADShapeBuilder factory = CADShapeBuilder.factory;
		CADShape shape = factory.newShape(brepfilename);
		CADExplorer expF = factory.newExplorer();
		boolean relDefl = relDeflProp.equals("true");
		int numFace = Integer.parseInt(numFaceProp);
		int minFace = Integer.parseInt(minFaceProp);
		int maxFace = Integer.parseInt(maxFaceProp);
		if (minFace != 0 || maxFace != 0)
			numFace=0;
		if (quadranglesProp.equals("true")) {
			discr *= 2.0;
			//defl *= 2.0;
		}
		if (processMesh1dProp.equals("true")) {
			//  Step 1: Compute 1D mesh
			logger.info("1D mesh");
			mesh1D = new MMesh1D(shape);
			mesh1D.setMaxLength(discr);
			if (defl <= 0.0)
				new UniformLength(mesh1D).compute();
			else
			{
				mesh1D.setMaxDeflection(defl);
				new UniformLengthDeflection(mesh1D).compute(relDefl);
				new Compat1D2D(mesh1D).compute(relDefl);
			}
			//  Store the 1D mesh onto disk
			MMesh1DWriter.writeObject(mesh1D, xmlDir, xmlFile, xmlBrepDir, brepFile);
		}
		if (processMesh2dProp.equals("true")) {
			//  Step 2: Read the 1D mesh and compute 2D meshes
			mesh1D = MMesh1DReader.readObject(xmlDir, xmlFile);
			shape = mesh1D.getGeometry();
			mesh1D.setMaxLength(discr);
			Metric3D.setLength(discr);
			Metric3D.setDeflection(defl);
			Metric3D.setRelativeDeflection(relDefl);
	
			//  Prepare 2D discretization
			mesh1D.duplicateEdges();
			//  Compute node labels shared by all 2D and 3D meshes
			mesh1D.updateNodeLabels();
			
			int iFace = 0;
			int nTryMax = 20;
			logger.debug("org.jcae.mesh.Mesher.minFace="+minFace);
			logger.debug("org.jcae.mesh.Mesher.maxFace="+maxFace);
			logger.debug("org.jcae.mesh.Mesher.meshFace="+numFace);
			int nrFaces = 0;
			for (expF.init(shape, CADExplorer.FACE); expF.more(); expF.next())
				nrFaces++;			
			for (expF.init(shape, CADExplorer.FACE); expF.more(); expF.next())
			{
				CADFace F = (CADFace) expF.current();
				iFace++;
				if (numFace != 0 && iFace != numFace)
					continue;
				if ((minFace != 0 || maxFace != 0) && (iFace < minFace || iFace > maxFace))
					continue;
				
				logger.info("Meshing face " + iFace+"/"+nrFaces);
				//  This variable can be modified, thus reset it
				Metric2D.setLength(discr);
				if(Boolean.getBoolean("org.jcae.mesh.Mesher.explodeBrep"))
					F.writeNative("face."+iFace+".brep");
				Mesh mesh = new Mesh(F); 
				int nTry = 0;
				while (nTry < nTryMax)
				{
					try
					{
						new BasicMesh(mesh, mesh1D).compute();
						new CheckDelaunay(mesh).compute();
						if (defl > 0.0 && !relDefl)
							new EnforceAbsDeflection(mesh).compute();
						mesh.removeDegeneratedEdges();
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
						else if (ex instanceof InvalidFaceException)
						{
							logger.warn("Face "+iFace+" is invalid, skipping...");
							mesh = new Mesh(F); 
							xmlFile = "jcae2d."+iFace;
							MeshWriter.writeObject(mesh, xmlDir, xmlFile, xmlBrepDir, brepFile, iFace);
							badGroups.add(iFace);
							break;
						}
						badGroups.add(iFace);
						logger.warn(ex.getMessage());
						ex.printStackTrace();
					}
					break;
				}
				if (nTry == nTryMax)
				{
					logger.error("Face "+iFace+" cannot be triangulated, skipping...");
					badGroups.add(iFace);
					mesh = new Mesh(F); 
					xmlFile = "jcae2d."+iFace;
					MeshWriter.writeObject(mesh, xmlDir, xmlFile, xmlBrepDir, brepFile, iFace);
				}
			}
		}

		if (processMesh3dProp.equals("true")) {
			// Step 3: Read 2D meshes and compute 3D mesh
			try
			{
				int iFace = 0;
				MeshToMMesh3DConvert m2dTo3D = new MeshToMMesh3DConvert(xmlDir);
				m2dTo3D.exportUNV(exportUNVProp.equals("true"), unvName);
				logger.info("Read informations on boundary nodes");
				for (expF.init(shape, CADExplorer.FACE); expF.more(); expF.next())
				{
					CADFace F = (CADFace) expF.current();
					iFace++;
					if (numFace != 0 && iFace != numFace)
						continue;
					if ((minFace != 0 || maxFace != 0) && (iFace < minFace || iFace > maxFace))
						continue;
					xmlFile = "jcae2d."+iFace;
					m2dTo3D.computeRefs(xmlFile);
				}
				m2dTo3D.initialize("jcae3d", writeNormalsProp.equals("true"));
				iFace = 0;
				for (expF.init(shape, CADExplorer.FACE); expF.more(); expF.next())
				{
					CADFace F = (CADFace) expF.current();
					iFace++;
					if (numFace != 0 && iFace != numFace)
						continue;
					if ((minFace != 0 || maxFace != 0) && (iFace < minFace || iFace > maxFace))
						continue;
					xmlFile = "jcae2d."+iFace;
					logger.info("Importing face "+iFace);
					m2dTo3D.convert(xmlFile, iFace, F);
				}
				m2dTo3D.finish();
			}
			catch(Exception ex)
			{
				logger.warn(ex.getMessage());
				ex.printStackTrace();
			}
			if (exportMESHProp.equals("true"))
			{
				logger.info("Exporting MESH");
				String MESHName=brepfilename.substring(0, brepfilename.lastIndexOf('.'))+".mesh";
				new UNVConverter(xmlDir).writeMESH(MESHName);
			}
			if (exportTriangleSoupProp.equals("true")) {
				// Step 3: Read 2D meshes and compute raw 3D mesh
				try
				{
					int iFace = 0;
					MeshToSoupConvert m2dTo3D = new MeshToSoupConvert(xmlDir);
					m2dTo3D.initialize("soup", false);
					iFace = 0;
					for (expF.init(shape, CADExplorer.FACE); expF.more(); expF.next())
					{
						CADFace F = (CADFace) expF.current();
						iFace++;
						if (numFace != 0 && iFace != numFace)
							continue;
						if ((minFace != 0 || maxFace != 0) && !(iFace >= minFace && iFace <= maxFace))
							continue;
						xmlFile = "jcae2d."+iFace;
						logger.info("Importing face "+iFace);
						m2dTo3D.convert(xmlFile, iFace, F);
					}
					m2dTo3D.finish();
				}
				catch(Exception ex)
				{
					logger.warn(ex.getMessage());
					ex.printStackTrace();
				}
			}
		}
		if (badGroups.size() > 0)
		{
			logger.info("Number of faces which cannot be meshed: "+badGroups.size());
			logger.info(""+badGroups);
		}
		return badGroups;
	}
	
	private static void report(String brepfilename, TIntArrayList badGroups, String xmlDir, double discr, double defl, String startDate)
	{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String endDate = sdf.format(new Date());
		String outfile = System.getProperty("org.jcae.mesh.Mesher.reportFile");
		if (outfile == null)
			return;
		try
		{
			PrintStream out = new PrintStream(new FileOutputStream(new File(outfile)));
			InputStream in = Mesher.class.getResourceAsStream("/timestamp.properties");
			Properties prop = new Properties();
			prop.load(in);
			String buildDate = prop.getProperty("build.time");
			int [] res = MMesh3DReader.getInfos(xmlDir, "jcae3d");
			out.println("MESH REPORT");
			out.println("===========");
			out.println("Start date: "+startDate);
			out.println("End date: "+endDate);
			out.println("Geometry: "+brepfilename);
			out.println("Edge length criterion: "+discr);
			out.println("Deflection criterion: "+defl);
			out.println("Number of nodes: "+res[0]);
			out.println("Number of triangles: "+res[1]);
			out.println("Number of groups: "+res[2]);
			out.println("Number of groups which cannot be meshed: "+badGroups.size());
			if (badGroups.size() > 0)
				out.println(""+badGroups);
			out.println("jcae.jar build time: "+buildDate);
			Properties sys = System.getProperties();
			sys.list(out);
			out.close();
		}
		catch (java.io.IOException ex)
		{
		}
	}

	private static File relativize(File file, File reference)
	{
		File current=file;
		Stack l=new Stack();
		while(current!=null && !current.equals(reference))
		{
			l.push(current.getName());
			current=current.getParentFile();
		}
		if(l.isEmpty())
			return new File(".");
		else if(current==null)
			return file;
		else
		{
			current=new File(l.pop().toString());
			while(!l.isEmpty())
			{
				current=new File(current, l.pop().toString());
			}
			return current;
		}
	}

	static public boolean deleteDirectory(File path, File avoid)
	{
		if (path.exists())
		{
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++)
			{
				if (files[i].isDirectory())
				{
					deleteDirectory(files[i], avoid);
				} else
				{
					if(!files[i].equals(avoid))
						files[i].delete();
				}
			}
		}
		return (path.delete());
	}
	
	static private int countFaces(String brepfilename)
	{
		//count faces in the brep File 
		CADShapeBuilder factory = CADShapeBuilder.factory;
		CADShape shape = factory.newShape(brepfilename);
		CADExplorer expF = factory.newExplorer();
		int nrFaces = 0;
		for (expF.init(shape, CADExplorer.FACE); expF.more(); expF.next())
			nrFaces++;
		return nrFaces;
	}
	
	/**
	 * main method, reads 2 arguments and calls mesh() method
	 * @param args an array of String, filename, algorithm type and constraint
	 * value
	 */
	public static void main(String args[])
	{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String startDate = sdf.format(new Date());
		try
		{
			if (args.length < 2 || args.length > 4)
			{
				System.out.println("Usage : Mesher filename output_directory edge_length deflection");
				System.exit(0);
			}
			String filename=args[0];
			String unvName=System.getProperty("org.jcae.mesh.unv.name");
			
			if(unvName==null)
			{
				unvName=filename.substring(0, filename.lastIndexOf('.'))+".unv";
				if(!Boolean.getBoolean("org.jcae.mesh.unv.nogz"))
					unvName += ".gz";
			}
			
			if (filename.endsWith(".step") || filename.endsWith(".stp") || filename.endsWith(".igs"))
			{
				CADShape shape = CADShapeBuilder.factory.newShape(filename);
				filename = filename.substring(0, filename.lastIndexOf('.')) + ".tmp.brep";
				shape.writeNative(filename);
			}
			
			// if what we want is just the mesh count, print it and exit
			if(Boolean.getBoolean("org.jcae.mesh.countFaces"))
				System.exit(countFaces(filename));
			
			//Init xmlDir
			String xmlDir;
			if(Boolean.getBoolean("org.jcae.mesh.tmpDir.auto"))
			{
				File f=File.createTempFile("jcae","");
				f.delete();
				f.mkdirs();
				xmlDir=f.getPath();
			}
			else
			{
				xmlDir = args[1];
			}
			
			//Do some checks on xmlDir
			File xmlDirF=new File(xmlDir);
			xmlDirF.mkdirs();
			if(!xmlDirF.exists() || !xmlDirF.isDirectory())
			{
				System.out.println("Cannot write to "+xmlDir);
				return;
			}
			
			Double discr=new Double(args[2]);
			Double defl=new Double(args[3]);
			TIntArrayList badGroups = mesh(filename, unvName, xmlDir, discr.doubleValue(), defl.doubleValue());
			report(args[0], badGroups, xmlDir, discr.doubleValue(), defl.doubleValue(), startDate);
			if(Boolean.getBoolean("org.jcae.mesh.tmpDir.delete"))
			{
				deleteDirectory(new File(xmlDir), new File(unvName));
			}
			
			logger.info("End mesh");
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
