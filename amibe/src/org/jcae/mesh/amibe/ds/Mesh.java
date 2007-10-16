/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2004,2005,2006, by EADS CRC
    Copyright (C) 2007 by EADS France

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

package org.jcae.mesh.amibe.ds;

import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.amibe.traits.TriangleTraitsBuilder;
import java.util.Collection;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.io.Serializable;
import org.apache.log4j.Logger;

/**
 * Mesh data structure.
 * A mesh is composed of triangles, edges and vertices.  There are
 * many data structures to represent meshes, and we focused on the
 * following constraints:
 * <ul>
 *   <li>Memory usage must be minimal in order to perform very large
 *       meshes.</li>
 *   <li>Mesh traversal must be cheap.</li>
 * </ul>
 * We decided to implement a triangle-based data structure which is known
 * to be more memory efficient.  A {@link Triangle} is composed of three
 * {@link Vertex}, three links to adjacent triangles, and each vertex
 * contains a backward link to one of its incident triangles.  It is
 * then possible to loop within triangles or around vertices.
 * 
 * But there is also a need for lighter data structures.  For instance,
 * visualization does not need adjacency relations, we only need to
 * store triangle vertices.
 *
 * {@link Mesh} constructor takes an optional {@link MeshTraitsBuilder}
 * argument to fully describe the desired mesh data structure.  Once a
 * {@link Mesh} instance is created, its features cannot be modified.
 * With this argument, it is possible to specify if adjacent relations
 * between triangles have to be computed, if an octree is needed to
 * locate vertices, if triangles and/or nodes are stored in a list
 * or a set, etc.  Example:
 * <pre>
   MeshTraitsBuilder mtb = new MeshTraitsBuilder();
   // Store triangles into a set
   mtb.addTriangleSet();
   TriangleTraitsBuilder ttb = new TriangleTraitsBuilder();
   // Store adjacency relations with HalfEdge
   ttb.addHalfEdge();
   mtb.add(ttb);
   // Create a new instance with these features
   Mesh mesh = new Mesh(mtb);
   // Then each triangle created by mesh.createTriangle
   // will contain objects needed to store adjacency relations.
   Triangle t = (Triangle) mesh.createTriangle(...);
   // Vertices must be created by mesh.createVertex
   Vertex v = (Vertex) mesh.createVertex(...);
 * </pre>
 * 
 */

public class Mesh extends AbstractMesh implements Serializable
{
	private static Logger logger=Logger.getLogger(Mesh.class);
	
	/**
	 * Vertex at infinite.
	 */
	public Vertex outerVertex = new OuterVertex();
	
	//  AbstractTriangle list
	private final Collection<AbstractTriangle> triangleList;
	
	//  Node list.
	private final Collection<AbstractVertex> nodeList;

	//  Entity factory
	protected ElementFactoryInterface factory = null;

	protected int maxLabel = 0;
	
	//  Minimal topological edge length
	protected double epsilon = 1.;
	
	protected boolean accumulateEpsilon = false;
	
	// Utility class to improve debugging output
	private static class OuterVertex extends Vertex
	{
		
		public OuterVertex()
		{
			super();
			setReadable(false);
			setWritable(false);
		}

		@Override
		public String toString()
		{
			return "outer";
		}
	}

	/**
	 * Stores Integer(0..2) into an array to reuse these immutable objects.
	 */
	private static final Integer [] int3 = new Integer[3];
	static {
		int3[0] = Integer.valueOf(0);
		int3[1] = Integer.valueOf(1);
		int3[2] = Integer.valueOf(2);
	}

	/**
	 * Creates an empty mesh.  When no <code>MeshTraitsBuilder</code>
	 * is passed, {@link MeshTraitsBuilder#getDefault3D} is called
	 * implicitly.
	 */
	public Mesh()
	{
		super(MeshTraitsBuilder.getDefault3D());
		factory = new ElementFactory(traitsBuilder);
		triangleList = new ArrayList<AbstractTriangle>();
		if (traitsBuilder.hasNodes())
			nodeList = new ArrayList<AbstractVertex>();
		else
			nodeList = null;
	}
	
	/**
	 * Creates an empty mesh with specific features.
	 *
	 * @param mtb mesh traits builder
	 */
	public Mesh(MeshTraitsBuilder mtb)
	{
		super(mtb);
		factory = new ElementFactory(traitsBuilder);
		triangleList = mtb.getTriangles(traits);
		nodeList = mtb.getNodes(traits);
	}
	
	public void scaleTolerance(double scale)
	{
		epsilon *= scale;
	}
	
	/**
	 * Adds an existing triangle to triangle list.
	 *
	 * @param t  triangle being added.
	 */
	public void add(AbstractTriangle t)
	{
		triangleList.add(t);
	}
	
