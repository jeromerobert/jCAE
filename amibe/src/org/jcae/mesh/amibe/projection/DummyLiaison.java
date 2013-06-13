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
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Location;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;

/**
 * A MeshLiaison which concider the current mesh as background mesh.
 * In this liaison vertices projection never move points and the normal is
 * either provided in the constructor (best for 2D meshes), either computed
 * from a neighbor triangle.
 * @author Jerome Robert
 */
public class DummyLiaison extends MeshLiaison {
	private double[] normal;

	public DummyLiaison(Mesh backgroundMesh, double nx, double ny, double nz)
	{
		super(backgroundMesh, backgroundMesh.getBuilder());
		normal = new double[]{nx, ny, nz};
	}

	public DummyLiaison(Mesh backgroundMesh)
	{
		super(backgroundMesh, backgroundMesh.getBuilder());
	}

	public DummyLiaison(Mesh backgroundMesh, MeshTraitsBuilder mtb) {
		super(backgroundMesh, mtb);
	}

	@Override
	protected void init(Collection<Vertex> backgroundNodeset) {
	}

	@Override
	public void backupRestore(Vertex v, boolean restore, int group) {
	}

	@Override
	protected boolean move(Vertex v, Location target, boolean backup, int group,
		boolean doCheck) {
		v.moveTo(target);
		return true;
	}

	@Override
	public double[] getBackgroundNormal(Vertex v) {
		double[] toReturn = new double[3];
		if(normal == null)
		{
			Triangle t = (Triangle) v.getLink();
			Matrix3D.computeNormal3D(t.vertex[0], t.vertex[1], t.vertex[2],
				work1, work2, toReturn);
		}
		else
			System.arraycopy(normal, 0, toReturn, 0, 3);
		return toReturn;
	}

	@Override
	public void initBgMap(TIntIntHashMap numberOfTriangles,
		Collection<Vertex> nodeset) {
	}

	@Override
	public void clearBgMap() {
	}

	@Override
	public void addVertexInNeighborBgMap(Vertex v, Triangle bgT) {
	}

	@Override
	public void addVertex(Vertex v, Triangle bgT) {
	}

	@Override
	public void removeVertex(Vertex v) {
	}

	@Override
	public void replaceVertex(Vertex oldV, Vertex newV) {
	}

	@Override
	public void updateAll() {
	}

	@Override
	public Triangle addVertex(Vertex v, Vertex start, double maxError, int group) {
		return null;
	}

	@Override
	public void addVertex(Vertex newV, Vertex existingVertex) {
	}

	@Override
	public void addVertex(Vertex v, Vertex existingVertex, double[] normal) {
		Triangle t = (Triangle) v.getLink();
		if(this.normal == null)
			Matrix3D.computeNormal3D(t.vertex[0], t.vertex[1], t.vertex[2],
				work1, work2, normal);
		else
			System.arraycopy(this.normal, 0, normal, 0, 3);
	}
}
