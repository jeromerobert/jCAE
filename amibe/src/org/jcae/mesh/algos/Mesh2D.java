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
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */

package org.jcae.mesh.algos;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;
import org.jcae.mesh.sd.*;
import org.jcae.mesh.util.*;
import org.jcae.opencascade.jni.*;
import org.apache.log4j.Logger;
/**
 * Mesh2D inherites from MeshMesh, and allows meshing process in 2D space.
 * @author Cyril BRANDY & Marie-Helene GARAT
 */
public class Mesh2D extends MeshOfCAD
{
	private static Logger logger=Logger.getLogger(Mesh2D.class);
	/** A list of wire edges. */
	protected ArrayList edgelist2D;
	/** one of the farest node form the barycenter. */
	private MeshNode2D maxNode;
	
	/**
	 * Constructor with geometric entity.
	 * @param shape : a TopoDS_Shape instance, the geometric shape of the MeshMesh
	 */
	public Mesh2D(TopoDS_Shape shape)
	{
		super(shape);
	}
	
	/**
	 * Method to construct the first triangle in order to mesh a face.
	 * This triangle includes all the points of the face frontier.
	 * @param surface : a TopoSD_Face instance, the face to mesh
	 * @return MeshFace : the first triangle of the meshing method
	 */
	public MeshFace getTriangleMax(TopoDS_Face surface)
	{
		HashSet myNodeList=new HashSet(nodelist);
		Triangle2D triangle;
		// list of pts constituying the face frontier
		if (nodelist.isEmpty())
		{
			logger.debug("No boundary points");
			return null;
		}
		// find the barycentre of this list of nodes
		double rayon = 0.0;
		MeshNode2D bcentre = null;
		Pair cercle_circonscrit;
		cercle_circonscrit = computeBarycentre();
		bcentre = (MeshNode2D)cercle_circonscrit.first;
		rayon = ((Double)cercle_circonscrit.second).doubleValue();
		// Enlarge rayon a little bit
		rayon *= 1.05;

		// Create the 3 points of the triangle (equilateral)
		double u0 = bcentre.getX();
		double v0 = bcentre.getY();
		MeshNode2D n1 = new MeshNode2D(u0, v0 + 2.0 * rayon);
		MeshNode2D n2 = new MeshNode2D(u0 - rayon * Math.sqrt(3.0), v0 - rayon); // u0-2*rayon*cos(30)
		MeshNode2D n3 = new MeshNode2D(u0 + rayon * Math.sqrt(3.0), v0 - rayon); // u0+2*rayon*cos(30)
		maxNode=n1;
		
		// inserts the nodes
		n1=(MeshNode2D)addNode(n1);
		n2=(MeshNode2D)addNode(n2);
		n3=(MeshNode2D)addNode(n3);
				
		triangle=addTriangle2D(n1, n2, n3);
		triangle.setNodes(myNodeList);
		Iterator it=triangle.getEdgesIterator();
		while(it.hasNext()) ((MeshEdge)it.next()).setFrozen(true);
		return triangle;
	}
	
	/**
	 * Retrieves the Triangle2D that have the biggest number of points into.
	 * @return Pair - a pair,  - the first element is the triangle
	 * 						   - the second element is a boolean set to false if the method fails in finding triangle.
	 */
	public Pair getTriangle()
	{
		// for all triangles of mesh2D
		Pair result = new Pair();
		int max_pts = 0, nb_pts = 0;
		Iterator it = getFacesIterator();
		while (it.hasNext())
		{
			Triangle2D t = (Triangle2D)it.next();
			nb_pts = t.getNbNodes();
			if (nb_pts > max_pts)
			{
				max_pts = nb_pts;
				result.first = t;
			}
		}
		if (max_pts == 0) result.second = new Boolean(false);
		else
			result.second = new Boolean(true);
		return result;
	}
		
	/**
	 * From the original triangle, creates 3 new triangles.
	 * @param pt : a MeshNode2D instance, the new node inserted
	 * @param triangle0 : a Triangle2D instance, the original triangle
	 * @return boolean : set to \c false if method failed
	 */
	public boolean updateMesh2D(MeshNode2D pt, Triangle2D triangle0)
	{
		logger.debug("Method updateMesh2D: begin");
		// the big original triangle: triangle0
		pt=(MeshNode2D)addNode(pt);
		Iterator it = triangle0.getNodesIterator();
		MeshNode2D P1 = (MeshNode2D)it.next();
		MeshNode2D P2 = (MeshNode2D)it.next();
		MeshNode2D P3 = (MeshNode2D)it.next();
		// Triangle pt,P1,P2
		Triangle2D triangle1 = addTriangle2D(pt, P1, P2);
		triangle1.fillTriangle2D(triangle0);

		// Triangle pt,P1,P3
		Triangle2D triangle2 = addTriangle2D(pt, P1, P3);
		triangle2.fillTriangle2D(triangle0);
		
		// Triangle pt,P3,P2
		Triangle2D triangle3=addTriangle2D(pt, P3,  P2);
		triangle3.fillTriangle2D(triangle0);
		
		if (! triangle0.getNodelist().isEmpty())
		{
			//  Not optimized, but we should seldom go here
			for (Iterator itn = triangle0.getNodelist().iterator(); itn.hasNext(); )
			{
				boolean found = false;
				MeshNode2D pt0 = (MeshNode2D) itn.next();
				for (Iterator itf = P1.getFacesIterator(); itf.hasNext(); )
				{
					Triangle2D t2 = (Triangle2D) itf.next();
					if (t2 == triangle0)
						continue;
					if (t2.ptInTriangle(pt0))
					{
						t2.addNodelist(pt);
						found = true;
						break;
					}
				}
				if (!found)
					throw new RuntimeException("Unable to locate point "+pt0);
			}
		}
		rmFace(triangle0);
		// check edges
		HashSet list = new HashSet();
		MeshEdge e = getEdgeDefinedByNodes(P1, P2);
		if (!(e.isWire()))
			list.add(e);
		e = getEdgeDefinedByNodes(P1, P3);
		if (!(e.isWire()))
			list.add(e);
		e = getEdgeDefinedByNodes(P2, P3);
		if (!(e.isWire()))
			list.add(e);
		checkAndSwap(list);

		logger.debug("Method updateMesh2D: end");
		return true;
	}
	
