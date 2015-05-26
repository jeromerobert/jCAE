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
 * (C) Copyright 2015, by Airbus France
 */

package org.jcae.mesh.amibe.projection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.jcae.mesh.amibe.algos2d.Initial;
import org.jcae.mesh.amibe.algos3d.HoleCutter;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.MeshParameters;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.metrics.Location;
import org.jcae.mesh.amibe.patch.Mesh2D;
import org.jcae.mesh.amibe.patch.Vertex2D;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.amibe.traits.TriangleTraitsBuilder;
import org.jcae.mesh.amibe.util.HashFactory;

/**
 * Cut a 2D polyline in a 3D mesh using a delaunay 2D algorithm
 * @author Jerome Robert
 */
abstract public class Delaunay2DProjector {
	private final Mesh mesh;
	private final EdgeTrianglesLocator edgeTrianglesLocator;
	private Collection<Triangle> trianglesToRemove;
	private final Collection<Triangle> trianglesToAdd = new ArrayList<Triangle>();
	public Delaunay2DProjector(Mesh mesh, TriangleKdTree kdTree) {
		this.mesh = mesh;
		this.edgeTrianglesLocator = new EdgeTrianglesLocator(kdTree);
	}

	private static abstract class LoopBuilder {
		public List<AbstractHalfEdge> build(Vertex cutterVertex) {
			Triangle t = (Triangle)cutterVertex.getLink();
			AbstractHalfEdge e = t.getAbstractHalfEdge();
			for(int i = 0; i < 3; i++) {
				if(isValid(e))
					break;
				e = e.next();
			}
			return build(e);
		}

		public List<AbstractHalfEdge> build(AbstractHalfEdge e) {
			Vertex start = e.origin();
			ArrayList<AbstractHalfEdge> loop = new ArrayList<AbstractHalfEdge>();
			loop.add(e);
			while(e.destination() != start) {
				while(!isValid(e.next()))
					e = e.next().sym();
				e = e.next();
				loop.add(e);
			}
			return loop;
		}
		abstract protected boolean isValid(AbstractHalfEdge edge);
	}

	private double loopLength(List<AbstractHalfEdge> loop) {
		double l = 0;
		for(AbstractHalfEdge e: loop)
			l += e.origin().distance3D(e.destination());
		return l;
	}

	/** Return the border of the domain cuttee triangle set to cut */
	private List<AbstractHalfEdge> cutteeLoop(Collection<AbstractHalfEdge> cutterEdges, double tolerance) {
		assert !cutterEdges.isEmpty();
		final Collection<Triangle> toRemove = HashFactory.createSet();
		final Collection<AbstractHalfEdge> toRemoveEdges = HashFactory.createSet();
		for(AbstractHalfEdge e: cutterEdges) {
			edgeTrianglesLocator.locate(e.origin(), e.destination(), -1, tolerance);
			toRemove.addAll(edgeTrianglesLocator.getResult());
			for(Triangle t: edgeTrianglesLocator.getResult()) {
				AbstractHalfEdge ee = t.getAbstractHalfEdge();
				for(int i = 0; i < 3; i++) {
					toRemoveEdges.add(ee);
					ee = ee.next();
				}
			}
		}

		LoopBuilder loopBuilder = new LoopBuilder() {
			@Override
			protected boolean isValid(AbstractHalfEdge edge) {
				return !toRemove.contains(edge.sym().getTri());
			}
		};
		List<AbstractHalfEdge> bestLoop = null;
		double bestLoopLength = 0;
		while(true) {
			AbstractHalfEdge ae = null;
			// search a border edge of the toRemove set
			for(AbstractHalfEdge e: toRemoveEdges) {
				if(!toRemove.contains(e.sym().getTri())) {
					ae = e;
					break;
				}
			}
			if(ae == null) {
				break;
			} else {
				List<AbstractHalfEdge> loop = loopBuilder.build(ae);
				toRemoveEdges.removeAll(loop);
				// TODO this is not a good criteria. An internal loop may be
				// longer than an external loop. We do not want the longest but
				// the most external loop.
				double l = loopLength(loop);
				if(l > bestLoopLength) {
					bestLoop = loop;
					bestLoopLength = l;
				}
			}
		}
		assert bestLoop != null;
		return bestLoop;
	}

	public void cut(Mesh cutter, Vertex cutterVertex, int cutteeGroup, double tolerance) {
		List<AbstractHalfEdge> freeEdges = new LoopBuilder(){
			@Override
			protected boolean isValid(AbstractHalfEdge edge) {
				return edge.hasAttributes(AbstractHalfEdge.BOUNDARY);
			}
		}.build(cutterVertex);

		List<AbstractHalfEdge> cutteeEdges = cutteeLoop(freeEdges, tolerance);
		HoleCutter hc = new HoleCutter() {
			@Override
			protected boolean isNormalCut(AbstractHalfEdge edge) {
				return false;
			}
		};
		int group = cutteeEdges.get(0).getTri().getGroupId();
		trianglesToRemove = hc.cut(cutteeEdges);
		Map<Vertex, Vertex> v2dTov3d = HashFactory.createMap();
		Mesh2D m2d = createMesh2D();
		int k = 0;
		Vertex2D[] border = new Vertex2D[freeEdges.size() + 1 + cutteeEdges.size() + 1];
		for(AbstractHalfEdge e: cutteeEdges) {
			Vertex2D v2d = (Vertex2D) m2d.createVertex(0, 0);
			transformTo2D(e.origin(), v2d);
			v2dTov3d.put(v2d, e.origin());
			border[k++] = v2d;
		}
		border[k++] = border[0];
		int start2 = k;
		Collections.reverse(freeEdges);
		for(AbstractHalfEdge e: freeEdges) {
			Vertex2D v2d = (Vertex2D) m2d.createVertex(0, 0);
			transformTo2D(e.origin(), v2d);
			v2dTov3d.put(v2d, e.origin());
			border[k++] = v2d;
		}
		border[k] = border[start2];
		new Initial(m2d, m2d.getBuilder(), border, null).compute();
		trianglesToAdd.clear();
		for(Triangle t:m2d.getTriangles())
		{
			if(t.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			Triangle t3d = mesh.createTriangle(
				v2dTov3d.get(t.getV0()),
				v2dTov3d.get(t.getV1()),
				v2dTov3d.get(t.getV2()));
			t3d.setGroupId(group);
			trianglesToAdd.add(t3d);
		}
	}

	private Mesh2D createMesh2D() {
		TriangleTraitsBuilder ttb = new TriangleTraitsBuilder();
		ttb.addVirtualHalfEdge();
		MeshTraitsBuilder mtb = new MeshTraitsBuilder();
		mtb.addKdTree(2);
		mtb.add(ttb);
		return new Mesh2D(mtb, new MeshParameters(), null);
	}

	public Collection<Triangle> getTrianglesToAdd() {
		return trianglesToAdd;
	}

	public Collection<Triangle> getTrianglesToRemove() {
		return trianglesToRemove;
	}

	abstract protected void transformTo2D(Location location, Vertex2D v2d);
}
