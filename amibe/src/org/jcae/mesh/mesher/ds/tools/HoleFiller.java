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

package org.jcae.mesh.mesher.ds.tools;

import org.jcae.mesh.mesher.ds.*;
import org.jcae.mesh.cad.CADGeomSurface;
import java.util.Iterator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.log4j.Logger;

/**
 * Meshes a hole.
 * This package has two entry points:
 * <ol>
 *   <li><code>rebuildBoundary</code>, called after performing the
 *       initial Delaunay mesh to retreive original boundary edges.</li>
 *   <li><code>collapseEdge</code>, called by algorithms when deleting
 *       edges.</li>
 * </ol>
 */

public class HoleFiller
{
	private static Logger logger=Logger.getLogger(HoleFiller.class);	

	//  The SubMesh2D instance on which methods are applied
	private SubMesh2D submesh2d;
	
	/**
	 * Constructor.
	 *
	 * @param  m   the <code>SubMesh2D</code> being modified.
	 */
	public HoleFiller(SubMesh2D m)
	{
		submesh2d = m;
	}
	
	/**
	 * Constrained remeshing.
	 *
	 * @param  edgelist  list of constrained edges
	 */
	public void rebuildBoundary(Collection edgelist)
	{
		for (Iterator ite = edgelist.iterator(); ite.hasNext(); )
		{
			MEdge2D eb = (MEdge2D) ite.next();
			MNode2D node1 = eb.getNodes1();
			MNode2D node2 = eb.getNodes2();
			MEdge2D em = submesh2d.getEdgeDefinedByNodes(node1, node2);
			
			if (null == em)
			{
				if (!constrainEdge(node1, node2, eb.getRef()))
					throw new RuntimeException("Unable to build edge "+eb);
				em = submesh2d.getEdgeDefinedByNodes(node1, node2);
			}
			if (em != eb)
			{
				for (Iterator itf = em.getFacesIterator(); itf.hasNext(); )
				{
					MFace2D f = (MFace2D) itf.next();
					submesh2d.addEdge(eb);
					f.substEdge(em, eb);
				}
				submesh2d.getEdges().remove(em);
			}
		}
		assert submesh2d.isValid();
	}
	
