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
import java.util.Map;
import java.util.Set;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.amibe.util.HashFactory;

/**
 * Create a hole in a mesh from a loop of half edges.
 * The orientation of the triangles of half edges decide of the inside and
 * outside of the loop.
 * @author Jerome Robert
 */
public class HoleCutter {
	private boolean isClosedLoop(Collection<AbstractHalfEdge> edges)
	{
		Map<Vertex, Vertex> map = HashFactory.createMap(edges.size());
		Vertex start = null;
		for(AbstractHalfEdge e: edges)
		{
			map.put(e.origin(), e.destination());
			start = e.destination();
		}
		int nbEdges = 0;
		Vertex current = start;
		while(true)
		{
			current = map.get(current);
			nbEdges ++;
			if(current == null)
				return false;

			if(current == start)
				return nbEdges == edges.size();

			if(nbEdges > edges.size())
				//we are in a sub loop
				return false;
		}
	}

	/**
	 * Compute the vector normal to an half edge on its triangle
	 * @param result the normalized result vector
	 */
	private void edgeNormal(AbstractHalfEdge edge, double[] result, double[] tmp1, double[] tmp2)
	{
		for(int i = 0; i < 3; i++)
		{
			tmp1[i] = edge.destination().get(i) -
				edge.origin().get(i); //OD
			tmp2[i] = edge.apex().get(i) -
				edge.origin().get(i); //OA
		}
		double alpha = Matrix3D.prodSca(tmp1, tmp2) /
			Matrix3D.prodSca(tmp1, tmp1);

		for(int i = 0; i < 3; i++)
			result[i] = tmp2[i] - alpha * tmp1[i];

		double norm = Matrix3D.norm(result);
		for(int i = 0; i < 3; i++)
			result[i] /= norm;
	}

	/**
	 * return the list of triangles in the hole
	 */
	public Collection<Triangle> cut(Collection<AbstractHalfEdge> edges)
	{
		Set<Triangle> toReturn = HashFactory.createSet();
		if(!isClosedLoop(edges))
			return toReturn;
		Set<AbstractHalfEdge> loop = HashFactory.createSet(edges.size());
		double[] tmp1 = new double[3];
		double[] tmp2 = new double[3];
		double[] normal = new double[3];
		double[] triDir = new double[3];
		for(AbstractHalfEdge edge: edges)
		{
			AbstractHalfEdge cEdge = getCutter(edge);
			if(isNormalCut(edge))
			{
				// compute the opposite of the triangle normal because in
				// normalCut mode, the normal target the out side of the loop
				Matrix3D.computeNormal3D(cEdge.origin(), cEdge.apex(), cEdge.destination(), tmp1, tmp2, normal);
			}
			else
				edgeNormal(cEdge, normal, tmp1, tmp2);
			Iterator<AbstractHalfEdge> it = cEdge.fanIterator();
			AbstractHalfEdge bestEdge = null;
			double bestDot = Double.NEGATIVE_INFINITY;
			while(it.hasNext())
			{
				AbstractHalfEdge otherEdge = it.next();
				// select the fan which is inside the loop
				if(otherEdge != cEdge && isCutted(otherEdge))
				{
					edgeNormal(otherEdge, triDir, tmp1, tmp2);
					double dot = Matrix3D.prodSca(triDir, normal);
					// keep the most parallel vector
					if(dot > bestDot)
					{
						bestDot = dot;
						bestEdge = otherEdge;
					}
				}
			}
			if(bestEdge != null)
			{
				loop.add(bestEdge);
			}
		}
		if(!loop.isEmpty())
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

	/**
	 * return true if the cutting triangle is expected to be perpendicular
	 * to the cutted surface, false if the cutting triangle is expected to be
	 * parallel to the cutted surface.
	 */
	protected boolean isNormalCut(AbstractHalfEdge edge) {
		return true;
	}
	/**
	 * tag the triangle which are inside the hole
	 * @param triangle the list of tagged triangles
	 * @param loop the border of the hole
	 */
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
			assert e != null: front.get(front.size() - 1);
			front.remove(front.size() - 1);
			Triangle t = e.getTri();
			if(triangle.add(t))
			{
				if(!loop.contains(e.next()))
				{
					assert !e.next().hasAttributes(AbstractHalfEdge.OUTER): e+"\n"+e.next();
					front.add(e.next());
				}
				if(!loop.contains(e.prev()))
				{
					assert !e.prev().hasAttributes(AbstractHalfEdge.OUTER): e+"\n"+e.next();
					front.add(e.prev());
				}
			}
		}
	}
}
