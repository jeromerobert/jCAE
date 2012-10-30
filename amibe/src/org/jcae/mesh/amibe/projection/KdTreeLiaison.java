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

import java.util.Collection;
import java.util.NoSuchElementException;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;

/**
 *
 * @author Jerome Robert
 */
public class KdTreeLiaison extends MeshLiaison{
	private final TriangleKdTree kdTree;
	private transient double[] tmpCoords = new double[3];
	public KdTreeLiaison(Mesh backgroundMesh, MeshTraitsBuilder mtb) {
		super(backgroundMesh, mtb);
		kdTree = new TriangleKdTree(backgroundMesh);
	}

	@Override
	protected void init(Collection<Vertex> backgroundNodeset) { }

	@Override
	public void backupRestore(Vertex v, boolean restore, int group) { }

	@Override
	protected boolean move(Vertex v, double[] target, boolean backup, int group,
		boolean doCheck) {
		Triangle t = kdTree.getClosestTriangle(target, tmpCoords, group);
		v.moveTo(tmpCoords[0], tmpCoords[1], tmpCoords[2]);
		return t != null;
	}

	@Override
	public Triangle getBackgroundTriangle(Vertex v) {
		Triangle toReturn = kdTree.getClosestTriangle(v.getUV(), null, -1);
		if(toReturn == null)
			throw new NoSuchElementException(v.toString());
		return toReturn;
	}

	@Override
	public double[] getBackgroundNormal(Vertex v) {
		double[] normal = new double[3];
		Triangle t = getBackgroundTriangle(v);
		Matrix3D.computeNormal3D(t.vertex[0].getUV(), t.vertex[1].getUV(),
		t.vertex[2].getUV(), work1, work2, normal);
		return normal;
	}

	@Override
	public void addVertex(Vertex v, Triangle bgT) {}

	@Override
	public Triangle removeVertex(Vertex v) {
		return getBackgroundTriangle(v);
	}

	@Override
	public void updateAll() { }

}