	/**
	 * Removes a triangle from triangle list.
	 *
	 * @param t  triangle being removed.
	 */
	public void remove(AbstractTriangle t)
	{
		triangleList.remove(t);
		if (!(t instanceof TriangleHE))
			return;
		TriangleHE that = (TriangleHE) t;
		// Remove links to help the garbage collector
		HalfEdge e = (HalfEdge) that.getAbstractHalfEdge();
		HalfEdge last = e;
		for (int i = 0; i < 3; i++)
		{
			e = (HalfEdge) e.next();
			e.setAdj(null);
			last.setNext(null);
			last = e;
		}
	}
	
	/**
	 * Returns triangle list.
	 *
	 * @return triangle list.
	 */
	public Collection<AbstractTriangle> getTriangles()
	{
		return triangleList;
	}

	/**
	 *  Adds a vertex to vertex list.
	 */
	public void add(AbstractVertex vertex)
	{
		nodeList.add(vertex);
	}
	
	/**
	 * Removes a vertex from vertex list.
	 *
	 * @param v  vertex being removed.
	 */
	public void remove(AbstractVertex v)
	{
		nodeList.remove(v);
	}
	
	/**
	 * Returns vertex list.
	 *
	 * @return vertex list.
	 */
	public Collection<AbstractVertex> getNodes()
	{
		return nodeList;
	}

	/**
	 * Tells whether nodes are stored.
	 *
	 * @return <code>true</code> if mesh was created with a <code>MeshTraitsBuilder</code>
	 * instance defining nodes, and <code>false</code> otherwise.
	 */
	public boolean hasNodes()
	{
		return traitsBuilder.hasNodes();
	}

	/**
	 * Creates a triangle composed of three vertices.
	 *
	 * @param v  array of three vertices
	 * @return a new {@link AbstractTriangle} instance composed of three vertices
	 */
	public AbstractTriangle createTriangle(AbstractVertex [] v)
	{
		assert v.length == 3;
		return factory.createTriangle(v);
	}

	/**
	 * Creates a triangle composed of three vertices.
	 *
	 * @param v0  first vertex
	 * @param v1  second vertex
	 * @param v2  third vertex
	 * @return a new {@link AbstractTriangle} instance composed of three vertices
	 */
	public AbstractTriangle createTriangle(AbstractVertex v0, AbstractVertex v1, AbstractVertex v2)
	{
		return factory.createTriangle(v0, v1, v2);
	}

 	/**
	 * Clones a triangle.
	 *
	 * @param that  triangle to clone
	 * @return a new {@link AbstractTriangle} instance
	 */
	public AbstractTriangle createTriangle(AbstractTriangle that)
	{
		return factory.createTriangle(that);
	}

	/**
	 * Creates a 2D or 3D vertex.
	 *
	 * @param p  coordinates
	 * @return a new {@link AbstractVertex} instance with this location.
	 */
	public AbstractVertex createVertex(double [] p)
	{
		return factory.createVertex(p);
	}

	/**
	 * Creates a 2D vertex.
	 *
	 * @param u  first coordinate
	 * @param v  second coordinate
	 * @return a new {@link AbstractVertex} instance with this location.
	 */
	public AbstractVertex createVertex(double u, double v)
	{
		return factory.createVertex(u, v);
	}

	/**
	 * Creates a 3D vertex.
	 *
	 * @param x  first coordinate
	 * @param y  second coordinate
	 * @param z  third coordinate
	 * @return a new {@link AbstractVertex} instance with this location.
	 */
	public AbstractVertex createVertex(double x, double y, double z)
	{
		return factory.createVertex(x, y, z);
	}

	/**
	 * Tells whether mesh contains adjacency relations.
	 * @return <code>true</code> if mesh contains adjacency relations,
	 *         <code>false</code> otherwise.
	 */
	public boolean hasAdjacency()
	{
		return factory.hasAdjacency();
	}

	/**
	 * Build adjacency relations between triangles.
	 */
	public void buildAdjacency()
	{
		buildAdjacency(-1.0);
	}

	/**
	 * Build adjacency relations between triangles.
	 * @param minAngle  when an edge has a dihedral angle greater than this value,
	 *   it is considered as a ridge and its endpoints are treated as if they
	 *   belong to a CAD edge.  By convention, a negative value means that this
	 *   check is not performed.
	 */
	public void buildAdjacency(double minAngle)
	{
		Collection<AbstractVertex> vertices;
		if (nodeList == null)
		{
			vertices = new LinkedHashSet<AbstractVertex>(triangleList.size()/2);
			for (AbstractTriangle at: triangleList)
				for (AbstractVertex av: at.vertex)
					vertices.add(av);
		}
		else
			vertices = nodeList;
		buildAdjacency(vertices, minAngle);
	}

	/**
	 * Build adjacency relations between triangles
	 * @deprecated
	 */
	public void buildAdjacency(Vertex [] vertices, double minAngle)
	{
		Collection<AbstractVertex> list = new ArrayList<AbstractVertex>(vertices.length);
		for (AbstractVertex v: vertices)
			list.add(v);
		buildAdjacency(list, minAngle);
	}

