/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2003,2004,2005, by EADS CRC
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

import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import java.util.Date;
import java.text.SimpleDateFormat;

import java.util.logging.Logger;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.algos3d.*;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;

/**
 * Reads a 3D mesh, performs computations and stores it back.
 */
public class Mesher3D
{
	private static final Logger logger=Logger.getLogger(Mesher3D.class.getName());

	/** 
	 * Mesh a CAD surface.
	 *
	 * @param brepfilename  the filename of the brep file	 
	 * @param xmlDir  directory where output files are stored
	 * @param discr  length constraint
	 * @param defl   deflection constraint
	 */
	private static void mesh(String brepfilename, String unvName, String xmlDir, double discr, double defl)
	{
		//  Declare all variables here
		//  xmlDir:      absolute path name where XML files are stored
		//  xmlFile:     basename of the main XML file
		//  brepFile:    basename of the brep file
		
		String brepFile = (new File(brepfilename)).getName();		
		Mesh mesh = new Mesh();
		String ridgeAngleProp = System.getProperty("org.jcae.mesh.xmldata.MeshReader.ridgeAngleDegre");
		if (ridgeAngleProp == null)
		{
			ridgeAngleProp = "-1.0";
			System.setProperty("org.jcae.mesh.xmldata.MeshReader.ridgeAngleDegre", ridgeAngleProp);
		}
		double ridgeAngle = Double.parseDouble(ridgeAngleProp);
		try
		{
			if (brepFile.endsWith(".unv"))
				org.jcae.mesh.amibe.util.UNVReader.readMesh(mesh, brepFile);
			else
				MeshReader.readObject3D(mesh, xmlDir);
			if (ridgeAngle > 0.0)
				mesh.buildRidges(ridgeAngle);
			HashMap<String, String> opts = new HashMap<String, String>();
			opts.put("iterations", "5");
			opts.put("boundaries", "true");
			new SmoothNodes3D(mesh, opts).compute();
			MeshWriter.writeObject3D(mesh, xmlDir, brepFile);
		}
		catch(IOException ex)
		{
			logger.warning(ex.getMessage());
			ex.printStackTrace();
		}
	}
	
	private static void report(String brepfilename, String xmlDir, double discr, double defl, String startDate)
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
			int [] res = MeshReader.getInfos(xmlDir);
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
			out.println("amibe.jar build time: "+buildDate);
			Properties sys = System.getProperties();
			sys.list(out);
			out.close();
		}
		catch (IOException ex)
		{
		}
	}

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
				unvName=filename.substring(0, filename.lastIndexOf('.'))+".unv";
			
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
			mesh(filename, unvName, xmlDir, discr.doubleValue(), defl.doubleValue());
			report(args[0], xmlDir, discr.doubleValue(), defl.doubleValue(), startDate);
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
