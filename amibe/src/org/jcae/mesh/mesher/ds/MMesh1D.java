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

import org.jcae.mesh.bora.ds.*;
import org.jcae.mesh.cad.*;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;

/**
 * 1D discretization of the whole shape.
 * In order to ensure fitting in 3D space, edges are discretized only once
 * even if they appear with several orientations.
 */

public class MMesh1D extends MMesh0D
{
	private static Logger logger = Logger.getLogger(MMesh1D.class);	

	private CADShape shape;
	
	//  Length criterion.
	private static double maxLength;
	private static double maxDeflection = 0.0;
	
	//  Edge map.
	private LinkedHashMap mapTEdgeToSubMesh1D;
	private HashMap mapTEdgeToFaces;
	
	/**
	 * Creates a <code>MMesh1D</code> instance by discretizing all edges
	 * of a given shape.
	 *
	 * @param s  topological shape
	 */
	public MMesh1D(CADShape s)
	{
		super(s);
		shape = s;

		CADExplorer expE = CADShapeBuilder.factory.newExplorer();
		//  HashMap size will not be greater than the number of edges,
		//  so allocate them after computing their maximal size, they
		//  won't be resized.
		int edges = 0;
		for (expE.init(shape, CADShapeEnum.EDGE); expE.more(); expE.next())
			edges++;
		if (edges == 0)
			return;
		mapTEdgeToSubMesh1D = new LinkedHashMap(edges);
		
		for (expE.init(shape, CADShapeEnum.EDGE); expE.more(); expE.next())
		{
			CADEdge E = (CADEdge) expE.current();
			//  Edges may get connected to several faces
			if (mapTEdgeToSubMesh1D.containsKey(E))
				continue;
			SubMesh1D submesh1d = new SubMesh1D(E, (MMesh0D) this);
			mapTEdgeToSubMesh1D.put(E, submesh1d);
		}
		mapTEdgeToFaces = new HashMap(edges);
		for (Iterator it = mapTEdgeToSubMesh1D.keySet().iterator(); it.hasNext(); )
		{
			CADEdge E = (CADEdge) it.next();
			mapTEdgeToFaces.put(E, new LinkedHashSet());
		}
		CADExplorer expF = CADShapeBuilder.factory.newExplorer();
		for (expF.init(shape, CADShapeEnum.FACE); expF.more(); expF.next())
		{
			CADFace F = (CADFace) expF.current();
			LinkedHashSet set;
			for (expE.init(F, CADShapeEnum.EDGE); expE.more(); expE.next())
			{
				CADEdge E = (CADEdge) expE.current();
				set = (LinkedHashSet) mapTEdgeToFaces.get(E);
				set.add(F);
			}
		}
		assert(isValid());
	}
	
	public MMesh1D(BModel model)
	{
		super(model);
		BCADGraphCell root = model.getGraph().getRootCell();
		shape = root.getShape();

		int edgediscrs = 0;
		// estimation of the maximum number of nodes created on the vertices of the CAD
		for (Iterator itn = root.shapesExplorer(CADShapeEnum.EDGE); itn.hasNext(); )
		{
			BCADGraphCell cell = (BCADGraphCell) itn.next();
			for (Iterator itpd = cell.discretizationIterator(); itpd.hasNext(); itpd.next())
			{
				edgediscrs++;
			}
		}
		if (edgediscrs == 0)
			return;

		mapTEdgeToSubMesh1D = new LinkedHashMap(edgediscrs);
		edgediscrs = 0;
		for (Iterator itn = root.shapesExplorer(CADShapeEnum.EDGE); itn.hasNext(); )
		{
			BCADGraphCell cell = (BCADGraphCell) itn.next();
			for (Iterator itpd = cell.discretizationIterator(); itpd.hasNext(); )
			{
				BDiscretization discr = (BDiscretization)  itpd.next();
				//  Edges may get connected to several faces
				if (mapTEdgeToSubMesh1D.containsKey(discr))
					continue;
				SubMesh1D submesh1d = new SubMesh1D(discr, (MMesh0D) this);
				mapTEdgeToSubMesh1D.put(discr, submesh1d);
				edgediscrs++;
			}
		}
		System.out.println("Number of Edge discretizations created in MMesh1D: "+ edgediscrs);

		mapTEdgeToFaces = new HashMap(edgediscrs);
		for (Iterator it = mapTEdgeToSubMesh1D.keySet().iterator(); it.hasNext(); )
		{
			BDiscretization discr = (BDiscretization) it.next();
			mapTEdgeToFaces.put(discr, new LinkedHashSet());
		}

		LinkedHashSet set;
		for (Iterator itp = root.shapesExplorer(CADShapeEnum.FACE); itp.hasNext(); )
		{
			BCADGraphCell pcell = (BCADGraphCell) itp.next();
			for (Iterator itpd = pcell.discretizationIterator(); itpd.hasNext(); )
			{
				BDiscretization pd = (BDiscretization) itpd.next();
				for (Iterator itc = pcell.shapesExplorer(CADShapeEnum.EDGE); itc.hasNext(); )
				{
					BCADGraphCell ccell = (BCADGraphCell) itc.next();
					for (Iterator itcd = ccell.discretizationIterator(); itcd.hasNext(); )
					{
						BDiscretization cd = (BDiscretization) itcd.next();
						if (pd.contained(cd))
						{
							// here mapTEdgeToFaces maps the parent discretizations on 
							// the faces to the child discretizations on the edge
							set = (LinkedHashSet) mapTEdgeToFaces.get(cd);
							set.add(pd);
						}
					}
				}
			}
		}

		assert(isValid());
	}
	