	@SuppressWarnings("unchecked")
	private void buildAdjacency(Collection<AbstractVertex> vertices, double minAngle)
	{
		//  1. For each vertex, build the list of triangles
		//     connected to this vertex.
		logger.debug("Build the list of triangles connected to each vertex");
		HashMap<AbstractVertex, ArrayList<AbstractTriangle>> tVertList = new HashMap<AbstractVertex, ArrayList<AbstractTriangle>>(vertices.size());
		for (AbstractVertex v: vertices)
			tVertList.put(v, new ArrayList<AbstractTriangle>(10));
		for (AbstractTriangle t: triangleList)
		{
			for (int i = 0; i < 3; i++)
			{
				ArrayList<AbstractTriangle> list = tVertList.get(t.vertex[i]);
				list.add(t);
				for (AbstractVertex av: t.vertex)
					((Vertex) av).setLink(t);
			}
		}
		//  2. Connect all edges together
		logger.debug("Connect triangles");
		for (AbstractVertex v: vertices)
			checkNeighbours(v, tVertList);
		//  tVertList is no more needed, remove all references
		//  to help the garbage collector.
		for (AbstractVertex v: vertices)
		{
			ArrayList<AbstractTriangle> list = tVertList.get(v);
			list.clear();
			tVertList.put(v, null);
		}
		tVertList.clear();
		//  3. Mark boundary edges and bind them to virtual triangles.
		logger.debug("Mark boundary edges");
		ArrayList<Triangle> newTri = new ArrayList<Triangle>();
		for (AbstractTriangle at: triangleList)
		{
			Triangle t = (Triangle) at;
			AbstractHalfEdge ot = t.getAbstractHalfEdge();
			for (int i = 0; i < 3; i++)
			{
				ot = ot.next();
				if (ot.getAdj() == null)
				{
					ot.setAttributes(AbstractHalfEdge.BOUNDARY);
					Triangle adj = (Triangle) factory.createTriangle(outerVertex, ot.destination(), ot.origin());
					newTri.add(adj);
					adj.setOuter();
					adj.setReadable(false);
					adj.setWritable(false);
					AbstractHalfEdge sym = adj.getAbstractHalfEdge();
					sym.setAttributes(AbstractHalfEdge.BOUNDARY);
					ot.glue(sym);
				}
			}
		}
		//  4. Mark non-manifold edges and bind them to virtual triangles.
		logger.debug("Mark non-manifold edges");
		for (AbstractTriangle at: triangleList)
		{
			Triangle t = (Triangle) at;
			AbstractHalfEdge ot = t.getAbstractHalfEdge();
			for (int i = 0; i < 3; i++)
			{
				ot = ot.next();
				if (!(ot.getAdj() instanceof LinkedHashMap))
					continue;
				// Create a virtual symmetric triangle, and put shared list 
				// of adjacent triangles into this virtual triangle.
				LinkedHashMap<Triangle, Integer> list = (LinkedHashMap<Triangle, Integer>) ot.getAdj();
				Triangle adj = (Triangle) factory.createTriangle(outerVertex, ot.destination(), ot.origin());
				newTri.add(adj);
				adj.setOuter();
				adj.setReadable(false);
				adj.setWritable(false);
				AbstractHalfEdge sym = adj.getAbstractHalfEdge();
				ot.glue(sym);
				ot.setAttributes(AbstractHalfEdge.NONMANIFOLD);
				sym.setAttributes(AbstractHalfEdge.NONMANIFOLD);
				sym = sym.next();
				// By convention, put LinkedHashMap on next edge
				sym.setAdj(list);
			}
		}
		
		//  5. Find the list of vertices which are on mesh boundary
		logger.debug("Build the list of nodes on boundaries and non-manifold edges");
		HashSet<Vertex> bndNodes = new HashSet<Vertex>();
		maxLabel = 0;
		for (AbstractTriangle at: triangleList)
		{
			Triangle t = (Triangle) at;
			AbstractHalfEdge ot = t.getAbstractHalfEdge();
			for (int i = 0; i < 3; i++)
			{
				ot = ot.next();
				if (ot.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD))
				{
					bndNodes.add(ot.origin());
					maxLabel = Math.max(maxLabel, ot.origin().getRef());
					bndNodes.add(ot.destination());
					maxLabel = Math.max(maxLabel, ot.destination().getRef());
				}
			}
		}