	/**
	 * Removes an edge and all connected faces, and meshes the generated hole.
	 *
	 * @param  edge  an edge to collapse
	 */
	public HashSet collapseEdge(MEdge2D edge, boolean force)
	{
		//  First check that edge collapse does preserve topology
		MNode2D pt1 = edge.getNodes1();
		MNode2D pt2 = edge.getNodes2();
		if (!pt1.isMutable() && !pt2.isMutable())
			return null;
		Iterator it = edge.getFacesIterator();
		
		//  Save faces connected to pt1 or pt2 in a HashSet
		HashSet facesConnected = new HashSet(pt1.getElements2D());
		facesConnected.addAll(pt2.getElements2D());

		// Retrieve the edges not connected to pt1 or pt2
		HashSet edgesNotConnected = new HashSet();
		Iterator itfc = facesConnected.iterator();
		while(itfc.hasNext())
		{
			MFace2D f = (MFace2D) itfc.next();
			Iterator ittemp = f.getEdgesIterator();
			while (ittemp.hasNext())
			{
				MEdge2D e = (MEdge2D) ittemp.next();
				MNode2D e1 = e.getNodes1();
				MNode2D e2 = e.getNodes2();
				if (!e.isMutable() ||
				    (e1 != pt1 && e1 != pt2 && e2 != pt1 && e2 != pt2))
				{
					//  Do not collapse if it reduces the connectivity
					//  of a degenerated node
					if (null != pt1.getRef() && pt1.getRef().isDegenerated())
						return null;
					if (null != pt2.getRef() && pt2.getRef().isDegenerated())
						return null;
					if (edgesNotConnected.contains(e))
						//  In fact e is not a boundary
						edgesNotConnected.remove(e);
					else
						edgesNotConnected.add(e);
				}
			}
		}
		
		// Computes new point
		MNode2D np;
		ArrayList edgelist = new ArrayList();
		edgelist.addAll(edgesNotConnected);
		
		assert (pt1.isMutable() || pt2.isMutable());
		if (pt1.isMutable() && pt2.isMutable())
			np = pt1;
		else
		{
			if (!pt1.isMutable())
				np = pt1;
			else
				np = pt2;
			for (Iterator ite = edgelist.iterator(); ite.hasNext(); )
			{
				MEdge2D e = (MEdge2D) ite.next();
				//  Do not build a triangle which will not be mutable
				if (!e.isMutable())
					return null;
			}
		}
		
		HashMap nodes2edges = mapNodesToEdges(edgelist);
		//  Get orientation
		boolean isPositive = true;
		MEdge2D newEdge = null;
		for (Iterator itn = nodes2edges.values().iterator(); itn.hasNext(); )
		{
			MEdge2D [] e = (MEdge2D []) itn.next();
			MNode2D p1 = e[1].getNodes1();
			MNode2D p2 = e[1].getNodes2();
			if (null == submesh2d.getFaceDefinedByNodes(p1, p2, np))
				continue;
			double orientation = np.orient2d(p1, p2, submesh2d.getGeomSurface());
			if (orientation == 0.0)
				continue;
			newEdge = e[1];
			isPositive = (orientation > 0.0);
			break;
		}
		if  (null == newEdge)
			return null;
		
		double qmin = Double.MAX_VALUE;
		if (force)
			qmin = -1.0;
		else
			for (Iterator itf = facesConnected.iterator(); itf.hasNext(); )
			{
				MFace2D f = (MFace2D) itf.next();
				qmin = Math.min(qmin, submesh2d.compGeom().quality(f));
			}
		ArrayList newNodelist = meshHole(nodes2edges, newEdge, qmin, isPositive);
		if (null == newNodelist)
		{
			assert submesh2d.isValid();
			return null;
		}
		
		HashSet toReturn = new HashSet();
		for (Iterator itn = newNodelist.iterator(); itn.hasNext(); )
		{
			MNode2D f1 = (MNode2D) itn.next();
			MNode2D f2 = (MNode2D) itn.next();
			MNode2D f3 = (MNode2D) itn.next();
			MFace2D f = submesh2d.addTriangle(f1, f2, f3);
			toReturn.addAll(f.getEdges());
		}
		for (Iterator itf = facesConnected.iterator(); itf.hasNext(); )
		{
			MFace2D f = (MFace2D) itf.next();
			submesh2d.rmFace(f);
		}
		assert submesh2d.isValid();
		return toReturn;
	}
	
