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
 * Run the Netgen 3D mesher.
 * Netgen is copyrighted by Joachim Schöberl <js@jku.at>
 * and is released under the GNU Lesser General Public License
 * as published by the Free Software Foundation, version 2 or above.
 * See http://www.hpfem.jku.at/netgen/
 */
public class Netgen implements AlgoInterface
{
	private static final Logger LOGGER=Logger.getLogger(Netgen.class.getName());
	private final double maxlen;
	private static boolean available = true;
	private static String banner = null;

	public Netgen(double len)
	{
		maxlen = len;
		if (banner == null)
		{
			available = true;
			banner = "";
			try {
				Process p = Runtime.getRuntime().exec(new String[] {"netgen", "-batchmode"});
				p.waitFor();
				if (p.exitValue() != 0)
					available = false;
				else
				{
					BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
					banner += input.readLine();
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
		LOGGER.info("Running Netgen "+banner);
		BSubMesh s = d.getFirstSubMesh();
		MeshTraitsBuilder mtb = MeshTraitsBuilder.getDefault3D();
		mtb.addNodeList();
		Mesh m = new Mesh(mtb);
		Storage.readAllFaces(m, d.getGraphCell(), s);
		String outDir = "netgen.tmp"+File.separator+"s"+s.getId();
		try
		{
			MeshWriter.writeObject3D(m, outDir, d.getGraphCell().getGraph().getModel().getCADFile());
		} catch (java.io.IOException ex) {
			ex.printStackTrace();
			return false;
		}
		String pfx = "netgen-"+d.getId();
		new MeshExporter.STL(outDir).write(pfx+".stl");
		try {
			Process p = Runtime.getRuntime().exec(new String[] {"netgen", "-batchmode", "-meshfile="+pfx+".mesh", "-geofile="+pfx+".stl"});
			p.waitFor();
			if (p.exitValue() != 0)
				return false;
			// Import volume mesh...
			m = new Mesh(mtb);
			MESHReader.readMesh(m, pfx+".mesh");
			d.setMesh(m);
			// ... and store it on disk
			Storage.writeSolid(d);
			// Remove temporary files
			//new File(pfx+".stl").delete();
			//new File(pfx+".mesh").delete();
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	
	@Override
	public final String toString()
	{
		String ret = "Algo: "+getClass().getName();
		ret += "\n"+banner;
		ret += "\nTarget size: "+maxlen;
		return ret;
	}
}