		//  6. Build links for non-manifold vertices
		logger.debug("Compute links for non-manifold vertices");
		Vertex [] endpoints = new Vertex[2];
		for (AbstractTriangle at: triangleList)
		{
			Triangle t = (Triangle) at;
			AbstractHalfEdge ot = t.getAbstractHalfEdge();
			for (int i = 0; i < 3; i++)
			{
				ot = ot.next();
				if (ot.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
				{
					endpoints[0] = ot.origin();
					endpoints[1] = ot.destination();
					for (int j = 0; j < 2; j++)
					{
						if (endpoints[j].getLink() instanceof Triangle)
						{
							LinkedHashSet<Triangle> link = new LinkedHashSet<Triangle>();
							link.add((Triangle) endpoints[j].getLink());
							endpoints[j].setLink(link);
						}
					}
					for (Iterator<AbstractHalfEdge> it = ot.fanIterator(); it.hasNext(); )
					{
						Triangle t2 = it.next().getTri();
						for (int j = 0; j < 2; j++)
						{
							LinkedHashSet<Triangle> link = (LinkedHashSet<Triangle>) endpoints[j].getLink();
							link.add(t2);
						}
					}
				}
			}
		}
		// Replace LinkedHashSet by AbstractTriangle[], and keep only one
		// AbstractTriangle by fan.
		int nrNM = 0;
		int nrFE = 0;
		for (AbstractTriangle at: triangleList)
		{
			Triangle t = (Triangle) at;
			AbstractHalfEdge ot = t.getAbstractHalfEdge();
			for (int i = 0; i < 3; i++)
			{
				Vertex v = (Vertex) t.vertex[i];
				if (v.getLink() instanceof LinkedHashSet)
				{
					nrNM++;
					v.setLinkFan((LinkedHashSet<Triangle>) v.getLink());
				}
				ot = ot.next();
				if (ot.hasAttributes(AbstractHalfEdge.BOUNDARY))
					nrFE++;
			}
		}
		if (nrNM > 0)
			logger.debug("Found "+nrNM+" non manifold vertices");
		if (nrFE > 0)
			logger.debug("Found "+nrFE+" free edges");
		//  7. If vertices are on inner boundaries and there is
		//     no ridge, change their label.
		logger.debug("Set interior nodes mutable");
		int nrJunctionPoints = 0;
		double cosMinAngle = Math.cos(Math.PI*minAngle/180.0);
		if (minAngle < 0.0)
			cosMinAngle = -2.0;
		double [][] temp = new double[4][3];
		for (AbstractVertex av: vertices)
		{
			if (bndNodes.contains(av))
				continue;
			Vertex v = (Vertex) av;
			int label = v.getRef();
			if (v.getLink() instanceof Triangle[])
			{
				nrJunctionPoints++;
				if (label == 0)
				{
					maxLabel++;
					v.setRef(maxLabel);
				}
			}
			else if (0 != label)
			{
				//  Check for ridges
				Triangle t = (Triangle) v.getLink();
				if (checkRidges(v, cosMinAngle, t, temp))
					v.setRef(-label);
			}
		}
		if (nrJunctionPoints > 0)
			logger.info("Found "+nrJunctionPoints+" junction points");
		// Add outer triangles
		triangleList.addAll(newTri);
	}
	
	private static final void checkNeighbours(AbstractVertex v, HashMap<AbstractVertex, ArrayList<AbstractTriangle>> tVertList)
	{
		//  Mark all triangles having v as vertex
		ArrayList<AbstractTriangle> neighTriList = tVertList.get(v);
		AbstractTriangle.List markedTri = new AbstractTriangle.List();
		for (AbstractTriangle at: neighTriList)
			markedTri.add(at);
		//  Loop on all edges incident to v
		for (AbstractTriangle at: neighTriList)
		{
			Triangle t = (Triangle) at;
			AbstractHalfEdge ot = t.getAbstractHalfEdge();
			AbstractHalfEdge sym = t.getAbstractHalfEdge();
			if (ot.destination() == v)
				ot = ot.next();
			else if (ot.apex() == v)
				ot = ot.prev();
			assert ot.origin() == v;
			// Skip this edge if adjacency relations already exist, 
			if (ot.getAdj() != null)
				continue;
			Vertex v2 = ot.destination();
			// Edge (v,v2) has not yet been processed.
			// List of triangles incident to v2.
			ArrayList<AbstractTriangle> neighTriV2List = tVertList.get(v2);
			boolean manifold = true;
			LinkedHashMap<Triangle, Integer> adj = null;
			for (AbstractTriangle at2: neighTriV2List)
			{
				Triangle t2 = (Triangle) at2;
				if (t == t2 || !markedTri.contains(t2))
					continue;
				// t2 contains v and v2, we now look for an edge
				// (v,v2) or (v2,v)
				AbstractHalfEdge ot2 = t2.getAbstractHalfEdge();
				if (ot2.destination() == v2)
					ot2 = ot2.next();
				else if (ot2.apex() == v2)
					ot2 = ot2.prev();
				if (manifold && ot2.destination() == v && ot.getAdj() == null && ot2.getAdj() == null)
				{
					// This edge seems to be manifold.
					// It may become non manifold later when
					// other neighbours are processed.
					ot.glue(ot2);
					continue;
				}
				manifold = false;
				if (ot2.destination() != v)
					ot2 = ot2.prev();
				// We are sure now that ot2 == (v,v2) or (v2,v)
				assert (v == ot2.origin() && v2 == ot2.destination()) || (v2 == ot2.origin() && v == ot2.destination());
				// This edge is non manifold.  In this routine, we
				// replace adjacency relation by a list of all adjacent
				// triangles.  This adjacency relation will be modified
				// later in buildAdjacency.
				// TODO: set final adjacency relations here.
				//
				// We need to store adjacent triangles and local number
				// of symmetric edge.  This can be achieved by an ArrayList,
				// but we use a LinkedHashMap instead for type safety.
				if (adj == null)
					adj = new LinkedHashMap<Triangle, Integer>();
				if (ot.getAdj() == null)
				{
					// All adjacent edges share the same LinkedHashMap,
					// thus put ot in it.
					adj.put(t, int3[ot.getLocalNumber()]);
					ot.setAdj(adj);
				}
				else if (ot.getAdj() instanceof HalfEdge)
				{
					sym = ot.sym(sym);
					assert sym.getAdj() == ot;
					adj.put(t, int3[ot.getLocalNumber()]);
					adj.put(sym.getTri(), int3[sym.getLocalNumber()]);
					ot.setAdj(adj);
					sym.setAdj(adj);
				}
				else if (ot.getAdj() instanceof Triangle)
				{
					sym = ot.sym(sym);
					assert sym.getAdj() == t;
					assert sym.getTri().getAdjLocalNumber(sym.getLocalNumber()) == ot.getLocalNumber();
					adj.put(t, int3[ot.getLocalNumber()]);
					adj.put(sym.getTri(), int3[sym.getLocalNumber()]);
					ot.setAdj(adj);
					sym.setAdj(adj);
				}
				adj.put(t2, int3[ot2.getLocalNumber()]);
				ot2.setAdj(adj);
			}
			if (logger.isDebugEnabled() && adj != null)
				logger.debug("Non-manifold edge: "+v+" "+v2+" "+" connected to "+adj.size()+" fans");
		}
		//  Unmark adjacent triangles
		markedTri.clear();
	}
	
