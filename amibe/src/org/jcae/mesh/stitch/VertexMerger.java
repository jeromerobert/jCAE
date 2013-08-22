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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Location;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.amibe.util.HashFactory;

/**
 *
 * @author Jerome Robert
 */
class VertexMerger {

	private final Map<Vertex, Collection<Triangle>> map = HashFactory.createMap(10);
	private final Collection<AbstractHalfEdge> edgeToClear= HashFactory.createSet();
	private final List<Triangle> addedTriangles = new ArrayList<Triangle>();
	private final Collection<Collection<Triangle>> trianglesByFan = new ArrayList<Collection<Triangle>>();
	private final Set<Triangle> triToRemove = HashFactory.createSet();
	/** add neigbour triangle of the vertex to the map */
	private void indexify(Mesh mesh, Vertex vertex)
	{
		Iterator<AbstractHalfEdge> it = vertex.getNeighbourIteratorAbstractHalfEdge();
		while(it.hasNext())
		{
			AbstractHalfEdge edge = it.next();
			if(edge.destination() == mesh.outerVertex)
				continue;
			if(edge.hasAttributes(AbstractHalfEdge.OUTER))
				edge = edge.sym();
			assert !edge.hasAttributes(AbstractHalfEdge.OUTER): edge+"\n"+edge.sym();
			edgeToClear.add(edge);
			Triangle t = edge.getTri();
			for(int i = 0; i < 3; i++)
			{
				Vertex v = t.getV(i);
				Collection<Triangle> l = map.get(v);
				if(l == null)
				{
					l = new ArrayList<Triangle>(10);
					map.put(v, l);
				}
				assert !t.hasAttributes(AbstractHalfEdge.OUTER);
				if(!l.contains(t))
					l.add(t);
			}
		}
	}

	private void indexify3(Mesh mesh, Collection<Triangle> triangles)
	{
		for(Triangle t:triangles)
		{
			for(int i = 0; i < 3; i++)
			{
				Vertex v = t.getV(i);
				Collection<Triangle> l = map.get(v);
				if(l == null)
				{
					l = new ArrayList<Triangle>(10);
					map.put(v, l);
				}
				assert !t.hasAttributes(AbstractHalfEdge.OUTER);
				if(!l.contains(t))
					l.add(t);
			}
		}
	}

	/** add neigbour triangle of the neighbour vertices of v to the map */
	private void indexify2(Mesh mesh, Vertex vertex)
	{
		for(Entry<Vertex, Collection<Triangle>> e: map.entrySet())
		{
			if(e.getKey() == vertex)
				continue;
			Vertex toIndex = e.getKey();
			Iterator<Triangle> itt = toIndex.getNeighbourIteratorTriangle();
			Collection<Triangle> l = map.get(toIndex);
			assert l != null: toIndex+" "+vertex;
			while(itt.hasNext())
			{
				Triangle t = itt.next();
				if(!t.hasAttributes(AbstractHalfEdge.OUTER) && !l.contains(t))
					l.add(t);
			}
			// Mesh.rebuildVertexLinks only support manifold link
			if(!toIndex.isManifold())
			{
				Triangle[] link = (Triangle[]) toIndex.getLink();
				toIndex.setLink(link[0]);
			}
		}
	}

	private void clearAdjacency(Mesh mesh)
	{
		triToRemove.clear();
		for(AbstractHalfEdge e:edgeToClear)
		{
			AbstractHalfEdge s = e.sym();
			if(e.hasAttributes(AbstractHalfEdge.OUTER))
				triToRemove.add(e.getTri());
			else
			{
				e.glue(null);
				e.clearAttributes(AbstractHalfEdge.BOUNDARY);
				e.clearAttributes(AbstractHalfEdge.NONMANIFOLD);
			}
			if(s != null)
			{
				if(s.hasAttributes(AbstractHalfEdge.OUTER))
					triToRemove.add(s.getTri());
				else
				{
					s.glue(null);
					s.clearAttributes(AbstractHalfEdge.BOUNDARY);
					s.clearAttributes(AbstractHalfEdge.NONMANIFOLD);
				}
			}
		}
		for(Triangle t: triToRemove)
			mesh.remove(t);
	}

	//for debugging
	private void checkMap()
	{
		for(Entry<Vertex, Collection<Triangle>> e:map.entrySet())
		{
			for(Triangle t:e.getValue())
			{
				assert !t.hasAttributes(AbstractHalfEdge.OUTER);
				assert t.getV0() == e.getKey() || t.getV1() == e.getKey() || t.getV2() == e.getKey();
			}
			assert e.getKey().isManifold(): e.getKey();
		}
		int n = 0;
		Collection<Triangle> test = HashFactory.createSet();
		for(Collection<Triangle> l:trianglesByFan)
		{
			n += l.size();
			test.addAll(l);
		}
		assert n == test.size();
	}

