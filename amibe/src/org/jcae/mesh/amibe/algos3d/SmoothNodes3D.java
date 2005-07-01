/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2003 Jerome Robert <jeromerobert@users.sourceforge.net>

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

package org.jcae.mesh.amibe.algos3d;

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.util.PAVLSortedTree;
import java.util.HashSet;
import java.util.Iterator;
import org.apache.log4j.Logger;

/**
 * Laplacian smoothing.
 */

public class SmoothNodes3D
{
	private static Logger logger=Logger.getLogger(SmoothNodes3D.class);
	private Mesh mesh;
	private int nloop = 10;
	
	/**
	 * Creates a <code>SmoothNodes3D</code> instance.
	 *
	 * @param m  the <code>Mesh</code> instance to refine.
	 */
	public SmoothNodes3D(Mesh m)
	{
		mesh = m;
	}
	
	/**
	 * Creates a <code>SmoothNodes3D</code> instance.
	 *
	 * @param m  the <code>Mesh</code> instance to refine.
	 * @param n  the number of iterations.
	 */
	public SmoothNodes3D(Mesh m, int n)
	{
		mesh = m;
		nloop = n;
	}
	
	/**
	 * Moves all nodes until all iterations are done.
	 *
	 * @see #computeFace
	 */
	public void compute()
	{
		logger.debug("Running SmoothNodes3D");
		for (int i = 0; i < nloop; i++)
			computeMesh(mesh);
	}
	
	/*
	 * Moves all nodes using a laplacian smoothing.
	 */
	private static void computeMesh(Mesh mesh)
	{
		HashSet nodeset = new HashSet(2*mesh.getTriangles().size());
		// First compute triangle quality
		PAVLSortedTree tree = new PAVLSortedTree();
		for (Iterator itf = mesh.getTriangles().iterator(); itf.hasNext(); )
		{
			Triangle f = (Triangle) itf.next();
			if (f.isOuter())
				continue;
			double alpha0 = f.vertex[0].angle3D(f.vertex[1], f.vertex[2]);
			double alpha1 = f.vertex[1].angle3D(f.vertex[2], f.vertex[0]);
			double alpha2 = f.vertex[2].angle3D(f.vertex[0], f.vertex[1]);
			tree.insert(f, Math.min(alpha0, Math.min(alpha1, alpha2)));
		}
		for (Object o = tree.first(); o != null; o = tree.next())
		{
			Triangle f = (Triangle) o;
			for (int i = 0; i < 3; i++)
			{
				Vertex n = f.vertex[i];
				if (nodeset.contains(n))
					continue;
				nodeset.add(n);
				if (!n.isMutable())
					continue;
				smoothNode(mesh, n);
			}
		}
	}
			
	private static void smoothNode(Mesh mesh, Vertex n)
	{
		double[] oldp3 = n.getUV();
		
		//  Compute 3D coordinates centroid
		int nn = 0;
		double[] centroid3 = new double[3];
		centroid3[0] = centroid3[1] = centroid3[2] = 0.;
		for (Iterator itn=n.getNeighboursNodes().iterator(); itn.hasNext(); )
		{
			nn++;
			double[] newp3 = ((Vertex) itn.next()).getUV();
			centroid3[0] += newp3[0];
			centroid3[1] += newp3[1];
			centroid3[2] += newp3[2];
		}
		assert (nn > 0);
		centroid3[0] /= nn;
		centroid3[1] /= nn;
		centroid3[2] /= nn;
		n.moveTo(centroid3[0], centroid3[1], centroid3[2]);
	}
}