	/**
	 * From the original triangle, creates 2 new triangles . The point inserted is on an edge of the triangle.
	 * @param pt : a MeshNode2D instance, the new node inserted
	 * @param triangle0 : a Triangle2D instance, the original triangle
	 * @param it_P : a MeshNode2D instance, the apex of the edge that contains the inserted point
	 * @return boolean : set to \c false if method failed
	 */
	public boolean updateMesh2D(MeshNode2D pt, Triangle2D triangle0, MeshNode2D it_P, Triangle2D adj_triangle)
	{
		logger.debug("Method updateMesh2D bis: begin");
		pt=(MeshNode2D)addNode(pt);
		Iterator it = triangle0.getNodesIterator();
		MeshNode2D P1 = (MeshNode2D)it.next();
		MeshNode2D P2 = (MeshNode2D)it.next();
		MeshNode2D P3 = (MeshNode2D)it.next();
		
		Triangle2D triangle1, triangle2;
		if(it_P == P1)
		{
			triangle1=addTriangle2D(pt, P1, P2);
			triangle1.fillTriangle2D(triangle0);
			triangle2=addTriangle2D(pt, P3, P1);
			triangle2.fillTriangle2D(triangle0);
		}
		else if(it_P == P2)
		{
			triangle1=addTriangle2D(pt, P2, P3);
			triangle1.fillTriangle2D(triangle0);
			triangle2=addTriangle2D(pt, P1, P2);
			triangle2.fillTriangle2D(triangle0);
		}
		else // it_P == P3
		{
			assert (it_P==P3);
			triangle1=addTriangle2D(pt, P3, P1);
			triangle1.fillTriangle2D(triangle0);
			triangle2=addTriangle2D(pt, P2, P3);
			triangle2.fillTriangle2D(triangle0);
		}
		if (!triangle0.getNodelist().isEmpty()) {
			//   Transfer remaining nodes to adjacent triangle
			assert null != adj_triangle;
			adj_triangle.addNodelist(triangle0.getNodelist());
		}
		rmFace(triangle0);

		logger.debug("Method updateMesh2D bis: end");
		return true;
	}
	
	/**
	 * Refines the original triangle into 3 triangle, update the mesh2D
	 * @param triangle0 : a Triangle2D instance, the original triangle
	 * @return boolean : set to \c false if there is no more node to add
	 */
	public boolean insertPt(Triangle2D triangle0)
	{
		logger.debug("Method insertPt: begin");
		if (triangle0.getNodelist().isEmpty())
			return false;
		HashSet set0 = triangle0.getNodelist();
		Iterator it = set0.iterator();
		MeshNode2D pt = (MeshNode2D)it.next();
		logger.debug("pt: "+pt);
		set0.remove(pt);
		// refine the mesh with this new point
		// Retrive the 3 vertices constituying the original triangle
		it = triangle0.getNodesIterator();
		// first vertex
		MeshNode2D vertex1 = (MeshNode2D)it.next();
		// second vertex
		MeshNode2D vertex2 = (MeshNode2D)it.next();
		// third vertex
		MeshNode2D vertex3 = (MeshNode2D)it.next();
		
		assert (!(pt.equals(vertex1) || pt.equals(vertex2) || pt.equals(vertex3)));
		// Test if this point belongs to an edge of the original triangle
		Triangle2D adj_triangle;
		Pair res = ptOnTriangle(pt, triangle0);
		if (!((Boolean)res.second).booleanValue())
		{
			// Create the three new triangles and update the mesh_2D
			updateMesh2D(pt, triangle0);
			adj_triangle = null;
		}
		else
		{
			adj_triangle = (Triangle2D)(res.first);
			MeshNode2D point1 = null, point2 = null;
			//Create an insert_iterator for odd
			HashSet adj_vertex = new HashSet();
			MeshEdge edgetodelete = null;
			HashSet edgetocheck = new HashSet();
			// Identification de l'edge en commun
			if (pt.ptOnEdge(vertex1, vertex2))
			{
				// create 2 triangles: (pt, vertex1, vertex3) et (pt, vertex2, vertex3)
				updateMesh2D(pt, triangle0, vertex3, adj_triangle);
				edgetodelete = getEdgeDefinedByNodes(vertex1, vertex2);
				edgetocheck.add(getEdgeDefinedByNodes(vertex1, vertex3));
				edgetocheck.add(getEdgeDefinedByNodes(vertex2, vertex3));

				point1 = vertex1;
				point2 = vertex2;
			}
			else if (pt.ptOnEdge(vertex2, vertex3))
			{
				// Create the 2 Triangle2D : (vertex2, pt, vertex1) and ( pt, vertex3, vertex1)
				updateMesh2D(pt, triangle0, vertex1, adj_triangle);
				edgetodelete = getEdgeDefinedByNodes(vertex3, vertex2);
				edgetocheck.add(getEdgeDefinedByNodes(vertex1, vertex2));
				edgetocheck.add(getEdgeDefinedByNodes(vertex1, vertex3));
				point1 = vertex2;
				point2 = vertex3;
			}
			else
			{
				assert (pt.ptOnEdge(vertex3, vertex1));
				// Create the 2 Triangle2D : (vertex3, pt, vertex2) and  (pt,vertex1,vertex2)
				updateMesh2D(pt, triangle0, vertex2, adj_triangle);
				edgetodelete = getEdgeDefinedByNodes(vertex1, vertex3);
				edgetocheck.add(getEdgeDefinedByNodes(vertex2, vertex1));
				edgetocheck.add(getEdgeDefinedByNodes(vertex2, vertex3));
				point1 = vertex1;
				point2 = vertex3;
			}
			// retrieve the 3rd vertex of the adjacent triangle
			MeshNode2D adjv=null;
			Iterator itAdj=adj_triangle.getNodesIterator();
			boolean vertexFound=false; //Should this test be removed ?
			while(itAdj.hasNext())
			{
				adjv=(MeshNode2D)itAdj.next();
				if((adjv!=vertex1)&&(adjv!=vertex2)&&(adjv!=vertex3))
				{
					vertexFound=true;
					break;
				}
			}
			if(!vertexFound)
				throw new RuntimeException("adj_triangle and triangle0 are the same !");
						
			updateMesh2D(pt, adj_triangle, adjv, null);
			if (edgetodelete != null)
				rmEdge(edgetodelete);
			edgetocheck.add(getEdgeDefinedByNodes(point1, adjv));
			edgetocheck.add(getEdgeDefinedByNodes(point2, adjv));
			// check edge
			checkAndSwap(edgetocheck);
		}
		logger.debug("Method insertPt: end");
		return true;
	}
	
	/**
	 * Test if a point is on an edge of a triangle
	 * @param pt : a MeshNode2D instance, the point to test
	 * @param triangle : a Triangle2D instance, the triangle
	 * @return Pair - a pair. First component is the adjacent triangle. Set to \c null if not
	 * exists second component is a boolean, set to \c true if the point is on an edge, \c false if not.
	 */
	public Pair ptOnTriangle(MeshNode2D pt, Triangle2D triangle)
	{
		logger.debug("Method ptOnTriangle: begin");
		Triangle2D e_otriangle = null;
		Pair result = new Pair();
		// Retrieve original triangle (itriangle)
		Iterator it = triangle.getNodesIterator();
		MeshNode2D P1 = (MeshNode2D)it.next();
		MeshNode2D P2 = (MeshNode2D)it.next();
		MeshNode2D P3 = (MeshNode2D)it.next();
		if (pt.ptOnEdge(P1, P2))
			e_otriangle = getTriangleVoisin(triangle, getEdgeDefinedByNodes(P1, P2));
		else if (pt.ptOnEdge(P2, P3))
			e_otriangle = getTriangleVoisin(triangle, getEdgeDefinedByNodes(P2, P3));
		else if (pt.ptOnEdge(P3, P1))
			e_otriangle = getTriangleVoisin(triangle, getEdgeDefinedByNodes(P3, P1));
		else
		{
			result.second = new Boolean(false);
			logger.debug("Method ptOnTriangle: end");
			return result;
		}
		result.first = e_otriangle;
		if (e_otriangle == null)
			result.second = new Boolean(false);
		else
			result.second = new Boolean(true);
		
		logger.debug("Method ptOnTriangle: end");
		return result;
	}
	
