/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2005, by EADS CRC
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

package org.jcae.mesh.amibe.algos3d;

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.VirtualHalfEdge;
import org.jcae.mesh.amibe.ds.AbstractTriangle;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.util.QSortedTree;
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
	private static VirtualHalfEdge temp = new VirtualHalfEdge();
	
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
		logger.info("Run "+getClass().getName());
		PAVLSortedTree tree = new PAVLSortedTree();
		unmarkEdges();
		computeTree(tree);
		processAllTriangles(tree);
	}
	
	private void unmarkEdges()
	{
		VirtualHalfEdge ot = new VirtualHalfEdge();
		for (AbstractTriangle at: mesh.getTriangles())
		{
			Triangle f = (Triangle) at;
			if (f.isOuter())
				continue;
			ot.bind(f);
			for (int i = 0; i < 3; i++)
			{
				ot.next();
				ot.clearAttributes(VirtualHalfEdge.MARKED);
			}
		}
	}
	
	private void computeTree(PAVLSortedTree tree)
	{
		//  Compute triangle quality
		for (AbstractTriangle at: mesh.getTriangles())
		{
			Triangle f = (Triangle) at;
			if (f.isOuter())
				continue;
			tree.insert(f, cost(f));
		}
	}
	
	private boolean processAllTriangles(PAVLSortedTree tree)
	{
		int swapped = 0;
		VirtualHalfEdge ot = new VirtualHalfEdge();
		VirtualHalfEdge sym = new VirtualHalfEdge();
		while (!tree.isEmpty())
		{
			Triangle t = null;
			int localNumber = -1;
			for (Iterator<QSortedTree.Node> itt = tree.iterator(); itt.hasNext(); )
			{
				QSortedTree.Node q = itt.next();
				t = (Triangle) q.getData();
				if (t.isMarked())
					continue;
				double quality = -1.0;
				// Find the best edge candidate
				ot.bind(t);
				localNumber = -1;
				for (int i = 0; i < 3; i++)
				{
					ot.next();
					if (ot.hasAttributes(VirtualHalfEdge.BOUNDARY))
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
			VirtualHalfEdge.symOTri(ot, sym);
			tree.remove(t);
			tree.remove(sym.getTri());
			// Before: ot = (oda)   sym = (don)
			mesh.edgeSwap(ot);
			swapped++;
			// After:  ot = (ona)   sym = (dan)
			assert sym.apex() == ot.destination() : ot+" "+sym;
			assert ot.destination() != mesh.outerVertex : ot+" "+sym;
			for (int i = 0; i < 2; i++)
			{
				if (ot.getAdj() != null)
				{
					VirtualHalfEdge.symOTri(ot, sym);
					sym.getTri().unsetMarked();
				}
				ot.prev();
			}
			// ot = (nao)
			t = ot.getTri();
			t.unsetMarked();
			tree.insert(t, cost(t));
			ot.sym();  // (and)
			t = ot.getTri();
			t.unsetMarked();
			tree.insert(t, cost(t));
			for (int i = 0; i < 2; i++)
			{
				ot.prev();
				if (ot.getAdj() != null)
				{
					VirtualHalfEdge.symOTri(ot, sym);
					sym.getTri().unsetMarked();
				}
			}
		}
		assert mesh.isValid();
		logger.info("Number of swapped edges: "+swapped);
		return swapped > 0;
	}
	
	private double cost(Triangle f)
	{
		temp.bind(f);
		assert f.vertex[0] != mesh.outerVertex && f.vertex[1] != mesh.outerVertex && f.vertex[2] != mesh.outerVertex : f;
		double p = f.vertex[0].distance3D(f.vertex[1]) + f.vertex[1].distance3D(f.vertex[2]) + f.vertex[2].distance3D(f.vertex[0]);
		double area = temp.area();
		// No need to multiply by 12.0 * Math.sqrt(3.0)
		return area/p/p;
	}
	
}
