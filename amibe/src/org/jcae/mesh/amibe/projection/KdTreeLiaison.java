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

package org.jcae.mesh.amibe.projection;

import gnu.trove.map.hash.TIntIntHashMap;
import java.util.Collection;
import java.util.NoSuchElementException;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Location;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;

/**
 *
 * @author Jerome Robert
 */
//TODO possible optimization:
// - one TriangleKdTree by group ?
// - check how MeshLiaison methods calls sequence to limit the number of call
//   to getClosestTriangle
public class KdTreeLiaison extends MeshLiaison{
	private final TriangleKdTree kdTree;
	private final transient Location tmpCoords = new Location();
	public KdTreeLiaison(Mesh backgroundMesh, MeshTraitsBuilder mtb) {
		super(backgroundMesh, mtb);
		kdTree = new TriangleKdTree(backgroundMesh);
	}

	@Override
	protected void init(Collection<Vertex> backgroundNodeset) { }

	@Override
	public void backupRestore(Vertex v, boolean restore, int group) { }

	@Override
	protected boolean move(Vertex v, Location target, boolean backup, int group,
		boolean doCheck) {
		Triangle t = kdTree.getClosestTriangle(target, tmpCoords, group);
		v.moveTo(tmpCoords);
		return t != null;
	}

	private Triangle getBackgroundTriangle(Vertex v) {
		Triangle toReturn = kdTree.getClosestTriangle(v, null, -1);
		if(toReturn == null)
			throw new NoSuchElementException(v.toString());
		return toReturn;
	}

	@Override
	public double[] getBackgroundNormal(Vertex v) {
		double[] normal = new double[3];
		Triangle t = getBackgroundTriangle(v);
		Matrix3D.computeNormal3D(t.vertex[0], t.vertex[1], t.vertex[2],
			work1, work2, normal);
		return normal;
	}

	@Override
	public Triangle addVertex(Vertex v, Vertex start, double maxError, int group)
	{
		move(v, v, false, -1, false);
		return null;
	}

	@Override
	public void addVertex(Vertex v, Triangle bgT) {}

	@Override
	public void addVertex(Vertex newV, Vertex existingVertex) { }

	@Override
	public void addVertex(Vertex v, Vertex existingVertex, double[] normal) {
		Triangle t = kdTree.getClosestTriangle(v, tmpCoords, -1);
		v.moveTo(tmpCoords);
		Matrix3D.computeNormal3D(t.vertex[0], t.vertex[1], t.vertex[2],
			work1, work2, normal);
	}

	@Override
	public void removeVertex(Vertex v) {
	}

	@Override
	public void replaceVertex(Vertex oldV, Vertex newV) {
	}

	@Override
	public void updateAll() { }

	@Override
	public void initBgMap(TIntIntHashMap numberOfTriangles,
		Collection<Vertex> nodeset) {
	}

	@Override
	public void clearBgMap() { }

	@Override
	public void addVertexInNeighborBgMap(Vertex v, Triangle bgT) { }
}
