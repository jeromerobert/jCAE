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

import org.jcae.mesh.bora.ds.BCADGraphCell;
import org.jcae.mesh.amibe.algos2d.*;
import org.jcae.mesh.xmldata.UNVConverter;
import org.jcae.mesh.cad.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.apache.log4j.Logger;

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
	private static Logger logger=Logger.getLogger(TetGen.class);
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
				Process p = Runtime.getRuntime().exec(new String[] {"tetgen", "-version"});
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

	public boolean compute(BCADGraphCell mesh)
	{
		CADSolid S = (CADSolid) mesh.getShape();
		logger.info("Running TetGen "+banner);
		// mesh.export(s, "tetgen.poly", ExportMesh.FORMAT_POLY);
		new UNVConverter(mesh.getGraph().getModel().getOutputDir()).writePOLY("tetgen.poly");
		try {
			Process p = Runtime.getRuntime().exec(new String[] {"tetgen", "-a"+volume+"pYNEFg", "tetgen"});
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
		ret += "\nMax volume: "+volume;
		return ret;
	}
}
