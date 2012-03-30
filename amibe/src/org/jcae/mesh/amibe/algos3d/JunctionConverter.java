/*
 * This source code is the property of EADS France. No part of it shall
 * be reproduced or transmitted without the express prior written
 * authorization of EADS France, and its contents shall not be disclosed.
 * Copyright EADS France.
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
