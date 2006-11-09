/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.
 
    Copyright (C) 2006, by EADS CRC
 
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

import org.jcae.mesh.bora.ds.Mesh;
import org.jcae.mesh.bora.ds.ExportMesh;
import org.jcae.mesh.amibe.algos2d.*;
import org.jcae.mesh.amibe.metrics.Metric2D;
import org.jcae.mesh.amibe.metrics.Metric3D;
import org.jcae.mesh.mesher.ds.MMesh1D;
import org.jcae.mesh.xmldata.Bora1DReader;
import org.jcae.mesh.xmldata.MeshWriter;
import org.jcae.mesh.xmldata.UNVConverter;
import org.jcae.mesh.cad.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.apache.log4j.Logger;

/**
 * Run the Netgen 3D mesher.
 * Netgen is copyrighted by Joachim Schöberl <js@jku.at>
 * and is released under the GNU Lesser General Public License
 * as published by the Free Software Foundation, version 2 or above.
 * See http://www.hpfem.jku.at/netgen/
 */
public class Netgen implements AlgoInterface
{
	private static Logger logger=Logger.getLogger(Netgen.class);
	private Mesh mesh = null;
	private double maxlen;
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
					String line;
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

	public boolean compute(Mesh mesh, CADShape s, int id)
	{
		CADSolid S = (CADSolid) s;
		logger.info("Running TetGen "+banner);
		// mesh.export(s, "tetgen.poly", ExportMesh.FORMAT_POLY);
		new UNVConverter(mesh.getOutputDir()).writeSTL("netgen.stl");
		try {
			Process p = Runtime.getRuntime().exec(new String[] {"netgen", "-batchmode", "-meshfile=netgen.mesh", "-geofile=netgen.stl"});
			p.waitFor();
			if (p.exitValue() != 0)
				return false;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}
	
	public String toString()
	{
		String ret = "Algo: "+getClass().getName();
		ret += "\n"+banner;
		ret += "\nTarget size: "+maxlen;
		return ret;
	}
}
