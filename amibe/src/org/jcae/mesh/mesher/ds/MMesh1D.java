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

package org.jcae.mesh.mesher.ds;

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
			//  Edges may get connected to more than 2 faces
			if (mapTEdgeToSubMesh1D.containsKey(E))
				continue;
			SubMesh1D submesh1d = null;
			if (!mapTEdgeToSubMesh1D.containsKey(E.reversed()))
				submesh1d = new SubMesh1D(E, (MMesh0D) this);
			//  By convention, a null value means that E.reversed()
			//  has already been stored.
			mapTEdgeToSubMesh1D.put(E, submesh1d);
			listTEdge.add(E);
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
	 * Update node labels.
	 */
	public void updateNodeLabels()
	{
		//  Resets all labels
		CADExplorer expE = CADShapeBuilder.factory.newExplorer();
		HashSet setSeenEdges = new HashSet();
		for (expE.init(shape, CADExplorer.EDGE); expE.more(); expE.next())
		{
			CADEdge E = (CADEdge) expE.current();
			SubMesh1D submesh1d = (SubMesh1D) mapTEdgeToSubMesh1D.get(E);
			if (null == submesh1d || setSeenEdges.contains(E))
				continue;
			setSeenEdges.add(E);
			for (Iterator it = submesh1d.getNodesIterator(); it.hasNext(); )
			{
				MNode1D n = (MNode1D) it.next();
				n.setLabel(-1);
			}
		}
		int i = 0;
		setSeenEdges.clear();
		for (expE.init(shape, CADExplorer.EDGE); expE.more(); expE.next())
		{
			CADEdge E = (CADEdge) expE.current();
			SubMesh1D submesh1d = (SubMesh1D) mapTEdgeToSubMesh1D.get(E);
			if (null == submesh1d || setSeenEdges.contains(E))
				continue;
			setSeenEdges.add(E);
			for (Iterator it = submesh1d.getNodesIterator(); it.hasNext(); )
			{
				MNode1D n = (MNode1D) it.next();
				if (-1 == n.getMaster().getLabel())
				{
					n.getMaster().setLabel(i);
					i++;
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
		HashSet seen = new HashSet();
		CADExplorer expV = CADShapeBuilder.factory.newExplorer();
		for (expV.init(shape, CADExplorer.VERTEX); expV.more(); expV.next())
		{
			CADVertex V = (CADVertex) expV.current();
			MNode1D[] vnodelist = getArrayHasRef(V);
			if (vnodelist.length <= 1)
				continue;
			if (seen.contains(vnodelist[0]))
				continue;
			seen.add(vnodelist[0]);
			vnodelist[0].setMaster(null);
			for (int i = 1; i<vnodelist.length; i++)
				vnodelist[i].setMaster(vnodelist[0]);
		}
		seen.clear();
		CADExplorer expF = CADShapeBuilder.factory.newExplorer();
		for (expF.init(shape, CADExplorer.FACE); expF.more(); expF.next())
		{
			CADShape F = (CADShape) expF.current();
			buildFaceBoundary(F);
		}
		assert(isValid());
	}
	
	//  Build a mesh bound to a face.
	private void buildFaceBoundary(CADShape F)
	{
		CADExplorer expE = CADShapeBuilder.factory.newExplorer();
		for (expE.init(F, CADExplorer.EDGE); expE.more(); expE.next())
		{
			CADEdge E = (CADEdge) expE.current();
			SubMesh1D submesh1d = (SubMesh1D) mapTEdgeToSubMesh1D.get(E);
			if (null != submesh1d)
				continue;
			
			SubMesh1D submeshbis = (SubMesh1D) mapTEdgeToSubMesh1D.get(E.reversed());
			assert(null != submeshbis);
			ArrayList oldnodelist = submeshbis.getNodes();
			ArrayList oldedgelist = submeshbis.getEdges();
			
			submesh1d = new SubMesh1D(E);
			ArrayList nodelist = submesh1d.getNodes();
			ArrayList edgelist = submesh1d.getEdges();
			HashMap map = new HashMap(oldnodelist.size());
			for (Iterator it = oldnodelist.iterator(); it.hasNext(); )
			{
				MNode1D p1 = (MNode1D) it.next();
				MNode1D p2 = new MNode1D(p1);
				nodelist.add(p2);
				map.put(p1, p2);
			}
			for (Iterator it = oldedgelist.iterator(); it.hasNext(); )
			{
				MEdge1D e1 = (MEdge1D) it.next();
				MNode1D p1 = (MNode1D) map.get(e1.getNodes1());
				MNode1D p2 = (MNode1D) map.get(e1.getNodes2());
				MEdge1D e2 = new MEdge1D(p1, p2, e1, false);
				edgelist.add(e2);
			}
			mapTEdgeToSubMesh1D.put(E, submesh1d);
		}
		for (expE.init(F, CADExplorer.EDGE); expE.more(); expE.next())
		{
			CADEdge E = (CADEdge) expE.current();
			assert (null != mapTEdgeToSubMesh1D.get(E));
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
	 * @return the list of nodes inserted on this edge, or <code>null</code>
	 * if <code>E.reversed()</code> is already discretized.
	 * @throws NoSuchElementException if <code>E</code> and
	 * <code>E.reversed()</code> are unknown edges.
	 */
	public ArrayList getNodelistFromMap(CADEdge E)
           throws NoSuchElementException
	{
		SubMesh1D submesh1d = (SubMesh1D) mapTEdgeToSubMesh1D.get(E);
		if (null != submesh1d)
			return submesh1d.getNodes();
		if (mapTEdgeToSubMesh1D.containsKey(E.reversed()))
			return null;
		throw new NoSuchElementException("TEdge : "+E);
	}
	
	/**
	 * Returns the list of edges inserted on a given topological edge.
	 *
	 * @param E  a topological edge.
	 * @return the list of edges inserted on this edge, or <code>null</code>
	 * if <code>E.reversed()</code> is already discretized.
	 * @throws NoSuchElementException if <code>E</code> and
	 * <code>E.reversed()</code> are unknown edges.
	 */
	public ArrayList getEdgelistFromMap(CADEdge E)
	       throws NoSuchElementException
	{
		SubMesh1D submesh1d = (SubMesh1D) mapTEdgeToSubMesh1D.get(E);
		if (null != submesh1d)
			return submesh1d.getEdges();
		if (mapTEdgeToSubMesh1D.containsKey(E.reversed()))
			return null;
		throw new NoSuchElementException("TEdge : "+E);
	}
	
	/**
	 * Returns the list of nodes which are linked to a given vertex.
	 *
	 * @param V  a topological vertex
	 * @return an array of <code>MNode1D</code> instances which are
	 * bounded to <code>V</code>.
	 */
	public MNode1D[] getArrayHasRef(CADVertex V)
	{
		ArrayList list = new ArrayList();
		Iterator ite = mapTEdgeToSubMesh1D.values().iterator();
		while (ite.hasNext())
		{
			SubMesh1D submesh1d = (SubMesh1D) ite.next();
			if (null == submesh1d)
				continue;
			Iterator itn = submesh1d.getNodesIterator();
			while (itn.hasNext())
			{
				MNode1D pt = (MNode1D) itn.next();
				if (null != pt.getRef() && V.isSame(pt.getRef()))
					list.add(pt);
			}
		}
		MNode1D [] toreturn = new MNode1D[list.size()];
		System.arraycopy(list.toArray(), 0, toreturn, 0, toreturn.length);
		return toreturn;
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