	/**
	 * Local remeshing to force a given edge to be part of the mesh.
	 * This method handles two different situations:
	 * <ul>
	 *   <li>During the initial mesh to rebuild boundary edges.  In this
	 *       case, <code>edge1d</code> is not null.</li>
	 *   <li>When invoked from <code>algos2d</code> algorithms.  It is only
	 *       applied to inner edges since boundary edges are immutable,
	 *       so in this case <code>edge1d</code> is null.</li>
	 * </ul>
	 * All edges cutting the new edge are removed, the new edge is created,
	 * upper and lower regions are meshed.
	 *
	 * @param  n1  an end of the new edge.
	 * @param  n2  the other end of the new edge.
	 * @param  edge1d  the reference to the 1d edge, or <code>null</code>.
	 * @return <code>true</code> if successful, <code>false</code> otherwise.
	 */
	private boolean constrainEdge(MNode2D n1, MNode2D n2, MEdge1D edge1d)
	{
		logger.debug("In constrainEdge: "+n1+" "+n2+" "+edge1d);
		CADGeomSurface surf = submesh2d.getGeomSurface();
		FindEdge2DCutter edgeCutter = new FindEdge2DCutter(submesh2d);
		Collection cEdges = edgeCutter.getCuttingEdges(n1, n2);
		HashSet cFaces = new HashSet();
		HashSet boundEdges = new HashSet();
		for (Iterator ite = cEdges.iterator(); ite.hasNext(); )
		{
			MEdge2D e = (MEdge2D) ite.next();
			logger.debug("Edge removed: "+e);
			cFaces.addAll(e.getFaces());
		}
		
		//  Save triangles in keepNodelist and remove them from the mesh
		ArrayList keepNodelist = new ArrayList();
		double qmin = Double.MAX_VALUE;
		for (Iterator itf = cFaces.iterator(); itf.hasNext(); )
		{
			MFace2D f = (MFace2D) itf.next();
			if (null == edge1d)
				qmin = Math.min(qmin, submesh2d.compGeom().quality(f));
			boundEdges.addAll(f.getEdges());
			keepNodelist.addAll(f.getNodes());
			f.unlink();
			submesh2d.getFaces().remove(f);
		}
		boundEdges.removeAll(cEdges);
		for (Iterator ite = cEdges.iterator(); ite.hasNext(); )
		{
			MEdge2D e = (MEdge2D) ite.next();
			submesh2d.getEdges().remove(e);
		}
		
		MEdge2D newEdge = new MEdge2D(n1, n2, edge1d);
		MNode2D np1 = newEdge.getNodes1();
		MNode2D np2 = newEdge.getNodes2();
		//  The hole is separated into 2 adjacent regions
		ArrayList regionP = new ArrayList();
		ArrayList regionM = new ArrayList();
		regionP.add(newEdge);
		regionM.add(newEdge);
		for (Iterator ite = boundEdges.iterator(); ite.hasNext(); )
		{
			MEdge2D e = (MEdge2D) ite.next();
			double v1 = np1.orient2d(np2, e.getNodes1(), surf);
			double v2 = np1.orient2d(np2, e.getNodes2(), surf);
			if ((v1 < 0.0 && v2 > 0.0) || (v1 == 0.0 && v2 == 0.0))
				throw new RuntimeException("Malformed region");
			if (v1+v2 > 0.0)
				regionP.add(e);
			else
				regionM.add(e);
		}
		if (null != edge1d)
			qmin = -1.0;
		HashMap nodes2edgesP = mapNodesToEdges(regionP);
		ArrayList newNodelistP = meshHole(nodes2edgesP, newEdge, qmin, true);
		if (null != newNodelistP)
		{
			HashMap nodes2edgesM = mapNodesToEdges(regionM);
			ArrayList newNodelistM = meshHole(nodes2edgesM, newEdge, qmin, false);
			if (null != newNodelistM)
			{
				logger.debug("Edge "+newEdge+" successfully added");
				newNodelistP.addAll(newNodelistM);
				for (Iterator itn = newNodelistP.iterator(); itn.hasNext(); )
				{
					MNode2D pt1 = (MNode2D) itn.next();
					MNode2D pt2 = (MNode2D) itn.next();
					MNode2D pt3 = (MNode2D) itn.next();
					submesh2d.addTriangle(pt1, pt2, pt3);
				}
				return true;
			}
			for (Iterator itn = newNodelistP.iterator(); itn.hasNext(); )
			{
				MNode2D pt1 = (MNode2D) itn.next();
				MNode2D pt2 = (MNode2D) itn.next();
				MNode2D pt3 = (MNode2D) itn.next();
				MFace2D f = submesh2d.getFaceDefinedByNodes(pt1, pt2, pt3);
				assert f != null;
				f.unlink();
				submesh2d.getFaces().remove(f);
			}
		}
		
		//  Put original triangles back
		logger.debug("Edge "+newEdge+" not added");
		for (Iterator itn = keepNodelist.iterator(); itn.hasNext(); )
		{
			MNode2D pt1 = (MNode2D) itn.next();
			MNode2D pt2 = (MNode2D) itn.next();
			MNode2D pt3 = (MNode2D) itn.next();
			submesh2d.addTriangle(pt1, pt2, pt3);
		}
		
		return false;
	}
	
