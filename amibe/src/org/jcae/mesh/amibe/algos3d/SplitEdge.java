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

package org.jcae.mesh.amibe.algos3d;

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.OTriangle;
import org.jcae.mesh.amibe.ds.NotOrientedEdge;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.util.PAVLSortedTree;
import java.util.Iterator;
import org.apache.log4j.Logger;

/**
 * Laplacian smoothing.
 */

public class SplitEdge
{
	private static Logger logger=Logger.getLogger(SplitEdge.class);
	private Mesh mesh;
	private double minlen2;
	
	/**
	 * Creates a <code>SplitEdge</code> instance.
	 *
	 * @param m  the <code>Mesh</code> instance to refine.
	 * @param l  target size
	 */
	public SplitEdge(Mesh m, double l)
	{
		mesh = m;
		minlen2 = 2.0 * l * l;
	}
	
	/**
	 * Split all edges which are longer than minlen2*sqrt(2).
	 * Edges are sorted and the longest edge is always split.
	 */
	public void compute()
	{
		logger.debug("Running SplitEdge");
		NotOrientedEdge noe = new NotOrientedEdge();
		NotOrientedEdge sym = new NotOrientedEdge();
		PAVLSortedTree tree = new PAVLSortedTree();
		unmarkEdges();
		for (Iterator itf = mesh.getTriangles().iterator(); itf.hasNext(); )
		{
			Triangle f = (Triangle) itf.next();
			if (f.isOuter())
				continue;
			noe.bind(f);
			for (int i = 0; i < 3; i++)
			{
				noe.nextOTri();
				if (noe.hasAttributes(OTriangle.MARKED))
					continue;
				noe.setAttributes(OTriangle.MARKED);
				if (noe.getAdj() != null)
				{
					OTriangle.symOTri(noe, sym);
					if (sym.hasAttributes(OTriangle.MARKED))
						continue;
					sym.setAttributes(OTriangle.MARKED);
				}
				addToTree(noe, tree);
			}
		}
		splitAllEdges(tree);
	}
	
	private void addToTree(NotOrientedEdge noe, PAVLSortedTree tree)
	{
		if (!noe.isMutable())
			return;
		double [] p0 = noe.origin().getUV();
		double [] p1 = noe.destination().getUV();
		double l2 =
			(p1[0] - p0[0]) * (p1[0] - p0[0]) +
			(p1[1] - p0[1]) * (p1[1] - p0[1]) +
			(p1[2] - p0[2]) * (p1[2] - p0[2]);
		if (l2 > minlen2)
			tree.insert(new NotOrientedEdge(noe), l2);
	}
	
	private void unmarkEdges()
	{
		NotOrientedEdge noe = new NotOrientedEdge();
		for (Iterator itf = mesh.getTriangles().iterator(); itf.hasNext(); )
		{
			Triangle f = (Triangle) itf.next();
			if (f.isOuter())
				continue;
			noe.bind(f);
			for (int i = 0; i < 3; i++)
			{
				noe.nextOTri();
				noe.clearAttributes(OTriangle.MARKED);
			}
		}
	}
	
	private boolean splitAllEdges(PAVLSortedTree tree)
	{
		int splitted = 0;
//int niter = 1000;
		OTriangle ot = new OTriangle();
		NotOrientedEdge sym = new NotOrientedEdge();
		double [] newXYZ = new double[3];
		double sinMin = Math.sin(Math.PI / 36.0);
		while (tree.size() > 0)
		{
//niter--; if (niter == 0) break;
			NotOrientedEdge edge = (NotOrientedEdge) tree.last();
			double cost = tree.getKey(edge);
			if (logger.isDebugEnabled())
				logger.debug("Split edge: "+cost+" "+edge);
			// New point
			double [] p0 = edge.origin().getUV();
			double [] p1 = edge.destination().getUV();
			// FIXME
			for (int i = 0; i < 3; i++)
				newXYZ[i] = 0.5*(p0[i]+p1[i]);
			Vertex v = new Vertex(newXYZ[0], newXYZ[1], newXYZ[2]);
			edge.origin().discreteProject(v);
			// Do not build degenerate triangles.
			if (Math.abs(Math.sin(v.angle3D(edge.origin(), edge.apex()))) < sinMin ||
			    Math.abs(Math.sin(v.angle3D(edge.destination(), edge.apex()))) < sinMin)
			{
				tree.remove(edge);
				continue;
			}
			if (edge.getAdj() != null)
			{
				OTriangle.symOTri(edge, sym);
				if (Math.abs(Math.sin(v.angle3D(sym.origin(), sym.apex()))) < sinMin ||
				    Math.abs(Math.sin(v.angle3D(sym.destination(), sym.apex()))) < sinMin)
				{
					tree.remove(edge);
					continue;
				}
			}
			// 2 triangles aill be modified.  All their edges
			// have to be removed from tree because their
			// hashCode will change.
			for (int i = 0; i < 3; i++)
			{
				edge.nextOTri();
				tree.remove(edge);
				assert !tree.containsValue(edge);
			}
			if (edge.getAdj() != null)
			{
				OTriangle.symOTri(edge, sym);
				for (int i = 0; i < 2; i++)
				{
					sym.nextOTri();
					tree.remove(sym);
					assert !tree.containsValue(sym);
				}
			}
			edge.split(v);
			splitted++;
			// Update edge length
			for (int i = 0; i < 3; i++)
			{
				addToTree(edge, tree);
				edge.prevOTri();
			}
			edge.prevOTriDest();
			for (int i = 0; i < 2; i++)
			{
				edge.prevOTri();
				addToTree(edge, tree);
			}
			if (edge.getAdj() != null)
			{
				edge.symOTri();
				for (int i = 0; i < 2; i++)
				{
					edge.prevOTri();
					addToTree(edge, tree);
				}
				edge.symOTri();
				edge.prevOTri();
				addToTree(edge, tree);
			}
		}
		assert mesh.isValid();
		logger.info("Number of splitted edges: "+splitted);
		return splitted > 0;
	}
	
}