	private final boolean checkRidges(Vertex v, double cosMinAngle, Triangle t, double [][] temp)
	{
		AbstractHalfEdge ot = t.getAbstractHalfEdge();
		AbstractHalfEdge sym = t.getAbstractHalfEdge();
		if (ot.origin() != v)
			ot = ot.next();
		if (ot.origin() != v)
			ot = ot.next();
		assert ot.origin() == v;
		Vertex first = ot.destination();
		int id = ot.getTri().getGroupId();
		// First check that all triangles belong to the same group
		while (true)
		{
			Vertex d = ot.destination();
			if (d != outerVertex && 0 != d.getRef())
			{
				if (id != ot.getTri().getGroupId())
					return false;
				sym = ot.sym(sym);
				if (id != sym.getTri().getGroupId())
					return false;
			}
			ot = ot.nextOrigin();
			if (ot.destination() == first)
				break;
		}
		// Now check for coplanarity
		if (cosMinAngle < -1.0)
			return true;
		while (true)
		{
			Vertex d = ot.destination();
			if (d != outerVertex && 0 != d.getRef())
			{
				sym = ot.sym(sym);
				Matrix3D.computeNormal3D(v.getUV(), d.getUV(), ot.apex().getUV(), temp[0], temp[1], temp[2]);
				Matrix3D.computeNormal3D(d.getUV(), v.getUV(), sym.apex().getUV(), temp[0], temp[1], temp[3]);
				double angle = Matrix3D.prodSca(temp[2], temp[3]);
				if (angle > -cosMinAngle)
					return false;
			}
			ot = ot.nextOrigin();
			if (ot.destination() == first)
				break;
		}
		return true;
	}
	
	/**
	 * Sets an unused boundary reference on a vertex.
	 */
	public void setRefVertexOnboundary(Vertex v)
	{
		maxLabel++;
		v.setRef(maxLabel);
	}
	
	/**
	 * Checks whether a length is lower than a threshold.
	 *
	 * @param len   the length to be checked.
	 * @return <code>true</code> if this length is too small to be considered,
	 *         <code>false</code> otherwise.
	 */
	public boolean tooSmall(double len, double accumulatedLength)
	{
		if (accumulateEpsilon)
			len += accumulatedLength;
		return (len < epsilon);
	}
	
	/**
	 * Returns distance between 2 vertices.
	 */
	public double distance(Vertex start, Vertex end, Vertex vm)
	{
		double [] x1 = start.getUV();
		double [] x2 = end.getUV();
		assert x1.length == 3;
		double dx = x1[0] - x2[0];
		double dy = x1[1] - x2[1];
		double dz = x1[2] - x2[2];
		return Math.sqrt(dx*dx + dy*dy + dz*dz);
	}
	
	public double radius2d(Vertex v)
	{
		return 1.0;
	}
	
	/**
	 * Checks whether an edge can be contracted.
	 *
	 * @param e   edge to be checked
	 * @param v   the resulting vertex
	 * @return <code>true</code> if this edge can be contracted into the single vertex n, <code>false</code> otherwise.
	 */
	public boolean canCollapseEdge(AbstractHalfEdge e, AbstractVertex v)
	{
		return e.canCollapse(v);
	}
	
