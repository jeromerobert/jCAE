/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2003,2004,2005, by EADS CRC
    Copyright (C) 2008, by EADS France
 
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

package org.jcae.mesh.amibe.algos1d;

import org.jcae.mesh.amibe.ds.MEdge1D;
import org.jcae.mesh.amibe.ds.MMesh1D;
import org.jcae.mesh.amibe.ds.MNode1D;
import org.jcae.mesh.amibe.ds.SubMesh1D;
import org.jcae.mesh.cad.CADVertex;
import org.jcae.mesh.cad.CADEdge;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Refines all edges by a given number.
 */
public class Refine
{
	private static final Logger LOGGER=Logger.getLogger(Refine.class.getName());
	private final MMesh1D mesh1d;
	private int divisions = 2;
	
	/**
	 * Creates a <code>Refine</code> instance.
	 *
	 * @param m  the <code>MMesh1D</code> instance to refine.
	 */
	public Refine(MMesh1D m, final Map<String, String> options)
	{
		mesh1d = m;

		for (final Map.Entry<String, String> opt: options.entrySet())
		{
			final String key = opt.getKey();
			final String val = opt.getValue();
			if (key.equals("divisions"))
				divisions = Integer.valueOf(val).intValue();
			else
				throw new RuntimeException("Unknown option: "+key);
		}
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
		Iterator<CADEdge> ite = mesh1d.getTEdgeIterator();
		while (ite.hasNext())
		{
			CADEdge E = ite.next();
			SubMesh1D submesh1d = mesh1d.getSubMesh1DFromMap(E);
			nbNodes -= submesh1d.getNodes().size();
			nbEdges -= submesh1d.getEdges().size();
			if (computeEdge(submesh1d))
				nbTEdges++;
			nbNodes += submesh1d.getNodes().size();
			nbEdges += submesh1d.getEdges().size();
		}

		LOGGER.fine("TopoEdges discretisees "+nbTEdges);
		LOGGER.fine("Edges   "+nbEdges);
		LOGGER.fine("Nodes   "+nbNodes);
		assert(mesh1d.isValid());
	}
	
	/*
	 * Explores each edge of the mesh and divides it into the same number of
	 * divisions.
	 *
	 * @param divisions  the number of divisions for each edge.
	 * @param submesh1d  the 1D mesh being updated.
	 * @return <code>true</code> if this edge was successfully discretized,
	 * <code>false</code> otherwise.
	 */
	private boolean computeEdge(SubMesh1D submesh1d)
	{
		if (divisions < 2)
			throw new java.lang.IllegalArgumentException("Division number must be > 1");

		CADEdge E = submesh1d.getGeometry();
		if (E.isDegenerated())
			//  Do noi refine degenerated edges
			return false;
		
		ArrayList<MEdge1D> edgelist = submesh1d.getEdges();
		ArrayList<MNode1D> nodelist = submesh1d.getNodes();
		//  Copy edgelist to be able to iterate over it
		//  Edges have to be sorted, not nodes
		ArrayList<MEdge1D> oldedgelist = new ArrayList<MEdge1D>(edgelist);
		edgelist.clear();
		Iterator<MEdge1D> ite = oldedgelist.iterator();
		while (ite.hasNext())
		{
			//  Add intermeediate nodes
			MEdge1D edge = ite.next();
			MNode1D firstNode = edge.getNodes1();
			MNode1D lastNode = edge.getNodes2();
			MNode1D n1, n2;
			n1 = firstNode;
			double delta = (lastNode.getParameter() - firstNode.getParameter()) / divisions;
			for (int i = 1; i < divisions; i++)
			{
				double param = firstNode.getParameter() + i * delta;
				n2 = new MNode1D(param, (CADVertex) null);
				//  Would be useful if degenerated edges are refined for
				//  any reason
				n2.isDegenerated(n1.isDegenerated());
				nodelist.add(n2);
				MEdge1D e=new MEdge1D(n1, n2);
				edgelist.add(e);
				n1 = n2;
			}
			MEdge1D e=new MEdge1D(n1, lastNode);
			edgelist.add(e);
		}
		assert(submesh1d.isValid());
		return true;
	}
}
