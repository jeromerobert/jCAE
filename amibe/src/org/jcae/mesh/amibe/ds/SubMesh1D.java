/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2003,2004,2005, by EADS CRC
    Copyright (C) 2007,2008, by EADS France
 
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

package org.jcae.mesh.amibe.ds;

import org.jcae.mesh.cad.*;
import org.jcae.mesh.bora.ds.BDiscretization;
import org.jcae.mesh.bora.ds.BCADGraphCell;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Logger;

/**
 * 1D discretization of a single topological edge.
 * The <code>SubMesh1D</code> class represents a 1D discretization of
 * topological edges.
 * In order to ensure fitting in 3D space, edges are discretized only once
 * even if they appear with several orientations.
 */

public class SubMesh1D
{
	private static final Logger logger=Logger.getLogger(SubMesh1D.class.getName());	

	//  Topological edge being discretized
	private CADEdge edge;
	
	//  Discretization definition
	private BDiscretization discr;
	
	//  Edge list
	private ArrayList<MEdge1D> edgelist = new ArrayList<MEdge1D>();
	
	//  Node list
	private ArrayList<MNode1D> nodelist = new ArrayList<MNode1D>();
	
	/**
	 * Creates a <code>SubMesh1D</code> instance and initializes it with
	 * vertices found in a <code>MMesh0D</code> instance.  The returned
	 * <code>SubMesh1D</code> instance can be seen as a very rough
	 * approximation of the geometrical edge, which can be refined via
	 * algorithms found in <code>org.jcae.mesh.amibe.algos1d</code>.
	 *
	 * @param E  the topological edge being disretized,
	 * @param m0d  the mesh containing all vertices
	 */
	protected SubMesh1D(CADEdge E, MMesh0D m0d)
	{
		edge = E;
		double range[] = edge.range();
		boolean degenerated = edge.isDegenerated();
		CADVertex[] V = edge.vertices();
		MNode1D n1 = new MNode1D(range[0], m0d.getGeometricalVertex(V[0]));
		MNode1D n2 = new MNode1D(range[1], m0d.getGeometricalVertex(V[1]));
		n1.isDegenerated(degenerated);
		n2.isDegenerated(degenerated);
		MEdge1D e=new MEdge1D(n1, n2);
		nodelist.add(n1);
		nodelist.add(n2);
		edgelist.add(e);
		assert(isValid());
	}

	/**
	 * Creates a <code>SubMesh1D</code> instance and initializes it with
	 * vertices found in a <code>MMesh0D</code> instance.  The returned
	 * <code>SubMesh1D</code> instance can be seen as a very rough
	 * approximation of the geometrical edge, which can be refined via
	 * algorithms found in <code>org.jcae.mesh.amibe.algos1d</code>.
	 *
	 * @param d  container for the requested discretization
	 * @param m0d  the mesh containing all vertices
	 */
	protected SubMesh1D(BDiscretization d, MMesh0D m0d)
	{
		discr = d;
		BCADGraphCell cell = discr.getGraphCell();
		if (cell.getType() != CADShapeEnum.EDGE)
			throw new RuntimeException("Attempt to use invalid discretization "+d+" in SubMesh1D ");
		edge = (CADEdge) cell.getShape();
		double range[] = edge.range();
		boolean degenerated = edge.isDegenerated();
		CADVertex[] V = edge.vertices();

		MNode1D n1 = new MNode1D(range[0], m0d.getChildDiscretization(V[0], cell, d));
		MNode1D n2 = new MNode1D(range[1], m0d.getChildDiscretization(V[1], cell, d));
		n1.isDegenerated(degenerated);
		n2.isDegenerated(degenerated);
		MEdge1D e=new MEdge1D(n1, n2);
		nodelist.add(n1);
		nodelist.add(n2);
		edgelist.add(e);
		assert(isValid());
	}
	
	/**
	 * Creates an empty  <code>SubMesh1D</code> instance.
	 *
	 * @param E  the topological edge being disretized,
	 */
	public SubMesh1D(CADEdge E)
	{
		edge = E;
	}
	
	/**
	 * Returns the topological edge.
	 *
	 * @return the topological edge.
	 */
	public CADEdge getGeometry()
	{
		return edge;
	}
	
	/**
	 * Returns the list of edges.
	 *
	 * @return the list of edges.
	 */
	public ArrayList<MEdge1D> getEdges()
	{
		return edgelist;
	}
	
	/**
	 * Returns the list of nodes.
	 *
	 * @return the list of nodes.
	 */
	public ArrayList<MNode1D> getNodes()
	{
		return nodelist;
	}
	
	/**
	 * Returns an iterator over the list of edges.
	 *
	 * @return an iterator over the list of edges.
	 */
	public Iterator<MEdge1D> getEdgesIterator()
	{
		return edgelist.iterator();
	}
	
	/**
	 * Returns an iterator over the list of nodes.
	 *
	 * @return an iterator over the list of nodes.
	 */
	public Iterator<MNode1D> getNodesIterator()
	{
		return nodelist.iterator();
	}
	
	/**
	 * Checks the validity of a 1D discretization.
	 * This method is called within assertions, this is why it returns a
	 * <code>boolean</code>.
	 *
	 * @return <code>true</code> if all checks pass.
	 * @throws AssertException if a check fails.
	 */
	public boolean isValid()
	{
		HashSet<MNode1D> tempset = new HashSet<MNode1D>(nodelist);
		Iterator<MEdge1D> ite = edgelist.iterator();
		while (ite.hasNext())
		{
			MEdge1D e = ite.next();
			assert (nodelist.contains(e.getNodes1()));
			assert (nodelist.contains(e.getNodes2()));
			tempset.remove(e.getNodes1());
			tempset.remove(e.getNodes2());
		}
		assert (tempset.isEmpty());
		return true;
	}
	
	/**
	 * Prints edge lengths.
	 */
	protected void printInfos()
	{
		int n = 0;
		double minlen = -1.0, maxlen = 0.0,  avglen = 0.0;
		CADGeomCurve3D c3d = CADShapeFactory.getFactory().newCurve3D(edge);
		
		Iterator<MEdge1D> ite = edgelist.iterator();
		while (ite.hasNext())
		{
			n++;
			MEdge1D e = ite.next();
			double []pt1 = c3d.value(e.getNodes1().getParameter());
			double []pt2 = c3d.value(e.getNodes2().getParameter());
			double len = Math.sqrt(
				(pt1[0] - pt2[0]) * (pt1[0] - pt2[0]) +
				(pt1[1] - pt2[1]) * (pt1[1] - pt2[1]) +
				(pt1[2] - pt2[2]) * (pt1[2] - pt2[2])
			);
			if (minlen >= 0.0)
				minlen = Math.min(minlen, len);
			else
				minlen = len;
			maxlen = Math.max(maxlen, len);
			avglen += len;
		}
		if (n > 0)
			avglen /= n;
		logger.info(""+n+" edges");
		logger.info("\tSmallest length: "+minlen);
		logger.info("\tLongest length: "+maxlen);
		logger.info("\tAverage length: "+avglen);
	}
	
	@Override
	public String toString()
	{
		String cr=System.getProperty("line.separator");
		StringBuilder r = new StringBuilder("SubMesh1D"+cr);
		logger.fine("Printing "+r.toString());
		for(MNode1D n: nodelist)
			r.append(n+cr);
		for(MEdge1D e: edgelist)
			r.append(e+cr);
		logger.fine("...done");
		return r.toString();
	}
}