	/**
	 * Contracts an edge.
	 *
	 * @param e   edge to contract
	 * @param v   the resulting vertex
	 * @return edge starting from <code>n</code> and with the same apex
	 * @throws IllegalArgumentException if edge belongs to an outer triangle,
	 * because there would be no valid return value.  User must then run this
	 * method against symmetric edge, this is not done automatically.
	 */
	public AbstractHalfEdge edgeCollapse(AbstractHalfEdge e, AbstractVertex v)
	{
		return e.collapse(this, v);
	}
	
	/**
	 * Splits an edge.  This is the opposite of {@link #edgeCollapse}.
	 *
	 * @param e   edge being splitted
	 * @param v   the resulting vertex
	 */
	public AbstractHalfEdge vertexSplit(AbstractHalfEdge e, AbstractVertex v)
	{
		return e.split(this, v);
	}
	
	/**
	 * Swaps an edge.
	 *
	 * @return swapped edge, origin and apical vertices are the same as in original edge
	 * @throws IllegalArgumentException if edge is on a boundary or belongs
	 * to an outer triangle.
	 */
	public AbstractHalfEdge edgeSwap(AbstractHalfEdge e)
	{
		return e.swap();
	}
	
	/**
	 * Checks whether this mesh is valid.
	 * This routine returns <code>isValid(true)</code>.
	 *
	 * @see #isValid(boolean)
	 */
	public boolean isValid()
	{
		return isValid(true);
	}
	