	/**
	 * Retrieves one edge that cut the segment defined by is 2 nodes.
	 * @param n1 : a MeshNode2D instance, one of the bounding point of the segment
	 * @param n2 : a MeshNode2D instance, the other bounding point of the segment.
	 * @return Pair : a pair constitued by the first edge found
	 * and the triangle made up with the node n1 and the edge found.
	 */
	private Pair findEdgeCuttingSegment(MeshNode2D n1, MeshNode2D n2)
	{
		MeshEdge E;
		Pair pair_toreturn = new Pair();
		
		// For all triangles linked to n1
		Iterator itt = n1.getFacesIterator();
		while (itt.hasNext())
		{
			MeshFace f = (MeshFace)itt.next();
			//HashSet listeEdges = f.getEdges();
			// It must be triangle:
			if(f.numberOfEdges()!=3)
			{
				throw new RuntimeException("Find an element with "+
					f.numberOfEdges()+" edges while triangle expected");
			}
			// Find the edge	E : N1 - N2
			Iterator it_edge = f.getEdgesIterator();
			MeshEdge e1 = (MeshEdge)it_edge.next();
			MeshEdge e2 = (MeshEdge)it_edge.next();
			MeshEdge e3 = (MeshEdge)it_edge.next();
			MeshNode2D e1n1 = (MeshNode2D)e1.getNodes1();
			MeshNode2D e1n2 = (MeshNode2D)e1.getNodes2();
			MeshNode2D e2n1 = (MeshNode2D)e2.getNodes1();
			MeshNode2D e2n2 = (MeshNode2D)e2.getNodes2();
			if ((n1.equals(e1n1)) || (n1.equals(e1n2)))
			{
				if ((n1.equals(e2n1)) || (n1.equals(e2n2)))
					E = e3;
				else
					E = e2;
			}
			else
				E = e1;
			MeshNode2D En1 = (MeshNode2D)E.getNodes1();
			MeshNode2D En2 = (MeshNode2D)E.getNodes2();
			if (En1.orient2D(n1, n2) * En2.orient2D(n1, n2) <= 0.0 &&
			    n1.orient2D(En1, En2) * n2.orient2D(En1, En2) <= 0.0)
			{
				pair_toreturn.first = E;
				// get the triangle
				pair_toreturn.second = f;
				return pair_toreturn;
			}
		}
		throw new RuntimeException("Method findEdgeCuttingSegment failed");
	}
	
	/**
	 * Constructs a set of edges cutting the segment n1-n2
	 * @param n1 : a MeshNode2D instance, the end node n1
	 * @param n2 : a MeshNode2D instance, the end-node n2
	 * @return HashSet : a list of edges cutting the segment n1-n2
	 */
	public Collection findEdgesCuttingSegment(MeshNode2D n1, MeshNode2D n2)
	{
		HashSet toReturn = new HashSet();
		HashSet trianglelist = new HashSet();
		logger.debug("Method findEdgesCuttingSegment");
		Pair ET = findEdgeCuttingSegment(n1, n2);
		MeshEdge cur_edge = (MeshEdge)ET.first;
		Triangle2D cur_tri = (Triangle2D)ET.second;
		assert (null != cur_edge);
		assert (null != cur_tri);
		while (true)
		{
			toReturn.add(cur_edge);
			// Get the adjacent triangle of the current edge
			Triangle2D Tvoisin = getTriangleVoisin(cur_tri, cur_edge);
			// Find the apex
			MeshNode2D ap = (MeshNode2D)Tvoisin.apex(cur_edge);
			if (ap == null)
				throw new RuntimeException ("Unable to find apex");
			
			// Is it the last edge?
			if (ap.equals(n2))
				break;
			MeshNode2D p1 = (MeshNode2D)cur_edge.getNodes1();
			MeshNode2D p2 = (MeshNode2D)cur_edge.getNodes2();
			// Find the cutting edge
			if (p1.orient2D(n1, n2) * ap.orient2D(n1, n2) <= 0.0)
				cur_edge = getEdgeDefinedByNodes(p1, ap);
			else
				cur_edge = getEdgeDefinedByNodes(p2, ap);
			// the current triangle
			cur_tri = (Triangle2D)getFaceDefinedByNodes(new MeshNode[]{p1,p2,ap});
		}
		return toReturn;
	}
	
	/**
	 * Retrieves the adjacent triangle passing an edge and a triangle
	 * @param oritriangle : a Triangle2D instance, the input triangle
	 * @param comedge : a MeshEdge instance, an edge of the input triangle
	 * @return Triangle2D : the adjacent triangle. Set to \c null if the adjacent triangle does not exist
	 * or if the method failed.
	 */
	public Triangle2D getTriangleVoisin(Triangle2D oritriangle, MeshEdge comedge)
	{
		Triangle2D T = null;
		HashSet trianglelist = comedge.getFaces();
		assert (2 == trianglelist.size());
		Iterator it = trianglelist.iterator();
		T = (Triangle2D)it.next();
		if (T != oritriangle)
			return T;
		else
			return (Triangle2D)it.next();
	}
	
	/**
	 * Adds an edge to the edgelist2D set, a set made up with the edges underlying a wire.
	 * @param edge : a MeshEdge instance, the edge to add
	 */
	public void addEdge2D(MeshEdge edge)
	{
		edgelist2D.add(edge);
	}
	
	/**
	 * Adds an edge to the list of edges2D defining a wire.
	 * @param n1 : a MeshNode instance, one of both node bounding the edge to add.
	 * @param n2 : a MeshNode instance, the other node bounding the edge to add.
	 */
	public void addEdge2D(MeshNode n1, MeshNode n2)
	{
		edgelist2D.add(new MeshEdge(n1, n2));
	}
	
	/**
	 * Get the list of edges constituying the wire.
	 * @return ArrayList : the edge list.
	 */
	public ArrayList getEdgeList2D()
	{
		return edgelist2D;
	}
	
