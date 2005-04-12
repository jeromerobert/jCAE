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

package org.jcae.mesh.mesher.algos1d;

import org.jcae.mesh.mesher.ds.*;
import org.jcae.mesh.cad.*;
import org.jcae.mesh.*;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.log4j.Logger;

/**
 * Refines all edges by a given number.
 */
public class Refine
{
	private static Logger logger=Logger.getLogger(Refine.class);
	private MMesh1D mesh1d;
	private int divisions = 2;
	
	/**
	 * Creates a <code>Refine</code> instance.
	 *
	 * @param m  the <code>MMesh1D</code> instance to refine.
	 */
	public Refine(MMesh1D m)
	{
		mesh1d = m;
	}

	/**
	 * Sets the desired number of divisions for each edge.
	 *
	 * @param n  the desired number of divisions for each edge.
	 * @return the current instance.
	 */
	public Refine setParameters(int n)
	{
		divisions = n;
		return this;
	}
	
	/**
	 * Explores each edge of the mesh and divides it into the same number of
	 * divisions.
	 */
	public void compute()
	{
		int nbTEdges = 0, nbNodes = 0, nbEdges = 0;
		if (divisions < 2)
			throw new java.lang.IllegalArgumentException("Division number must be > 1");

		/* Explore the shape for each edge */
		Iterator ite = mesh1d.getTEdgeList().iterator();
		while (ite.hasNext())
		{
			CADEdge E = (CADEdge) ite.next();
			SubMesh1D submesh1d = mesh1d.getSubMesh1DFromMap(E);
			nbNodes -= submesh1d.getNodes().size();
			nbEdges -= submesh1d.getEdges().size();
			if (computeEdge(divisions, submesh1d))
				nbTEdges++;
			nbNodes += submesh1d.getNodes().size();
			nbEdges += submesh1d.getEdges().size();
		}

		logger.debug("TopoEdges discretisees "+nbTEdges);
		logger.debug("Edges   "+nbEdges);
		logger.debug("Nodes   "+nbNodes);
		assert(mesh1d.isValid());
	}
	
	/*
	 * Explores each edge of the mesh and divides it into the same number of
	 * divisions.
	 *
	 * @param divisions  the number of divisions for each edge.
	 * @param submesh1d  the 1D mesh being updated.
	 * @return <code>true</code> if this edge was successfully discrtetized,
	 * <code>false</code> otherwise.
	 */
	private static boolean computeEdge(int divisions, SubMesh1D submesh1d)
	{
		if (divisions < 2)
			throw new java.lang.IllegalArgumentException("Division number must be > 1");

		CADEdge E = submesh1d.getGeometry();
		if (E.isDegenerated())
			//  Do noi refine degenerated edges
			return false;
		
		ArrayList edgelist = submesh1d.getEdges();
		ArrayList nodelist = submesh1d.getNodes();
		//  Copy edgelist to be able to iterate over it
		//  Edges have to be sorted, not nodes
		ArrayList oldedgelist = new ArrayList(edgelist);
		edgelist.clear();
		Iterator ite = oldedgelist.iterator();
		while (ite.hasNext())
		{
			//  Add intermeediate nodes
			MEdge1D edge = (MEdge1D) ite.next();
			MNode1D firstNode = edge.getNodes1();
			MNode1D lastNode = edge.getNodes2();
			MNode1D n1, n2;
			n1 = firstNode;
			double delta = (lastNode.getParameter() - firstNode.getParameter()) / divisions;
			for (int i = 1; i < divisions; i++)
			{
				double param = firstNode.getParameter() + i * delta;
				n2 = new MNode1D(param, null);
				//  Would be useful if degenerated edges are refined for
				//  any reason
				n2.isDegenerated(n1.isDegenerated());
				nodelist.add(n2);
				MEdge1D e=new MEdge1D(n1, n2, false);
				edgelist.add(e);
				n1 = n2;
			}
			MEdge1D e=new MEdge1D(n1, lastNode, false);
			edgelist.add(e);
		}
		assert(submesh1d.isValid());
		return true;
	}
}