	private void connectBoundaryTriangles(Mesh mesh,
		Iterable<AbstractHalfEdge> edges, Collection<Triangle> newOuterTriangles)
	{
		for(AbstractHalfEdge ot: edges)
		{
			if (!ot.hasSymmetricEdge() && !ot.hasAttributes(AbstractHalfEdge.OUTER))
			{
				ot.setAttributes(AbstractHalfEdge.BOUNDARY);
				Triangle adj = mesh.createTriangle(mesh.outerVertex, ot.destination(), ot.origin());
				newOuterTriangles.add(adj);
				adj.setAttributes(AbstractHalfEdge.OUTER);
				adj.setReadable(false);
				adj.setWritable(false);
				AbstractHalfEdge sym = adj.getAbstractHalfEdge();
				sym.setAttributes(AbstractHalfEdge.BOUNDARY);
				ot.glue(sym);
			}
		}
	}

	/**
	 * Debug method to check that in trianglesByFan each triangles is in only
	 * one fan
	 */
	private void checkFan(Vertex mainVertex)
	{
		Map<Triangle, Integer> checkMap = HashFactory.createMap();
		int fanID = 0;
		for(Collection<Triangle> l: trianglesByFan)
		{
			System.err.println("fan "+fanID);
			for(Triangle t: l)
			{
				System.err.println(t.getGroupId());
				checkMap.put(t, fanID);
			}
			fanID ++;
		}
		Set<Integer> fanIDs = HashFactory.createSet();
		//check that each vertex is in only one fan
		for(Entry<Vertex, Collection<Triangle>> e: map.entrySet())
		{
			for(Triangle t:e.getValue())
				fanIDs.add(checkMap.get(t));
			//assert fanIDs.size() == 1: e.getKey()+" "+fanIDs+" "+mainVertex;
			fanIDs.clear();
		}
	}

	/** Debug method to check that half edge sym() is valid */
	public static void checkHalfEdge(Mesh mesh, Vertex v)
	{
		for(Triangle t : mesh.getTriangles())
		{
			AbstractHalfEdge e = t.getAbstractHalfEdge();
			for(int i = 0; i < 3; i++)
			{
				if(e.hasSymmetricEdge() && e.origin() != mesh.outerVertex &&
					e.destination() != mesh.outerVertex)
				{
					assert e.origin() == e.sym().destination();
					assert e.destination() == e.sym().origin(): e.sym()+" "+v;
				}
				e = e.next();
			}
		}
	}
	/** Convert non manifold edges around v to boundary edges */
	public void unmerge(Mesh mesh, Vertex v)
	{
		edgeToClear.clear();
		addedTriangles.clear();
		map.clear();
		trianglesByFan.clear();
		if(v.isManifold())
			return;
		for(Triangle t: (Triangle[])v.getLink())
		{
			ArrayList<Triangle> fan = new ArrayList<Triangle>();
			AbstractHalfEdge current = v.getIncidentAbstractHalfEdge(t, null);
			Vertex startV = current.destination();
			do
			{
				if(!current.hasAttributes(AbstractHalfEdge.OUTER))
				{
					assert current.getTri().getV0() == v ||
						current.getTri().getV1() == v ||
						current.getTri().getV2() == v;
					fan.add(current.getTri());
				}
				assert current.getTri() != null;
				edgeToClear.add(current);
				edgeToClear.add(current.prev());
				current = current.nextOriginLoop();
			}
			while(current.destination() != startV);
			trianglesByFan.add(fan);
		}
		clearAdjacency(mesh);
		boolean first = true;
		for(Collection<Triangle> fan: trianglesByFan)
		{
			assert fan.size() == HashFactory.createSet(fan).size();
			Vertex newV = first ? v : mesh.createVertex(v);
			first = false;
			for(Triangle t:fan)
			{
				assert t.getV0() == v || t.getV1() == v || t.getV2() == v;
				for(int i = 0; i < 3; i++)
				{
					if(t.getV(i) == v)
						t.setV(i, newV);
				}
				newV.setLink(t);
			}
			indexify3(mesh, fan);
		}
		mesh.glueSymmetricHalfEdges(map, addedTriangles);
		connectBoundaryTriangles(mesh, edgeToClear, addedTriangles);
		for(Triangle t:addedTriangles)
			mesh.add(t);
		indexify2(mesh, v);
		Mesh.rebuildVertexLinks(map);
	}

