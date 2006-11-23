/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2003,2004,2005,2006, by EADS CRC

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

import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.OTriangle;
import org.jcae.mesh.amibe.patch.Mesh2D;
import org.jcae.mesh.amibe.patch.OTriangle2D;
import org.jcae.mesh.amibe.patch.Vertex2D;
import java.util.Iterator;
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
	private Mesh2D mesh = null;
	
	/**
	 * Creates a <code>CheckDelaunay</code> instance.
	 *
	 * @param m  the <code>CheckDelaunay</code> instance to check.
	 */
	public CheckDelaunay(Mesh2D m)
	{
		mesh = m;
	}
	
	/**
	 * Swap edges which are not Delaunay.
	 */
	public void compute()
	{
		Triangle t;
		OTriangle2D ot, sym;
		Vertex2D v;
		int cnt = 0;
		mesh.pushCompGeom(3);
		logger.debug(" Checking Delaunay criterion");
		ot = new OTriangle2D();
		sym = new OTriangle2D();

		boolean redo = false;
		int niter = mesh.getTriangles().size();
		Collection oldList = (Collection) mesh.getTriangles();
		do {
			redo = false;
			cnt = 0;
			ArrayList toSwap = new ArrayList();
			HashSet newList = new HashSet();
			niter--;
			for (Iterator it = oldList.iterator(); it.hasNext(); )
			{
				t = (Triangle) it.next();
				ot.bind(t);
				for (int i = 0; i < 3; i++)
				{
					ot.nextOTri();
					ot.clearAttributes(OTriangle.SWAPPED);
					OTriangle.symOTri(ot, sym);
					sym.clearAttributes(OTriangle.SWAPPED);
				}
			}
			
			for (Iterator it = oldList.iterator(); it.hasNext(); )
			{
				t = (Triangle) it.next();
				ot.bind(t);
				ot.prevOTri();
				for (int i = 0; i < 3; i++)
				{
					ot.nextOTri();
					if (!ot.isMutable())
						continue;
					OTriangle.symOTri(ot, sym);
					if (ot.hasAttributes(OTriangle.SWAPPED) || sym.hasAttributes(OTriangle.SWAPPED))
						continue;
					ot.setAttributes(OTriangle.SWAPPED);
					sym.setAttributes(OTriangle.SWAPPED);
					v = (Vertex2D) sym.apex();
					if (!ot.isDelaunay(mesh, v))
					{
						cnt++;
						toSwap.add(t);
						toSwap.add(new Integer(i));
					}
				}
			}
			logger.debug(" Found "+cnt+" non-Delaunay triangles");
			for (Iterator it = toSwap.iterator(); it.hasNext(); )
			{
				t = (Triangle) it.next();
				int orient = ((Integer) it.next()).intValue();
				ot.bind(t);
				for (int i = 0; i < orient; i++)
					ot.nextOTri();
				if (ot.hasAttributes(OTriangle.SWAPPED))
				{
					newList.add(ot.getTri());
					OTriangle.symOTri(ot, sym);
					newList.add(sym.getTri());
					ot.swap();
					redo = true;
				}
			}
			//  The niter variable is introduced to prevent loops.
			//  With large meshes. its initial value may be too large,
			//  so we lower it now.
			if (niter > 10 * cnt)
				niter = 10 * cnt;
			oldList = (Collection) newList;
		} while (redo && niter > 0);
		mesh.popCompGeom(3);
	}
	
}
