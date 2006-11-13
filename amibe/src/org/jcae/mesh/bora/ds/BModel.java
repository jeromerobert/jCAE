/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

   (C) Copyright 2006, by EADS CRC

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


package org.jcae.mesh.bora.ds;

import org.jcae.mesh.bora.xmldata.*;
import org.jcae.mesh.cad.CADShapeBuilder;
import org.jcae.mesh.cad.CADShape;
import java.util.LinkedHashSet;
import java.util.Stack;
import java.io.File;

import org.apache.log4j.Logger;

/**
 * CAD object.
 */
public class BModel
{
	private static Logger logger=Logger.getLogger(BModel.class);
	//   Next available index
	private static int freeIndex = 1;
	//   Model number
	private int id;
	//   CAD graph
	private BCADGraph cad;
	//   User-defined groups
	private LinkedHashSet groups;
	//   Geometry file
	private String cadFile;
	//   Output variables
	private String xmlDir;
	private String xmlBrepDir;

	/**
	 * Bind a CAD representation to a disk directory.
	 */
	public BModel (String brep, String out)
	{
		id = freeIndex;
		if (logger.isDebugEnabled())
			logger.debug("Building model "+id+" from "+brep+" into "+out);
		freeIndex++;
		CADShapeBuilder factory = CADShapeBuilder.factory;
		xmlDir = out;
		File xmlDirF = new File(xmlDir);
		xmlDirF.mkdirs();
		if(!xmlDirF.exists() || !xmlDirF.isDirectory())
		{
			System.out.println("Cannot write to "+xmlDir);
			return;
		}

		cadFile = (new File(brep)).getAbsoluteFile().getPath();
		xmlBrepDir = relativize(new File(brep).getAbsoluteFile().getParentFile(), new File(xmlDir).getAbsoluteFile()).getPath();
		// CAD graph
		cad = new BCADGraph(this, factory.newShape(brep));
	}
	
	public int getId()
	{
		return id;
	}

	public String getCADFile()
	{
		return cadFile;
	}

	public CADShape getCADShape()
	{
		return cad.getRootCell().getShape();
	}

	public String getOutputDir()
	{
		return xmlDir;
	}

	public BCADGraph getGraph()
	{
		return cad;
	}

	private static File relativize(File file, File reference)
	{
		File current = file;
		Stack l = new Stack();
		while (current != null && !current.equals(reference))
		{
			l.push(current.getName());
			current = current.getParentFile();
		}
		if (l.isEmpty())
			return new File(".");
		else if (current == null)
			return file;
		else
		{
			current = new File(l.pop().toString());
			while(!l.isEmpty())
				current = new File(current, l.pop().toString());
			return current;
		}
	}

	public BGroup newMesh()
	{
		BGroup ret = new BGroup(this, cad.getFreeIndex());
		return ret;
	}

	// Sample test
	public static void main(String args[])
	{
		String file = "brep/2cubes.brep";

		BModel model = new BModel(file, "out");
		model.cad.printShapes();
		BModelWriter.writeObject(model, "zzz", "ooo");

	}
}
