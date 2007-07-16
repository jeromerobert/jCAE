/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2003,2004,2005, by EADS CRC
 
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

package org.jcae.mesh.mesher.ds;

import org.jcae.mesh.cad.*;
import org.jcae.mesh.bora.ds.BModel;
import org.jcae.mesh.bora.ds.BDiscretization;
import org.jcae.mesh.bora.ds.BCADGraphCell;
import java.util.Iterator;
import java.util.NoSuchElementException;
import gnu.trove.TObjectIntHashMap;

/**
 * List of vertices of the whole shape.
 * A <code>MMesh0D</code> instance is a list of vertices.  All topological
 * vertices which share the same location are merged into a unique vertex,
 * and <code>MMesh0D</code> contains the list of these unique vertices.
 */

public class MMesh0D
{
	//  Array of distinct geometric nodes
	private CADVertex[] vnodelist;
	private int vnodesize = 0;
	private TObjectIntHashMap vnodeset;

	//  Array of distinct discretizations of geometric nodes
	private BDiscretization[] vnodediscrlist;
	private int vnodediscrsize = 0;
	private TObjectIntHashMap vnodediscrset;
	
	/**
	 * Creates a <code>MMesh0D</code> instance by merging all topological
	 * vertices which have the same location.
	 *
	 * @param shape  topological shape
	 */
	public MMesh0D(CADShape shape)
	{
		CADExplorer expV = CADShapeBuilder.factory.newExplorer();
		int nodes = 0;
		for (expV.init(shape, CADShapeEnum.VERTEX); expV.more(); expV.next())
			nodes++;

		//  Merge topological vertices found at the same geometrical point
		vnodelist = new CADVertex[nodes];
		vnodeset = new TObjectIntHashMap(nodes);
		for (expV.init(shape, CADShapeEnum.VERTEX); expV.more(); expV.next())
			addGeometricalVertex((CADVertex) expV.current());
	}
	
	/**
	 * Creates one node for each different discretization of the
	 * BCADGraphCell of type Vertex
	 * The current method may be unnecessary: even if there are many
	 * discretizations on a 0D Vertex, we may want to use the same CADVertex.
	 * However, as we need the method public MMesh1D(BModel model) we have
	 * to define a method public MMesh0D(BModel model)
	 */
	public MMesh0D(BModel model)
	{
		BCADGraphCell root = model.getGraph().getRootCell();

		CADShape shape = root.getShape();
		// This is a copy of the first method.
		CADExplorer expV = CADShapeBuilder.factory.newExplorer();
		int nodes = 0;
		for (expV.init(shape, CADShapeEnum.VERTEX); expV.more(); expV.next())
			nodes++;

		//  Merge topological vertices found at the same geometrical point
		vnodelist = new CADVertex[nodes];
		vnodeset = new TObjectIntHashMap(nodes);
		for (expV.init(shape, CADShapeEnum.VERTEX); expV.more(); expV.next())
			addGeometricalVertex((CADVertex) expV.current());

		// Processing of the found discretizations; is this necessary?
		int nodediscrs = 0;
		// estimation of the maximum number of nodes created on the vertices of the CAD
		for (Iterator itn = root.shapesExplorer(CADShapeEnum.VERTEX); itn.hasNext(); )
		{
			BCADGraphCell cell = (BCADGraphCell) itn.next();
			for (Iterator itpd = cell.discretizationIterator(); itpd.hasNext(); itpd.next())
			{
				nodediscrs++;
			}
		}

		//  Merge nodes at the same discretization
		vnodediscrlist = new BDiscretization[nodediscrs];
		vnodediscrset = new TObjectIntHashMap(nodediscrs);
		for (Iterator itn = root.shapesExplorer(CADShapeEnum.VERTEX); itn.hasNext();)
		{
			BCADGraphCell cell = (BCADGraphCell) itn.next();
			for (Iterator itpd = cell.discretizationIterator(); itpd.hasNext(); )
			{
				BDiscretization discr = (BDiscretization)  itpd.next();
				addVertexDiscretization(discr);
			}
		}
                System.out.println("Number of Vertex discretizations created in MMesh0D: "+ vnodediscrsize);

	}
	
