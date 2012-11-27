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
		skeleton = new Skeleton(mesh, sizeInf);
		this.mesh = mesh;
	}

	public void addGroup(String name, double size)
	{
		groupsMetric.put(mesh.getGroupIDs(name)[0], size);
	}

	/**
	 * @param groupNames
	 * @param size0 size of edges when distance is between zero and d0
	 * @param d0
	 * @param d1 Use the size set by addGroup when distance is greater than d1
	 */
	public void addFrontier(List<String> groupNames, double size0, double d0, double d1)
	{
		int[] ids = mesh.getGroupIDs(groupNames.toArray(new String[groupNames.size()]));
		for(AbstractHalfEdge edge: skeleton.getByGroups(ids))
		{
			double[] o = edge.origin().getUV();
			double[] d = edge.destination().getUV();
			addLine(o[0], o[1], o[2], true, d[0], d[1], d[2], true, size0, d0, d1);
		}
	}

	@Override
	public double getTargetSize(double x, double y, double z, int groupId) {
		Double gm = groupsMetric.get(groupId);
		if(gm == null)
			return super.getTargetSize(x, y, z, groupId);
		else
			return gm;
	}

	@Override
	public double getSize(int group) {
		return groupsMetric.get(group);
	}
}