	/*
	 * nodes2edges is a map: MNode2D -> MEdge2D[2]
	 * newEdge is an edge of the boundary, which gives the orientation.
	 * Returns a sorted list of nodes, and end points of nodes2edges are
	 * alos sorted.
	 */
	private ArrayList buildWire(HashMap nodes2edges, MEdge2D newEdge)
	{
		ArrayList nodelist = new ArrayList();
		MNode2D end = newEdge.getNodes1();
		MNode2D guard = end;
		MNode2D start = null;
		MEdge2D curEdge = newEdge;
		for (int count = 0; count < nodes2edges.size(); count++)
		{
			start = end;
			MNode2D p1 = curEdge.getNodes1();
			MNode2D p2 = curEdge.getNodes2();
			if (p2 == start)
			{
				curEdge.swapNodes();
				p1 = curEdge.getNodes1();
				p2 = curEdge.getNodes2();
			}
			end = p2;
			nodelist.add(end);
			MEdge2D [] edges = (MEdge2D []) nodes2edges.get(end);
			assert edges != null && edges.length == 2;
			if (edges[1] == curEdge)
			{
				//  Swap edges
				edges[1] = edges[0];
				edges[0] = curEdge;
			}
			curEdge = edges[1];
		}
		
		return nodelist;
	}
	
	private void printWire(ArrayList nodelist, HashMap nodes2edges)
	{
		for (Iterator itn = nodelist.iterator(); itn.hasNext(); )
		{
			MNode2D n = (MNode2D) itn.next();
			logger.debug(""+n);
			MEdge2D [] e = (MEdge2D []) nodes2edges.get(n);
			logger.debug("\t"+e[0]);
			logger.debug("\t"+e[1]);
		}
	}
	
	/*
	 * nodes2edges is a map: MNode2D -> MEdge2D[2]
	 * isPositive: true when meshing the + region, false otherwise
	 */
	private ArrayList meshHole(HashMap nodes2edges, MEdge2D newEdge, double qmin, boolean isPositive)
	{
		// 1.  Build the wire
		ArrayList nodelist = buildWire(nodes2edges, newEdge);
		CADGeomSurface surf = submesh2d.getGeomSurface();
		assert nodelist.size() > 0;
		//printWire(nodelist, nodes2edges);
		// 2.  Mesh
		ArrayList toReturn = new ArrayList();
		while (true)
		{
			boolean found = false;
			MNode2D n0 = null, n1 = null, n2 = null;
			int index = 0, i = 0;
			double qnewmin = Double.MAX_VALUE;
			for (Iterator itn = nodelist.iterator(); itn.hasNext(); )
			{
				n1 = (MNode2D) itn.next();
				MEdge2D [] edges = (MEdge2D []) nodes2edges.get(n1);
				assert 2 == edges.length;
				assert n1 == edges[0].getNodes2();
				assert n1 == edges[1].getNodes1();
				n0 = edges[0].getNodes1();
				n2 = edges[1].getNodes2();
				double orient = n0.orient2d(n1, n2, surf);
				if ((isPositive && orient > 0.0) ||
					(!isPositive && orient < 0.0))
				{
					double qnew = submesh2d.compGeom().quality(n0, n1, n2);
					//if (qmin < 0.0 || qnew > qmin)
					if (qnew < qnewmin)
					{
						qnewmin = qnew;
						index = i;
					}
				}
				i++;
			}
			ArrayList newList = new ArrayList(nodelist.size());
			index--;
			if (index < 0)
				index = nodelist.size()-1;
			newList.addAll(nodelist.subList(index, nodelist.size()-1));
			if (index > 1)
				newList.addAll(nodelist.subList(0, index-1));
			index = -1;
			for (Iterator itn = nodelist.iterator(); itn.hasNext(); )
			{
				n1 = (MNode2D) itn.next();
				index++;
				if (index == 1)
					continue;
				MEdge2D [] edges = (MEdge2D []) nodes2edges.get(n1);
				assert 2 == edges.length;
				assert n1 == edges[0].getNodes2();
				assert n1 == edges[1].getNodes1();
				n0 = edges[0].getNodes1();
				n2 = edges[1].getNodes2();
				double orient = n0.orient2d(n1, n2, surf);
				if ((isPositive && orient > 0.0) ||
					(!isPositive && orient < 0.0))
				{
					double qnew = submesh2d.compGeom().quality(n0, n1, n2);
					//if (qmin < 0.0 || qnew > qmin)
					if (qnew == 0.0 && nodelist.size() == 3)
					{
						found = true;
						break;
					}

					if (qmin < 0.0 || qnew > 0.0)
					{
						//  Check that there are no interior nodes
						boolean inner = false;
						for (Iterator itn2 = nodelist.iterator(); itn2.hasNext(); )
						{
							MNode2D o1 = (MNode2D) itn2.next();
							if (o1 == n0 || o1 == n1 || o1 == n2)
								continue;
							if (o1.inTriangle(n0, n1, n2, surf) >= 0)
							{
								inner = true;
								break;
							}
						}
						if (!inner)
						{
							found = true;
							break;
						}
					}
				}
			}
			if (qmin < 0.0)
				assert found;
			if (!found)
				return null;
			toReturn.add(n0);
			toReturn.add(n1);
			toReturn.add(n2);
			//  Remove n1 from nodelist
			nodelist.remove(index);
			if (nodelist.size() < 3)
				break;
			nodes2edges.remove(n1);
			MEdge2D e = new MEdge2D(n0, n2);
			MEdge2D [] before = (MEdge2D []) nodes2edges.get(n0);
			MEdge2D [] after = (MEdge2D []) nodes2edges.get(n2);
			after[0] = e;
			before[1] = e;
		}
		return toReturn;
	}
	
