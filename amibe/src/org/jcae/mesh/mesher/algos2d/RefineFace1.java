/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2003 Jerome Robert <jeromerobert@users.sourceforge.net>

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.mesher.algos2d;

import org.jcae.mesh.mesher.ds.*;
import org.jcae.mesh.amibe.ds.*;
import org.jcae.mesh.amibe.util.*;
import org.jcae.mesh.amibe.algos2d.Insertion;
import org.jcae.mesh.mesher.InitialTriangulationException;
import java.util.Iterator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Collection;
import java.util.ArrayList;
import org.apache.log4j.Logger;

/**
 * Performs an initial surface triangulation.
 * The value of discretisation is provided by the constraint hypothesis.
 */

public class RefineFace1
{
	private static Logger logger=Logger.getLogger(RefineFace1.class);
	private SubMesh2D submesh2d = null;
	
	/**
	 * Creates a <code>RefineFace1</code> instance.
	 *
	 * @param m  the <code>RefineFace1</code> instance to refine.
	 */
	public RefineFace1(SubMesh2D m)
	{
		submesh2d = m;
	}
	
	/**
	 * Launch method to mesh a surface.
	 *
	 * @see #computeFace
	 */
	public void compute()
	{
			computeFace(submesh2d);
	}
	
	/**
	 * Launch method to mesh a surface.
	 * <ul>
	 *   <li>Creation of an unconstrained Delaunay triangulation containing
	 *       all boundary nodes.</li>
	 *   <li>Edge swap to restore boundary edges.</li>
	 *   <li>Removal of outer triangles.</li>
	 * </ul>
	 * @see RefineFaceOld
	 *
	 * @param submesh2d  the mesh data structure which is updated.
	 */
	public static void computeFace(SubMesh2D submesh2d)
	{
		Triangle t;
		OTriangle ot;
		Vertex v;
		
		HashSet boundaryEdges = new HashSet(submesh2d.getEdges());
		HashSet innerNodes = new HashSet(submesh2d.getNodes());
		submesh2d.getEdges().clear();
		submesh2d.getNodes().clear();
		if (innerNodes.size() < 3)
		{
			logger.warn("Boundary face contains less than 3 points, it is skipped...");
			return;
		}
		logger.debug(" Unconstrained Delaunay triangulation");
		double umin = Double.MAX_VALUE;
		double umax = Double.MIN_VALUE;
		double vmin = Double.MAX_VALUE;
		double vmax = Double.MIN_VALUE;
		for (Iterator it = innerNodes.iterator(); it.hasNext(); )
		{
			MNode2D pt = (MNode2D) it.next();
			double [] uv = pt.getUV();
			if (uv[0] > umax)
				umax = uv[0];
			if (uv[0] < umin)
				umin = uv[0];
			if (uv[1] > vmax)
				vmax = uv[1];
			if (uv[1] < vmin)
				vmin = uv[1];
		}
		if (umin > 0.0)
			umin *= 0.99;
		else
			umin *= 1.01;
		if (umax > 0.0)
			umax *= 1.01;
		else
			umax *= 0.99;
		if (vmin > 0.0)
			vmin *= 0.99;
		else
			vmin *= 1.01;
		if (vmax > 0.0)
			vmax *= 1.01;
		else
			vmax *= 0.99;
		Mesh m = new Mesh();
		m.initQuadTree(umin, umax, vmin, vmax);
		m.setGeometry(submesh2d.getGeometry());
		m.pushCompGeom(3);
		HashMap map = new HashMap(innerNodes.size());
		HashMap mapinv = new HashMap(innerNodes.size());
		ArrayList savedNodes = new ArrayList();
		{
			//  Initializes mesh
			Iterator it = innerNodes.iterator();
			MNode2D pt = (MNode2D) it.next();
			double [] uv = pt.getUV();
			Vertex v1 = new Vertex(uv[0], uv[1]);
			map.put(v1, pt);
			mapinv.put(pt, v1);
			pt = (MNode2D) it.next();
			uv = pt.getUV();
			Vertex v2 = new Vertex(uv[0], uv[1]);
			map.put(v2, pt);
			mapinv.put(pt, v2);
			Vertex v3 = null;
			//  Ensure that 1sr triangle is not flat and well oriented.
			while (it.hasNext())
			{
				pt = (MNode2D) it.next();
				uv = pt.getUV();
				v3 = new Vertex(uv[0], uv[1]);
				if (v3.onLeft(v1, v2) != 0L)
				{
					map.put(v3, pt);
					mapinv.put(pt, v3);
					break;
				}
				savedNodes.add(v3);
				savedNodes.add(pt);
			}
			assert v3 != null;
			m.bootstrap(v1, v2, v3);
			for (Iterator its = savedNodes.iterator(); its.hasNext(); )
			{
				v = (Vertex) its.next();
				pt = (MNode2D) its.next();
				ot = v.getSurroundingOTriangle();
				ot.split3(v, true); 
				v.addToQuadTree();
				map.put(v, pt);
				mapinv.put(pt, v);
			}
			while (it.hasNext())
			{
				pt = (MNode2D) it.next();
				uv = pt.getUV();
				v = new Vertex(uv[0], uv[1]);
				ot = v.getSurroundingOTriangle();
				ot.split3(v, true); 
				v.addToQuadTree();
				map.put(v, pt);
				mapinv.put(pt, v);
			}
		}
		m.popCompGeom(3);
		m.pushCompGeom(2);
		logger.debug(" Rebuild boundary edges");
		ArrayList saveList = new ArrayList();
		for (Iterator it = boundaryEdges.iterator(); it.hasNext(); )
		{
			MEdge2D e = (MEdge2D) it.next();
			MNode2D pt1 = e.getNodes1();
			MNode2D pt2 = e.getNodes2();
			Vertex v1 = (Vertex) mapinv.get(pt1);
			Vertex v2 = (Vertex) mapinv.get(pt2);
			OTriangle s = m.forceBoundaryEdge(v1, v2);
			saveList.add(s);
		}
		for (Iterator it = saveList.iterator(); it.hasNext(); )
		{
			OTriangle s = (OTriangle) it.next();
			s.setAttributes(OTriangle.BOUNDARY);
			s.symOTri();
			s.setAttributes(OTriangle.BOUNDARY);
		}
		m.popCompGeom(2);

		logger.debug(" Mark outer elements");
		t = Vertex.outer.tri;
		ot = new OTriangle(t, 0);
		if (ot.origin() == Vertex.outer)
				ot.nextOTri();
		else if (ot.destination() == Vertex.outer)
				ot.prevOTri();
		assert ot.apex() == Vertex.outer : ot;
		
		ArrayList pool = new ArrayList(m.getTriangles().size());
		Vertex first = ot.origin();
		OTriangle sym = new OTriangle();
		do
		{
			for (int i = 0; i < 3; i++)
			{
				ot.setAttributes(OTriangle.OUTER);
				ot.nextOTri();
			}
			pool.add(ot.getTri());
			ot.nextOTriApex();
		}
		while (ot.origin() != first);
		logger.debug(" Mark holes");
		while (!pool.isEmpty())
		{
			ArrayList newPool = new ArrayList(m.getTriangles().size());
			for (Iterator it = pool.iterator(); it.hasNext(); )
			{
				t = (Triangle) it.next();
				ot.bind(t);
				boolean outer = ot.hasAttributes(OTriangle.OUTER);
				for (int i = 0; i < 3; i++)
				{
					ot.nextOTri();
					if (ot.hasAttributes(OTriangle.MARKED))
						continue;
					ot.setAttributes(OTriangle.MARKED);
					OTriangle.symOTri(ot, sym);
					newPool.add(sym.getTri());
					if (ot.hasAttributes(OTriangle.BOUNDARY))
					{	
						if (!outer)
						{
							sym.setAttributes(OTriangle.OUTER);
							sym.nextOTri();
							sym.setAttributes(OTriangle.OUTER);
							sym.nextOTri();
							sym.setAttributes(OTriangle.OUTER);
							sym.nextOTri();
						}
						else if (sym.hasAttributes(OTriangle.OUTER))
								throw new InitialTriangulationException();
					}
					else
					{	
						if (outer)
						{
							sym.setAttributes(OTriangle.OUTER);
							sym.nextOTri();
							sym.setAttributes(OTriangle.OUTER);
							sym.nextOTri();
							sym.setAttributes(OTriangle.OUTER);
							sym.nextOTri();
						}
						else if (sym.hasAttributes(OTriangle.OUTER))
								throw new InitialTriangulationException();
					}
				}
			}
			pool = newPool;
		}

/*
m.pushCompGeom(3);
new Insertion(m).compute();
m.popCompGeom(3);
*/
		logger.debug(" Transfer into submesh2d");
		for (Iterator it = m.getTriangles().iterator(); it.hasNext(); )
		{
			t = (Triangle) it.next();
			ot.bind(t);
			if (ot.hasAttributes(OTriangle.OUTER))
				continue;
			MNode2D pt0 = (MNode2D) map.get(t.vertex[0]);
			if (null == pt0)
			{
				double [] uv = t.vertex[0].getUV();
				pt0 = new MNode2D(uv[0], uv[1]);
				map.put(t.vertex[0], pt0);
				mapinv.put(pt0, t.vertex[0]);
			}
			MNode2D pt1 = (MNode2D) map.get(t.vertex[1]);
			if (null == pt1)
			{
				double [] uv = t.vertex[1].getUV();
				pt1 = new MNode2D(uv[0], uv[1]);
				map.put(t.vertex[1], pt1);
				mapinv.put(pt1, t.vertex[1]);
			}
			MNode2D pt2 = (MNode2D) map.get(t.vertex[2]);
			if (null == pt2)
			{
				double [] uv = t.vertex[2].getUV();
				pt2 = new MNode2D(uv[0], uv[1]);
				map.put(t.vertex[2], pt2);
				mapinv.put(pt2, t.vertex[2]);
			}
			submesh2d.addTriangle(pt0, pt1, pt2);
		}
		for (Iterator it = boundaryEdges.iterator(); it.hasNext(); )
		{
			MEdge2D eb = (MEdge2D) it.next();
			MNode2D node1 = eb.getNodes1();
			MNode2D node2 = eb.getNodes2();
			MEdge2D em = submesh2d.getEdgeDefinedByNodes(node1, node2);
			assert null != em : eb+" "+node1+" "+node2;
			submesh2d.addEdge(eb);
			for (Iterator itf = em.getFacesIterator(); itf.hasNext(); )
			{
				MFace2D f = (MFace2D) itf.next();
				f.substEdge(em, eb);
			}
			submesh2d.getEdges().remove(em);
		}
		
		assert (submesh2d.isValid());
	}
	
}