	public void merge(Mesh mesh, Location target, Vertex ... vertices)
	{
		map.clear();
		addedTriangles.clear();
		edgeToClear.clear();
		for(Vertex v:vertices)
			indexify(mesh, v);
		clearAdjacency(mesh);
		Vertex targetV = vertices[vertices.length - 1];
		Collection<Triangle> targetL = map.get(targetV);
		for(int i = 0; i < vertices.length - 1; i++)
		{
			Collection<Triangle> l = map.get(vertices[i]);
			for(Triangle t:l)
			{
				for(int j = 0; j < 3; j++)
				{
					assert t.getV(j) != targetV: "Cannot merge "+vertices[i]+" to "+targetV;
					if(t.getV(j) == vertices[i])
						t.setV(j, targetV);
				}
			}
			map.remove(vertices[i]);
			targetL.addAll(l);
		}
		mesh.glueSymmetricHalfEdges(map, addedTriangles);
		connectBoundaryTriangles(mesh, edgeToClear, addedTriangles);
		for(Triangle t:addedTriangles)
			mesh.add(t);
		indexify2(mesh, targetV);
		// Mesh.rebuildVertexLinks only support manifold link
		if(!targetV.isManifold())
			targetV.setLink(((Triangle[])targetV.getLink())[0]);
		Mesh.rebuildVertexLinks(map);
		targetV.moveTo(target);
	}

	private int counter;
	private void isValid(Mesh mesh)
	{
		assert mesh.isValid();
		assert mesh.checkNoDegeneratedTriangles();
		assert mesh.checkNoInvertedTriangles();
		/*mesh.checkAdjacency();
		try {
			MeshWriter.writeObject3D(mesh, "/tmp/tmp.amibe", null);
			new Amibe2VTK("/tmp/tmp.amibe").write("/tmp/merger"+(counter++)+".vtp");
		} catch (Exception ex) {
			Logger.getLogger(VertexMerger.class.getName()).log(Level.SEVERE,
				null, ex);
		}*/
	}

	public void test1()
	{
		Mesh mesh = new Mesh(MeshTraitsBuilder.getDefault3D());
		Vertex v0 = mesh.createVertex(0, 0, 0);
		Vertex v1 = mesh.createVertex(0, 1, 0);
		Vertex v11 = mesh.createVertex(0, 1, 0);
		Vertex v2 = mesh.createVertex(1, 0, 0);
		Vertex v3 = mesh.createVertex(-1, 0, 0);
		Vertex v4 = mesh.createVertex(0, 0, 1);
		Triangle tA = mesh.createTriangle(v0, v1, v2);
		Triangle tB = mesh.createTriangle(v0, v3, v1);
		Triangle tC = mesh.createTriangle(v0, v11, v4);
		mesh.add(tA);
		mesh.add(tB);
		mesh.add(tC);
		mesh.buildAdjacency();
		assert mesh.checkAdjacency();
		merge(mesh, v11, v1, v11);
		isValid(mesh);
		assert !v11.isManifold();
		unmerge(mesh, v11);
		isValid(mesh);
		assert v11.isManifold();
	}

	private Vertex[][] createGrid(Mesh mesh, int n, int m, Location start, double[] v1, double[] v2)
	{
		Vertex[][] toReturn = new Vertex[n][m];
		for(int i = 0; i < n; i++)
			for(int j = 0; j < m; j++)
			{
				toReturn[i][j] = mesh.createVertex(
					start.getX()+i*v1[0]+j*v2[0],
					start.getY()+i*v1[1]+j*v2[1],
					start.getZ()+i*v1[2]+j*v2[2]);
			}
		for(int i = 0; i < n - 1; i++)
			for(int j = 0; j < m - 1; j++)
			{
				mesh.add(mesh.createTriangle(toReturn[i][j], toReturn[i+1][j], toReturn[i][j+1]));
				mesh.add(mesh.createTriangle(toReturn[i+1][j+1], toReturn[i][j+1], toReturn[i+1][j]));
			}
		return toReturn;
	}

	public void test2()
	{
		Mesh mesh = new Mesh(MeshTraitsBuilder.getDefault3D());
		Vertex[][] vs1 = createGrid(mesh, 3, 5, new Location(),
			new double[]{1, 0, 0}, new double[]{0, 1, 0});
		Vertex[][] vs2 = createGrid(mesh, 2, 5, new Location(1,0,0),
			new double[]{0, 0, 1}, new double[]{0, 1, 0});
		mesh.buildAdjacency();
		isValid(mesh);
		merge(mesh, vs2[0][0], vs1[1][0], vs2[0][0]);
		isValid(mesh);
		merge(mesh, vs2[0][1], vs1[1][1], vs2[0][1]);
		isValid(mesh);
		merge(mesh, vs2[0][3], vs1[1][3], vs2[0][3]);
		isValid(mesh);
		merge(mesh, vs2[0][4], vs1[1][4], vs2[0][4]);
		isValid(mesh);
		merge(mesh, vs2[0][2], vs1[1][2], vs2[0][2]);
		isValid(mesh);
		unmerge(mesh, vs2[0][2]);
		isValid(mesh);
		unmerge(mesh, vs2[0][4]);
		isValid(mesh);
		unmerge(mesh, vs2[0][3]);
		isValid(mesh);
		unmerge(mesh, vs2[0][1]);
		isValid(mesh);
		unmerge(mesh, vs2[0][0]);
		isValid(mesh);
	}

	public static void test() {
		new VertexMerger().test1();
		new VertexMerger().test2();
	}
}