	//  Add a vertex discretization if necessary
	private void addVertexDiscretization(BDiscretization d)
	{
		// test to see if this discretization has already been processed
		if (vnodediscrset.contains(d))
			return;
		// if not, create a node
		vnodediscrset.put(d, vnodediscrsize);
		vnodediscrlist[vnodediscrsize] = d;
		vnodediscrsize++;
	}

	//  Add a geometrical vertex.
	private void addGeometricalVertex(CADVertex V)
	{
		if (vnodeset.contains(V))
			return;
		vnodeset.put(V, vnodesize);
		vnodelist[vnodesize] = V;
		vnodesize++;
	}
	
	/**
	 * Returns the vertex which has the same location as the argument.
	 * This method must be used by 1D algos to ensure that vertices
	 * are unique.
	 *
	 * @param V  vertex
	 * @return the vertex which has the same location as V.
	 */
	public CADVertex getGeometricalVertex(CADVertex V)
	{
		if (!vnodeset.contains(V))
			throw new NoSuchElementException("TVertex : "+V);
		return vnodelist[vnodeset.get(V)];
	}
	
	/**
	 * Returns the discretization that is the same as the argument if available.
	 * This routine does not seem useful, it is here in order to clone the 
	 * old behaviour without BDiscretization structure. It checks that the
	 * discretizations needed here are already available.
	 * The discretization is already unique for the vertices of both orientations
	 * that share the same location.
	 */
	private BDiscretization getVertexDiscretization(BDiscretization d)
	{
		if (!vnodediscrset.contains(d))
			throw new NoSuchElementException("Discretization : "+d);
		if (d != vnodediscrlist[vnodediscrset.get(d)])
			throw new RuntimeException("In getVertexDiscretization. Discretization : "+d);
		return d;
	}

	/**
	 * Returns the discretization of CADVertex V related to parent discretization pd on cell
	 *
	 * @param V  vertex
	 * @param pcell  graphcell of a parent edge of V
	 * @param pd  discretization of cell, parent discretization 
	 * @return child discretization on V
	 */
	public BDiscretization getChildDiscretization(CADVertex V, BCADGraphCell pcell, BDiscretization pd)
	{
		// Selection of the cell's child that has the same shape as V. The orientation of ccell
		// is of no importance because both orientations share the same discretizations
		for (Iterator itc = pcell.shapesExplorer(CADShapeEnum.VERTEX); itc.hasNext(); )
		{
			BCADGraphCell ccell = (BCADGraphCell) itc.next();
			if ( V.isSame(ccell.getShape()) )
			{
				// Selection of the child cell's discretization of parent pd
				for (Iterator itcd = ccell.discretizationIterator(); itcd.hasNext(); )
				{
					BDiscretization cd = (BDiscretization) itcd.next();
					if (pd.contained(cd))
						return getVertexDiscretization(cd); // equivalent to return cd if everything went right
				}
			}
		}
		throw new RuntimeException("Invalid use of getChildDiscretization. Vertex: "+V+" Shape: "+pcell+" Discretization: "+pd);
	}

	/**
	 * Returns an index of the vertex which has the same location as the argument.
	 *
	 * @param V  vertex
	 * @return the index of the vertex which has the same location as V.
	 */
	public int getIndexGeometricalVertex(CADVertex V)
	{
		if (!vnodeset.contains(V))
			return -1;
		return vnodeset.get(V);
	}
	
	/**
	 * Returns the vertex represented by its index.
	 *
	 * @param index  the index of the vertex.
	 * @return the geometrical vertex.
	 */
	public CADVertex getGeometricalVertex(int index)
	{
		return vnodelist[index];
	}
}
