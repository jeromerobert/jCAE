/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2005
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

import java.util.Iterator;
import org.jcae.mesh.amibe.ds.MFace3D;
import org.jcae.mesh.amibe.ds.MMesh3D;
import org.jcae.mesh.xmldata.MMesh3DReader;
import org.jcae.mesh.amibe.validation.*;
import org.apache.log4j.Logger;

/**
 * This class illustrates how to perform quality checks.
 */
public class MeshValid3D
{
	private static Logger logger=Logger.getLogger(MeshValid3D.class);

	private static void check(String brepfilename, String xmlDir)
	{
		logger.info("Reading 3D mesh");
		String xmlFile = "jcae3d";
		MMesh3D mesh3D = MMesh3DReader.readObject(xmlDir, xmlFile);
		QualityFloat data = new QualityFloat(1000);
		NodeConnectivity3D qproc = new NodeConnectivity3D(mesh3D);
		data.setQualityProcedure(qproc);
		for (Iterator itf = mesh3D.getFacesIterator(); itf.hasNext();)
		{
			MFace3D f= (MFace3D) itf.next();
			data.compute(f);
		}
		data.finish();
		data.split(10);
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
		check(filename, xmlDir);
	}
}
