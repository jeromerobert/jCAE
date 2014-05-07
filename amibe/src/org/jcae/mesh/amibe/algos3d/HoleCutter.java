/*
 * Project Info:  http://jcae.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * (C) Copyright 2014, by EADS France
 */


package org.jcae.mesh.amibe.algos3d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.amibe.util.HashFactory;

/**
 * Create a hole in a mesh from a loop of half edges.
 * The orientation of the triangles of half edges decide of the inside and
 * outside of the loop.
 * @author Jerome Robert
 */
public class HoleCutter {
	/**
	 * return the list of triangles in the hole
	 */
	public Collection<Triangle> cut(Set<AbstractHalfEdge> edges)
	{
		Set<Triangle> toReturn = HashFactory.createSet();
		Set<AbstractHalfEdge> loop = HashFactory.createSet(edges.size());
		double[] tmp1 = new double[3];
		double[] tmp2 = new double[3];
		double[] normal = new double[3];
		double[] triDir = new double[3];
		for(AbstractHalfEdge edge: edges)
		{
			edge = getCutter(edge);
			Matrix3D.computeNormal3D(edge.origin(), edge.destination(), edge.apex(), tmp1, tmp2, normal);
			Iterator<AbstractHalfEdge> it = edge.fanIterator();
			AbstractHalfEdge bestEdge = null;
			double bestDot = Double.MAX_VALUE;
			while(it.hasNext())
			{
				AbstractHalfEdge otherEdge = it.next();
				// select the fan which is inside the loop
				if(otherEdge != edge && isCutted(otherEdge))
				{
					for(int i = 0; i < 3; i++)
					{
						tmp1[i] = otherEdge.destination().get(i) -
							otherEdge.origin().get(i); //OD
						tmp2[i] = otherEdge.apex().get(i) -
							otherEdge.origin().get(i); //OA
					}
					double alpha = Matrix3D.prodSca(tmp1, tmp2) /
						Matrix3D.prodSca(tmp1, tmp1);

					for(int i = 0; i < 3; i++)
						triDir[i] = tmp2[i] - alpha * tmp1[i];

					double norm = Matrix3D.norm(triDir);
					for(int i = 0; i < 3; i++)
						triDir[i] /= norm;
					double dot = Matrix3D.prodSca(triDir, normal);
					if(dot < bestDot)
					{
						bestDot = dot;
						bestEdge = otherEdge;
					}
				}
			}
			if(bestEdge != null)
			{
				loop.add(bestEdge);
				assert bestEdge.apex().getX() < -2600: bestEdge;
				assert bestEdge.apex().getX() > -2800: bestEdge;
			}
		}
		cutHole(toReturn, loop);
		return toReturn;
	}

	private AbstractHalfEdge getCutter(AbstractHalfEdge edge)
	{
		Iterator<AbstractHalfEdge> it = edge.fanIterator();
		AbstractHalfEdge cutterEdge = null;
		while(it.hasNext())
		{
			AbstractHalfEdge e = it.next();
			if(isCutter(e))
				cutterEdge = e;
		}
		return cutterEdge;
	}

	/** Allow sub class to choose the fan which is the cutter */
	protected boolean isCutter(AbstractHalfEdge edge)
	{
		return true;
	}

	/** Allow sub class to choose the fan which is cutted */
	protected boolean isCutted(AbstractHalfEdge edge)
	{
		return true;
	}

	private static void cutHole(Collection<Triangle> triangle, Set<AbstractHalfEdge> loop) {
		AbstractHalfEdge start = loop.iterator().next();
		ArrayList<AbstractHalfEdge> front = new ArrayList<AbstractHalfEdge>();
		if(!loop.contains(start.next()))
			front.add(start.next());
		if(!loop.contains(start.prev()))
			front.add(start.prev());
		triangle.add(start.getTri());
		while(!front.isEmpty())
		{
			AbstractHalfEdge e = front.get(front.size() - 1).sym();
			front.remove(front.size() - 1);
			Triangle t = e.getTri();
			if(triangle.add(t))
			{
				if(!loop.contains(e.next()))
					front.add(e.next());
				if(!loop.contains(e.prev()))
					front.add(e.prev());
			}
		}
	}
}
