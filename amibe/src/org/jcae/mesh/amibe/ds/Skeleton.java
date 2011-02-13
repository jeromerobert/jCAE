/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2011, by EADS France

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

package org.jcae.mesh.amibe.ds;

import gnu.trove.TIntObjectHashMap;
import java.util.Collection;
import java.util.ArrayList;

import java.util.logging.Logger;

public class Skeleton
{
	private static final Logger LOGGER=Logger.getLogger(Skeleton.class.getName());
	private final TIntObjectHashMap<Collection<Line>> mapGroupBorder = new TIntObjectHashMap<Collection<Line>>();

	public Skeleton(Mesh mesh)
	{
		if (!mesh.hasAdjacency())
			throw new IllegalArgumentException("Mesh does not contain adjacency relations");
		AbstractHalfEdge ot = null;
		for (Triangle t : mesh.getTriangles())
		{
			if (t.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			int groupId = t.getGroupId();
			Collection<Line> borders = mapGroupBorder.get(groupId);
			if (borders == null)
			{
				borders = new ArrayList<Line>();
				mapGroupBorder.put(groupId, borders);
			}
			// This test is performed here so that mapGroupBorder.get(N)
			// is not null if a group has no boundary edge.
			if (!t.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD))
				continue;
			ot = t.getAbstractHalfEdge(ot);
			for (int i = 0; i < 3; i++)
			{
				ot = ot.next();
				if (ot.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD))
					borders.add(new Line(ot.origin(), ot.destination()));
			}
		}
	}

	public double getSqrDistance(Vertex v, int groupId)
	{
		Collection<Line> borders = mapGroupBorder.get(groupId);
		if (borders == null)
			throw new IllegalArgumentException("group identifier not found");
		double[] pos = v.getUV();
		double dMin = Double.MAX_VALUE;
		for (Line l : borders)
		{
			double d = l.sqrDistance(pos);
			if (d < dMin)
				dMin = d;
		}
		return dMin;
	}

	public boolean isNearer(Vertex v, int groupId, double distance2)
	{
		Collection<Line> borders = mapGroupBorder.get(groupId);
		if (borders == null)
			throw new IllegalArgumentException("group identifier "+groupId+" not found");
		double[] pos = v.getUV();
		for (Line l : borders)
		{
			if (l.sqrDistance(pos) <= distance2)
			{
				return true;
			}
		}
		return false;
	}

	private static class Line
	{
		private final double[] origin = new double[3];
		private final double[] direction = new double[3];
		private final double sqrNormDirection;
		public Line(Vertex v1, Vertex v2)
		{
			System.arraycopy(v1.getUV(), 0, origin, 0, 3);
			double[] end = v2.getUV();
			direction[0] = end[0] - origin[0];
			direction[1] = end[1] - origin[1];
			direction[2] = end[2] - origin[2];
			sqrNormDirection = direction[0] * direction[0] + direction[1] * direction[1] + direction[2] * direction[2];
		}

		public double sqrDistance(double[] v)
		{
			double t =
				direction[0] * (v[0] - origin[0]) +
				direction[1] * (v[1] - origin[1]) +
				direction[2] * (v[2] - origin[2]);
			if (t <= 0)
				t = 0.0;
			else if (t >= sqrNormDirection)
				t = 1.0;
			else
				t /= sqrNormDirection;
			double dx = v[0] - (origin[0] + t * direction[0]);
			double dy = v[1] - (origin[1] + t * direction[1]);
			double dz = v[2] - (origin[2] + t * direction[2]);
			return dx * dx + dy * dy + dz * dz;
		}
	}
}