	/**
	 * Marks all faces of the mesh as interior or exterior. The method starts with an
	 * extreme face exterior) and run over the mesh calling internaltag method.
	 */
	public void tague()
	{
		// retrieve the Triangle2D associated to this node
		HashSet facelist = maxNode.getFaces();
		Iterator it = facelist.iterator();
		while (it.hasNext())
		{
			Triangle2D t = (Triangle2D)it.next();
			internaltag(t, 0);
		}
	}
	
	/**
	 * Marks the faces status of mesh. Faces flag is set to 1 if the face is exterior and 0 if not. A Face not marked
	 * has a flag equals to -1.
	 * @param t : a Triangle2D instance, the triangle to mark
	 * @param tag : an integer value, the value of the flag: -1=none, 0=interior, 1=exterior
	 */
	private void internaltag(Triangle2D t, int tag)
	{
		t.setFlag(tag);
		Iterator it = t.getEdgesIterator();
		while (it.hasNext())
		{
			MeshEdge e = (MeshEdge)it.next();
			if (e.getFaces().size() != 2)
				continue;
			Triangle2D f = getTriangleVoisin(t, e);
			if (f == null || f.getFlag() != -1)
				continue;
			if (e.isWire())
				internaltag(f, 1-tag);
			else
				internaltag(f, tag);
		}
	}
	
