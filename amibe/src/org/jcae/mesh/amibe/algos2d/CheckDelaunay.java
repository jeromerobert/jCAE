/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2003,2004,2005,2006, by EADS CRC
    Copyright (C) 2007, by EADS France

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

import org.jcae.mesh.amibe.ds.TriangleVH;
import org.jcae.mesh.amibe.ds.AbstractTriangle;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.patch.Mesh2D;
import org.jcae.mesh.amibe.patch.VirtualHalfEdge2D;
import org.jcae.mesh.amibe.patch.Vertex2D;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Collection;
import org.apache.log4j.Logger;

/**
 * Swap edges which are not Delaunay.  In an Euclidian 2D metrics, there
 * is a unique Delaunay mesh, so edges can be processed in any order.
 * But with a Riemannian metrics this is no more true, edges with the
 * poorest quality should be processed first to improve the overall
 * quality.  This is not implemented yet, edges are currently not
 * sorted.
 */
public class CheckDelaunay
{
	private static Logger logger=Logger.getLogger(CheckDelaunay.class);
	private final Mesh2D mesh;
	
	/**
	 * Creates a <code>CheckDelaunay</code> instance.
	 *
	 * @param m  the <code>CheckDelaunay</code> instance to check.
	 */
	public CheckDelaunay(Mesh2D m)
	{
		mesh = m;
	}
	
	private static final class FakeEdge
	{
		final TriangleVH triangle;
		final int localNumber;

		FakeEdge(TriangleVH t, int l)
		{
			triangle = t;
			localNumber = l;
		}
	}

	/**
	 * Swap edges which are not Delaunay.
	 */
	public void compute()
	{
		TriangleVH t;
		VirtualHalfEdge2D ot, sym;
		Vertex2D v;
		int cnt = 0;
		mesh.pushCompGeom(3);
		logger.debug(" Checking Delaunay criterion");
		ot = new VirtualHalfEdge2D();
		sym = new VirtualHalfEdge2D();

		boolean redo = false;
		int niter = mesh.getTriangles().size();
		Collection<AbstractTriangle> oldList = mesh.getTriangles();
		do {
			redo = false;
			cnt = 0;
			ArrayList<FakeEdge> toSwap = new ArrayList<FakeEdge>();
			HashSet<AbstractTriangle> newList = new HashSet<AbstractTriangle>();
			niter--;
			for (AbstractTriangle at: oldList)
			{
				t = (TriangleVH) at;
				ot.bind(t);
				for (int i = 0; i < 3; i++)
				{
					ot.next();
					ot.clearAttributes(AbstractHalfEdge.SWAPPED);
					sym.bind((TriangleVH) ot.getTri(), ot.getLocalNumber());
					sym.sym();
					sym.clearAttributes(AbstractHalfEdge.SWAPPED);
				}
			}
			
			for (AbstractTriangle at: oldList)
			{
				t = (TriangleVH) at;
				ot.bind(t);
				ot.prev();
				for (int i = 0; i < 3; i++)
				{
					ot.next();
					if (!ot.isMutable())
						continue;
					sym.bind((TriangleVH) ot.getTri(), ot.getLocalNumber());
					sym.sym();
					if (ot.hasAttributes(AbstractHalfEdge.SWAPPED) || sym.hasAttributes(AbstractHalfEdge.SWAPPED))
						continue;
					ot.setAttributes(AbstractHalfEdge.SWAPPED);
					sym.setAttributes(AbstractHalfEdge.SWAPPED);
					v = (Vertex2D) sym.apex();
					if (!ot.isDelaunay(mesh, v))
					{
						cnt++;
						toSwap.add(new FakeEdge(t, i));
					}
				}
			}
			logger.debug(" Found "+cnt+" non-Delaunay triangles");
			for (FakeEdge e: toSwap)
			{
				ot.bind(e.triangle);
				for (int i = 0; i < e.localNumber; i++)
					ot.next();
				if (ot.hasAttributes(AbstractHalfEdge.SWAPPED))
				{
					newList.add(ot.getTri());
					sym.bind((TriangleVH) ot.getTri(), ot.getLocalNumber());
					sym.sym();
					newList.add(sym.getTri());
					mesh.edgeSwap(ot);
					redo = true;
				}
			}
			//  The niter variable is introduced to prevent loops.
			//  With large meshes. its initial value may be too large,
			//  so we lower it now.
			if (niter > 10 * cnt)
				niter = 10 * cnt;
			oldList = newList;
		} while (redo && niter > 0);
		mesh.popCompGeom(3);
	}
	
}
