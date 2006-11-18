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
import java.util.HashSet;
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
	private HashMap mapTEdgeToSubMesh1D;
	private HashMap mapTEdgeToFaces;;
	
	//  Edge list.
	private ArrayList listTEdge;
	
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
		for (expE.init(shape, CADExplorer.EDGE); expE.more(); expE.next())
			edges++;
		if (edges == 0)
			return;
		mapTEdgeToSubMesh1D = new HashMap(edges);
		listTEdge = new ArrayList(edges);
		
		for (expE.init(shape, CADExplorer.EDGE); expE.more(); expE.next())
		{
			CADEdge E = (CADEdge) expE.current();
			//  Edges may get connected to several faces
			if (mapTEdgeToSubMesh1D.containsKey(E))
				continue;
			SubMesh1D submesh1d = new SubMesh1D(E, (MMesh0D) this);
			mapTEdgeToSubMesh1D.put(E, submesh1d);
			listTEdge.add(E);
		}
		mapTEdgeToFaces = new HashMap(edges);
		for (Iterator it = listTEdge.iterator(); it.hasNext(); )
		{
			CADEdge E = (CADEdge) it.next();
			mapTEdgeToFaces.put(E, new HashSet());
		}
		CADExplorer expF = CADShapeBuilder.factory.newExplorer();
		for (expF.init(shape, CADExplorer.FACE); expF.more(); expF.next())
		{
			CADFace F = (CADFace) expF.current();
			HashSet set;
			for (expE.init(F, CADExplorer.EDGE); expE.more(); expE.next())
			{
				CADEdge E = (CADEdge) expE.current();
				set = (HashSet) mapTEdgeToFaces.get(E);
				set.add(F);
			}
		}
		assert(isValid());
	}
	
	public MMesh1D(BModel model)
	{
		super(model);
		shape = null;

		//  HashMap size will not be greater than the number of edges,
		//  so allocate them after computing their maximal size, they
		//  won't be resized.
		int edges = 0;
		BCADGraphCell root = model.getGraph().getRootCell();
		for (Iterator ite = root.shapesExplorer(CADShapeEnum.EDGE); ite.hasNext(); ite.next())
			edges++;
		if (edges == 0)
			return;
		mapTEdgeToSubMesh1D = new HashMap(edges);
		listTEdge = new ArrayList(edges);
		
		for (Iterator ite = root.shapesExplorer(CADShapeEnum.EDGE); ite.hasNext(); )
		{
			BCADGraphCell c = (BCADGraphCell) ite.next();
			CADEdge E = (CADEdge) c.getShape();
			//  Edges may get connected to several faces
			if (mapTEdgeToSubMesh1D.containsKey(E))
				continue;
			SubMesh1D submesh1d = (SubMesh1D) c.mesh;
			if (submesh1d == null && c.getReversed() != null)
				submesh1d = (SubMesh1D) c.getReversed().mesh;
			mapTEdgeToSubMesh1D.put(E, submesh1d);
			listTEdge.add(E);
		}
		mapTEdgeToFaces = new HashMap(edges);
		for (Iterator it = listTEdge.iterator(); it.hasNext(); )
		{
			CADEdge E = (CADEdge) it.next();
			mapTEdgeToFaces.put(E, new HashSet());
		}
		CADExplorer expE = CADShapeBuilder.factory.newExplorer();
		for (Iterator itf = root.shapesExplorer(CADShapeEnum.FACE); itf.hasNext(); )
		{
			BCADGraphCell s = (BCADGraphCell) itf.next();
			CADFace F = (CADFace) s.getShape();
			HashSet set;
			for (expE.init(F, CADExplorer.EDGE); expE.more(); expE.next())
			{
				CADEdge E = (CADEdge) expE.current();
				set = (HashSet) mapTEdgeToFaces.get(E);
				set.add(F);
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
	public HashSet getAdjacentFaces(CADEdge E)
	{
		HashSet ret = (HashSet) mapTEdgeToFaces.get(E);
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
		for (expE.init(shape, CADExplorer.EDGE); expE.more(); expE.next())
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
		for (expE.init(shape, CADExplorer.EDGE); expE.more(); expE.next())
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
		for (expV.init(shape, CADExplorer.VERTEX); expV.more(); expV.next())
			nVertex++;
		HashMap vertex2Ref = new HashMap(nVertex);
		for (expV.init(shape, CADExplorer.VERTEX); expV.more(); expV.next())
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
		
		HashSet seen = new HashSet();
		for (expV.init(shape, CADExplorer.VERTEX); expV.more(); expV.next())
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
	public ArrayList getTEdgeList()
	{
		return listTEdge;
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
		String r="MMesh1D"+cr;
		logger.debug("Printing "+r);
		for(Iterator it=mapTEdgeToSubMesh1D.values().iterator();it.hasNext();)
		{
			SubMesh1D submesh1d = (SubMesh1D) it.next();
			if (null != submesh1d)
				r += submesh1d;
		}
		logger.debug("...done");
		return r;
	}
}

