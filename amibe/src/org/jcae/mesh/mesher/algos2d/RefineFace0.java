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

package org.jcae.mesh.mesher.algos2d;

import org.jcae.mesh.mesher.ds.*;
import org.jcae.mesh.cad.CADGeomSurface;
import java.util.Iterator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Collection;
import java.util.ArrayList;
import org.apache.log4j.Logger;

/**
 * Performs an initial surface triangulation.
 * The value of discretisation is provided by the constraint hypothesis.
 */

public class RefineFace0
{
	private static Logger logger=Logger.getLogger(RefineFace0.class);
	private SubMesh2D submesh2d = null;
	
	/**
	 * Creates a <code>RefineFace0</code> instance.
	 *
	 * @param m  the <code>RefineFace0</code> instance to refine.
	 */
	public RefineFace0(SubMesh2D m)
	{
		submesh2d = m;
	}
	
	/**
	 * Launch method to mesh a surface.
	 *
	 * @see #computeFace
	 */
	public void compute()
	{
			computeFace(submesh2d);
	}
	
	/**
	 * Launch method to mesh a surface.
	 * <ul>
	 *   <li>Creation of an unconstrained Delaunay triangulation containing
	 *       all boundary nodes.</li>
	 *   <li>Edge swap to restore boundary edges.</li>
	 *   <li>Removal of outer triangles.</li>
	 * </ul>
	 * @see RefineFaceOld
	 *
	 * @param submesh2d  the mesh data structure which is updated.
	 */
	public static void computeFace(SubMesh2D submesh2d)
	{
		HashSet boundaryEdges = new HashSet(submesh2d.getEdges());
		submesh2d.getEdges().clear();
		logger.debug(" Unconstrained Delaunay triangulation");
		MNode2D infNode0 = buildUnconstrainedDelaunay(submesh2d);
		logger.debug(" Rebuild boundary edges");
		Object[] edges = boundaryEdges.toArray();
		int nb_edges = edges.length;
		int j = 0;
		int nb_swap = 0, edge_ok = 0;

		while (nb_swap + edge_ok < nb_edges) {
			MEdge2D eb = (MEdge2D) edges[j];
			MNode2D node1 = eb.getNodes1();
			MNode2D node2 = eb.getNodes2();
			
			MEdge2D em = submesh2d.getEdgeDefinedByNodes(node1, node2);
			if (null == em)
			{
				Collection set = submesh2d.findEdgesCuttingEndpoints(node1,node2);
				Iterator ite = set.iterator();
				while (ite.hasNext())
				{
					MEdge2D elem = (MEdge2D) ite.next();
					HashSet faces = elem.getFaces();
					assert (faces.size() == 2);
					MFace2D [] oldT = new MFace2D[2];
					System.arraycopy(faces.toArray(), 0, oldT, 0, 2);;
					submesh2d.flipEdge(elem, oldT, true);
				}
				if (eb == submesh2d.getEdgeDefinedByNodes(node1, node2))
					nb_swap++;
			}
			else
			{
				// the edge is OK, but em must be replaced by eb in the mesh
				edge_ok++;
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

			j++;
			if ((j == nb_edges) && (nb_swap + edge_ok != nb_edges)) {
				j = 0;
				edge_ok = 0;
				nb_swap = 0;
			}
		}

		logger.debug(" Remove outer elements");
		for (Iterator it = submesh2d.getEdgesIterator(); it.hasNext(); )
		{
			MEdge2D e = (MEdge2D) it.next();
			e.setFrozen(false);
		}
		int nF = submesh2d.getFaces().size();
		ArrayList keepFaces = new ArrayList(nF);
		ArrayList removeFaces = new ArrayList(nF);
		int nK = 0, nR = 0;
		removeFaces.addAll(infNode0.getElements2D());
		while (nK + nR < nF)
		{
			if (nK < keepFaces.size())
			{
				MFace2D f = (MFace2D) keepFaces.get(nK);
				if (removeFaces.contains(f))
					throw new RuntimeException("Open shape: discarded");
				for (Iterator ite = f.getEdgesIterator(); ite.hasNext(); )
				{
					MEdge2D e = (MEdge2D) ite.next();
					for (Iterator itf = e.getFacesIterator(); itf.hasNext(); )
					{
						MFace2D fn = (MFace2D) itf.next();
						if (fn == f)
							continue;
						//  Add fn to the right list
						if (boundaryEdges.contains(e))
						{
							if (!removeFaces.contains(fn))
								removeFaces.add(fn);
						}
						else if (!keepFaces.contains(fn))
							keepFaces.add(fn);
					}
				}
				nK++;
			}
			else
			{
				MFace2D f = (MFace2D) removeFaces.get(nR);
				if (keepFaces.contains(f))
					throw new RuntimeException("Open shape: discarded");
				if (nR >= removeFaces.size())
					throw new RuntimeException("Internal error when removing outer elements");
				for (Iterator ite = f.getEdgesIterator(); ite.hasNext(); )
				{
					MEdge2D e = (MEdge2D) ite.next();
					for (Iterator itf = e.getFacesIterator(); itf.hasNext(); )
					{
						MFace2D fn = (MFace2D) itf.next();
						if (fn == f)
							continue;
						//  Add fn to the right list
						if (boundaryEdges.contains(e))
						{
							if (!keepFaces.contains(fn))
								keepFaces.add(fn);
						}
						else if (!removeFaces.contains(fn))
							removeFaces.add(fn);
					}
				}
				nR++;
			}
		}
		for (Iterator it = removeFaces.iterator(); it.hasNext(); )
		{
			MFace2D f = (MFace2D) it.next();
			submesh2d.rmFace(f);
		}
		
		assert (submesh2d.isValid());
	}
	
	private static MNode2D buildUnconstrainedDelaunay(SubMesh2D submesh2d)
	{
		//  computeBoundingTriangle adds 3 nodes to the mesh, so
		//  innerNodes must be set first.
		CADGeomSurface surf = submesh2d.getGeomSurface();
		HashSet innerNodes = new HashSet(submesh2d.getNodes());
		MFace2D triangle0 = submesh2d.computeBoundingTriangle();
		for (Iterator it = triangle0.getEdgesIterator(); it.hasNext(); )
		{
			MEdge2D e = (MEdge2D) it.next();
			e.setFrozen(true);
		}
		//  Clear node set so that submesh2d validity can be checked
		//  at any time.
		submesh2d.getNodes().clear();
		logger.debug("Outer triangle "+triangle0);
		HashMap mapTriangleToNodeset = new HashMap();
		mapTriangleToNodeset.put(triangle0, innerNodes);
		
		//  infNode0 is used later as a starting point to remove
		//  outer elements.
		MNode2D infNode0 = (MNode2D) triangle0.getNodesIterator().next();
		MNode2D firstNode = null;
		int nFaces = 1, nEdges = 3, nNodes = 3;

		while (!mapTriangleToNodeset.isEmpty())
		{
			MFace2D f = null;
			int nP = -1;
			//  Find the triangle which contains the highest number of points,
			//  and pick up the first point.
			for (Iterator it = mapTriangleToNodeset.keySet().iterator(); it.hasNext(); )
			{
				MFace2D fn = (MFace2D) it.next();
				HashSet remainingNodes = (HashSet) mapTriangleToNodeset.get(fn);
				assert (null != remainingNodes);
				if (remainingNodes.size() > nP)
				{
					nP = remainingNodes.size();
					f = fn;
					firstNode = (MNode2D) remainingNodes.iterator().next();
				}
			}
			assert (null != f);
			logger.debug("Inserting point "+firstNode+" in triangle "+f);
			
			/*
			 * The explodeTriangle() method adds a point to the
			 * triangulation.  If it is interior to the triangle,
			 * 3 new triangles are created and returned.  This is
			 * the more common case, and it also handles the
			 * following degenerated cases:
			 *   - If the inserted point is an existing vertex,
			 *     null is returned.
			 *   - If the inserted point lies on an edge, both
			 *     triangles are split into 2 smaller triangles,
			 *     which gives 4 new triangles.  But we need to
			 *     know which other triangle has been split, so
			 *     it is returned as the 5th argument.
			 */
			
			MFace2D [] newT = submesh2d.explodeTriangle(f, firstNode);
			assert (null != newT);
			HashSet remainingNodes = (HashSet) mapTriangleToNodeset.get(f);
			assert (null != remainingNodes);
			remainingNodes.remove(firstNode);
			HashSet edgesToCheck = new HashSet();
			if (newT.length == 3)
			{
				//  Outer boundary is unchanged, so all nodes must
				//  appear in one of those triangles
				HashSet [] hashnodes = updateNodes(remainingNodes, newT, surf, false);
				for (int i = 0; i < hashnodes.length; i++)
					if (!hashnodes[i].isEmpty())
						mapTriangleToNodeset.put(newT[i], hashnodes[i]);
				
				Iterator it = f.getNodesIterator();
				MNode2D P1 = (MNode2D) it.next();
				MNode2D P2 = (MNode2D) it.next();
				MNode2D P3 = (MNode2D) it.next();

				submesh2d.rmFace(f);
				mapTriangleToNodeset.remove(f);
				edgesToCheck.add(submesh2d.getEdgeDefinedByNodes(P1, P2));
				edgesToCheck.add(submesh2d.getEdgeDefinedByNodes(P2, P3));
				edgesToCheck.add(submesh2d.getEdgeDefinedByNodes(P3, P1));
			}
			else if (5 == newT.length)
			{
				//  newT[0..1] replace f
				//  newT[2..3] replace the adjacent triangle adj_tri
				//  newT[4] is adj_tri
				MFace2D adj_tri = newT[4];
				HashSet remainingNodes2 = (HashSet) mapTriangleToNodeset.get(adj_tri);
				//  remainingNodes2 may be null if adj_tri contains no points.
				if (null == remainingNodes2)
					remainingNodes2 = new HashSet();
				HashSet [] hashnodes_1 = updateNodes(remainingNodes, newT, 0, 2, surf, true);
				remainingNodes2.addAll(hashnodes_1[2]);
				remainingNodes.clear();
				//  Next insert points in newT[2..3]
				HashSet [] hashnodes_2 = updateNodes(remainingNodes2, newT, 2, 2, surf, true);
				remainingNodes.addAll(hashnodes_2[2]);
				//  Last pass in case some points where moved to f.
				//  But in this case they must not be reassigned
				//  back to adj_tri.
				HashSet [] hashnodes_3 = updateNodes(remainingNodes, newT, 0, 2, surf, false);
				hashnodes_1[0].addAll(hashnodes_3[0]);
				hashnodes_1[1].addAll(hashnodes_3[1]);
				for (int i = 0; i < 2; i++)
				{
					if (!hashnodes_1[i].isEmpty())
						mapTriangleToNodeset.put(newT[i], hashnodes_1[i]);
					if (!hashnodes_2[i].isEmpty())
						mapTriangleToNodeset.put(newT[i+2], hashnodes_2[i]);
				}
				
				HashSet set1 = new HashSet(f.getEdges());
				HashSet set2 = new HashSet(adj_tri.getEdges());
				edgesToCheck.addAll(set1);
				edgesToCheck.addAll(set2);
				set1.retainAll(set2);
				edgesToCheck.removeAll(set1);
				
				submesh2d.rmFace(f);
				mapTriangleToNodeset.remove(f);
				submesh2d.rmFace(adj_tri);
				mapTriangleToNodeset.remove(adj_tri);
			}
			else
				throw new RuntimeException("Duplicate nodes found... aborting");
			
			assert (submesh2d.getNodes().size() == nNodes + 1);
			nNodes++;
			assert (submesh2d.getEdges().size() == nEdges + 3);
			nEdges += 3;
			assert (submesh2d.getFaces().size() == nFaces + 2);
			nFaces += 2;
			//  Flip edges
			flipEdges(submesh2d, edgesToCheck, mapTriangleToNodeset);
			assert (submesh2d.getNodes().size() == nNodes);
			assert (submesh2d.getEdges().size() == nEdges);
			assert (submesh2d.getFaces().size() == nFaces);
			assert (1 == nNodes - nEdges + nFaces);
		}
		return infNode0;
	}
	
	private static void flipEdges(SubMesh2D submesh2d, HashSet edges, HashMap mapTriangleToNodeset)
	{
		CADGeomSurface surf = submesh2d.getGeomSurface();
		HashSet seenEdges = new HashSet();
		while (!edges.isEmpty())
		{
			MEdge2D e = (MEdge2D) edges.iterator().next();
			logger.debug("Checking edge "+e);
			edges.remove(e);
			seenEdges.add(e);
			HashSet trianglelist = e.getFaces();
			if (2 > trianglelist.size())
				continue;
			MFace2D [] oldT = new MFace2D[2];
			System.arraycopy(trianglelist.toArray(), 0, oldT, 0, 2);;
			//  Building the list of edges which will have to be checked
			//  can easily be done here, so here we go even if it is
			//  useless when edge is not flipped.
			HashSet edgesToCheck = new HashSet();
			edgesToCheck.addAll(oldT[0].getEdges());
			edgesToCheck.addAll(oldT[1].getEdges());
			edgesToCheck.remove(e);
			
			MFace2D [] newT = submesh2d.flipEdge(e, oldT, false);
			if (null == newT)
				continue;
			logger.debug("Edge flipped:");
			HashSet list = new HashSet();
			// union of the 2 lists of nodes
			HashSet remainingNodes2 = (HashSet) mapTriangleToNodeset.get(oldT[0]);
			if (null != remainingNodes2)
			{
				list.addAll(remainingNodes2);
				mapTriangleToNodeset.remove(oldT[0]);
			}
			remainingNodes2 = (HashSet) mapTriangleToNodeset.get(oldT[1]);
			if (null != remainingNodes2)
			{
				list.addAll(remainingNodes2);
				mapTriangleToNodeset.remove(oldT[1]);
			}
			// assign nodes to new triangles
			HashSet [] hashnodes = updateNodes(list, newT, surf, false);
			if (!hashnodes[0].isEmpty())
				mapTriangleToNodeset.put(newT[0], hashnodes[0]);
			if (!hashnodes[1].isEmpty())
				mapTriangleToNodeset.put(newT[1], hashnodes[1]);
			
			// Check edges
			for (Iterator it = edgesToCheck.iterator(); it.hasNext(); )
			{
				MEdge2D newedge = (MEdge2D) it.next();
				if (!seenEdges.contains(newedge))
					edges.add(newedge);
			}
		}
	}
	
	private static HashSet [] updateNodes(HashSet innerNodes, MFace2D [] triangles, CADGeomSurface surf, boolean garbage)
	{
		int extra = (garbage ? 1 : 0);
		HashSet [] hashnodes = new HashSet[triangles.length+extra];
		for (int i = 0; i < hashnodes.length; i++)
			hashnodes[i] = new HashSet();
		for (Iterator it = innerNodes.iterator(); it.hasNext(); )
		{
			MNode2D pt = (MNode2D) it.next();
			boolean found = false;
			for (int i = 0; i < triangles.length; i++)
			{
				if (pt.inTriangle(triangles[i], surf) >= 0)
				{
					hashnodes[i].add(pt);
					found = true;
					break;
				}
			}
			if (!found)
			{
				if (garbage)
					hashnodes[triangles.length].add(pt);
				else
					throw new RuntimeException("Internal error when collecting inner nodes");
			}
		}
		return hashnodes;
	}
	
	private static HashSet [] updateNodes(HashSet innerNodes, MFace2D [] triangles, int first, int length, CADGeomSurface surf, boolean garbage)
	{
		MFace2D [] temp = new MFace2D[length];
		System.arraycopy(triangles, first, temp, 0, length);
		return updateNodes(innerNodes, temp, surf, garbage);
	}
	
	private static void splitInnerEdges(SubMesh2D submesh2d, int div)
	{
		if (div < 2)
			return;
		CADGeomSurface surf = submesh2d.getGeomSurface();
		HashSet innerNodes = new HashSet();
		HashMap mapTriangleToNodeset = new HashMap();
		for (Iterator it = submesh2d.getEdgesIterator(); it.hasNext(); )
		{
			MEdge2D e = (MEdge2D) it.next();
			MNode2D n1 = e.getNodes1();
			MNode2D n2 = e.getNodes2();
			if (n1.getRef() != null && n2.getRef() != null &&
			    e.getRef() == null)
			{
				//  Split e if n1 and n2 are on duplicated edges
				boolean found = false;
				for (Iterator it1 = n1.getEdgesIterator(); it1.hasNext(); )
				{
					MEdge2D e1 = (MEdge2D) it1.next();
					if (null == e1.getRef())
						continue;
					for (Iterator it2 = n2.getEdgesIterator(); it2.hasNext(); )
					{
						MEdge2D e2 = (MEdge2D) it2.next();
						if (null == e2.getRef())
							continue;
						if (e1.getRef() == e2.getRef())
						{
							found = true;
							break;
						}
					}
					if (found)
						break;
				}
				if (!found)
					continue;
				MFace2D f = (MFace2D) e.getFacesIterator().next();
				HashSet nodeset = (HashSet) mapTriangleToNodeset.get(f);
				if (nodeset == null)
				{
					nodeset = new HashSet();
					mapTriangleToNodeset.put(f, nodeset);
				}
				double [] p1 = e.getNodes1().getUV();
				double [] p2 = e.getNodes2().getUV();
				double delta = 1.0 / ((double) div);
				for (int i = 1; i < div; i++)
				{
					nodeset.add(new MNode2D(
						p1[0] + (p2[0]-p1[0])*i*delta,
						p1[1] + (p2[1]-p1[1])*i*delta
					));
				}
			}
		}
		
		MNode2D firstNode = null;
		int nFaces = submesh2d.getFaces().size();
		int nEdges = submesh2d.getEdges().size();
		int nNodes = submesh2d.getNodes().size();
		while (!mapTriangleToNodeset.isEmpty())
		{
			MFace2D f = null;
			int nP = 0;
			//  Find the triangle which contains the highest number of points,
			//  and pick up the first point.
			for (Iterator it = mapTriangleToNodeset.keySet().iterator(); it.hasNext(); )
			{
				MFace2D fn = (MFace2D) it.next();
				HashSet remainingNodes = (HashSet) mapTriangleToNodeset.get(fn);
				assert (null != remainingNodes);
				if (remainingNodes.size() > nP)
				{
					nP = remainingNodes.size();
					f = fn;
					firstNode = (MNode2D) remainingNodes.iterator().next();
				}
			}
			assert (null != f);
			logger.debug("Inserting point "+firstNode+" in triangle "+f);
			
			/*
			 * The explodeTriangle() method adds a point to the
			 * triangulation.  If it is interior to the triangle,
			 * 3 new triangles are created and returned.  This is
			 * the more common case, and it also handles the
			 * following degenerated cases:
			 *   - If the inserted point is an existing vertex,
			 *     null is returned.
			 *   - If the inserted point lies on an edge, both
			 *     triangles are split into 2 smaller triangles,
			 *     which gives 4 new triangles.  But we need to
			 *     know which other triangle has been split, so
			 *     it is returned as the 5th argument.
			 */
			
			MFace2D [] newT = submesh2d.explodeTriangle(f, firstNode);
			assert (null != newT);
			HashSet remainingNodes = (HashSet) mapTriangleToNodeset.get(f);
			assert (null != remainingNodes);
			remainingNodes.remove(firstNode);
			HashSet edgesToCheck = new HashSet();
			if (5 == newT.length)
			{
				//  newT[0..1] replace f
				//  newT[2..3] replace the adjacent triangle adj_tri
				//  newT[4] is adj_tri
				MFace2D adj_tri = newT[4];
				HashSet remainingNodes2 = (HashSet) mapTriangleToNodeset.get(adj_tri);
				//  remainingNodes2 may be null if adj_tri contains no points.
				if (null == remainingNodes2)
					remainingNodes2 = new HashSet();
				HashSet [] hashnodes_1 = updateNodes(remainingNodes, newT, 0, 2, surf, true);
				remainingNodes2.addAll(hashnodes_1[2]);
				remainingNodes.clear();
				//  Next insert points in newT[2..3]
				HashSet [] hashnodes_2 = updateNodes(remainingNodes2, newT, 2, 2, surf, true);
				remainingNodes.addAll(hashnodes_2[2]);
				//  Last pass in case some points where moved to f.
				//  But in this case they must not be reassigned
				//  back to adj_tri.
				HashSet [] hashnodes_3 = updateNodes(remainingNodes, newT, 0, 2, surf, false);
				hashnodes_1[0].addAll(hashnodes_3[0]);
				hashnodes_1[1].addAll(hashnodes_3[1]);
				for (int i = 0; i < 2; i++)
				{
					if (!hashnodes_1[i].isEmpty())
						mapTriangleToNodeset.put(newT[i], hashnodes_1[i]);
					if (!hashnodes_2[i].isEmpty())
						mapTriangleToNodeset.put(newT[i+2], hashnodes_2[i]);
				}
				
				submesh2d.rmFace(f);
				mapTriangleToNodeset.remove(f);
				submesh2d.rmFace(adj_tri);
				mapTriangleToNodeset.remove(adj_tri);
			}
			else
				throw new RuntimeException("Duplicate nodes found... aborting"+" "+newT.length);
			
			assert (submesh2d.getNodes().size() == nNodes + 1);
			nNodes++;
			assert (submesh2d.getEdges().size() == nEdges + 3);
			nEdges += 3;
			assert (submesh2d.getFaces().size() == nFaces + 2);
			nFaces += 2;
			assert (submesh2d.getNodes().size() == nNodes);
			assert (submesh2d.getEdges().size() == nEdges);
			assert (submesh2d.getFaces().size() == nFaces);
		}
	}
	
}