	/**
	 * Deletes all faces exterior to the geometric surface.
	 */
	public void finalize2D()
	{
		Triangle2D[] cpfacelist=new Triangle2D[numberOfFaces()];
		Iterator it=getFacesIterator();
		
		for(int i=0; i<cpfacelist.length; i++)
			cpfacelist[i]=(Triangle2D)it.next();
		
		for(int i=0; i<cpfacelist.length; i++)
		{
			Triangle2D t = cpfacelist[i];
			if (t.getFlag() == 0)
				rmFace(t);
		}
	}
	
	
	/**
	 * Computes the barycentre of the points constituying the frontier of the face. The barycentre is the center of the
	 * circle containing all points.
	 * @return Pair : - a MeshNode2D, the center of the cercle
	 * 				  - a double, the radius of the cercle.
	 */
	private Pair computeBarycentre()
	{
		Pair cercle_circonscrit = new Pair();
		double dist_max = 0.0;
		int nbnodes = nodelist.size();
		double u = 0.0, v = 0.0;
		if (nodelist.isEmpty())
		{
			cercle_circonscrit.first = null;
			cercle_circonscrit.second = new Double(0.0);
			return cercle_circonscrit;
		}
		// Compute barycentre
		MeshNode2D n = (MeshNode2D)(nodelist.iterator().next());
		Iterator it = nodelist.iterator();
		while (it.hasNext())
		{
			MeshNode2D node = (MeshNode2D)it.next();
			u += node.getX();
			v += node.getY();
		}
		u /= nbnodes;
		v /= nbnodes;
		MeshNode2D centre = new MeshNode2D(u, v);
		// compute radius: dist_max
		it = nodelist.iterator();
		while (it.hasNext())
		{
			double dist = 0.0;
			dist = centre.distance((MeshNode2D)it.next());
			dist_max = ((dist_max > dist) ? dist_max : dist);
		}
		cercle_circonscrit.first = centre;
		cercle_circonscrit.second = new Double(dist_max);
		return cercle_circonscrit;
	}
	
	
	/**
	 * Refines the current mesh in 2D space.
	 */
	public void refine2D()
	{
		HashSet edgesadded = new HashSet();
		
		//Create a copy of the list of MeshFace.
		MeshFace[] facesArray=new MeshFace[numberOfFaces()];
		Iterator facesIterator=getFacesIterator();
		for(int i=0;i<facesArray.length;i++)
			facesArray[i]=(MeshFace)facesIterator.next();
		
		for(int i=0;i<facesArray.length;i++)
		{
			MeshFace f = facesArray[i];
			if(f.numberOfEdges()!=3)
			{
				throw new RuntimeException ("Element should be a triangle but it has "+
					f.numberOfEdges()+" edges");
			}
			MeshEdge e1 = null;
			MeshEdge e2 = null;
			MeshEdge e3 = null;
			//count mid point
			int count = 0;
			Iterator ite = f.getEdgesIterator();
			while (ite.hasNext())
			{
				MeshEdge e = (MeshEdge) ite.next();
				if (e.getMidNode() != null)
				{
					count++;
					if (e1 == null)
						e1 = e;
					else if (e2 == null)
						e2 = e;
					else
						e3 = e;
				} else
				{
					if (e3 == null)
						e3 = e;
					else if (e2 == null)
						e2 = e;
					else
						e1 = e;
				}
			}
			//apply pattern
			switch (count)
			{
				case 1 :
				{
					//e1 is the edge to cut
					MeshNode2D pt1 = (MeshNode2D)e1.getNodes1();
					MeshNode2D pt2 = (MeshNode2D)e1.getNodes2();
					MeshNode2D mid = (MeshNode2D)e1.getMidNode();
					
					MeshNode2D A;
					if ((MeshNode2D)e2.getNodes1() == pt1 || (MeshNode2D)e2.getNodes1() == pt2)
					{
						A = (MeshNode2D)e2.getNodes2();
					}
					else
					{
						A = (MeshNode2D)e2.getNodes1();
					}
					//Construct 3 new edges;
					MeshEdge ne1 = new MeshEdge(pt1, mid);
					MeshEdge ne2 = new MeshEdge(pt2, mid);
					if (e1.isWire())
					{
						ne1.setWire(true);
						ne2.setWire(true);
						ne1.setFrozen(true);
						ne2.setFrozen(true);
						
					}
					MeshEdge ne3 = new MeshEdge(mid, A);
					e2 = getEdgeDefinedByNodes(pt1, A);
					e3 = getEdgeDefinedByNodes(pt2, A);
					ne1 = addEdge(ne1);
					ne2 = addEdge(ne2);
					ne3 = addEdge(ne3);
					e2 = addEdge(e2);
					e3 = addEdge(e3);
					//Construct two new triangles
					MeshFace f1 = addTriangle(ne1,ne3,e2);
					MeshFace f2 = addTriangle(ne2,ne3,e3);
					rmFace(f);
					// add the new edges to the set
					if (!ne1.isWire()) edgesadded.add(ne1);
					if (!ne2.isWire()) edgesadded.add(ne2);
					if (!ne3.isWire()) edgesadded.add(ne3);
					if (!e2.isWire()) edgesadded.add(e2);
					if (!e3.isWire()) edgesadded.add(e3);
					break;
				}
				case 2 :
				{
					//e1 & e2 are the edges to cut
					MeshNode2D pt1 = (MeshNode2D)e3.getNodes1();
					MeshNode2D pt2 = (MeshNode2D)e3.getNodes2();
					MeshNode2D mid1 = (MeshNode2D)e1.getMidNode();
					MeshNode2D mid2 = (MeshNode2D)e2.getMidNode();
					
					MeshNode2D A = null;
					if ((MeshNode2D)e1.getNodes1() == (MeshNode2D)e2.getNodes1())
					{
						A = (MeshNode2D)e1.getNodes1();
						pt1 = (MeshNode2D)e1.getNodes2();
						pt2 = (MeshNode2D)e2.getNodes2();
					}
					if ((MeshNode2D)e1.getNodes1() == (MeshNode2D)e2.getNodes2())
					{
						A = (MeshNode2D)e1.getNodes1();
						pt1 = (MeshNode2D)e1.getNodes2();
						pt2 = (MeshNode2D)e2.getNodes1();
					}
					if ((MeshNode2D)e1.getNodes2() == (MeshNode2D)e2.getNodes1())
					{
						A = (MeshNode2D)e1.getNodes2();
						pt1 = (MeshNode2D)e1.getNodes1();
						pt2 = (MeshNode2D)e2.getNodes2();
					}
					if ((MeshNode2D)e1.getNodes2() == (MeshNode2D)e2.getNodes2())
					{
						A = (MeshNode2D)e1.getNodes2();
						pt1 = (MeshNode2D)e1.getNodes1();
						pt2 = (MeshNode2D)e2.getNodes1();
					}
					//Construct 6 new edges;
					MeshEdge ne1 = new MeshEdge(A, mid1);
					MeshEdge ne2 = new MeshEdge(mid1, pt1);
					MeshEdge ne3 = new MeshEdge(A, mid2);
					MeshEdge ne4 = new MeshEdge(mid2, pt2);
					MeshEdge ne5 = new MeshEdge(mid1, mid2);
					MeshEdge ne6 = new MeshEdge(mid2, pt1);
					if (e1.isWire())
					{
						ne1.setWire(true);
						ne2.setWire(true);
						ne1.setFrozen(true);
						ne2.setFrozen(true);
						
					}
					if (e2.isWire())
					{
						ne3.setWire(true);
						ne4.setWire(true);
						ne3.setFrozen(true);
						ne4.setFrozen(true);
						
					}
					ne1 = addEdge(ne1);
					ne2 = addEdge(ne2);
					ne3 = addEdge(ne3);
					ne4 = addEdge(ne4);
					ne5 = addEdge(ne5);
					ne6 = addEdge(ne6);
					e3 = addEdge(e3);
					//Construct the three new triangles
					MeshFace f1 = addTriangle(ne1,ne3,ne5);
					MeshFace f2 = addTriangle(ne2,ne5,ne6);
					MeshFace f3 = addTriangle(ne6,ne4,e3);
					rmFace(f);
					// add the new edges to the set
					if (!ne1.isWire()) edgesadded.add(ne1);
					if (!ne2.isWire()) edgesadded.add(ne2);
					if (!ne3.isWire()) edgesadded.add(ne3);
					if (!ne4.isWire()) edgesadded.add(ne4);
					if (!ne5.isWire()) edgesadded.add(ne5);
					if (!ne6.isWire()) edgesadded.add(ne6);
					if (!e3.isWire()) edgesadded.add(e3);
					break;
				}
				case 3 :
				{
					//e1 & e2 & e3 are the edges to cut
					MeshNode2D pt1 = null;
					MeshNode2D pt2 = null;
					MeshNode2D pt3 = null;
					MeshNode2D mid1 = (MeshNode2D)e1.getMidNode();
					MeshNode2D mid2 = (MeshNode2D)e2.getMidNode();
					MeshNode2D mid3 = (MeshNode2D)e3.getMidNode();
					
					if ((MeshNode2D)e1.getNodes1()==(MeshNode2D)e2.getNodes1())
					{
						pt2 = (MeshNode2D)e1.getNodes1();
						pt3 = (MeshNode2D)e2.getNodes2();
						pt1 = (MeshNode2D)e1.getNodes2();
					}
					if ((MeshNode2D)e1.getNodes1()==(MeshNode2D)e2.getNodes2())
					{
						pt2 = (MeshNode2D)e1.getNodes1();
						pt3 = (MeshNode2D)e2.getNodes1();
						pt1 = (MeshNode2D)e1.getNodes2();
					}
					if ((MeshNode2D)e1.getNodes2()==(MeshNode2D)e2.getNodes1())
					{
						pt2 = (MeshNode2D)e1.getNodes2();
						pt3 = (MeshNode2D)e2.getNodes2();
						pt1 = (MeshNode2D)e1.getNodes1();
					}
					if ((MeshNode2D)e1.getNodes2()== (MeshNode2D)e2.getNodes2())
					{
						pt2 = (MeshNode2D)e1.getNodes2();
						pt3 = (MeshNode2D)e2.getNodes1();
						pt1 = (MeshNode2D)e1.getNodes1();
					}
					
					//Construct 9 new edges;
					MeshEdge ne1 = new MeshEdge(pt1, mid1);
					MeshEdge ne2 = new MeshEdge(mid1, pt2);
					MeshEdge ne3 = new MeshEdge(pt2, mid2);
					MeshEdge ne4 = new MeshEdge(mid2, pt3);
					MeshEdge ne5 = new MeshEdge(pt3, mid3);
					MeshEdge ne6 = new MeshEdge(mid3, pt1);
					MeshEdge ne7 = new MeshEdge(mid1, mid3);
					MeshEdge ne8 = new MeshEdge(mid1, mid2);
					MeshEdge ne9 = new MeshEdge(mid2, mid3);
					if (e1.isWire())
					{
						ne1.setWire(true);
						ne2.setWire(true);
						ne1.setFrozen(true);
						ne2.setFrozen(true);
					}
					if (e2.isWire())
					{
						ne3.setWire(true);
						ne4.setWire(true);
						ne3.setFrozen(true);
						ne4.setFrozen(true);
					}
					if (e3.isWire())
					{
						ne5.setWire(true);
						ne6.setWire(true);
						ne5.setFrozen(true);
						ne6.setFrozen(true);
					}
					ne1 = addEdge(ne1);
					ne2 = addEdge(ne2);
					ne3 = addEdge(ne3);
					ne4 = addEdge(ne4);
					ne5 = addEdge(ne5);
					ne6 = addEdge(ne6);
					ne7 = addEdge(ne7);
					ne8 = addEdge(ne8);
					ne9 = addEdge(ne9);
					//Construct 4 new triangles
					MeshFace f1 = addTriangle(ne1,ne6,ne7);
					MeshFace f2 = addTriangle(ne2,ne3,ne8);
					MeshFace f3 = addTriangle(ne9,ne4,ne5);
					MeshFace f4 = addTriangle(ne7,ne8,ne9);
					rmFace(f);
					
					// add the new edges to the set
					if (!ne1.isWire()) edgesadded.add(ne1);
					if (!ne2.isWire()) edgesadded.add(ne2);
					if (!ne3.isWire()) edgesadded.add(ne3);
					if (!ne4.isWire()) edgesadded.add(ne4);
					if (!ne5.isWire()) edgesadded.add(ne5);
					if (!ne6.isWire()) edgesadded.add(ne6);
					// There is no reason to swap ne[7-9]
					break;
				}
			}
		}
		
		Iterator it = edgesadded.iterator();
		while (it.hasNext())
		{
			check2D((MeshEdge) it.next());
		}
	}
	
