/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2006, by EADS CRC
    Copyright (C) 2007,2008,2009, by EADS France
 
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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.bora.algo;

import org.jcae.mesh.bora.ds.BDiscretization;
import org.jcae.mesh.bora.ds.BSubMesh;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.bora.xmldata.Storage;
import org.jcae.mesh.bora.xmldata.MESHReader;
import org.jcae.mesh.xmldata.MeshExporter;
import org.jcae.mesh.xmldata.MeshWriter;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.logging.Logger;

/**
 * Run the Tetgen 3D mesher.
 * TetGen is Copyright 2002, 2004, 2005, 2006 Hang Si
 * Rathausstr. 9, 10178 Berlin, Germany
 * si@wias-berlin.de
 * This is *not* a free software and can not be redistributed
 * in jCAE.
 */
public class TetGen implements AlgoInterface
{
	private static final Logger LOGGER=Logger.getLogger(TetGen.class.getName());
	private static final String tetgenCmd = "tetgen";
	private double volume;
	private static boolean available = true;
	private static String banner = null;

	public TetGen(double len)
	{
		// Max volume
		volume = 5.0*len*len*len;
		if (banner == null)
		{
			available = true;
			banner = "";
			try {
				Process p = Runtime.getRuntime().exec(new String[] {tetgenCmd, "-version"});
				p.waitFor();
				if (p.exitValue() != 0)
					available = false;
				else
				{
					String line;
					BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
					while ((line = input.readLine()) != null)
						banner += line;
					input.close();
				}
			} catch (Exception ex) {
				available = false;
			}
		}
	}

	public boolean isAvailable()
	{
		return available;
	}

	public int getOrientation(int o)
	{
		return o;
	}

	public boolean compute(BDiscretization d)
	{
		LOGGER.info("Running TetGen "+banner);
		BSubMesh s = d.getFirstSubMesh();
		MeshTraitsBuilder mtb = MeshTraitsBuilder.getDefault3D();
		mtb.addNodeList();
		Mesh m = new Mesh(mtb);
		Storage.readAllFaces(m, d.getGraphCell(), s);
		String outDir = "tetgen.tmp"+File.separator+"s"+s.getId();
		try
		{
			MeshWriter.writeObject3D(m, outDir, d.getGraphCell().getGraph().getModel().getCADFile());
		} catch (java.io.IOException ex) {
			ex.printStackTrace();
			return false;
		}
		String pfx = "tetgen-"+d.getId();
		new MeshExporter.POLY(outDir).write(pfx+".poly");
		try {
			String args = "-a"+volume+"pYNEFg";
			Process p = Runtime.getRuntime().exec(new String[] {tetgenCmd, args, pfx});
			p.waitFor();
			if (p.exitValue() != 0)
				return false;
			// Import volume mesh...
			m = new Mesh(mtb);
			MESHReader.readMesh(m, pfx+".1.mesh");
			d.setMesh(m);
			// ... and store it on disk
			Storage.writeSolid(d);
			// Remove temporary files
			new File(pfx+".poly").delete();
			new File(pfx+".1.mesh").delete();
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	
	@Override
	public String toString()
	{
		String ret = "Algo: "+getClass().getName();
		ret += "\n"+banner;
		ret += "\nMax volume: "+volume;
		return ret;
	}
}
