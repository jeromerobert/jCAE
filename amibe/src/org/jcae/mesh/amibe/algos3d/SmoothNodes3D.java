/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2003, 2005
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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.amibe.algos3d;

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.OTriangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.util.PAVLSortedTree;
import java.util.HashSet;
import java.util.Iterator;
import org.apache.log4j.Logger;

/**
 * 3D node smoothing.  Triangles are sorted according to their quality,
 * They are processed iteratively beginning with the worst triangle.
 * Its three vertices are moved if they have not already been moved
 * when processing a previous triangle.  A modified Laplacian smoothing
 * is performed, as briefly explained in
 * <a href="http://www.ann.jussieu.fr/~frey/publications/ijnme4198.pdf">Adaptive Triangular-Quadrilateral Mesh Generation</a>, by Houman Borouchaky and
 * Pascal J. Frey.
 * If the final position do not invert triangles, the point is moved.
 */
// Note 1: alternatives should be tested.
// Note 2: the point should be moved only if triangle quality is improved.
public class SmoothNodes3D
{
	private static Logger logger=Logger.getLogger(SmoothNodes3D.class);
	private Mesh mesh;
	private double sizeTarget = -1.0;
	private int nloop = 10;
	private static OTriangle temp = new OTriangle();
	private static double speed = 0.1;
	
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
	 * @param s  the size target.
	 */
	public SmoothNodes3D(Mesh m, double s)
	{
		mesh = m;
		sizeTarget = s;
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
	 * Creates a <code>SmoothNodes3D</code> instance.
	 *
	 * @param m  the <code>Mesh</code> instance to refine.
	 * @param s  the size target.
	 * @param n  the number of iterations.
	 */
	public SmoothNodes3D(Mesh m, double s, int n)
	{
		mesh = m;
		sizeTarget = s;
		nloop = n;
	}
	
	/**
	 * Moves all nodes until all iterations are done.
	 */
	public void compute()
	{
		logger.debug("Running SmoothNodes3D");
		int cnt = 0;
		for (int i = 0; i < nloop; i++)
			cnt += computeMesh(mesh, sizeTarget);
		logger.info("Number of moved points: "+cnt);
	}
	
	/*
	 * Moves all nodes using a laplacian smoothing.
	 */
	private static int computeMesh(Mesh mesh, double sizeTarget)
	{
		int ret = 0;
		HashSet nodeset = new HashSet(2*mesh.getTriangles().size());
		// First compute triangle quality
		PAVLSortedTree tree = new PAVLSortedTree();
		for (Iterator itf = mesh.getTriangles().iterator(); itf.hasNext(); )
		{
			Triangle f = (Triangle) itf.next();
			if (f.isOuter())
				continue;
			tree.insert(f, cost(f));
		}
		OTriangle ot = new OTriangle();
		for (Object o = tree.first(); o != null; o = tree.next())
		{
			Triangle f = (Triangle) o;
			ot.bind(f);
			for (int i = 0; i < 3; i++)
			{
				ot.nextOTri();
				Vertex n = ot.origin();
				if (nodeset.contains(n))
					continue;
				nodeset.add(n);
				if (!n.isMutable())
					continue;
				if (smoothNode(mesh, ot, sizeTarget))
					ret++;
			}
		}
		return ret;
	}
			
	private static boolean smoothNode(Mesh mesh, OTriangle ot, double sizeTarget)
	{
		Vertex n = ot.origin();
		double[] oldp3 = n.getUV();
		
		//  Compute 3D coordinates centroid
		Vertex c = new Vertex(0.0, 0.0, 0.0);
		int nn = 0;
		double[] centroid3 = c.getUV();
		centroid3[0] = centroid3[1] = centroid3[2] = 0.;
		double lmin = Double.MAX_VALUE;
		for (Iterator itn=n.getNeighboursNodes().iterator(); itn.hasNext(); )
		{
			nn++;
			Vertex v = ((Vertex) itn.next());
			double l = n.distance3D(v);
			if (l < lmin)
				lmin = l;
			double[] newp3 = v.getUV();
			if (sizeTarget > 0.0)
			{
				// Find the point on this edge which has the
				// desired length
				if (l <= 0.0)
				{
					nn--;
					continue;
				}
				l = sizeTarget / l;
				if (l > 2.0)
					l = 2.0;
				else if (l < 0.5)
					l = 0.5;
				for (int i = 0; i < 3; i++)
					centroid3[i] += newp3[i] + l * (oldp3[i] - newp3[i]);
			}
			else
			{
				for (int i = 0; i < 3; i++)
					centroid3[i] += newp3[i];
			}
		}
		assert (nn > 0);
		for (int i = 0; i < 3; i++)
			centroid3[i] /= nn;
		double l = n.distance3D(c);
		// Move c within a sphere of radius lmin/2
		lmin *= 0.5;
		if (l > lmin && lmin > 0.0)
			l = lmin / l;
		else
			l = 1.0;
		for (int i = 0; i < 3; i++)
			centroid3[i] = oldp3[i] + speed * l * (centroid3[i] - oldp3[i]);
		if (!ot.checkNewRingNormals(centroid3))
			return false;
		if (!n.discreteProject(c))
			return false;
		n.moveTo(centroid3[0], centroid3[1], centroid3[2]);
		return true;
	}
	
	private static double cost(Triangle f)
	{
		temp.bind(f);
		assert f.vertex[0] != Vertex.outer && f.vertex[1] != Vertex.outer && f.vertex[2] != Vertex.outer : f;
		double p = f.vertex[0].distance3D(f.vertex[1]) + f.vertex[1].distance3D(f.vertex[2]) + f.vertex[2].distance3D(f.vertex[0]);
		double area = temp.computeArea();
		// No need to multiply by 12.0 * Math.sqrt(3.0)
		return area/p/p;
	}
}