	public boolean collapse2D(MeshNode2D na, MeshNode2D nb)
	{
		// Topological condition: both nodes, defining the edge are not classified on a vertex
		if ( (na.getPosition().getType()==PST_Position.VERTEX) &&
		(nb.getPosition().getType()==PST_Position.VERTEX) ) return false;
		// test if edge defined by both node exists
		MeshEdge edge = getEdgeDefinedByNodes(na,nb);
		if (edge==null) return false;
		
		// retrieve faces linked to the edge
		HashSet faces = edge.getFaces();
		if (faces.size()!=2)
		{
			logger.warn("edge non connectee a deux faces");
			return false;
		}
		
		// Faces destruction
		Iterator itf = faces.iterator();
		MeshFace face1 = (MeshFace)itf.next();
		MeshFace face2 = (MeshFace)itf.next();
		
		rmFace(face1);
		rmFace(face2);
		
		// Save remained faces connected to na and nb in a HashSet
		HashSet facesConnected = new HashSet();
		facesConnected.addAll(na.getElements());
		facesConnected.addAll(nb.getElements());
		// Retrieve the edges non connected to na or nb
		HashSet edgesNonConnected = new HashSet();
		Iterator itfc = facesConnected.iterator();
		while(itfc.hasNext())
		{
			MeshElement e = (MeshElement)itfc.next();
			if (e.getType()==MeshElement.FACE)
			{
				Iterator ittemp = ((MeshFace)e).getEdgesIterator();
				while (ittemp.hasNext())
				{
					MeshEdge ed = (MeshEdge)ittemp.next();
					if (!(ed.getNodes1().equals(na)) && !(ed.getNodes1().equals(nb)) && !(ed.getNodes2().equals(na)) && !(ed.getNodes2().equals(nb)))
						edgesNonConnected.add(ed);
				}
			}
		}
		
		// Destruction des faces
		itfc = facesConnected.iterator();
		while(itfc.hasNext())
		{
			rmFace((MeshFace)itfc.next());
		}
		
		// Calcul du point milieu
		MeshNode np = new MeshNode();
		
		// if one of both nodes is a vertex position, it must be kept
		if (na.getPosition().getType()==PST_Position.VERTEX)
		{
			np.setX(na.getX()); np.setY(na.getY());
			np.setPosition(na.pos);
		}
		else if (nb.getPosition().getType()==PST_Position.VERTEX)
		{
			np.setX(nb.getX()); np.setY(nb.getY());
			np.setPosition(nb.pos);
		}
		// Other case
		else
		{
			//nc = createMidPt(na, nb);
			np = na.middle(nb);
			np.pos=new PST_SurfacePosition((TopoDS_Face)getGeometry(),np.getX(),np.getY());
		}
		np = addNode(np);
		
		// Construction des nouvelles faces
		Iterator ite = edgesNonConnected.iterator();
		while (ite.hasNext())
		{
			MeshEdge e = (MeshEdge)ite.next();
			addNode(e.getNodes1());
			addNode(e.getNodes2());
			addTriangle(e.getNodes1(), e.getNodes2(), np);
		}
		return true;
	}
	
	public HashSet getEdgesInfTo(double param)
	{
		HashSet toreturn = new HashSet();
		
		Iterator it = edgelist.iterator();
		while (it.hasNext())
		{
			MeshEdge e = (MeshEdge) it.next();
			if (e.length() < param)
				toreturn.add(e);
		}
		return toreturn;
	}
	
	public HashSet getEdgesSupTo(double param)
	{
		HashSet toreturn = new HashSet();
		
		Iterator it = edgelist.iterator();
		while (it.hasNext())
		{
			MeshEdge e = (MeshEdge) it.next();
			if (e.length() > param)
				toreturn.add(e);
		}
		return toreturn;
	}
	
	/**
	 * Checks and swaps an edge if possible.
	 * @param e : a MeshEdge instance, the edge to swap.
	 * @return boolean : set to \c true if swapping process done without any problem.
	 */
	public boolean swapEdge(MeshEdge e)
	{
		// find the 2 triangles with edge E in common
		HashSet trianglelist = e.getFaces();
		assert (2 == trianglelist.size());
		
		// Swapping condition: the edge is not classified on a line
		if (e.isFrozen()||e.isWire())
			throw new RuntimeException("Edge cannot be swapped");
		
		Iterator itt = trianglelist.iterator();
		MeshFace T1 = (MeshFace) itt.next();
		MeshFace T2 = (MeshFace) itt.next();
		//Find the apex:
		MeshNode2D a1 = (MeshNode2D)T1.apex(e);
		assert (null != a1);
		
		MeshNode2D a2 = (MeshNode2D)T2.apex(e);
		assert (null != a2);
		
		// Do not swap if (pt, a1, n1, a2) is not a convex hull
		MeshNode2D pt = (MeshNode2D)e.getNodes1();
		MeshNode2D n1 = (MeshNode2D)e.getNodes2();
		if (a1.orient2D(a2, pt) * a1.orient2D(a2, n1) >= 0.0)
			return false;
		if (a2.orient2D(a1, pt) * a2.orient2D(a1, n1) >= 0.0)
			return false;
		
		int before = numberOfEdges();
		rmFace(T1);
		rmFace(T2);
		rmEdge(e);
		assert (T1 instanceof Triangle2D);
		assert (T2 instanceof Triangle2D);
		addTriangle2D(a1, pt, a2);
		addTriangle2D(a1, n1, a2);
		assert (numberOfEdges() == before) : ""+before+" != "+numberOfEdges();
		return true;
	}
	
	/**
	 * Checks an edge. Non iterative method.
	 * @param e : a MeshEdge instance, the edge to check
	 * @see org.jcae.mesh.algos.MiscAlgos#check
	 */
	public boolean check2D(MeshEdge e)
	{
		if (e.isWire() || e.isFrozen())
			return false;
		MeshFace triangle = null, adj_tri = null;
		HashSet trianglelist = e.getFaces();
		assert (2 == trianglelist.size());
		Iterator it = trianglelist.iterator();
		triangle = (MeshFace) it.next();
		adj_tri = (MeshFace) it.next();
		MeshNode2D pt = (MeshNode2D)triangle.apex(e);
		boolean swap = true;
		// Compute quality of this new triangle
		double Qtriangle = triangle.qualite();
		// find the adjacent triangle  and compute its quality
		double Qadj_tri = adj_tri.qualite();
		// the quality of these 2 triangles is :
		double Q = Math.min(Qtriangle, Qadj_tri);
		// suppose we swap:
		MeshNode2D p = (MeshNode2D)adj_tri.apex(e);
		MeshNode2D e_p1 = (MeshNode2D)e.getNodes1();
		MeshNode2D e_p2 = (MeshNode2D)e.getNodes2();
		double qtriangle = MeshFace.qualite(p, pt, e_p1);
		double qadj_tri = MeshFace.qualite(p, pt, e_p2);
		// Compute the new quality
		double q = Math.min(qtriangle, qadj_tri);
		// Take the best quality
		if (q > Q)
		{
			swap = swapEdge(e);
		}
		return swap;
	}
	
