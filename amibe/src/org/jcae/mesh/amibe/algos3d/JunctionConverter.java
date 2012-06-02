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
 * (C) Copyright 2012, by EADS France
 */
package org.jcae.mesh.amibe.algos3d;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Vertex;

/**
 * Convert junctions between beams and triangles to group of node
 * @author Jerome Robert
 */
public class JunctionConverter {
	private transient int groupID = 1;
	private final Collection<String> groupNames = new HashSet<String>();
	private final Mesh mesh;
	public JunctionConverter(Mesh mesh)
	{
		this.mesh = mesh;
		for(int i = 0; i < mesh.getNumberOfGroups(); i++)
			groupNames.add(mesh.getGroupName(i));
	}

	public void compute()
	{
		List<Vertex> beams = mesh.getBeams();
		int n = beams.size();
		for(int i = 0; i < n; i++)
		{
			Vertex v = beams.get(i);
			if(v.getLink() != null)
				processVertex(i, v, mesh);
		}
	}

	private String groupName()
	{
		String r;
		do
		{
			r = groupName(groupID++);
		}
		while(groupNames.contains(r));
		return r;
	}

	protected String groupName(int i)
	{
		return "J_"+i;
	}

	private void processVertex(int i, Vertex v, Mesh mesh) {
		Iterator<AbstractHalfEdge> it = v.getNeighbourIteratorAbstractHalfEdge();
		double min = Double.MAX_VALUE;
		Vertex bestOther = null;
		while(it.hasNext())
		{
			AbstractHalfEdge edge = it.next();
			Vertex other = edge.destination();
			double d2 = other.sqrDistance3D(v);
			if(d2 < min)
			{
				min = d2;
				bestOther = other;
			}
		}
		String group = groupName();
		Vertex newV = mesh.createVertex(v.getUV());
		newV.setMutable(false);
		mesh.add(newV);
		mesh.setBeam(i, newV);
		mesh.setVertexGroup(v, group);
		mesh.setVertexGroup(newV, group);
		mesh.setVertexGroup(bestOther, group);
	}
}
