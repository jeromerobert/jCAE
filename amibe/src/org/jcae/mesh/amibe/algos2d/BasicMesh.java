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

package org.jcae.mesh.amibe.algos2d;

import org.jcae.mesh.mesher.ds.*;
import org.jcae.mesh.amibe.ds.*;
import org.jcae.mesh.amibe.util.*;
import org.jcae.mesh.mesher.InitialTriangulationException;
import org.jcae.mesh.cad.*;
import java.util.Iterator;
import java.util.ArrayList;
import org.apache.log4j.Logger;

/**
 * Performs an initial surface triangulation.
 * The value of discretisation is provided by the constraint hypothesis.
 */

public class BasicMesh
{
	private static Logger logger=Logger.getLogger(BasicMesh.class);
	private Mesh mesh = null;
	private MMesh1D mesh1d = null;
	
	/**
	 * Creates a <code>BasicMesh</code> instance.
	 *
	 * @param m  the <code>BasicMesh</code> instance to refine.
	 */
	public BasicMesh(Mesh m, MMesh1D m1d)
	{
		mesh = m;
		mesh1d = m1d;
	}
	
	/**
	 * Launch method to mesh a surface.
	 *
	 * @see #computeFace
	 */
	public void compute()
	{
		Triangle t;
		OTriangle ot;
		Vertex v;
		
		Vertex [] bNodes = boundaryNodes();
		if (bNodes.length < 3)
		{
			logger.warn("Boundary face contains less than 3 points, it is skipped...");
			return;
		}
		logger.debug(" Unconstrained Delaunay triangulation");
		double umin = Double.MAX_VALUE;
		double umax = Double.MIN_VALUE;
		double vmin = Double.MAX_VALUE;
		double vmax = Double.MIN_VALUE;
		for (int i = 0; i < bNodes.length; i++)
		{
			double [] uv = bNodes[i].getUV();
			if (uv[0] > umax)
				umax = uv[0];
			if (uv[0] < umin)
				umin = uv[0];
			if (uv[1] > vmax)
				vmax = uv[1];
			if (uv[1] < vmin)
				vmin = uv[1];
		}
		mesh.initQuadTree(umin, umax, vmin, vmax);
		//  Initial point insertion sometimes fail on 2D,
		//  this needs to be investigated.
		mesh.pushCompGeom(2);
		Vertex firstOnWire = null;
		{
			//  Initializes mesh
			int i = 0;
			Vertex v1 = bNodes[i];
			firstOnWire = v1;
			i++;
			Vertex v2 = bNodes[i];
			i++;
			Vertex v3 = null;
			//  Ensure that 1st triangle is not flat
			for (; i < bNodes.length; i++)
			{
				v3 = bNodes[i];
				if (firstOnWire == v3)
				{
					logger.warn("Weird wire, face is skipped...");
					return;
				}
				if (v3.onLeft(v1, v2) != 0L)
					break;
			}
			assert i < bNodes.length;
			mesh.bootstrap(v1, v2, v3);
			int i3 = i;
			for (i=2; i < bNodes.length; i++)
			{
				if (i == i3)
					continue;
				v = bNodes[i];
				if (firstOnWire == v)
					firstOnWire = null;
				else
				{
					ot = v.getSurroundingOTriangle();
					ot.split3(v, true); 
					v.addToQuadTree();
					if (firstOnWire == null)
						firstOnWire = v;
				}
			}
		}
		if (!mesh.isValid(false))
			throw new InitialTriangulationException();
		mesh.popCompGeom(2);
		mesh.pushCompGeom(2);
		logger.debug(" Rebuild boundary edges");
		ArrayList saveList = new ArrayList();
		firstOnWire = null;
		for (int i = 0; i < bNodes.length; i++)
		{
			if (firstOnWire == null)
				firstOnWire = bNodes[i];
			else
			{
				OTriangle s = mesh.forceBoundaryEdge(bNodes[i-1], bNodes[i]);
				saveList.add(s);
				if (firstOnWire == bNodes[i])
					firstOnWire = null;
			}
		}
		assert firstOnWire == null;
		for (Iterator it = saveList.iterator(); it.hasNext(); )
		{
			OTriangle s = (OTriangle) it.next();
			s.setAttributes(OTriangle.BOUNDARY);
			s.symOTri();
			s.setAttributes(OTriangle.BOUNDARY);
		}
		mesh.popCompGeom(2);
		
		logger.debug(" Select 3D smaller diagonals");
		mesh.pushCompGeom(3);
		ot = new OTriangle();
		for (Iterator it = mesh.getTriangles().iterator(); it.hasNext(); )
		{
			t = (Triangle) it.next();
			ot.bind(t);
			for (int i = 0; i < 3; i++)
			{
				ot.nextOTri();
				ot.clearAttributes(OTriangle.SWAPPED);
			}
		}
		boolean redo = true;
		//  With riemanian metrics, there may be infinite loops,
		//  make sure to exit this loop.
		int niter = bNodes.length;

		while (redo && niter > 0)
		{
			redo = false;
			--niter;
			for (Iterator it = saveList.iterator(); it.hasNext(); )
			{
				OTriangle s = (OTriangle) it.next();
				if (s.apex() == Vertex.outer)
					s.symOTri();
				s.nextOTri();
				if (s.hasAttributes(OTriangle.SWAPPED))
					continue;
				if (s.checkSmallerAndSwap() != 0)
					redo = true;
			}
		}
		mesh.popCompGeom(3);
		
		logger.debug(" Mark outer elements");
		t = Vertex.outer.tri;
		ot = new OTriangle(t, 0);
		if (ot.origin() == Vertex.outer)
				ot.nextOTri();
		else if (ot.destination() == Vertex.outer)
				ot.prevOTri();
		assert ot.apex() == Vertex.outer : ot;
		
		ArrayList pool = new ArrayList(mesh.getTriangles().size());
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
		for (Iterator it = mesh.getTriangles().iterator(); it.hasNext(); )
		{
			t = (Triangle) it.next();
			ot.bind(t);
			for (int i = 0; i < 3; i++)
			{
				ot.nextOTri();
				ot.clearAttributes(OTriangle.MARKED);
			}
		}
		while (!pool.isEmpty())
		{
			ArrayList newPool = new ArrayList(mesh.getTriangles().size());
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
		assert (mesh.isValid());

		mesh.pushCompGeom(3);
		new Insertion(mesh).compute();
		mesh.popCompGeom(3);
		
		assert (mesh.isValid());
	}
	
	/*
	 *  Builds the patch boundary.
	 *  Returns a list of Vertex.
	 */
	private Vertex [] boundaryNodes()
	{
		//  Rough approximation of the final size
		int roughSize = 10*mesh1d.maximalNumberOfNodes();
		ArrayList result = new ArrayList(roughSize);
		CADFace face = (CADFace) mesh.getGeometry();
		CADExplorer expW = CADShapeBuilder.factory.newExplorer();
		CADWireExplorer wexp = CADShapeBuilder.factory.newWireExplorer();
		for (expW.init(face, CADExplorer.WIRE); expW.more(); expW.next())
		{
			MNode1D p1 = null;
			Vertex p20 = null, p2 = null;
			for (wexp.init((CADWire) expW.current(), face); wexp.more(); wexp.next())
			{
				CADEdge te = wexp.current();
				if (mesh.tooSmall(te))
					continue;

				double range[] = new double[2];
				CADGeomCurve2D c2d = CADShapeBuilder.factory.newCurve2D(te, face);

				Iterator itn = mesh1d.getNodelistFromMap(te).iterator();
				ArrayList nodes1 = new ArrayList();
				while (itn.hasNext())
				{
					p1 = (MNode1D) itn.next();
					nodes1.add(p1);
				}
				if (!te.isOrientationForward())
				{
					//  Sort in reverse order
					int size = nodes1.size();
					for (int i = 0; i < size/2; i++)
					{
						Object o = nodes1.get(i);
						nodes1.set(i, nodes1.get(size - i - 1));
						nodes1.set(size - i - 1, o);
					}
				}
				itn = nodes1.iterator();
				//  Except for the very first edge, the first
				//  vertex is constrained to be the last one
				//  of the previous edge.
				p1 = (MNode1D) itn.next();
				if (null == p20)
				{
					p20 = new Vertex(p1, c2d, face);
					result.add(p20);
				}
				while (itn.hasNext())
				{
					p1 = (MNode1D) itn.next();
					p2 = new Vertex(p1, c2d, face);
					result.add(p2);
				}
			}
			//  Overwrite the last value to close the wire
			result.set(result.size()-1, p20);
		}
		
		return (Vertex []) result.toArray(new Vertex[result.size()]);
	}
	
}
