/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2004,2005, by EADS CRC
    Copyright (C) 2007, by EADS France

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

package org.jcae.mesh.amibe.metrics;

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.viewer3d.bg.ViewableBG;
import org.jcae.viewer3d.View;

import java.io.*;
import java.util.StringTokenizer;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

/**
 * Unit test to check the influence of <code>BUCKETSIZE</code>.
 * To display an octree with 100 points per cell, run
 * <pre>
 *   OctreeSampleDisplayPLY 100 &lt; file.ply
 * </pre>
 */
public class OctreeSampleDisplayPLY extends OctreeSample
{
	public OctreeSampleDisplayPLY(KdTree<Vertex> o)
	{
		super(o);
	}
	
	public static void main(String args[])
	{
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String line;
		int nrNodes = 0;
		final Mesh mesh = new Mesh();
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
		double [] coord = new double[3*nrNodes];
		String oldLine = "foo";
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
		int bucketSize = 10;
		if (args.length > 0)
			bucketSize = Integer.parseInt(args[0]);
		double [] bbox = new double[6];
		for (int i = 0; i < 3; i++)
		{
			bbox[i] = umin[i];
			bbox[i+3] = umax[i];
		}
		final KdTree<Vertex> r = new KdTree<Vertex>(bbox, bucketSize);
		final OctreeSample t = new OctreeSample(r);
		double [] xyz = new double[3];
		for (int i = 0; i < nrNodes - nrDuplicates; i++)
		{
			System.arraycopy(coord, 3*i, xyz, 0, 3);
			r.add(mesh.createVertex(xyz));
		}
		//CheckCoordProcedure checkproc = new CheckCoordProcedure();
		//r.walk(checkproc);
		
		JFrame feFrame = new JFrame("PLY Viewer Demo");
		final View view = new View(feFrame);
		feFrame.setSize(800,600);
		feFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		view.add(new ViewableBG(t.bgOctree()));
		view.add(new ViewableBG(t.bgVertices()));
		view.fitAll(); 
		feFrame.getContentPane().add(view);
		feFrame.setVisible(true);
	}
}