	/**
	 * Checks whether this mesh is valid.
	 * This routine can be called at any stage, even before boundary
	 * edges have been enforced.  In this case, some tests must be
	 * removed because they do not make sense.
	 *
	 * @param constrained  <code>true</code> if mesh is constrained.
	 */
	public boolean isValid(boolean constrained)
	{
		for (AbstractTriangle t: triangleList)
		{
			if (t.vertex[0] == t.vertex[1] || t.vertex[1] == t.vertex[2] || t.vertex[2] == t.vertex[0])
			{
				logger.error("Duplicate vertices: "+t);
				return false;
			}
			if (t.vertex[0] == outerVertex || t.vertex[1] == outerVertex || t.vertex[2] == outerVertex)
			{
				if (constrained && t instanceof Triangle)
				{
					if (!((Triangle) t).isOuter())
					{
						logger.error("AbstractTriangle should be outer: "+t);
						return false;
					}
				}
			}
			for (int i = 0; i < 3; i++)
			{
				Vertex v = (Vertex) t.vertex[i];
				if (v.getLink() == null)
					continue;
				if (v.getLink() instanceof AbstractTriangle)
				{
					AbstractTriangle t2 = (AbstractTriangle) v.getLink();
					if (t2.vertex[0] != v && t2.vertex[1] != v && t2.vertex[2] != v)
					{
						logger.error("Vertex "+v+" linked to "+t2);
						return false;
					}
					if (!triangleList.contains(t2))
					{
						logger.error("Vertex "+v+" linked to a removed triangle");
						return false;
					}
				}
				else
				{
					// Check that all linked triangles are
					// still present in mesh
					Triangle [] links = (Triangle []) v.getLink();
					for (Triangle t2: links)
					{
						if (!triangleList.contains(t2))
						{
							logger.error("Vertex "+v+" linked to a removed triangle");
							return false;
						}
					}
				}
			}
			if (!checkVirtualHalfEdges(t))
				return false;
			if (!checkHalfEdges(t))
				return false;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	private boolean checkVirtualHalfEdges(AbstractTriangle t)
	{
		if (!t.traitsBuilder.hasCapability(TriangleTraitsBuilder.SHALLOWHALFEDGE))
			return true;
		VirtualHalfEdge ot = new VirtualHalfEdge();
		VirtualHalfEdge sym = new VirtualHalfEdge();
		ot.bind((Triangle) t);
		boolean isOuter = ot.hasAttributes(AbstractHalfEdge.OUTER);
		for (int i = 0; i < 3; i++)
		{
			ot = (VirtualHalfEdge) ot.next();
			if (isOuter != ot.hasAttributes(AbstractHalfEdge.OUTER))
			{
				logger.error("Inconsistent outer state: "+ot);
				return false;
			}
			if (ot.getAdj() == null)
				continue;
			if (ot.getAdj() instanceof Triangle)
			{
				Vertex v1 = ot.origin();
				Vertex v2 = ot.destination();
				sym = (VirtualHalfEdge) ot.sym(sym);
				if (sym.origin() != v2 || sym.destination() != v1)
				{
					logger.error("Vertex mismatch in adjacency relation: ");
					logger.error(" "+ot);
					logger.error(" "+sym);
					return false;
				}
				if (!(sym.getAdj() instanceof Triangle))
				{
					logger.error("Wrong adjacency relation: ");
					logger.error(" "+ot);
					logger.error(" "+sym);
					return false;
				}
				if (sym.getAdj() != t || sym.getTri().getAdjLocalNumber(sym.getLocalNumber()) != ot.getLocalNumber())
				{
					logger.error("Wrong adjacency relation: ");
					logger.error(" adj1: "+ot);
					logger.error(" adj2: "+sym);
					logger.error(""+sym.getAdj().getClass().getName());
					return false;
				}
				if (sym.hasAttributes(AbstractHalfEdge.BOUNDARY) != ot.hasAttributes(AbstractHalfEdge.BOUNDARY))
				{
					logger.error("Inconsistent boundary flag");
					logger.error(" "+ot);
					logger.error(" "+sym);
					return false;
				}
			}
			else
			{
				// Check that all edges share the same adjacency
				// list.
				LinkedHashMap<Triangle, Integer> adj = (LinkedHashMap<Triangle, Integer>) ot.getAdj();
				for (Map.Entry<Triangle, Integer> entry: adj.entrySet())
				{
					Triangle t2 = entry.getKey();
					int i2 = entry.getValue().intValue();
					sym.bind(t2, i2);
					sym = (VirtualHalfEdge) sym.sym();
					sym = (VirtualHalfEdge) sym.next();
					if (sym.getAdj() != adj)
					{
						logger.error("Multiple edges: Wrong adjacency relation");
						return false;
					}
				}
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	private boolean checkHalfEdges(AbstractTriangle t)
	{
		if (!t.traitsBuilder.hasCapability(TriangleTraitsBuilder.HALFEDGE))
			return true;
		HalfEdge e = (HalfEdge) ((Triangle) t).getAbstractHalfEdge();
		boolean isOuter = e.hasAttributes(AbstractHalfEdge.OUTER);
		for (int i = 0; i < 3; i++)
		{
			e = (HalfEdge) e.next();
			if (isOuter != e.hasAttributes(AbstractHalfEdge.OUTER))
			{
				logger.error("Inconsistent outer state: "+e);
				return false;
			}
			if (e.getAdj() == null)
				continue;
			if (e.getAdj() instanceof HalfEdge)
			{
				Vertex v1 = e.origin();
				Vertex v2 = e.destination();
				HalfEdge f = (HalfEdge) e.sym();
				if (f.origin() != v2 || f.destination() != v1)
				{
					logger.error("Vertex mismatch in adjacency relation: ");
					logger.error(" "+e);
					logger.error(" "+f);
					return false;
				}
				if (!(f.getAdj() instanceof HalfEdge))
				{
					logger.error("Wrong adjacency relation: ");
					logger.error(" "+e);
					logger.error(" "+f);
					return false;
				}
				if (f.sym() != e)
				{
					logger.error("Wrong adjacency relation: ");
					logger.error(" adj1: "+e);
					logger.error(" adj2: "+f);
					return false;
				}
				if (f.hasAttributes(AbstractHalfEdge.BOUNDARY) != e.hasAttributes(AbstractHalfEdge.BOUNDARY))
				{
					logger.error("Inconsistent boundary flag");
					logger.error(" "+e);
					logger.error(" "+f);
					return false;
				}
				if (!triangleList.contains(f.getTri()))
				{
					logger.error("Triangle not present in mesh: "+f.getTri());
					logger.error("Linked from "+e);
					return false;
				}
			}
			else
			{
				// Check that all edges share the same adjacency
				// list.
				LinkedHashMap<Triangle, Integer> adj = (LinkedHashMap<Triangle, Integer>) e.getAdj();
				for (Map.Entry<Triangle, Integer> entry: adj.entrySet())
				{
					Triangle t2 = entry.getKey();
					if (!triangleList.contains(t2))
					{
						logger.error("Triangle does no more exist: "+t2);
						return false;
					}
					int i2 = entry.getValue().intValue();
					HalfEdge f = (HalfEdge) t2.getAbstractHalfEdge();
					if (i2 == 1)
						f = (HalfEdge) f.next();
					else if (i2 == 2)
						f = (HalfEdge) f.prev();
					HalfEdge s = (HalfEdge) f.sym().next();
					if (!triangleList.contains(s.getTri()))
					{
						logger.error("Triangle not present in mesh: "+s.getTri());
						logger.error("Linked from "+e);
					}
					if (s.getAdj() != adj)
					{
						logger.error("Multiple edges: Wrong adjacency relation");
						return false;
					}
				}
				// Endpoints must link to at least 
				Vertex o = e.origin();
				Vertex a = e.apex();
				if (!(o.getLink() instanceof Triangle[]))
				{
					logger.error("Endpoint must be non-manifold: "+o);
					return false;
				}
				if (!(a.getLink() instanceof Triangle[]))
				{
					logger.error("Endpoint must be non-manifold: "+a);
					return false;
				}
				Triangle [] linkO = (Triangle[]) o.getLink();
				if (linkO.length < adj.size())
				{
					logger.error("Origin linked to "+linkO.length+" triangles, less than "+adj.size());
					return false;
				}
				Triangle [] linkA = (Triangle[]) a.getLink();
				if (linkA.length < adj.size())
				{
					logger.error("Origin linked to "+linkA.length+" triangles, less than "+adj.size());
					return false;
				}
			}
		}
		return true;
	}
	
	// Useful for debugging
	/*  Following imports must be moved at top.
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;
import gnu.trove.TObjectIntHashMap;

	public void printMesh()
	{
		System.out.println("Mesh:");
		for (AbstractTriangle at: triangleList)
			System.out.println(""+at);
		System.out.println("Outer Vertex: "+outerVertex);
	}
	public void writeUNV(String file)
	{
		String cr=System.getProperty("line.separator");
		PrintWriter out;
		try {
			if (file.endsWith(".gz") || file.endsWith(".GZ"))
				out = new PrintWriter(new GZIPOutputStream(new FileOutputStream(file)));
			else
				out = new PrintWriter(new FileOutputStream(file));
			out.println("    -1"+cr+"  2411");
			HashSet<Vertex> nodeset = new HashSet<Vertex>();
			for (AbstractTriangle at: triangleList)
			{
				Triangle t = (Triangle) at;
				if (t.isOuter())
					continue;
				if (t.vertex[0] == outerVertex || t.vertex[1] == outerVertex || t.vertex[2] == outerVertex)
					continue;
				nodeset.add(t.vertex[0]);
				nodeset.add(t.vertex[1]);
				nodeset.add(t.vertex[2]);
			}
			int count = 0;
			TObjectIntHashMap<Vertex> labels = new TObjectIntHashMap<Vertex>(nodeset.size());
			for(Vertex node: nodeset)
			{
				count++;
				labels.put(node, count);
				double [] uv = node.getUV();
				out.println(count+"         1         1         1");
				if (uv.length == 2)
					out.println(""+uv[0]+" "+uv[1]+" 0.0");
				else
					out.println(""+uv[0]+" "+uv[1]+" "+uv[2]);
			}
			out.println("    -1");
			out.println("    -1"+cr+"  2412");
			count = 0;
			for (AbstractTriangle at: triangleList)
			{
				Triangle t = (Triangle) at;
				if (t.isOuter())
					continue;
				if (t.vertex[0] == outerVertex || t.vertex[1] == outerVertex || t.vertex[2] == outerVertex)
					continue;
				count++;
				out.println(""+count+"        91         1         1         1         3");
				for(int i = 0; i < 3; i++)
				{
					int nodelabel = labels.get(t.vertex[i]);
					out.print(" "+nodelabel);
				}
				out.println("");
			}
			out.println("    -1");
			out.close();
		} catch (FileNotFoundException e)
		{
			logger.fatal(e.toString());
			e.printStackTrace();
		} catch (IOException e)
		{
			logger.fatal(e.toString());
			e.printStackTrace();
		}
	}
	public void writeMesh(String file)
	{
		String cr=System.getProperty("line.separator");
		PrintWriter out;
		try {
			if (file.endsWith(".gz") || file.endsWith(".GZ"))
				out = new PrintWriter(new GZIPOutputStream(new FileOutputStream(file)));
			else
				out = new PrintWriter(new FileOutputStream(file));
			out.println("MeshVersionFormatted 1"+cr+"Dimension"+cr+"3");
			HashSet<Vertex> nodeset = new HashSet<Vertex>();
			for(AbstractTriangle at: triangleList)
			{
				Triangle t = (Triangle) at;
				if (t.isOuter())
					continue;
				if (t.vertex[0] == outerVertex || t.vertex[1] == outerVertex || t.vertex[2] == outerVertex)
					continue;
				nodeset.add(t.vertex[0]);
				nodeset.add(t.vertex[1]);
				nodeset.add(t.vertex[2]);
			}
			int count = 0;
			TObjectIntHashMap<Vertex> labels = new TObjectIntHashMap<Vertex>(nodeset.size());
			out.println("Vertices"+cr+nodeset.size());
			for(Vertex node: nodeset)
			{
				count++;
				labels.put(node, count);
				double [] uv = node.getUV();
				if (uv.length == 2)
					out.println(""+uv[0]+" "+uv[1]+" 0.0 0");
				else
					out.println(""+uv[0]+" "+uv[1]+" "+uv[2]+" 0");
			}
			count = 0;
			for(AbstractTriangle at: triangleList)
			{
				Triangle t = (Triangle) at;
				if (t.isOuter())
					continue;
				if (t.vertex[0] == outerVertex || t.vertex[1] == outerVertex || t.vertex[2] == outerVertex)
					continue;
				count++;
			}
			out.println(cr+"Triangles"+cr+count);
			count = 0;
			for(AbstractTriangle at: triangleList)
			{
				Triangle t = (Triangle) at;
				if (t.isOuter())
					continue;
				if (t.vertex[0] == outerVertex || t.vertex[1] == outerVertex || t.vertex[2] == outerVertex)
					continue;
				count++;
				for(int i = 0; i < 3; i++)
				{
					int nodelabel = labels.get(t.vertex[i]);
					out.print(nodelabel+" ");
				}
				out.println("1");
			}
			out.println(cr+"End");
			out.close();
		} catch (FileNotFoundException e)
		{
			logger.fatal(e.toString());
			e.printStackTrace();
		} catch (IOException e)
		{
			logger.fatal(e.toString());
			e.printStackTrace();
		}
	}
	*/
	
}
