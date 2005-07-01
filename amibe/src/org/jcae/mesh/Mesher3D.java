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
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.algos3d.SmoothNodes3D;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;
import org.jcae.mesh.xmldata.MMesh3DReader;

/**
 * Reads a 3D mesh, performs computations and stores it back.
 */
public class Mesher3D
{
	private static Logger logger=Logger.getLogger(Mesher3D.class);

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
		//  xmlBrepDir:  path to brep file, relative to xmlDir
		//  brepFile:    basename of the brep file
		
		String brepFile = (new File(brepfilename)).getName();		
		String xmlBrepDir = relativize(new File(brepfilename).getAbsoluteFile().getParentFile(),
			new File(xmlDir).getAbsoluteFile()).getPath();
		Mesh mesh = MeshReader.readObject3D(xmlDir, "jcae3d");
		try
		{
			new SmoothNodes3D(mesh, 10).compute();
		}
		catch(Exception ex)
		{
			logger.warn(ex.getMessage());
			ex.printStackTrace();
		}
		
		MeshWriter.writeObject3D(mesh, xmlDir, "jcae3d", xmlBrepDir, brepFile, 1);
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
