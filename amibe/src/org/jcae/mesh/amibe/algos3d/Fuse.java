/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2003,2004,2005, by EADS CRC
    Copyright (C) 2007,2008, by EADS France

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

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.KdTree;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.amibe.util.HashFactory;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;

/**
 * Fuse near nodes in a Mesh instance.
 */
public class Fuse
{
	private static final Logger LOGGER=Logger.getLogger(Fuse.class.getName());
	private final Mesh mesh;
	private final double[] tolerances;

	/**
	 * Creates a <code>Fuse</code> instance.
	 *
	 * @param m  the <code>MMesh3D</code> instance to refine.
	 * @param eps  tolerance.
	 */
	public Fuse(Mesh m, double ... eps)
	{
		mesh = m;
		int k = 0;
		tolerances = new double[eps.length];
		for(double e: eps)
			tolerances[k++] = e * e;
	}

	private Collection<Vertex> getNodes()
	{
		if(mesh.hasNodes())
			return mesh.getNodes();
		else
		{
			Collection<Triangle> triangles = mesh.getTriangles();
			HashSet<Vertex> toReturn = new HashSet<Vertex>(triangles.size() / 2 * 4 / 3);
			for(Triangle t: triangles)
			{
				toReturn.add(t.getV0());
				toReturn.add(t.getV1());
				toReturn.add(t.getV2());
			}
			return toReturn;
		}
	}

	public void compute()
	{
		LOGGER.fine("Running Fuse");
		double [] bmin = new double[3];
		double [] bmax = new double[3];
		for (int i = 0; i < 3; i++)
		{
			bmin[i] = Double.MAX_VALUE;
			bmax[i] = Double.MIN_VALUE;
		}
		Collection<Vertex> nodes = getNodes();
		for (Vertex n: nodes)
		{
			bmin[0] = Math.min(bmin[0], n.getX());
			bmax[0] = Math.max(bmax[0], n.getX());
			bmin[1] = Math.min(bmin[1], n.getY());
			bmax[1] = Math.max(bmax[1], n.getY());
			bmin[2] = Math.min(bmin[2], n.getZ());
			bmax[2] = Math.max(bmax[2], n.getZ());
		}
		//  Enlarge the bounding box
		for (int i = 0; i < 3; i++)
		{
			if (bmin[i] > 0.0)
				bmin[i] *= 0.99;
			else
				bmin[i] *= 1.01;
			if (bmax[i] > 0.0)
				bmax[i] *= 1.01;
			else
				bmax[i] *= 0.99;
		}
		double [] bbox = new double[6];
		for (int i = 0; i < 3; i++)
		{
			bbox[i] = bmin[i];
			bbox[i+3] = bmax[i];
		}
		Map<Vertex, Vertex> map = HashFactory.createMap();
		for(double tolerance: tolerances)
		{
			KdTree<Vertex> octree = new KdTree<Vertex>(bbox);
			map.clear();
			for (Vertex n: nodes)
			{
				Vertex p = octree.getNearestVertex(mesh.getMetric(n), n);
				if (p == null || n.sqrDistance3D(p) > tolerance)
					octree.add(n);
				else
				{
					LOGGER.log(Level.FINE,
						"Node {0} is removed, it is too close from {1}",
						new Object[]{n, p});
					map.put(n, p);
					n.setRef(0);
				}
			}
			LOGGER.log(Level.INFO, "{0} node(s) are removed", map.size());
			Iterator<Triangle> itt = mesh.getTriangles().iterator();
			while(itt.hasNext())
			{
				Triangle t = itt.next();
				for (int j = 0; j < 3; j++)
				{
					Vertex n = t.getV(j);
					Vertex p = map.get(n);
					if (p != null)
					{
						t.setV(j, p);
						nodes.remove(n);
					}
				}
				if(t.getV0() == t.getV1() || t.getV0() == t.getV2() || t.getV1() == t.getV2())
					itt.remove();
			}
		}
	}
}