	/**
	 * Tests if the edge can be swapped and swaps it if possible while triangle becomes better. Iterative method .
	 * @param e : a MeshEdge instance, the edge to check and swap.
	 */
	private void checkAndSwap(HashSet edges)
	{
		HashSet seenEdges = new HashSet();
		while (!edges.isEmpty())
		{
			MeshEdge e = (MeshEdge) edges.iterator().next();
			edges.remove(e);
			seenEdges.add(e);
			if (e.isWire() || e.isFrozen())
				continue;
			HashSet trianglelist = e.getFaces();
			assert (2 == trianglelist.size());
			Iterator it = trianglelist.iterator();
			Triangle2D triangle = (Triangle2D) it.next();
			Triangle2D adj_tri = (Triangle2D) it.next();
			MeshNode2D pt = (MeshNode2D)triangle.apex(e);
			assert (null != pt);
			
			// Compute quality of this new triangle
			double Qtriangle = triangle.qualite();
			// find the adjacent triangle  and compute its quality
			double Qadj_tri = adj_tri.qualite();
			// the quality of these 2 triangles is :
			double Q = Math.min(Qtriangle, Qadj_tri);
			// suppose we swap:
			MeshNode2D p = (MeshNode2D)adj_tri.apex(e);
			MeshNode2D e_p1 = (MeshNode2D)e.getNodes1();
			MeshNode2D e_p2 = (MeshNode2D)e.getNodes2();
			double qtriangle = Triangle2D.qualite(p, pt, e_p1);
			double qadj_tri = Triangle2D.qualite(p, pt, e_p2);
			// Compute the new quality
			double q = Math.min(qtriangle, qadj_tri);
			// Do not swap if lower quality
			if (q <= Q)
				continue;
			
			HashSet list = new HashSet();
			// union of the 2 lists of nodes
			list.addAll(triangle.getNodelist());
			list.addAll(adj_tri.getNodelist());
			if (!swapEdge(e))
				continue;
			// retrieve new triangles
			MeshFace f=getFaceDefinedByNodes(new MeshNode[]{p,pt,e_p1});
			triangle = (Triangle2D)f;
			adj_tri = (Triangle2D) getFaceDefinedByNodes(new MeshNode[]{p,pt,e_p2});
			triangle.updateNodes(list);
			HashSet diff = new HashSet(list);
			diff.removeAll(triangle.getNodelist());
			adj_tri.updateNodes(diff);
			
			// New edges
			MeshEdge newedge = getEdgeDefinedByNodes(e_p1, p);
			if (newedge != null && !newedge.isFrozen() && !seenEdges.contains(newedge))
				edges.add(newedge);
			newedge = getEdgeDefinedByNodes(e_p1, pt);
			if (newedge != null && !newedge.isFrozen() && !seenEdges.contains(newedge))
				edges.add(newedge);
			newedge = getEdgeDefinedByNodes(e_p2, pt);
			if (newedge != null && !newedge.isFrozen() && !seenEdges.contains(newedge))
				edges.add(newedge);
			newedge = getEdgeDefinedByNodes(e_p2, p);
			if (newedge != null && !newedge.isFrozen() && !seenEdges.contains(newedge))
				edges.add(newedge);
		}
	}
	