	/**
	 * Returns the topological shape.
	 *
	 * @return the topological shape.
	 */
	public CADShape getGeometry()
	{
		return shape;
	}
	
	/**
	 * Returns the set of faces containing this topological edge.
	 *
	 * @return the set of faces containing this topological edge.
	 */
	public Set getAdjacentFaces(CADEdge E)
	{
		Set ret = (Set) mapTEdgeToFaces.get(E);
		// May be null for beams
		return ret;
	}
	
	/**
	 * Update node labels.
	 */
	public void updateNodeLabels()
	{
		logger.debug("Update node labels");
		//  Resets all labels
		CADExplorer expE = CADShapeBuilder.factory.newExplorer();
		for (expE.init(shape, CADShapeEnum.EDGE); expE.more(); expE.next())
		{
			CADEdge E = (CADEdge) expE.current();
			SubMesh1D submesh1d = (SubMesh1D) mapTEdgeToSubMesh1D.get(E);
			for (Iterator it = submesh1d.getNodesIterator(); it.hasNext(); )
			{
				MNode1D n = (MNode1D) it.next();
				n.setLabel(0);
			}
		}
		int i = 0;
		for (expE.init(shape, CADShapeEnum.EDGE); expE.more(); expE.next())
		{
			CADEdge E = (CADEdge) expE.current();
			SubMesh1D submesh1d = (SubMesh1D) mapTEdgeToSubMesh1D.get(E);
			for (Iterator it = submesh1d.getNodesIterator(); it.hasNext(); )
			{
				MNode1D n = (MNode1D) it.next();
				if (0 == n.getMaster().getLabel())
				{
					i++;
					n.getMaster().setLabel(i);
				}
			}
		}
	}
	
	/**
	 * Duplicates edges so that boundary faces are closed.
	 * This method must be used after all 1D algorithms have been applied,
	 * and before any 2D meshing is performed.
	 *
	 */
	public void duplicateEdges()
	{
		logger.debug("Compute vertex references");
		CADExplorer expV = CADShapeBuilder.factory.newExplorer();
		//  For each topological vertex, compute the list of
		//  MNode1D objects which are bound to this vertex.
		int nVertex = 0;
		for (expV.init(shape, CADShapeEnum.VERTEX); expV.more(); expV.next())
			nVertex++;
		HashMap vertex2Ref = new HashMap(nVertex);
		for (expV.init(shape, CADShapeEnum.VERTEX); expV.more(); expV.next())
			vertex2Ref.put(expV.current(), new ArrayList());
		
		Iterator ite = mapTEdgeToSubMesh1D.values().iterator();
		while (ite.hasNext())
		{
			SubMesh1D submesh1d = (SubMesh1D) ite.next();
			Iterator itn = submesh1d.getNodesIterator();
			while (itn.hasNext())
			{
				MNode1D pt = (MNode1D) itn.next();
				if (null != pt.getCADVertex())
					((ArrayList) vertex2Ref.get(pt.getCADVertex())).add(pt);
			}
		}
		
		LinkedHashSet seen = new LinkedHashSet();
		for (expV.init(shape, CADShapeEnum.VERTEX); expV.more(); expV.next())
		{
			CADVertex V = (CADVertex) expV.current();
			if (seen.contains(V))
				continue;
			seen.add(V);
			ArrayList vnodelist = (ArrayList) vertex2Ref.get(V);
			if (vnodelist.size() <= 1)
				continue;
			// Make sure that all MNode1D objects share the same master.
			MNode1D master = (MNode1D) vnodelist.get(0);
			master.setMaster(null);
			for (int i = 1; i<vnodelist.size(); i++)
				((MNode1D) vnodelist.get(i)).setMaster(master);
		}
		assert(isValid());
	}
	
	/**
	 * Returns the maximal desired length.
	 *
	 * @return the maximal desired length.
	 */
	public double getMaxLength()
	{
		return maxLength;
	}
	
