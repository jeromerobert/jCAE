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

package org.jcae.mesh.amibe.metrics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jcae.mesh.amibe.algos3d.Skeleton;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Vertex;

/**
 * Allow to set a given edge size at the frontier between 2 groups
 * @author Jerome Robert
 */
public class GroupMetric extends DistanceMetric {
	private final Skeleton skeleton;
	private final Mesh mesh;
	private final Map<Integer, Double> groupsMetric = new HashMap<Integer, Double>();
	public GroupMetric(Mesh mesh, double sizeInf) {
		super(sizeInf);
		skeleton = new Skeleton(mesh, Math.PI * 2);
		this.mesh = mesh;
	}

	public void addGroup(String name, double size)
	{
		groupsMetric.put(mesh.getGroupIDs(name)[0], size);
	}

	/**
	 * Add the frontier between n groups.
	 * If n is 1 only free edges will be added. if it's 2 only manifold edges
	 * will be added. If it's more than 2 only non-manifold edges will be added.
	 * @param groupNames
	 * @param size0 size of edges when distance is between zero and d0
	 * @param d0
	 * @param d1 Use the size set by addGroup when distance is greater than d1
	 */
	public void addFrontier(List<String> groupNames, double size0, double d0, double d1)
	{
		int[] ids = mesh.getGroupIDs(groupNames.toArray(new String[groupNames.size()]));
		addFrontier(skeleton.getByGroups(ids), size0, d0, d1);
	}

	private void addFrontier(Iterable<AbstractHalfEdge> edges, double size0, double d0, double d1)
	{
		for(AbstractHalfEdge edge: edges)
		{
			Vertex o = edge.origin();
			Vertex d = edge.destination();
			addLine(o.getX(), o.getY(), o.getZ(), true, d.getX(), d.getY(), d.getZ(), true, size0, d0, d1);
		}
	}

	/** Add the boundaries of a group, including free and non-manifold edges */
	public void addBoundary(String groupName, double size0, double d0, double d1)
	{
		addFrontier(skeleton.getByGroups(mesh.getGroupIDs(groupName)[0]),
			size0, d0, d1);
	}

	@Override
	public double getSize(int group) {
		Double toReturn = groupsMetric.get(group);
		return toReturn == null ? sizeInf : toReturn;
	}
}
