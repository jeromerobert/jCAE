/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2004,2005
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

package org.jcae.mesh.amibe.util.tests;

import org.apache.log4j.Logger;
import org.jcae.mesh.amibe.util.OctreeTest;
import org.jcae.mesh.amibe.ds.MNode3D;
import java.io.*;
import java.util.StringTokenizer;
import org.jcae.mesh.java3d.Viewer;

/**
 * Unit test to check the influence of <code>BUCKETSIZE</code>.
 * To display an octree with 100 points per cell, run
 * <pre>
 *   OctreeTestDisplayPLY 100 &lt; file.ply
 * </pre>
 */
public class OctreeTestDisplayPLY extends OctreeTest
{
	private static Logger logger=Logger.getLogger(OctreeTestDisplayPLY.class);	
	
	public OctreeTestDisplayPLY(double [] umin, double [] umax)
	{
		super (umin, umax);
	}
	
	public static void display(Viewer view, OctreeTest r)
	{
		view.addBranchGroup(r.bgOctree());
		view.setVisible(true);
		view.addBranchGroup(r.bgVertices());
		view.setVisible(true);
	}
	
	public static void main(String args[])
	{
		double u, v, w;
		boolean visu = true;
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String line;
		int nrNodes = 0;
		StringTokenizer st;
		try
		{
			line = in.readLine();
			assert line.equals("ply");
			in.readLine();
			in.readLine();
			line = in.readLine();
			st = new StringTokenizer(line);
			st.nextToken();
			st.nextToken();
			nrNodes = Integer.parseInt(st.nextToken());
			while (!line.equals("end_header"))
				line = in.readLine();
		} 
		catch (IOException e)
		{
		}
                double [] umin = { Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE };
                double [] umax = { Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE };
		logger.info("Found "+nrNodes+" nodes");
		logger.debug("Start insertion");
		double [] coord = new double[3*nrNodes];
		String oldLine = new String("foo");
		int nrDuplicates = 0;
		for (int i = 0; i + nrDuplicates< nrNodes; i++)
		{
			try
			{
				line = in.readLine();
				if (line.equals(oldLine))
				{
					nrDuplicates++;
					i--;
					continue;
				}
				oldLine = line;
				st = new StringTokenizer(line);
				coord[3*i] = Double.parseDouble(st.nextToken());
				coord[3*i+1] = Double.parseDouble(st.nextToken());
				coord[3*i+2] = Double.parseDouble(st.nextToken());
				for (int j = 0; j < 3; j++)
				{
					if (coord[3*i+j] < umin[j])
						umin[j] = coord[3*i+j];
					if (coord[3*i+j] > umax[j])
						umax[j] = coord[3*i+j];
				} 
			}
			catch (IOException e)
			{
			}
		}
		logger.info("Bounding box:");
		logger.info("  "+umin[0]+" "+umin[1]+" "+umin[2]);
		logger.info("  "+umax[0]+" "+umax[1]+" "+umax[2]);
		final OctreeTest r = new OctreeTest(umin, umax);
		int bucketSize = 10;
		if (args.length > 0)
			bucketSize = Integer.parseInt(args[0]);
		r.setBucketSize(bucketSize);
		double [] xyz = new double[3];
		for (int i = 0; i < nrNodes - nrDuplicates; i++)
		{
			System.arraycopy(coord, 3*i, xyz, 0, 3);
			r.add(new MNode3D(xyz, 0));
		}
		logger.info("Max level: "+r.getMaxLevel());
		logger.info("Number of cells: "+r.nCells);
		//CheckCoordProcedure checkproc = new CheckCoordProcedure();
		//r.walk(checkproc);
		
		final Viewer view=new Viewer();
		if (visu)
		{
			display(view, r);
			view.zoomTo(); 
/*
			view.callBack=new Runnable()
			{
				public void run()
				{
					double [] xyz = view.getLastClick();
					if (null != xyz)
					{
						MNode3D vt = r.getNearVertex(new MNode3D(xyz, 0));
						r.remove(vt);
						view.removeAllBranchGroup();
						display(view, r);
					}
				}
			};
*/
		}
	}
}