	public double getMaxDeflection()
	{
		return maxDeflection;
	}
	
	/**
	 * Sets the maximal desired length.
	 *
	 * @param length  the maximal desired length.
	 */
	public void setMaxLength(double length)
	{
		maxLength = length;
	}
	
	public void setMaxDeflection(double deflection)
	{
		maxDeflection = deflection;
	}
	
	/**
	 * Returns the list of topological edges.
	 *
	 * @return the list of topological edges.
	 */
	public Iterator getTEdgeIterator()
	{
		return mapTEdgeToSubMesh1D.keySet().iterator();
	}
	
	/**
	 * Returns the list of nodes inserted on a given topological edge.
	 *
	 * @param E  a topological edge.
	 * @return the list of nodes inserted on this edge.
	 */
	public ArrayList getNodelistFromMap(CADEdge E)
		throws NoSuchElementException
	{
		SubMesh1D submesh1d = (SubMesh1D) mapTEdgeToSubMesh1D.get(E);
		if (null == submesh1d)
			throw new NoSuchElementException("TEdge : "+E);
		return submesh1d.getNodes();
	}
	
	/**
	 * Returns the list of edges inserted on a given topological edge.
	 *
	 * @param E  a topological edge.
	 * @return the list of edges inserted on this edge.
	 */
	public ArrayList getEdgelistFromMap(CADEdge E)
	       throws NoSuchElementException
	{
		SubMesh1D submesh1d = (SubMesh1D) mapTEdgeToSubMesh1D.get(E);
		if (null == submesh1d)
			throw new NoSuchElementException("TEdge : "+E);
		return submesh1d.getEdges();
	}
	
	/** 
	 * Returns an upper limit of the total number of nodes
	 *
	 * @return an upper limit of the total number of nodes
	 */
	public int maximalNumberOfNodes()
	{
		int result = 0;
		for(Iterator it=mapTEdgeToSubMesh1D.values().iterator();it.hasNext();)
		{
			SubMesh1D submesh1d = (SubMesh1D) it.next();
			if (null != submesh1d)
				result += submesh1d.getNodes().size();
		}
		return result;
	}
	
	/**
	 * Returns the <code>SubMesh1D</code> instance associated with a
	 * CADEdge.
	 *
	 * @param E  a topological edge.
	 * @return the <code>SubMesh1D</code> instance associated with it.
	 */
	public SubMesh1D getSubMesh1DFromMap(CADEdge E)
	       throws NoSuchElementException
	{
		if (! mapTEdgeToSubMesh1D.containsKey(E))
			throw new NoSuchElementException("TEdge : "+E);
		return (SubMesh1D) mapTEdgeToSubMesh1D.get(E);
	}
	
	/**
	 * Returns the <code>SubMesh1D</code> instance associated with a
	 * BDiscretization.
	 *
	 * @param discrE  discretization of an edge.
	 * @return the <code>SubMesh1D</code> instance associated with it.
	 */
	public SubMesh1D getSubMesh1DFromMap(BDiscretization discrE)
	       throws NoSuchElementException
	{
		if (! mapTEdgeToSubMesh1D.containsKey(discrE))
			throw new NoSuchElementException("TEdge : "+discrE);
		return (SubMesh1D) mapTEdgeToSubMesh1D.get(discrE);
	}

	/**
	 * Checks the validity of a <code>MMesh1D</code> instance.
	 * This method is called within assertions, this is why it returns a
	 * <code>boolean</code>.
	 *
	 * @return <code>true</code> if all checks pass.
	 * @throws AssertException if a check fails.
	 */
	public boolean isValid()
	{
		for(Iterator it=mapTEdgeToSubMesh1D.values().iterator();it.hasNext();)
		{
			SubMesh1D submesh1d = (SubMesh1D) it.next();
			if (null != submesh1d)
				assert(submesh1d.isValid());
		}
		return true;
	}
	
	/**
	 * Prints edge lengths of a <code>MMesh1D</code> instance.
	 */
	public void printInfos()
	{
		for(Iterator it=mapTEdgeToSubMesh1D.values().iterator();it.hasNext();)
		{
			SubMesh1D submesh1d = (SubMesh1D) it.next();
			if (null != submesh1d)
				submesh1d.printInfos();
		}
	}
	
	public String toString()
	{
		String cr=System.getProperty("line.separator");
		StringBuffer r = new StringBuffer("MMesh1D"+cr);
		logger.debug("Printing "+r.toString());
		for(Iterator it=mapTEdgeToSubMesh1D.values().iterator();it.hasNext();)
		{
			SubMesh1D submesh1d = (SubMesh1D) it.next();
			if (null != submesh1d)
				r.append(submesh1d);
		}
		logger.debug("...done");
		return r.toString();
	}
}

