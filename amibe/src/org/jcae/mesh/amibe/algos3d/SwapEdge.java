/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2005, by EADS CRC

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

package org.jcae.mesh.amibe.algos3d;

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.OTriangle;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.util.PAVLSortedTree;
import java.util.Iterator;
import org.apache.log4j.Logger;

/**
 * Laplacian smoothing.
 */

public class SwapEdge
{
	private static Logger logger=Logger.getLogger(SwapEdge.class);
	private Mesh mesh;
	private double planarMin = 0.95;
	// Used by cost()
	private static OTriangle temp = new OTriangle();
	
	/**
	 * Creates a <code>SwapEdge</code> instance.
	 *
	 * @param m  the <code>Mesh</code> instance to refine.
	 * @param p  an edge is swapped only if the dot product of the two
	 *           adjacent triangles is greater than this coefficient.
	 */
	public SwapEdge(Mesh m, double p)
	{
		mesh = m;
		planarMin = p;
	}
	
	/**
	 * Moves all nodes until all iterations are done.
	 */
	public void compute()
	{
		logger.debug("Running SwapEdge");
		PAVLSortedTree tree = new PAVLSortedTree();
		unmarkEdges();
		computeTree(tree);
		processAllTriangles(tree);
	}
	
	private void unmarkEdges()
	{
		OTriangle ot = new OTriangle();
		for (Iterator itf = mesh.getTriangles().iterator(); itf.hasNext(); )
		{
			Triangle f = (Triangle) itf.next();
			if (f.isOuter())
				continue;
			ot.bind(f);
			for (int i = 0; i < 3; i++)
			{
				ot.nextOTri();
				ot.clearAttributes(OTriangle.MARKED);
			}
		}
	}
	
	private void computeTree(PAVLSortedTree tree)
	{
		//  Compute triangle quality
		for (Iterator itf = mesh.getTriangles().iterator(); itf.hasNext(); )
		{
			Triangle f = (Triangle) itf.next();
			if (f.isOuter())
				continue;
			tree.insert(f, cost(f));
		}
	}
	
	private boolean processAllTriangles(PAVLSortedTree tree)
	{
		int swapped = 0;
		OTriangle ot = new OTriangle();
		OTriangle sym = new OTriangle();
		while (!tree.isEmpty())
		{
			Triangle t = null;
			int localNumber = -1;
			for (Iterator itt = tree.iterator(); itt.hasNext(); )
			{
				t = (Triangle) itt.next();
				if (t.isMarked())
					continue;
				double quality = -1.0;
				// Find the best edge candidate
				ot.bind(t);
				localNumber = -1;
				for (int i = 0; i < 3; i++)
				{
					ot.nextOTri();
					if (ot.hasAttributes(OTriangle.BOUNDARY))
						continue;
					assert ot.getAdj() != null : ot;
					double qnew = ot.checkSwap3D(planarMin);
					if (qnew < 0.0)
						continue;
					if (qnew > quality)
					{
						localNumber = ot.getLocalNumber();
						quality = qnew;
					}
				}
				if (quality >= 0.0)
					break;
				// Mark this triangle so that it is not
				// processed again
				t.setMarked();
			}
			if (t == null || localNumber == -1)
				break;
			
			ot.bind(t, localNumber);
			if (logger.isDebugEnabled())
				logger.debug("Swap edge: "+ot);
			OTriangle.symOTri(ot, sym);
			tree.remove(t);
			tree.remove(sym.getTri());
			// Before: ot = (oda)   sym = (don)
			ot.swap();
			swapped++;
			// After:  ot = (ona)   sym = (dan)
			assert sym.apex() == ot.destination() : ot+" "+sym;
			assert ot.destination() != Vertex.outer : ot+" "+sym;
			for (int i = 0; i < 2; i++)
			{
				if (ot.getAdj() != null)
				{
					OTriangle.symOTri(ot, sym);
					sym.getTri().unsetMarked();
				}
				ot.prevOTri();
			}
			// ot = (nao)
			t = ot.getTri();
			t.unsetMarked();
			tree.insert(t, cost(t));
			ot.symOTri();  // (and)
			t = ot.getTri();
			t.unsetMarked();
			tree.insert(t, cost(t));
			for (int i = 0; i < 2; i++)
			{
				ot.prevOTri();
				if (ot.getAdj() != null)
				{
					OTriangle.symOTri(ot, sym);
					sym.getTri().unsetMarked();
				}
			}
		}
		assert mesh.isValid();
		logger.info("Number of swapped edges: "+swapped);
		return swapped > 0;
	}
	
	private static double cost(Triangle f)
	{
		temp.bind(f);
		assert f.vertex[0] != Vertex.outer && f.vertex[1] != Vertex.outer && f.vertex[2] != Vertex.outer : f;
		double p = f.vertex[0].distance3D(f.vertex[1]) + f.vertex[1].distance3D(f.vertex[2]) + f.vertex[2].distance3D(f.vertex[0]);
		double area = temp.computeArea();
		// No need to multiply by 12.0 * Math.sqrt(3.0)
		return area/p/p;
	}
	
}