	//  For each node on the boundary, get its 2 bounded segments
	private HashMap mapNodesToEdges(ArrayList edges)
	{
		HashMap nodes2edges = new HashMap();
		MNode2D [] nodes = new MNode2D[2];
		for (Iterator ite = edges.iterator(); ite.hasNext(); )
		{
			MEdge2D e = (MEdge2D) ite.next();
			nodes[0] = e.getNodes1();
			nodes[1] = e.getNodes2();
			for (int i = 0; i < 2; i++)
			{
				MEdge2D [] n2e = (MEdge2D []) nodes2edges.get(nodes[i]);
				if (null == n2e)
				{
					n2e = new MEdge2D[2];
					nodes2edges.put(nodes[i], n2e);
				}
				if (null == n2e[0])
					n2e[0] = e;
				else
					n2e[1] = e;
			}
		}
		//  Check that everything went fine
		for (Iterator ite = nodes2edges.values().iterator(); ite.hasNext(); )
		{
			MEdge2D [] e = (MEdge2D []) ite.next();
			if (null == e[0] || null == e[1])
				throw new RuntimeException("Malformed region");
		}
		return nodes2edges;
	}
	
	/**
	 * Local remeshing to force a given edge to be part of the mesh.
	 * This method tries to add an edge composed of the given two nodes
	 * to the mesh, be locally remeshing.  If this results to triangles
	 * with a lower quality, current mesh is unchanged.  This method
	 * only works for inner edges, boundary edges are handled by
	 * rebuildBoundary.
	 *
	 * @param  n1  an end of the new edge.
	 * @param  n2  the other end of the new edge.
	 * @return <code>true</code> if successful, <code>false</code> otherwise.
	 */
	public boolean tryMeshEdge(MNode2D n1, MNode2D n2)
	{
		return constrainEdge(n1, n2, null);
	}
	
	/**
	 * Constrained remeshing.
	 *
	 * @param  edgelist  list of boundary edges
	 * @param  oldEdge  old inner edge
	 */
	public void removeEdgeAndRemesh(Collection edgelist, MEdge2D oldEdge)
	{
	}
	
}
