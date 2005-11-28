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
		// FIXME: there is a but in splitAllEdges, some edges
		//   are not correctly removed from the tree.  As a
		//   workaround, this loop had been introduced.
		boolean redo = false;
		do
		{
			unmarkEdges();
			PAVLSortedTree tree = computeTree();
			redo = splitAllEdges(tree);
		}
		while (redo);
	}
	
	private PAVLSortedTree computeTree()
	{
		PAVLSortedTree tree = new PAVLSortedTree();
		NotOrientedEdge noe = new NotOrientedEdge();
		NotOrientedEdge sym = new NotOrientedEdge();
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
		return tree;
	}
	private void addToTree(NotOrientedEdge noe, PAVLSortedTree tree)
	{
		if (noe.hasAttributes(OTriangle.OUTER))
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
			f.unsetMarked();
		}
	}
	
	private boolean splitAllEdges(PAVLSortedTree tree)
	{
		int splitted = 0;
		NotOrientedEdge sym = new NotOrientedEdge();
		double [] newXYZ = new double[3];
		double sinMin = Math.sin(Math.PI / 36.0);
		while (tree.size() > 0)
		{
			NotOrientedEdge edge = (NotOrientedEdge) tree.last();
			double cost = tree.getKey(edge);
			tree.remove(edge);
			if (logger.isDebugEnabled())
				logger.debug("Split edge: "+cost+" "+edge);
			// New point
			double [] p0 = edge.origin().getUV();
			double [] p1 = edge.destination().getUV();
			for (int i = 0; i < 3; i++)
				newXYZ[i] = 0.5*(p0[i]+p1[i]);
			Vertex v = new Vertex(newXYZ[0], newXYZ[1], newXYZ[2]);
			if (edge.hasAttributes(OTriangle.BOUNDARY))
			{
				// FIXME: Check deflection
				mesh.setRefVertexOnboundary(v);
			}
			else
			{
				// Discrete differential operators, and thus
				// discreteProject, does not work on boundary
				// nodes.
				Vertex vm = edge.origin();
				if (!vm.isMutable())
					vm = edge.destination();
				if (!vm.discreteProject(v))
					continue;
			}
			// Do not build degenerate triangles.
			if (Math.abs(Math.sin(v.angle3D(edge.origin(), edge.apex()))) < sinMin ||
			    Math.abs(Math.sin(v.angle3D(edge.destination(), edge.apex()))) < sinMin ||
			    Math.abs(Math.sin(edge.apex().angle3D(edge.origin(), v))) < sinMin ||
			    Math.abs(Math.sin(edge.apex().angle3D(edge.destination(), v))) < sinMin)
				continue;
			if (!edge.hasAttributes(OTriangle.BOUNDARY))
			{
				OTriangle.symOTri(edge, sym);
				if (Math.abs(Math.sin(v.angle3D(sym.origin(), sym.apex()))) < sinMin ||
				    Math.abs(Math.sin(v.angle3D(sym.destination(), sym.apex()))) < sinMin ||
				    Math.abs(Math.sin(sym.apex().angle3D(sym.origin(), v))) < sinMin ||
				    Math.abs(Math.sin(sym.apex().angle3D(sym.destination(), v))) < sinMin)
					continue;
			}
			// 2 triangles will be modified.  All their edges
			// have to be removed from tree because their
			// hashCode will change.
			for (int i = 0; i < 3; i++)
			{
				edge.nextOTri();
				tree.remove(edge);
				assert !tree.containsValue(edge) : edge;
			}
			if (!edge.hasAttributes(OTriangle.BOUNDARY))
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
			assert edge.destination() == v : v+" "+edge;
			splitted++;
			// Update edge length
			for (int i = 0; i < 3; i++)
			{
				addToTree(edge, tree);
				edge.prevOTri();
			}
			edge.prevOTriDest();
			assert edge.destination() == v : v+" "+edge;
			for (int i = 0; i < 2; i++)
			{
				edge.prevOTri();
				addToTree(edge, tree);
			}
			if (edge.getAdj() != null)
			{
				/* FIXME: that does not work!
				edge.symOTri();
tree.rehash();
				assert !edge.hasAttributes(OTriangle.OUTER) : edge;
				assert edge.destination() == v : v+" "+edge;
				for (int i = 0; i < 2; i++)
				{
					edge.prevOTri();
					addToTree(edge, tree);
				}
				edge.symOTri();
				edge.prevOTri();
				addToTree(edge, tree);
				*/
			}
		}
		assert mesh.isValid();
		logger.info("Number of splitted edges: "+splitted);
		return splitted > 0;
	}
	
}
