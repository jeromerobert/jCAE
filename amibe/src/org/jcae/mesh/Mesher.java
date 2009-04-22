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

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.nio.channels.FileChannel;
import java.util.Properties;
import java.util.Date;
import java.util.HashMap;
import java.text.SimpleDateFormat;

import org.jcae.mesh.amibe.patch.InitialTriangulationException;
import org.jcae.mesh.amibe.patch.InvalidFaceException;
import org.jcae.mesh.amibe.patch.Mesh2D;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.amibe.algos1d.*;
import org.jcae.mesh.amibe.algos2d.*;
import org.jcae.mesh.amibe.ds.MMesh1D;
import org.jcae.mesh.amibe.ds.MeshParameters;
import org.jcae.mesh.xmldata.*;
import org.jcae.mesh.cad.*;
import java.util.logging.Logger;

import gnu.trove.TIntArrayList;
import gnu.trove.THashSet;

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
	private static Logger logger=Logger.getLogger(Mesher.class.getName());
	private static SimpleDateFormat DATE_FORMAT =
		new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	/** meshing constraint: edge length */
	protected double edgeLength;
	
	/** meshing constraint: deflection */
	protected double deflection;
	
	/** The geometry file to be meshed */
	protected String geometryFile;

	/** CAD shape */
	protected CADShape shape;
	
	/** The file where to export the mesh in UNV format */
	protected String unvName;
	
	/** The output directory */
	protected String outputDir;
	private boolean exportTriangleSoup;
	private boolean exportPOLY;
	private boolean exportSTL;
	private boolean exportMESH;
	private boolean writeNormals;
	private boolean exportUNV;
	private boolean processMesh3d=true;
	private boolean processMesh2d=true;
	private boolean processMesh1d=true;
	private boolean quadrangles;
	private int minFace=0;
	private int maxFace=0;
	private int numFace=0;
		
	/**
	 * Read system properties which affect the meshing behavior.
	 * See package level javadoc for more information. 
	 */
	protected void readProperties()
	{
		//TODO Clumsy code, should be rewrote. here are some avenues.
		//The default value for a boolean property is always false. Doing so
		//allows using Boolean.getBoolean. It means replacing
		//Metric3D.relativeDeflection by Metric3D.noRelativeDeflection.
		//When possible system properties should not be used as global variable,
		//here they are set, only to be read in the report method.
		//System.getProperty can take a parameter which is the default property
		//value.
		String numFaceProp = System.getProperty("org.jcae.mesh.Mesher.meshFace");
		if (numFaceProp == null)
		{
			numFaceProp = "0";
			System.setProperty("org.jcae.mesh.Mesher.meshFace", numFaceProp);
		}
		numFace=Integer.parseInt(numFaceProp);
		
		String minFaceProp = System.getProperty("org.jcae.mesh.Mesher.minFace");
		if (minFaceProp == null)
		{
			minFaceProp = "0";
			System.setProperty("org.jcae.mesh.Mesher.minFace", minFaceProp);
		}
		minFace=Integer.parseInt(minFaceProp);
		
		String maxFaceProp = System.getProperty("org.jcae.mesh.Mesher.maxFace");
		if (maxFaceProp == null)
		{
			maxFaceProp = "0";
			System.setProperty("org.jcae.mesh.Mesher.maxFace", maxFaceProp);
		}
		maxFace=Integer.parseInt(maxFaceProp);
		
		String processMesh1dProp = System.getProperty("org.jcae.mesh.Mesher.mesh1d");
		if (processMesh1dProp == null)
		{
			processMesh1dProp = "true";
			System.setProperty("org.jcae.mesh.Mesher.mesh1d", processMesh1dProp);
		}
		processMesh1d=processMesh1dProp.equals("true");
		
		String processMesh2dProp = System.getProperty("org.jcae.mesh.Mesher.mesh2d");
		if (processMesh2dProp == null)
		{
			processMesh2dProp = "true";
			System.setProperty("org.jcae.mesh.Mesher.mesh2d", processMesh2dProp);
		}
		processMesh2d=processMesh2dProp.equals("true");
		
		String processMesh3dProp = System.getProperty("org.jcae.mesh.Mesher.mesh3d");
		if (processMesh3dProp == null)
		{
			processMesh3dProp = "true";
			System.setProperty("org.jcae.mesh.Mesher.mesh3d", processMesh3dProp);
		}
		processMesh3d=processMesh3dProp.equals("true");
		
		String exportTriangleSoupProp = System.getProperty("org.jcae.mesh.Mesher.triangleSoup");
		if (exportTriangleSoupProp == null)
		{
			exportTriangleSoupProp = "false";
			System.setProperty("org.jcae.mesh.Mesher.triangleSoup", exportTriangleSoupProp);
		}
		exportTriangleSoup=exportTriangleSoupProp.equals("true");
		
		String writeNormalsProp = System.getProperty("org.jcae.mesh.Mesher.writeNormals");
		if (writeNormalsProp == null)
		{
			writeNormalsProp = "false";
			System.setProperty("org.jcae.mesh.Mesher.writeNormals", writeNormalsProp);
		}
		writeNormals=writeNormalsProp.equals("true");
		
		String exportUNVProp = System.getProperty("org.jcae.mesh.exportUNV");
		if (exportUNVProp == null)
		{
			exportUNVProp = "false";
			System.setProperty("org.jcae.mesh.exportUNV", exportUNVProp);
		}
		exportUNV=exportUNVProp.equals("true");
		
		String exportMESHProp = System.getProperty("org.jcae.mesh.exportMESH");
		if (exportMESHProp == null)
		{
			exportMESHProp = "false";
			System.setProperty("org.jcae.mesh.exportMESH", exportMESHProp);
		}
		exportMESH=exportMESHProp.equals("true");
		
		String exportSTLProp = System.getProperty("org.jcae.mesh.exportSTL");
		if (exportSTLProp == null)
		{
			exportSTLProp = "false";
			System.setProperty("org.jcae.mesh.exportSTL", exportSTLProp);
		}
		exportSTL=exportSTLProp.equals("true");
		
		String exportPOLYProp = System.getProperty("org.jcae.mesh.exportPOLY");
		if (exportPOLYProp == null)
		{
			exportPOLYProp = "false";
			System.setProperty("org.jcae.mesh.exportPOLY", exportPOLYProp);
		}
		exportPOLY=exportPOLYProp.equals("true");
		
		String quadranglesProp = System.getProperty("org.jcae.mesh.Mesher.quadrangles");
		if (quadranglesProp == null)
		{
			quadranglesProp = "false";
			System.setProperty("org.jcae.mesh.Mesher.quadrangles", quadranglesProp);
		}
		quadrangles=quadranglesProp.equals("true");
	}
	
	/**
	 * Compute 1D mesh
	 * @param brepFile basename of the BRep file
	 */
	protected MMesh1D mesh1D(String brepFile)
	{
		logger.info("1D mesh");
		MMesh1D mesh1D = new MMesh1D(geometryFile);
		HashMap<String, String> options1d = new HashMap<String, String>();
		options1d.put("size", ""+edgeLength);
		options1d.put("deflection", ""+deflection);
		MeshParameters mp = new MeshParameters(options1d);
		if (deflection <= 0.0)
			new UniformLength(mesh1D, options1d).compute();
		else
		{
			options1d.put("deflection", ""+deflection);
			new UniformLengthDeflection(mesh1D, options1d).compute();
			if (mp.isIsotropic())
				new Compat1D2D(mesh1D, options1d).compute();
		}
		//  Store the 1D mesh onto disk
		MMesh1DWriter.writeObject(mesh1D, outputDir, brepFile);
		return mesh1D;
	}
	
	/**
	 * Read the 1D mesh and compute 2D meshes
	 * @param iFace the id of the face to be meshed
	 * @param face topological face
	 * @param mesh1D the boundary mesh used to create this 2D mesh
	 * @param brepFile basename of the BRep file
	 * @param mtb container for 2D mesh traits
	 * @return <code>true</code> if face had been successfully meshed, <code>false</code> otherwise.
	 */
	protected boolean mesh2D(int iFace, CADFace face, MMesh1D mesh1D, MeshParameters mp,
		String brepFile, MeshTraitsBuilder mtb)
	{
		if(Boolean.getBoolean("org.jcae.mesh.Mesher.explodeBrep"))
			face.writeNative("face."+iFace+".brep");
		Mesh2D mesh = new Mesh2D(mtb, mp, face);
		boolean toReturn=true;
		try
		{
			new Initial(mesh, mtb, mesh1D).compute();
		}
		catch(InitialTriangulationException ex)
		{
			logger.severe("Face "+iFace+" cannot be triangulated, skipping...");
			toReturn=false;
		}
		catch(InvalidFaceException ex)
		{
			logger.severe("Face "+iFace+" is invalid, skipping...");
			toReturn=false;
		}
		catch(Exception ex)
		{
			logger.severe("Unexpected error when triangulating face "+iFace+", skipping...");
			ex.printStackTrace();
			toReturn=false;
		}
		if (toReturn)
		{
			new BasicMesh(mesh).compute();
			new ConstraintNormal3D(mesh).compute();
			new CheckDelaunay(mesh).compute();
			if (mp.hasDeflection() && !mp.hasRelativeDeflection())
				new EnforceAbsDeflection(mesh).compute();
		}
		else
		{
			// Creates an empty mesh to not break 3d conversion when some faces are missing
			mesh = new Mesh2D(mtb, mp, face);
		}
		try
		{
			MeshWriter.writeObject(mesh, outputDir, brepFile, iFace);
		}
		catch(IOException ex)
		{
			ex.printStackTrace();
		}
		return toReturn;
	}
	
	/**
	 * Export the created mesh to various format
	 */
	protected void exportMesh()
	{
		if (exportMESH)
		{
			logger.info("Exporting MESH");
			String MESHName=geometryFile.substring(0, geometryFile.lastIndexOf('.'))+".mesh";
			new MeshExporter.MESH(outputDir).write(MESHName);
		}
		if (exportSTL)
		{
			logger.info("Exporting STL");
			String STLName=geometryFile.substring(0, geometryFile.lastIndexOf('.'))+".stl";
			new MeshExporter.STL(outputDir).write(STLName);
		}
		if (exportPOLY)
		{
			logger.info("Exporting POLY");
			String MESHName=geometryFile.substring(0, geometryFile.lastIndexOf('.'))+".poly";
			new MeshExporter.POLY(outputDir).write(MESHName);
		}
	}
	
	/**
	 * Read 2D meshes and compute 3D mesh
	 * @param brepFile
	 */
	protected void mesh3D(String brepFile)
	{
		int iFace = 0;
		CADExplorer expF = CADShapeFactory.getFactory().newExplorer();
		MeshToMMesh3DConvert m2dTo3D = new MeshToMMesh3DConvert(outputDir, brepFile);
		m2dTo3D.exportUNV(exportUNV, unvName);
		logger.info("Read informations on boundary nodes");
		for (expF.init(shape, CADShapeEnum.FACE); expF.more(); expF.next())
		{
			iFace++;
			if (numFace != 0 && iFace != numFace)
				continue;
			if (minFace != 0 && iFace < minFace)
				continue;
			if (maxFace != 0 && iFace > maxFace)
				continue;
			m2dTo3D.computeRefs(iFace);
		}
		m2dTo3D.initialize(writeNormals);
		iFace = 0;
		for (expF.init(shape, CADShapeEnum.FACE); expF.more(); expF.next())
		{
			CADFace F = (CADFace) expF.current();
			iFace++;
			if (numFace != 0 && iFace != numFace)
				continue;
			if (minFace != 0 && iFace < minFace)
				continue;
			if (maxFace != 0 && iFace > maxFace)
				continue;
			logger.info("Importing face "+iFace);
			m2dTo3D.convert(iFace, F);
		}
		m2dTo3D.finish();
	}
	
	/** 
	 * Run the mesh
	 * @return the list of face id on which the mesher failed
	 */
	protected TIntArrayList mesh()
	{
		readProperties();
		//  Declare all variables here
		//  xmlDir:      absolute path name where XML files are stored
		//  xmlFile:     basename of the main XML file
		//  brepFile:    basename of the brep file
		
		String brepFile = (new File(geometryFile)).getName();		

		MMesh1D mesh1D = null;
		TIntArrayList badGroups = new TIntArrayList();
						
		logger.info("Loading " + geometryFile);

		CADExplorer expF = CADShapeFactory.getFactory().newExplorer();

		if (minFace != 0 || maxFace != 0)
			numFace=0;
		if (quadrangles) {
			edgeLength *= 2.0;
			//defl *= 2.0;
		}
		MeshTraitsBuilder mtb = MeshTraitsBuilder.getDefault2D();
		if (processMesh1d) {
			//  Step 1: Compute 1D mesh
			mesh1D = mesh1D(brepFile);
		}
		if (processMesh2d) {
			//  Step 2: Read the 1D mesh and compute 2D meshes
			if (mesh1D == null)
				mesh1D = MMesh1DReader.readObject(outputDir);
			//  Prepare 2D discretization
			mesh1D.duplicateEdges();
			//  Compute node labels shared by all 2D and 3D meshes
			mesh1D.updateNodeLabels();
			
			shape = mesh1D.getGeometry();
			HashMap<String, String> options2d = new HashMap<String, String>();
			options2d.put("size", ""+edgeLength);
			options2d.put("deflection", ""+deflection);

			int iFace = 0;

			logger.fine("org.jcae.mesh.Mesher.minFace="+minFace);
			logger.fine("org.jcae.mesh.Mesher.maxFace="+maxFace);
			logger.fine("org.jcae.mesh.Mesher.meshFace="+numFace);
			int nrFaces = 0;
			THashSet<CADShape> seen = new THashSet<CADShape>();
			for (expF.init(shape, CADShapeEnum.FACE); expF.more(); expF.next())
				seen.add(expF.current());
			nrFaces = seen.size();			
			seen.clear();
			for (expF.init(shape, CADShapeEnum.FACE); expF.more(); expF.next())
			{
				CADFace face = (CADFace) expF.current();
				iFace++;
				if (numFace != 0 && iFace != numFace)
					continue;
				if (minFace != 0 && iFace < minFace)
					continue;
				if (maxFace != 0 && iFace > maxFace)
					continue;
				
				if (seen.contains(face))
					continue;
				seen.add(face);
				logger.info("Meshing face " + iFace+"/"+nrFaces);
				MeshParameters mp = new MeshParameters(options2d);
				if(!mesh2D(iFace, face, mesh1D, mp, brepFile, mtb))
					badGroups.add(iFace);
			}
		}

		if (processMesh3d) {
			if (shape == null)
			{
				mesh1D = MMesh1DReader.readObject(outputDir);
				shape = mesh1D.getGeometry();
			}

			// Step 3: Read 2D meshes and compute 3D mesh
			try
			{
				mesh3D(brepFile);
			}
			catch(Exception ex)
			{
				logger.warning(ex.getMessage());
				ex.printStackTrace();
			}
			exportMesh();

		}
		if (exportTriangleSoup)
		{
			if (shape == null)
			{
				mesh1D = MMesh1DReader.readObject(outputDir);
				shape = mesh1D.getGeometry();
			}

			// Step 3bis: Read 2D meshes and compute raw 3D mesh
			MeshToSoupConvert.meshToSoup(outputDir, shape);
		}
		if (badGroups.size() > 0)
		{
			logger.info("Number of faces which cannot be meshed: "+badGroups.size());
			logger.info(""+badGroups);
		}
		return badGroups;
	}
	
	/**
	 * Create a report to the file specified by the
	 * org.jcae.mesh.Mesher.reportFile system property.
	 * @param badGroups The list of face id which failed
	 * @param startDate The date when the mesher was started
	 */
	protected void report(TIntArrayList badGroups, String startDate)
	{
		
		String endDate = DATE_FORMAT.format(new Date());
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
			int [] res = MeshReader.getInfos(outputDir);
			out.println("MESH REPORT");
			out.println("===========");
			out.println("Start date: "+startDate);
			out.println("End date: "+endDate);
			out.println("Geometry: "+geometryFile);
			out.println("Edge length criterion: "+edgeLength);
			out.println("Deflection criterion: "+deflection);
			out.println("Number of nodes: "+res[0]);
			out.println("Number of triangles: "+res[1]);
			out.println("Number of groups: "+res[2]);
			out.println("Number of groups which cannot be meshed: "+badGroups.size());
			if (badGroups.size() > 0)
				out.println(""+badGroups);
			out.println("amibe.jar build time: "+buildDate);
			Properties sys = System.getProperties();
			sys.list(out);
			out.close();
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
	}

	/**
	 * Delete a directory
	 * @param path The directory to be deleted
	 * @param avoid A file name (possibly null) which will not be deleted
	 * @return true on success
	 */
	private static boolean deleteDirectory(File path, File avoid)
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
		CADShapeFactory factory = CADShapeFactory.getFactory();
		CADShape shape = factory.newShape(brepfilename);
		CADExplorer expF = factory.newExplorer();
		int nrFaces = 0;
		for (expF.init(shape, CADShapeEnum.FACE); expF.more(); expF.next())
			nrFaces++;
		return nrFaces;
	}
	
	/**
	 * @param args The main function argument 
	 * @throws IOException If the creation of the output directory failed
	 */
	protected void parseCommandLine(String[] args) throws IOException
	{
		if (args.length < 2 || args.length > 4)
		{
			System.out.println("Usage : Mesher filename output_directory edge_length deflection");
			System.exit(0);
		}
		geometryFile=args[0];
		// if what we want is just the mesh count, print it and exit
		if(Boolean.getBoolean("org.jcae.mesh.countFaces"))
			System.exit(countFaces(geometryFile));
		
		//Init xmlDir
		if(Boolean.getBoolean("org.jcae.mesh.tmpDir.auto"))
		{
			File f=File.createTempFile("jcae","");
			f.delete();
			f.mkdirs();
			outputDir=f.getPath();
		}
		else
		{
			outputDir = args[1];
		}
		
		//Do some checks on xmlDir
		File xmlDirF=new File(outputDir);
		xmlDirF.mkdirs();
		if(!xmlDirF.exists() || !xmlDirF.isDirectory())
		{
			System.out.println("Cannot write to "+outputDir);
			return;
		}
		
		unvName=System.getProperty("org.jcae.mesh.unv.name");
		if(unvName==null)
		{
			unvName=geometryFile.substring(0, geometryFile.lastIndexOf('.'))+".unv";
			if(!Boolean.getBoolean("org.jcae.mesh.unv.nogz"))
				unvName += ".gz";
		}
		
		// Copy CAD file into xmlDir
		String geometryDir = ".";
		if (geometryFile.indexOf(File.separatorChar) >= 0)
		{
			int idx = geometryFile.lastIndexOf(File.separatorChar);
			geometryDir = geometryFile.substring(0, idx);
			geometryFile = geometryFile.substring(idx+1);
		}

		if (geometryFile.endsWith(".step") || geometryFile.endsWith(".stp") || geometryFile.endsWith(".igs"))
		{
			CADShape s = CADShapeFactory.getFactory().newShape(geometryDir+File.separator+geometryFile);
			geometryFile = geometryFile.substring(0, geometryFile.lastIndexOf('.')) + ".tmp.brep";
			s.writeNative(outputDir+File.separator+geometryFile);
		}
		else if (!geometryDir.equals(outputDir))
		{
			FileInputStream is = null;
			FileOutputStream os = null;
			try {
				is = new FileInputStream(geometryDir+File.separator+geometryFile);
				FileChannel iChannel = is.getChannel();
				os = new FileOutputStream(new File(outputDir, geometryFile), false);
				FileChannel oChannel = os.getChannel();
				oChannel.transferFrom(iChannel, 0, iChannel.size());
			}
			finally {
				if (is != null)
					is.close();
				if (os != null)
					os.close();
			}
		}
		geometryFile = outputDir+File.separator+geometryFile;
		
		edgeLength=Double.parseDouble(args[2]);
		deflection=Double.parseDouble(args[3]);		
	}
	
	/**
	 * main method, reads arguments and calls mesh() method
	 * @param args an array of String, filename, algorithm type and constraint
	 * value
	 */
	public static void main(String args[])
	{
		String startDate = DATE_FORMAT.format(new Date());
		Mesher m=new Mesher();
		try
		{
			m.parseCommandLine(args);
			TIntArrayList badGroups = m.mesh();
			m.report(badGroups, startDate);
			if(Boolean.getBoolean("org.jcae.mesh.tmpDir.delete"))
			{
				deleteDirectory(new File(m.outputDir), new File(m.unvName));
			}
			
			logger.info("End mesh");
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
