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
 * (C) Copyright 2013, by EADS France
 */
package org.jcae.mesh.stitch;

import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.algos3d.Fuse;
import org.jcae.mesh.amibe.algos3d.Skeleton;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Location;
import org.jcae.mesh.amibe.projection.TriangleKdTree;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.amibe.util.HashFactory;
import org.jcae.mesh.xmldata.MeshReader;

/**
 *
 * @author Jerome Robert
 */
public class NonManifoldStitch {

	private final static Logger LOGGER = Logger.getLogger(
		NonManifoldStitch.class.getName());
	private final Mesh mesh;
	private final TriangleKdTree kdTree;
	private int workingGroup;
	private int nbInsertedBeams;
	private double maxDistance = 10.0, tolerance = 1;

	public NonManifoldStitch(Mesh mesh) {
		this.mesh = mesh;
		checkNonManifold();
		kdTree = new TriangleKdTree(mesh);
		workingGroup = mesh.getNumberOfGroups();
		while(mesh.getGroupName(workingGroup) != null)
			workingGroup ++;
	}

	private void checkNonManifold()
	{
		Map<Integer, Collection<AbstractHalfEdge>> edges =
			new HashMap<Integer, Collection<AbstractHalfEdge>>();
		for(Triangle t:mesh.getTriangles())
		{
			AbstractHalfEdge e = t.getAbstractHalfEdge();
			for(int i = 0; i < 3; i ++)
			{
				if(e.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
				{
					int gid = e.getTri().getGroupId();
					Collection<AbstractHalfEdge> l = edges.get(gid);
					if(l == null)
					{
						l = new ArrayList<AbstractHalfEdge>();
						edges.put(gid, l);
					}
					l.add(e);
				}
				e = e.next();
			}
		}
		if(!edges.isEmpty())
		{
			final StringBuilder sb = new StringBuilder(
				"Cannot stitch non-manifold or badly oriented surfaces.");
			for(Map.Entry<Integer, Collection<AbstractHalfEdge>> e: edges.entrySet())
			{
				sb.append("\ngroup: "+mesh.getGroupName(e.getKey())+" / "+e.getKey());
				for(AbstractHalfEdge ee: e.getValue())
					sb.append("\n\t"+new Location(ee.origin())+"-"+new Location(ee.destination()));
			}
			throw new IllegalArgumentException()
			{
				// localized message so netbeans plaform open a popup
				@Override
				public String getLocalizedMessage() {
					return sb.toString();
				}
			};
		}
	}

	public double getMaxDistance() {
		return maxDistance;
	}

	public void setMaxDistance(double maxDistance) {
		this.maxDistance = maxDistance;
	}

	public double getTolerance() {
		return tolerance;
	}

	public void setTolerance(double tolerance) {
		this.tolerance = tolerance;
	}

	private void stitchBeams(List<Vertex> beams, int triaGroup, double weight,
		double maxDistance, double tolerance)
	{
		final Vertex dummyVertex = mesh.createVertex(0, 0, 0);
		int nbBeams = beams.size() / 2;
		Collection<AbstractHalfEdge> edgesToProject = new ArrayList<AbstractHalfEdge>(nbBeams);
		for(int i = 0; i < nbBeams; i++)
		{
			Vertex v1 = beams.get(2 * i);
			Vertex v2 = beams.get(2 * i + 1);
			assert v1 != v2;
			v1.setMutable(true);
			v2.setMutable(true);
			Triangle t = mesh.createTriangle(dummyVertex, v1, v2);
			edgesToProject.add(t.getAbstractHalfEdge());
			t.setGroupId(workingGroup);
			mesh.add(t);
		}
		//TODO do not clear adjacency, this is time consuming.
		mesh.clearAdjacency();
		mesh.buildAdjacency();

		EdgeProjector edgeProjector = new EdgeProjector(mesh, kdTree,
			edgesToProject, triaGroup, maxDistance, tolerance, weight)
		{
			@Override
			protected boolean isToProject(AbstractHalfEdge edge) {
				return edge.origin() != dummyVertex && edge.destination() != dummyVertex;
			}
		};
		edgeProjector.checkMerge = false;
		edgeProjector.project();
		nbInsertedBeams += nbBeams;
	}

	private Collection<AbstractHalfEdge> getBorder(int group)
	{
		ArrayList<AbstractHalfEdge> toReturn = new ArrayList<AbstractHalfEdge>();
		for(Triangle t:mesh.getTriangles())
		{
			if(t.getGroupId() == group)
			{
				AbstractHalfEdge e = t.getAbstractHalfEdge();
				for(int i = 0; i < 3; i++)
				{
					if(e.hasAttributes(AbstractHalfEdge.BOUNDARY))
						toReturn.add(e);
					e = e.next();
				}
			}
		}
		return toReturn;
	}

	public void stitch(int group1, double weight, boolean boundaryOnly) {
		Collection<AbstractHalfEdge> set1 = getBorder(group1);
		EdgeProjector edgeProjector = new EdgeProjector(mesh, kdTree, set1,
			group1, maxDistance, tolerance, weight);
		edgeProjector.setIgnoreGroup(true);
		edgeProjector.setBoundaryOnly(boundaryOnly);
		edgeProjector.project();
	}

	/**
	 * Stitch all borders of groups in the given mesh.
	 * This implementation is very slow because it recreate a new KdTree for
	 * each pair of groups. A faster implementation would require that
	 * EdgeProjector support non-manifold edges which is not yet available.
	 */
	public static void stitch(Mesh mesh, double maxDist, double cleanTol)
	{
		int nbGroup = mesh.getNumberOfGroups();
		TDoubleArrayList tmpCoords = new TDoubleArrayList();
		TIntArrayList tmpTria = new TIntArrayList();
		for(int gid1 = 1; gid1 < nbGroup; gid1++)
		{
			Mesh workingMesh = new Mesh(MeshTraitsBuilder.getDefault3D());
			tmpTria.clear();
			tmpCoords.clear();
			mesh.popGroup(tmpCoords, tmpTria, null, gid1);
			workingMesh.pushGroup(tmpCoords.toArray(), tmpTria.toArray(), null, gid1);
			for(int gid2 = gid1+1; gid2 <= nbGroup; gid2++)
			{
				tmpTria.clear();
				tmpCoords.clear();
				mesh.popGroup(tmpCoords, tmpTria, null, gid2);
				workingMesh.pushGroup(tmpCoords.toArray(), tmpTria.toArray(), null, gid2);

				workingMesh.clearAdjacency();
				workingMesh.buildAdjacency();
				NonManifoldStitch nms = new NonManifoldStitch(workingMesh);
				nms.setMaxDistance(maxDist);
				nms.setTolerance(cleanTol);
				EdgeProjector.saveAsVTK(workingMesh);
				nms.stitchBoth(gid1, gid2, 0);

				tmpTria.clear();
				tmpCoords.clear();
				workingMesh.popGroup(tmpCoords, tmpTria, null, gid2);
				mesh.pushGroup(tmpCoords.toArray(), tmpTria.toArray(), null, gid2);
			}
			tmpTria.clear();
			tmpCoords.clear();
			workingMesh.popGroup(tmpCoords, tmpTria, null, gid1);
			mesh.pushGroup(tmpCoords.toArray(), tmpTria.toArray(), null, gid1);
		}
		new Fuse(mesh, cleanTol).compute();
	}

	public void stitch(int group1, final int group2, double weight, boolean boundaryOnly) {
		Collection<AbstractHalfEdge> set1 = getBorder(group1);
		EdgeProjector edgeProjector = new EdgeProjector(mesh, kdTree, set1,
			group2, maxDistance, tolerance, weight);
		edgeProjector.setBoundaryOnly(boundaryOnly);
		edgeProjector.project();
	}

	private void stitchBoth(int group1, int group2, double weight)
	{
		stitch(group1, group2, weight, false);
		stitch(group2, group1, 1 - weight, false);
		EdgeProjector.saveAsVTK(mesh);
		finish();
		EdgeProjector.saveAsVTK(mesh);
	}

	public void intersect(int group1, int group2)
	{
		Intersection inter = new Intersection(mesh, kdTree);
		List<Vertex> beams = inter.intersect(group1, group2, tolerance);
		//copy the beam to insert because stitchBeams will move them.
		List<Vertex> beams2 = new ArrayList<Vertex>(beams.size());
		Map<Vertex, Vertex> copyMap = HashFactory.createMap(beams.size() / 2 + 5);
		for(Vertex b: beams)
		{
			Vertex copy = copyMap.get(b);
			if(copy == null)
			{
				copy = mesh.createVertex(b);
				copyMap.put(b, copy);
			}
			beams2.add(copy);
		}
		stitchBeams(beams, group1, 0, Double.MAX_VALUE, tolerance);
		stitchBeams(beams2, group2, 0, Double.MAX_VALUE, tolerance);
	}

	/** Actually fissure the surface and convert the working group to beams */
	public void finish()
	{
		new NonManifoldSplitter(mesh).compute();
		if(nbInsertedBeams > 0)
		{
			ArrayList<Triangle> toRemove = new ArrayList<Triangle>(nbInsertedBeams * 2);
			for(Triangle t: mesh.getTriangles())
			{
				if(t.getGroupId() == workingGroup)
					toRemove.add(t);
			}
			mesh.getTriangles().removeAll(toRemove);
		}
	}

	private static void test6(Mesh mesh)
	{
		long t1 = System.nanoTime();
		NonManifoldStitch nms = new NonManifoldStitch(mesh);
		long t2 = System.nanoTime();
		System.err.println("kdTree: "+(t2-t1)/1E9);
		nms.setMaxDistance(1.0);
		nms.setTolerance(0.01);
		// increase number of iteration to test perfs
		for(int k = 0; k < 1; k++)
		{
			for(int i = 1; i <= 4; i++)
			for(int j = 1; j <= 4; j++)
			{
				if(i != j)
					nms.stitch(i, j, 1.0, false);
			}
		}
		long t3 = System.nanoTime();
		System.err.println("projection: "+(t3-t2)/1E9);
	}

	/** Intersection of 4 groups with adjacent triangles */
	private static void test8(String data) throws IOException
	{
		Mesh mesh = new Mesh(MeshTraitsBuilder.getDefault3D());
		MeshReader.readObject3D(mesh, data+"case8.amibe");
		EdgeProjector.saveAsVTK(mesh);
		NonManifoldStitch nms = new NonManifoldStitch(mesh);
		nms.setTolerance(0.01);
		for(int i = 1; i <= mesh.getNumberOfGroups(); i++)
			System.err.println(i+" => "+mesh.getGroupName(i));
		for(int i = 1; i <= mesh.getNumberOfGroups()-1; i++)
		for(int j = i+1; j <= mesh.getNumberOfGroups(); j++)
		{
			System.err.println("intersecting "+i+" "+j);
			EdgeProjector.saveVTK = true;
			EdgeProjector.saveAsVTK(mesh);
			EdgeProjector.saveVTK = false;
			nms.intersect(i, j);
		}
		new NonManifoldSplitter(mesh).compute();
		mesh.popGroup(null, null, null, nms.workingGroup);
		EdgeProjector.saveVTK = true;
		EdgeProjector.saveAsVTK(mesh);
		EdgeProjector.saveVTK = false;
	}

	/**
	 * Functional tests
	 * @param args
	 */
	public static void main(final String[] args) {
		try {
			String data = args[0];
			Mesh mesh = new Mesh(MeshTraitsBuilder.getDefault3D());
			MeshReader.readObject3D(mesh, data+"case1.amibe");
			NonManifoldStitch nms = new NonManifoldStitch(mesh);
			EdgeProjector.saveVTK = true;
			nms.setMaxDistance(100);
			nms.stitchBoth(1, 2, 1.0);
			mesh = new Mesh(MeshTraitsBuilder.getDefault3D());
			MeshReader.readObject3D(mesh, data+"case3.amibe");
			nms = new NonManifoldStitch(mesh);
			nms.stitchBoth(1, 2, 1.0);
			mesh = new Mesh(MeshTraitsBuilder.getDefault3D());
			MeshReader.readObject3D(mesh, data+"case4.amibe");
			nms = new NonManifoldStitch(mesh);
			nms.setMaxDistance(50);
			nms.stitchBoth(1, 2, 1.0);

			mesh = new Mesh(MeshTraitsBuilder.getDefault3D());
			MeshReader.readObject3D(mesh, data+"case6.amibe");
			EdgeProjector.saveVTK = false;
			test6(mesh);
			EdgeProjector.saveVTK = true;
			EdgeProjector.saveAsVTK(mesh);
			test8(data);

			mesh = new Mesh(MeshTraitsBuilder.getDefault3D());
			MeshReader.readObject3D(mesh, data+"case7.amibe");
			nms = new NonManifoldStitch(mesh);
			nms.setMaxDistance(40);
			nms.setTolerance(0.1);
			EdgeProjector.saveVTK = false;
			nms.stitch(1, 2, 1.0, false);
			EdgeProjector.saveVTK = true;
			EdgeProjector.saveAsVTK(mesh);
		} catch (Exception ex) {
			Logger.getLogger(NonManifoldStitch.class.getName()).log(
				Level.SEVERE, null, ex);
		}
	}
}