	/** Adds a mesh to the main MeshMesh entity.
	 * Different steps:
	 * -# retrieves all faces
	 * -# retrieves all edges for each face
	 * -# for each edge, retrieves all the nodes, converts them in 3D coordinates and updates the element links \n
	 * In order to retrieve the right vertices of the edge, the nodeMaj array contains the node positions. If a given node has a
	 * corresponding nodeMaje array that contains two MeshNode entities, it means that this vertex belong to a circular edge connected with a
	 * 'normal' edge or not. thus we need to retrieve the right vertex position. Two cases can occur:
	 * - both node positions are line positions, its means the node is on an circular edge connected to another circular edge, so
	 *  we need to retrieve its bounding vertex.
	 * \code
	 * if ( (((PST_Position)nt1.pos)._type==((PST_Position)nt1bis.pos)._type) && (  (((PST_Position)nt1.pos)._type == PST_Position.EDGE)) ) {
	 * 	// Retrieve the Vertices of the edge
	 *	E1 = (TopoDS_Edge)((PST_LinePosition)nt1.pos).getShape();
	 *	TopoDS_Vertex[] vertices = new TopoDS_Vertex[2];
	 *	TopoDS_Vertex V1, V2;
	 *	vertices = TopExp.vertices(E1);
	 *	V1 = vertices[0];
	 *	V2 = vertices[1];
	 *	MeshMesh nm1 = getMeshFromMapOfSubMesh(V1);
	 *	nt1 = (MeshNode)(nm1.getNodelist()).iterator().next();
	 * }
	 * \endcode
	 * - one node position is a vertex position, so the node is on a circular edge connected to a non-circular edge. In this case,
	 * it is the vertex position to take into account.
	 * \code
	 * nt1 : first MeshNode element of nodeMaj array
	 * nt1bis : second MeshNode element of nodeMaj array
	 * if (((PST_Position)nt1.pos)._type!=((PST_Position)nt1bis.pos)._type) {
	 *	if (((PST_Position)nt1.pos)._type == PST_Position.VERTEX) {
	 *		nt1=(MeshNode)nodesOfSubMesh.add(nt1);
	 *	}
	 *	else if (((PST_Position)nt1bis.pos)._type == PST_Position.VERTEX) {
	 *		nt1=(MeshNode)nodesOfSubMesh.add(nt1bis);
	 *	}
	 * \endcode
	 * -# once all nodes added to the main mesh, adds edges then faces.
	 * @param father  the main MeshOfCAD entity.
	 */
	public void addMesh(MeshOfCAD father)
	{
		Iterator it_face = getFacesIterator();
		TopoDS_Face theface=(TopoDS_Face)getGeometry();
		MeshMesh meshFace=(MeshMesh)father.getMeshFromMapOfSubMesh(theface);
		
		// build nodesOfSubMesh
		HashSet nodesOfSubMesh=new HashSet();
		Iterator itnodesOfSubMesh=father.getNodesIterator();
		while(itnodesOfSubMesh.hasNext())
		{
			MeshNode n=(MeshNode)itnodesOfSubMesh.next();
			nodesOfSubMesh.addFast(n);
		}
		
		// For each face
		while (it_face.hasNext())
		{
			HashSet tempedge=new HashSet();
			MeshFace face = (MeshFace) it_face.next();
			Iterator it_edge = face.getEdgesIterator();
			// for each edge
			while (it_edge.hasNext())
			{
				MeshNode topet1=null;
				MeshNode topet2=null;
				MeshNode topetbis=null;
				
				MeshEdge tedge = (MeshEdge) (it_edge.next());
				MeshNode2D nnn1=(MeshNode2D)tedge.getNodes1();
				MeshNode2D nnn2=(MeshNode2D)tedge.getNodes2();

				MeshNode nt1=null;
				MeshNode nt2=null;
				HashSet nodesMaj = nnn1.getNodesMaj();
				// (topet1)
				if (nodesMaj.size() > 1 )
				{
					Iterator it=nodesMaj.iterator();
					nt1 = (MeshNode)it.next();
					MeshNode nt1bis = (MeshNode)it.next();
					// Case: Edge-Edge
					if ( (((PST_Position)nt1.pos).getType()==((PST_Position)nt1bis.pos).getType())
						&& (  (((PST_Position)nt1.pos).getType() == PST_Position.EDGE)) )
					{
						// Retrieve the Vertices of the edge
						TopoDS_Edge E1 = (TopoDS_Edge)((PST_LinePosition)nt1.pos).getShape();
						TopoDS_Vertex[] vertices = new TopoDS_Vertex[2];
						TopoDS_Vertex V1, V2;
						vertices = TopExp.vertices(E1);
						V1 = vertices[0];
						V2 = vertices[1];
						MeshMesh nm1 = father.getMeshFromMapOfSubMesh(V1);
						topet1=(MeshNode)nodesOfSubMesh.addIfNotPresent(nt1);
						topetbis=(MeshNode)nodesOfSubMesh.addIfNotPresent(nt1bis);
						nt1 = (MeshNode)nm1.getNodesIterator().next();
					}
					// Case Edge-Vertex ou Vertex-Edge
					else if (((PST_Position)nt1.pos).getType()!=((PST_Position)nt1bis.pos).getType())
					{
						if (((PST_Position)nt1.pos).getType() == PST_Position.VERTEX)
						{
							nt1=(MeshNode)nodesOfSubMesh.addIfNotPresent(nt1);
						}
						else if (((PST_Position)nt1bis.pos).getType() == PST_Position.VERTEX)
						{
							nt1=(MeshNode)nodesOfSubMesh.addIfNotPresent(nt1bis);
						}
					}
				}
				else if (nodesMaj.size() == 0)
				{
					nt1=meshFace.addNode(new MeshNode(nnn1));
				}
				else
				{
					Iterator it=nnn1.getNodesMaj().iterator();
					nt1=(MeshNode)it.next();
					nt1=(MeshNode)nodesOfSubMesh.addIfNotPresent(nt1);
				}
				
				nodesMaj = nnn2.getNodesMaj();
				// topet2
				if (nodesMaj.size() > 1 )
				{
					Iterator it=nodesMaj.iterator();
					nt2 = (MeshNode)it.next();
					MeshNode nt2bis = (MeshNode)it.next();
					// Case Edge-Edge
					if ( (((PST_Position)nt2.pos).getType()==((PST_Position)nt2bis.pos).getType())
						&& ( (((PST_Position)nt2.pos).getType() == PST_Position.EDGE)) )
					{
						// Retrieve the Vertices of the edge
						TopoDS_Edge E2 = null;
						E2 = (TopoDS_Edge)((PST_LinePosition)nt2.pos).getShape();
						TopoDS_Vertex[] vertices = new TopoDS_Vertex[2];
						TopoDS_Vertex V1, V2;
						vertices = TopExp.vertices(E2);
						V1 = vertices[0];
						V2 = vertices[1];
						MeshMesh nm2 = father.getMeshFromMapOfSubMesh(V1);
						topet2=(MeshNode)nodesOfSubMesh.addIfNotPresent(nt2);
						topetbis=(MeshNode)nodesOfSubMesh.addIfNotPresent(nt2bis);
						nt2 = (MeshNode)nm2.getNodesIterator().next();
					}
					// Case Edge-Vertex ou Vertex-Edge
					else if (((PST_Position)nt2.pos).getType()!=((PST_Position)nt2bis.pos).getType())
					{
						if (((PST_Position)nt2.pos).getType() == PST_Position.VERTEX)
						{
							nt2=(MeshNode)nodesOfSubMesh.addIfNotPresent(nt2);
						}
						else if (((PST_Position)nt2bis.pos).getType() == PST_Position.VERTEX)
						{
							nt2=(MeshNode)nodesOfSubMesh.addIfNotPresent(nt2bis);
						}
					}
				}
				else if (nodesMaj.size() == 0)
				{
					nt2=meshFace.addNode(new MeshNode(nnn2));
				}
				else
				{
					Iterator it=nnn2.getNodesMaj().iterator();
					nt2=(MeshNode)it.next();
					nt2=(MeshNode)nodesOfSubMesh.addIfNotPresent(nt2);
				}
				
				nt1.setCoord3D();
				nt2.setCoord3D();
				
				//MeshEdge edge=new MeshEdge(nt1,nt2);
				MeshEdge edge=meshFace.getEdgeDefinedByNodes(nt1,nt2);
				if(edge==null)
				{
					edge=new MeshEdge(nt1,nt2);
					meshFace.addEdge(edge);
					edge.setWire(tedge.isWire());
					edge.setFrozen(tedge.isFrozen());
				}
				tempedge.add(edge);
			} // while (it_face.hasNext())
			
			meshFace.rmFace(face);
			Iterator it=tempedge.iterator();
			face=meshFace.addTriangle(
				(MeshEdge)it.next(),
				(MeshEdge)it.next(),
				(MeshEdge)it.next());
		} // while (it_face.hasNext())
	}
	
	public Triangle2D addTriangle2D(MeshNode n1, MeshNode n2, MeshNode n3)
	{
		assert nodelist.contains(n1);
		assert nodelist.contains(n2);
		assert nodelist.contains(n3);
		
		MeshEdge e1=getEdgeDefinedByNodes(n1,n2);
		MeshEdge e2=getEdgeDefinedByNodes(n2,n3);
		MeshEdge e3=getEdgeDefinedByNodes(n3,n1);

		if(e1==null) e1=addEdge(new MeshEdge(n1,n2));
		if(e2==null) e2=addEdge(new MeshEdge(n2,n3));
		if(e3==null) e3=addEdge(new MeshEdge(n3,n1));
		Triangle2D f=(Triangle2D)addFace(new Triangle2D(e1,e2,e3));
		n1.link(f);
		n2.link(f);
		n3.link(f);
		return f;
	}
	/**
	 * @return An iterator on all nodes of the MeshMesh and its child MeshMesh
	 */
	public Iterator getNodesIterator()
	{
		return nodelist.iterator();
	}
	
	public Iterator getEdgesIterator()
	{
		return edgelist.iterator();
	}
	
	public Iterator getFacesIterator()
	{
		return facelist.iterator();
	}
}

