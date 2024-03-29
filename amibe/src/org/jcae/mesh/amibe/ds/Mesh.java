/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2004,2005,2006, by EADS CRC
    Copyright (C) 2007-2011, by EADS France

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

import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.jcae.mesh.amibe.traits.Traits;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.amibe.traits.TriangleTraitsBuilder;
import org.jcae.mesh.amibe.metrics.KdTree;
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.amibe.metrics.Metric;
import org.jcae.mesh.amibe.metrics.EuclidianMetric3D;
import org.jcae.mesh.amibe.metrics.Location;
import org.jcae.mesh.amibe.metrics.PoolWorkVectors;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Iterator;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.util.HashFactory;

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

public class Mesh implements Serializable
{
	private static final long serialVersionUID = 7130909528217390687L;
	private static final Logger logger=Logger.getLogger(Mesh.class.getName());
	
	/**
	 * User-defined traits builder.
	 */
	protected final MeshTraitsBuilder traitsBuilder;

	/**
	 * User-defined traits
	 */
	protected final Traits traits;

	/**
	 * User-defined mesh parameters.
	 */
	protected final MeshParameters meshParameters;

	/**
	 * Vertex at infinite.
	 */
	public Vertex outerVertex = new OuterVertex();
	
	//  Triangle list
	private final Collection<Triangle> triangleList;
	
	//  Node list.
	private final Collection<Vertex> nodeList;

	//  Entity factory
	protected ElementFactoryInterface factory = null;

	//  Set to true by Mesh2D, this subclass connects outer triangles
	protected boolean outerTrianglesAreConnected = false;
	
	//  Set to true if references must be written onto disk
	private boolean persistentReferences = false;

	private int maxLabel = 0;

	// 3D euclidian metric
	private final Metric euclidian_metric3d = new EuclidianMetric3D();

	// Temporary vectors used as work arrays in HalfEdge
	protected final PoolWorkVectors temp = new PoolWorkVectors();

	//  Complex algorithms require several VirtualHalfEdge, they are
	//  allocated here to prevent allocation/deallocation overhead.
	protected final VirtualHalfEdge[] tempVH = new VirtualHalfEdge[4];
	protected final HalfEdge[] tempHE = new HalfEdge[6];

	private List<Vertex> beams = new ArrayList<Vertex>();
	private TIntArrayList beamGroups = new TIntArrayList();
	private Map<Integer, String> groupNames = new HashMap<Integer, String>();
	private Map<String, Collection<Vertex>> vertexGroups =
		new HashMap<String, Collection<Vertex>>();

	// Utility class to improve debugging output
	private static class OuterVertex extends Vertex
	{
		private static final long serialVersionUID = -6535592611308767946L;

		public OuterVertex()
		{
			super(null);
			setReadable(false);
			setWritable(false);
			setMutable(false);
		}

		@Override
		public final String toString()
		{
			return "outer";
		}
	}

	/**
	 * Creates an empty mesh.  When no <code>MeshTraitsBuilder</code>
	 * is passed, {@link MeshTraitsBuilder#getDefault3D} is called
	 * implicitly.
	 */
	public Mesh()
	{
		this(MeshTraitsBuilder.getDefault3D(), new MeshParameters());
	}
	
	public Mesh(MeshTraitsBuilder builder)
	{
		this(builder, new MeshParameters());
	}
	
	/**
	 * Creates an empty mesh with specific features.
	 *
	 * @param builder mesh traits builder
	 */
	public Mesh(MeshTraitsBuilder builder, MeshParameters mp)
	{
		traitsBuilder = builder;
		if (builder != null)
			traits = builder.createTraits();
		else
			traits = null;
		factory = new ElementFactory(traitsBuilder);
		triangleList = traitsBuilder.getTriangles(traits);
		nodeList = traitsBuilder.getNodes(traits);
		meshParameters = mp;

		for (int i = 0; i < tempVH.length; i++)
			tempVH[i] = new VirtualHalfEdge();
		for (int i = 0; i < tempHE.length; i++)
			tempHE[i] = new HalfEdge(traitsBuilder.getHalfEdgeTraitsBuilder(), (TriangleHE) null, (byte) 0, (byte) 0);
	}

	public MeshTraitsBuilder getBuilder()
	{
		return traitsBuilder;
	}

	public final MeshParameters getMeshParameters()
	{
		return meshParameters;
	}
	
	/**
	 * Adds an existing triangle to triangle list.
	 *
	 * @param t  triangle being added.
	 */
	public final void add(Triangle t)
	{
		assert t!= null;
		triangleList.add(t);
	}

	public int getFreeGroupID()
	{
		int i = 1;
		while(groupNames.get(i) != null)
			i++;
		return i;
	}

	public int pushGroup(double[] coordinates, int[] triangles, int[] beams)
	{
		int groupId = getFreeGroupID();
		setGroupName(groupId, Integer.toString(groupId));
		return pushGroup(coordinates, triangles, beams, groupId);
	}

	public int pushGroup(double[] coordinates, int[] triangles, int[] beams, int groupId)
	{
		Vertex[] vertices = new Vertex[coordinates.length / 3];
		int k = 0;
		for(int i = 0; i < coordinates.length / 3; i++)
		{
			vertices[i] = createVertex(
				coordinates[k], coordinates[k+1], coordinates[k+2]);
			k += 3;
		}

		if(hasNodes())
		{
			for(Vertex v:vertices)
				add(v);
		}
		k = 0;
		if(triangles != null)
		{
			for(int i = 0; i < triangles.length / 3; i++)
			{
				Triangle t = createTriangle(vertices[triangles[k]],
					vertices[triangles[k + 1]], vertices[triangles[k + 2]]);
				add(t);
				t.setGroupId(groupId);
				k += 3;
			}
		}
		k = 0;
		if(beams != null)
		{
			for(int i = 0; i < beams.length / 2; i++)
			{
				addBeam(vertices[beams[k]], vertices[beams[k+1]], groupId);
				k += 2;
			}
		}
		return groupId;
	}

	public void popGroup(TDoubleList coordinates, TIntList triangles, TIntList beams, int group)
	{
		TObjectIntHashMap<Vertex> map = new TObjectIntHashMap<Vertex>(
			100, 0.5f, Integer.MIN_VALUE);
		int nextVertexId = 0;
		if(triangles != null)
		{
			Iterator<Triangle> it = getTriangles().iterator();
			while(it.hasNext())
			{
				Triangle t = it.next();
				if(t.getGroupId() == group)
				{
					for(int i = 0; i < 3; i++)
					{
						Vertex v = t.getV(i);
						int vid = map.putIfAbsent(v, nextVertexId);
						if(vid == map.getNoEntryValue())
						{
							vid = nextVertexId;
							nextVertexId++;
							coordinates.add(v.getX());
							coordinates.add(v.getY());
							coordinates.add(v.getZ());
						}
						triangles.add(vid);
					}
					it.remove();
				}
			}
		}
		int nb = getBeams().size() / 2;
		if(nb > 0 && beams != null)
		{
			ArrayList<Vertex> newBeams = new ArrayList<Vertex>(this.beams.size());
			TIntArrayList newBeamGroups = new TIntArrayList(beamGroups);
			for(int k = 0; k < nb; k++)
			{
				int g = getBeamGroup(k);
				if(g == group)
				{
					for(int i = 0; i < 2; i++)
					{
						Vertex v = this.beams.get(2 * k + i);
						int vid = map.putIfAbsent(v, nextVertexId);
						if(vid == map.getNoEntryValue())
						{
							vid = nextVertexId;
							nextVertexId++;
							coordinates.add(v.getX());
							coordinates.add(v.getY());
							coordinates.add(v.getZ());
						}
						beams.add(vid);
					}
				}
				else
				{
					newBeamGroups.add(g);
					newBeams.add(this.beams.get(2 * k));
					newBeams.add(this.beams.get(2 * k + 1));
				}
			}
			this.beamGroups = newBeamGroups;
			this.beams = newBeams;
		}
	}

	/**
	 * Removes a triangle from triangle list.
	 *
	 * @param t  triangle being removed.
	 */
	public final void remove(Triangle t)
	{
		triangleList.remove(t);
		if (!(t instanceof TriangleHE))
			return;
		TriangleHE that = (TriangleHE) t;
		// Remove links to help the garbage collector
		HalfEdge e = that.getAbstractHalfEdge();
		HalfEdge last = e;
		for (int i = 0; i < 3; i++)
		{
			e = e.next();
			e.glue(null);
			last.setNext(null);
			last = e;
		}
	}
	
	/**
	 * Returns triangle list.
	 *
	 * @return triangle list.
	 */
	public final Collection<Triangle> getTriangles()
	{
		return triangleList;
	}

	/**
	 * Returns a subset of the triangle list using triangle indexes.
	 *
	 * @param tags  triangle indexes
	 * @return triangle subset. Empty if <code>tags</code> is <code>null</code>,
	 * empty or invalid.
	 */
	public final Collection<Triangle> getTriangles(List<Integer> tags)
	{
		ArrayList<Triangle> triangles = new ArrayList<Triangle>();
		if (tags == null)
			return triangles;

		// sort tags to iterate through linked list
		TreeSet<Integer> tagsSorted = new TreeSet(tags);

		// invalid if index is out of range
		if (tagsSorted.last() >= getTriangles().size())
			return triangles;

		Iterator<Triangle> it = getTriangles().iterator();
		Triangle t = it.next();
		Integer prev = 0;
		for (Integer tag: tagsSorted)
		{
			for (Integer k=0; k<tag-prev; k++)
				t = it.next();
			triangles.add(t);
			prev = tag;
		}
		return triangles;
	}

	/**
	 * Resizes internal collections of vertices and triangles.
	 *
	 * @param triangles  desired number of triangles
	 */
	public final void ensureCapacity(int triangles)
	{
		traitsBuilder.ensureCapacity(triangles, traits);
	}

	/**
	 *  Adds a vertex to vertex list.
	 */
	public final void add(Vertex vertex)
	{
		nodeList.add(vertex);
	}
	
	/**
	 * Removes a vertex from vertex list.
	 *
	 * @param v  vertex being removed.
	 */
	public final void remove(Vertex v)
	{
		nodeList.remove(v);
	}
	
	/**
	 * Returns vertex list.
	 *
	 * @return vertex list.
	 */
	public Collection<Vertex> getNodes()
	{
		return nodeList;
	}

	/**
	 * Tells whether nodes are stored.
	 *
	 * @return <code>true</code> if mesh was created with a <code>MeshTraitsBuilder</code>
	 * instance defining nodes, and <code>false</code> otherwise.
	 */
	public final boolean hasNodes()
	{
		return traitsBuilder.hasNodes();
	}

	/**
	 * Returns the Trace instance associated with this mesh.
	 *
	 * @return the Trace instance associated with this mesh.
	 */
	public final TraceInterface getTrace()
	{
		return traitsBuilder.getTrace(traits);
	}

	/**
	 * Returns the Kd-tree associated with this mesh.
	 *
	 * @return the Kd-tree associated with this mesh.
	 */
	public final KdTree<Vertex> getKdTree()
	{
		return traitsBuilder.getKdTree(traits);
	}
	
	/**
	 * Initializes Kd-tree with a given bounding box.  This method must be called before
	 * putting any vertex into this Kd-tree.
	 *
	 * @param bbmin  coordinates of bottom-left vertex
	 * @param bbmax  coordinates of top-right vertex
	 */
	public final void resetKdTree(double [] bbmin, double [] bbmax)
	{
		double [] bbox = new double[2*bbmin.length];
		for (int i = 0; i < bbmin.length; i++)
		{
			bbox[i] = bbmin[i];
			bbox[i+bbmin.length] = bbmax[i];
		}
		KdTree kdtree = traitsBuilder.getKdTree(traits);
		kdtree.setup(bbox);
	}
	
	/**
	 * Creates a triangle composed of three vertices.
	 *
	 * @param v  array of three vertices
	 * @return a new {@link Triangle} instance composed of three vertices
	 */
	public final Triangle createTriangle(Vertex [] v)
	{
		assert v.length == 3;
		return factory.createTriangle(v[0], v[1], v[2]);
	}

	/**
	 * Creates a triangle composed of four vertices, to emulate a tetrahedron.
	 *
	 * @param v  array of four vertices
	 * @return a new {@link Triangle} instance composed of four vertices
	 */
	public final Triangle createTetrahedron(Vertex [] v)
	{
		assert v.length == 4;
		return new Tetrahedron(v);
	}

	/**
	 * Creates a triangle composed of three vertices.
	 *
	 * @param v0  first vertex
	 * @param v1  second vertex
	 * @param v2  third vertex
	 * @return a new {@link Triangle} instance composed of three vertices
	 */
	public final Triangle createTriangle(Vertex v0, Vertex v1, Vertex v2)
	{
		return factory.createTriangle(v0, v1, v2);
	}

 	/**
	 * Clones a triangle.
	 *
	 * @param that  triangle to clone
	 * @return a new {@link Triangle} instance
	 */
	public final Triangle createTriangle(Triangle that)
	{
		return factory.createTriangle(that);
	}

	/**
	 * Creates a 2D or 3D vertex.
	 *
	 * @param p  coordinates
	 * @return a new {@link Vertex} instance with this location.
	 */
	public final Vertex createVertex(Location p)
	{
		return factory.createVertex(p);
	}

	/**
	 * Creates a 2D vertex.
	 *
	 * @param u  first coordinate
	 * @param v  second coordinate
	 * @return a new {@link Vertex} instance with this location.
	 */
	public final Vertex createVertex(double u, double v)
	{
		return factory.createVertex(u, v);
	}

	/**
	 * Creates a 3D vertex.
	 *
	 * @param x  first coordinate
	 * @param y  second coordinate
	 * @param z  third coordinate
	 * @return a new {@link Vertex} instance with this location.
	 */
	public final Vertex createVertex(double x, double y, double z)
	{
		return factory.createVertex(x, y, z);
	}

	public void addBeam(Vertex v1, Vertex v2, int group)
	{
		addBeam(v1, v2, group, true);
	}

	public void addBeam(Vertex v1, Vertex v2, int group, boolean addVertices)
	{
		beams.add(v1);
		beams.add(v2);
		v1.setMutable(false);
		v2.setMutable(false);
		if (addVertices && hasNodes())
		{
			nodeList.add(v1);
			nodeList.add(v2);
		}
		beamGroups.add(group);
	}

	public List<Vertex> getBeams()
	{
		return Collections.unmodifiableList(beams);
	}

	public void setBeam(int i, Vertex v)
	{
		beams.set(i, v);
	}

	public int getBeamGroup(int i)
	{
		return beamGroups.get(i);
	}

	public void setBeamGroup(int i, int gid)
	{
		beamGroups.set(i, gid);
	}

	public void resetBeams()
	{
		beams.clear();
		beamGroups.clear();
	}

	public void clear() {
		resetBeams();
		triangleList.clear();
		nodeList.clear();
	}

	public void setGroupName(int id, String name)
	{
		if(name == null)
			groupNames.remove(id);
		else
			groupNames.put(id, name);
	}

	public String getGroupName(int id)
	{
		return groupNames.get(id);
	}

	public Collection<String> getGroupNames()
	{
		return groupNames.values();
	}

	public int[] getGroupIDs(String ... names)
	{
		Map<String, Integer> invertMap = new HashMap<String, Integer>();
		for(Entry<Integer, String> e:groupNames.entrySet())
			invertMap.put(e.getValue(), e.getKey());
		int[] groupIds = new int[names.length];
		int i = 0;
		for(String name:names)
			groupIds[i++] = invertMap.get(name);
		return groupIds;
	}

	public void setVertexGroup(Vertex v, String group)
	{
		Collection<Vertex> l = vertexGroups.get(group);
		if(l == null)
		{
			l = new ArrayList<Vertex>();
			vertexGroups.put(group, l);
		}
		l.add(v);
	}

	public Map<String, Collection<Vertex>> getVertexGroup()
	{
		return vertexGroups;
	}

	public int getNumberOfGroups()
	{
		return groupNames.size();
	}

	public boolean hasPersistentReferences()
	{
		return persistentReferences;
	}

	public void setPersistentReferences(boolean persistentReferences)
	{
		this.persistentReferences = persistentReferences;
	}

	/**
	 * Tells whether mesh contains adjacency relations.
	 * @return <code>true</code> if mesh contains adjacency relations,
	 *         <code>false</code> otherwise.
	 */
	public final boolean hasAdjacency()
	{
		return factory.hasAdjacency();
	}

	/**
	 * Build adjacency relations between triangles.
	 */
	public final void buildAdjacency()
	{
		buildAdjacency(0);
	}
	public final void buildAdjacency(int currentMaxLabel)
	{
		//  Connect all edges together
		logger.fine("Connect triangles");
		ArrayList<Triangle> newTri = new ArrayList<Triangle>();
		//  For each vertex, build the list of triangles
		//  connected to this vertex.
		Map<Vertex, Collection<Triangle>> tVertList = getMapVertexLinks();
		//  Connect all edges together
		glueSymmetricHalfEdges(tVertList, newTri);

		//  Mark boundary edges and bind them to virtual triangles.
		logger.fine("Connect boundary triangles");
		connectBoundaryTriangles(newTri);

		//  Fix links for junctions
		logger.fine("Fix vertex links");
		rebuildVertexLinks(tVertList);
		
		//  Find the list of vertices which are on mesh boundary
		logger.fine("Build the list of nodes on boundaries and non-manifold edges");
		LinkedHashSet<Vertex> bndNodes = new LinkedHashSet<Vertex>();
		maxLabel = currentMaxLabel;
		AbstractHalfEdge ot = null;
		for (Triangle t: triangleList)
		{
			ot = t.getAbstractHalfEdge(ot);
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
		logger.fine("Found "+bndNodes.size()+" nodes on boundaries and nonmanifold edges");

		// Set reference on boundary nodes if missing
		for (Vertex v : bndNodes)
		{
			if (0 == v.getRef())
				setRefVertexOnBoundary(v);
		}

		int nrJunctionPoints = 0;
		Collection<Vertex> freeVertices = new HashSet<Vertex>();
		for (Vertex v: tVertList.keySet())
		{
			if (bndNodes.contains(v))
				continue;
			if (v.getLink() instanceof Triangle[])
			{
				nrJunctionPoints++;
				if (v.getRef() == 0)
				{
					maxLabel++;
					v.setRef(maxLabel);
				}
			}
			else if(v.getLink() == null)
				freeVertices.add(v);
		}
		freeVertices.removeAll(beams);
		if (nrJunctionPoints > 0)
			logger.info("Found "+nrJunctionPoints+" junction points");
		if (!freeVertices.isEmpty() && hasNodes())
		{
			logger.log(Level.INFO, "Removed "+freeVertices.size()+" free points");
			nodeList.removeAll(freeVertices);
			freeVertices = null;
		}
		if (maxLabel != currentMaxLabel)
			logger.fine("Created "+(maxLabel - currentMaxLabel)+" more references");
		//  Remove all references to help the garbage collector.
		for (Collection<Triangle> list : tVertList.values())
			list.clear();
		// Add outer triangles
		triangleList.addAll(newTri);
		if (traitsBuilder.hasTrace())
		{
			traitsBuilder.getTrace(traits).println("self.m.buildAdjacency()");
			traitsBuilder.getTrace(traits).addAdjacentTriangles(this);
			traitsBuilder.getTrace(traits).println("# End: self.m.buildAdjacency()");
		}
	}

	private Map<Vertex, Collection<Triangle>> getMapVertexLinks()
	{
		Collection<Vertex> vertices;
		if (nodeList == null)
		{
			vertices = new LinkedHashSet<Vertex>(triangleList.size()/2);
			for (Triangle t: triangleList)
			{
				if (!t.isWritable())
					continue;
				t.addVertexTo(vertices);
			}
		}
		else
		{
			vertices = nodeList;
		}
		Map<Vertex, Collection<Triangle>> tVertList = new LinkedHashMap<Vertex, Collection<Triangle>>(vertices.size());
		for (Vertex v: vertices)
			tVertList.put(v, new ArrayList<Triangle>(10));
		for (Triangle t: triangleList)
		{
			if (t.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			for (int i = 0; i < 3; i++)
			{
				Vertex v = t.getV(i);
				if (v.isReadable())
				{
					Collection<Triangle> list = tVertList.get(v);
					list.add(t);
				}
				v.setLink(t);
			}
 		}
		return tVertList;
	}

	private void rebuildVertexLinks()
	{
		rebuildVertexLinks(getMapVertexLinks());
	}

	public static void rebuildVertexLinks(Map<Vertex, Collection<Triangle>> tVertList)
	{
		for (Map.Entry<Vertex, Collection<Triangle>> entry : tVertList.entrySet())
		{
			Vertex v = entry.getKey();
			Collection<Triangle> list = entry.getValue();
			int cnt = 0;
			AbstractHalfEdge ot = null;
			if (null == v.getLink())
				continue;
			assert v.getLink() instanceof Triangle: v;
			ot = v.getIncidentAbstractHalfEdge((Triangle) v.getLink(), ot);
			Vertex d = ot.destination();
			do
			{
				if (!ot.hasAttributes(AbstractHalfEdge.OUTER))
					cnt++;
				if (ot.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
				{
					cnt = 0;
					break;
				}
				ot = ot.nextOriginLoop();
			}
			while (ot.destination() != d);
			if (cnt == list.size())
				continue;
			// Non-manifold vertex
			LinkedHashSet<Triangle> neighbours = new LinkedHashSet<Triangle>(list);
			ArrayList<Triangle> fans = new ArrayList<Triangle>();
			while (!neighbours.isEmpty())
			{
				ot = v.getIncidentAbstractHalfEdge(neighbours.iterator().next(), ot);
				d = ot.destination();
				fans.add(ot.getTri());
				do
				{
					if (!ot.hasAttributes(AbstractHalfEdge.OUTER))
						neighbours.remove(ot.getTri());
					ot = ot.nextOriginLoop();
				}
				while (ot.destination() != d);
			}
			Triangle[] links = new Triangle[fans.size()];
			fans.toArray(links);
			v.setLink(links);
			logger.fine("Non-manifold vertex has "+fans.size()+" fans");
		}
	}

	private void connectBoundaryTriangles(ArrayList<Triangle> newTri)
	{
		AbstractHalfEdge ot = null;
		AbstractHalfEdge sym = null;
		for (Triangle t: triangleList)
		{
			ot = t.getAbstractHalfEdge(ot);
			for (int i = 0; i < 3; i++)
			{
				ot = ot.next();
				if (!ot.hasSymmetricEdge())
				{
					ot.setAttributes(AbstractHalfEdge.BOUNDARY);
					Triangle adj = factory.createTriangle(outerVertex, ot.destination(), ot.origin());
					newTri.add(adj);
					adj.setAttributes(AbstractHalfEdge.OUTER);
					adj.setReadable(false);
					adj.setWritable(false);
					sym = adj.getAbstractHalfEdge(sym);
					sym.setAttributes(AbstractHalfEdge.BOUNDARY);
					ot.glue(sym);
				}
				else if (ot.hasAttributes(AbstractHalfEdge.OUTER))
				{
					if (sym == null)
						sym = t.getAbstractHalfEdge(sym);
					sym = ot.sym(sym);
					if (!sym.hasAttributes(AbstractHalfEdge.OUTER))
					{
						ot.setAttributes(AbstractHalfEdge.BOUNDARY);
						sym.setAttributes(AbstractHalfEdge.BOUNDARY);
					}
				}
			}
		}
	}

	public void glueSymmetricHalfEdges(Map<Vertex,
		Collection<Triangle>> tVertList, Collection<Triangle> newTri)
	{
		Triangle.List markedTri = new Triangle.List();
		AbstractHalfEdge ot = null;
		AbstractHalfEdge ot2 = null;
		AbstractHalfEdge [] work = new AbstractHalfEdge[3];
		for (Map.Entry<Vertex, Collection<Triangle>> e: tVertList.entrySet())
		{
			//  Mark all triangles having v as vertex
			Vertex v = e.getKey();
			Collection<Triangle> neighTriList = e.getValue();
			if (neighTriList == null)
				continue;
			for (Triangle t: neighTriList)
				markedTri.add(t);
			//  Loop on all edges incident to v
			for (Triangle t: neighTriList)
			{
				ot = v.getIncidentAbstractHalfEdge(t, ot);
				// Skip this edge if adjacency relations already exist, 
				if (ot.hasSymmetricEdge())
					continue;
				Vertex v2 = ot.destination();
				Collection<Triangle> neighTriList2 = tVertList.get(v2);
				if (neighTriList2 == null)
					continue;
				// Edge (v,v2) has not yet been processed.
				// List of triangles incident to v2.
				boolean manifold = true;
				// Ensure that work[0] and work[1] are non null to avoid
				// tests in glueNonManifoldHalfEdges
				if (work[0] == null)
					work[0] = t.getAbstractHalfEdge(work[0]);
				if (work[1] == null)
					work[1] = t.getAbstractHalfEdge(work[1]);
				for (Triangle t2: neighTriList2)
				{
					if (t == t2 || !markedTri.contains(t2))
						continue;
					// t2 contains v and v2, we now look for an edge
					// (v,v2) or (v2,v)
					ot2 = v2.getIncidentAbstractHalfEdge(t2, ot2);
					if (manifold && ot2.destination() == v && !ot.hasSymmetricEdge() && !ot2.hasSymmetricEdge())
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
					glueNonManifoldHalfEdges(v, v2, ot, ot2, work, newTri);
				}
				if (logger.isLoggable(Level.FINE) && !manifold)
				{
					int cnt = 0;
					for (Iterator<AbstractHalfEdge> it = ot.fanIterator(); it.hasNext(); it.next())
						cnt++;
					logger.fine("Non-manifold edge: "+v+" "+v2+" "+" connected to "+cnt+" fans");
				}
			}
			//  Unmark adjacent triangles
			markedTri.clear();
		}
	}

	public void glueNonManifoldHalfEdges(Vertex v, Vertex v2, AbstractHalfEdge ot,
		AbstractHalfEdge ot2, AbstractHalfEdge [] work, Collection<Triangle> newTri)
	{
		assert v == ot.origin() && v2 == ot.destination();
		assert (v == ot2.origin() && v2 == ot2.destination()) || (v2 == ot2.origin() && v == ot2.destination());
		if (!ot.hasSymmetricEdge())
		{
			// ot has no symmetric edge yet; this happens only if this edge has
			// not been processed yet and ot and ot2 have incompatible orientations,
			// i.e. ot2 = (v, v2)
			assert v == ot2.origin() && v2 == ot2.destination(): "Wrong configuration around "+v+" and "+v2;
			// Link ot to a virtual triangle.
			work[0] = bindToVirtualTriangle(ot, work[0]);
			newTri.add(work[0].getTri());
			// Create an empty cycle
			work[1] = work[0].prev(work[1]);
			work[0].next().glue(work[1]);
		}
		else if (!ot.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
		{
			// ot was already linked to another edge, but it is in fact a
			// non-manifold edge.  Previous adjacency relation is removed
			// and both triangles are linked to virtual triangles.
			work[0] = ot.sym(work[0]);
			bindSymEdgesToVirtualTriangles(ot, work[0], work[1], work[2], newTri);
		}

		assert !ot2.hasSymmetricEdge(): ot2+"\n"+ot2.sym();
		// Link ot2 to a virtual triangle
		work[0] = bindToVirtualTriangle(ot2, work[0]);
		newTri.add(work[0].getTri());
		// Add ot2 to existing cycle
		work[0] = ot.sym(work[0]);
		work[0] = work[0].next();
		work[1] = work[0].sym(work[1]);
		ot2 = ot2.sym();
		ot2 = ot2.prev();
		ot2.glue(work[0]);
		ot2 = ot2.prev();
		ot2.glue(work[1]);
	}
	
	private AbstractHalfEdge bindToVirtualTriangle(AbstractHalfEdge ot, AbstractHalfEdge sym)
	{
		Triangle t = factory.createTriangle(outerVertex, ot.destination(), ot.origin());
		t.setAttributes(AbstractHalfEdge.OUTER);
		t.setReadable(false);
		t.setWritable(false);
		sym = t.getAbstractHalfEdge(sym);
		ot.glue(sym);
		ot.setAttributes(AbstractHalfEdge.NONMANIFOLD);
		sym.setAttributes(AbstractHalfEdge.NONMANIFOLD);
		return sym;
	}

	private void bindSymEdgesToVirtualTriangles(AbstractHalfEdge ot, AbstractHalfEdge sym,
		AbstractHalfEdge temp0, AbstractHalfEdge temp1, Collection<Triangle> newTriangles)
	{
		// Link ot to a virtual triangle
		temp0 = bindToVirtualTriangle(ot, temp0);
		newTriangles.add(temp0.getTri());
		// Link sym to another virtual triangle
		temp1 = bindToVirtualTriangle(sym, temp1);
		newTriangles.add(temp1.getTri());
		// Create a cycle
		temp1 = temp1.next();
		temp0 = temp0.prev();
		temp0.glue(temp1);
		temp1 = temp1.next();
		temp0 = temp0.prev();
		temp0.glue(temp1);
	}

	/**
	 * Add {@link AbstractHalfEdge#SHARP} attribute to sharp edges.
	 *
	 * @param coplanarity  when dot product of normals adjacent to an edge is
	 *   lower than this value, it is considered as a ridge and its endpoints
	 *   are treated as if they belong to a CAD edge.
	 */
	public final int buildRidges(double coplanarity)
	{
		int toReturn = 0;
		if (coplanarity < -1.0 || triangleList.isEmpty())
			return toReturn;

		double [][] temp = new double[4][3];

		AbstractHalfEdge ot  = null;
		AbstractHalfEdge sym = triangleList.iterator().next().getAbstractHalfEdge();
		ArrayList<Triangle> newTriangles = new ArrayList<Triangle>();
		AbstractHalfEdge temp0 = null;
		AbstractHalfEdge temp1 = null;

		for (Triangle t: triangleList)
		{
			ot = t.getAbstractHalfEdge(ot);
			if (t.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			for (int i = 0; i < 3; i++)
			{
				ot = ot.next();
				if (ot.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD | AbstractHalfEdge.SHARP))
					continue;
				sym = ot.sym(sym);
				Matrix3D.computeNormal3D(ot.origin(), ot.destination(), ot.apex(), temp[0], temp[1], temp[2]);
				Matrix3D.computeNormal3D(ot.destination(), ot.origin(), sym.apex(), temp[0], temp[1], temp[3]);
				if (Matrix3D.prodSca(temp[2], temp[3]) <= coplanarity)
				{
					ot.setAttributes(AbstractHalfEdge.SHARP);
					sym.setAttributes(AbstractHalfEdge.SHARP);
					if (ot.origin().getRef() == 0)
						setRefVertexOnInnerBoundary(ot.origin());
					if (ot.destination().getRef() == 0)
						setRefVertexOnInnerBoundary(ot.destination());
					bindSymEdgesToVirtualTriangles(ot, sym, temp0, temp1, newTriangles);
				}
			}
		}
		toReturn = newTriangles.size() / 2;
		triangleList.addAll(newTriangles);
		rebuildVertexLinks();
		if (toReturn > 0 && logger.isLoggable(Level.CONFIG))
			logger.log(Level.CONFIG, "Add virtual boundaries for "+toReturn+" sharp edges");
		if (traitsBuilder.hasTrace())
			traitsBuilder.getTrace(traits).println("self.m.buildRidges("+coplanarity+")");
		return toReturn;
	}

	public void createRidgesGroup(String groupName)
	{
		int ridgeGroup = 0;
		if (!beamGroups.isEmpty())
			ridgeGroup = beamGroups.max() + 1;
		boolean found = false;
		for (Map.Entry<Integer, String> entry : groupNames.entrySet())
		{
			if (entry.getValue().compareTo(groupName) == 0)
			{
				ridgeGroup = entry.getKey();
				found = true;
				break;
			}
		}
		if (found)
		{
			// FIXME: We cannot remove previous ridges from beams and
			//        beamGroups, so clear them.
			beams.clear();
			beamGroups.clear();
		}
		else
		{
			groupNames.put(ridgeGroup, groupName);
		}
		AbstractHalfEdge ot  = null;

		for (Triangle t: triangleList)
		{
			ot = t.getAbstractHalfEdge(ot);
			if (t.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			for (int i = 0; i < 3; i++)
			{
				ot = ot.next();
				if (ot.hasAttributes(AbstractHalfEdge.SHARP))
				{
					beams.add(ot.origin());
					beams.add(ot.destination());
					beamGroups.add(ridgeGroup);
				}
			}
		}
	}

	/**
	 * Build group boundaries.
	 */
	public final int buildGroupBoundaries()
	{
		return buildGroupBoundaries(null);
	}

	public final int buildGroupBoundaries(int[] groups)
	{
		if (triangleList.isEmpty())
			return 0;

		TIntHashSet groupSet = new TIntHashSet();
		if (null != groups)
			groupSet.addAll(groups);

		ArrayList<Triangle> newTriangles = new ArrayList<Triangle>();
		AbstractHalfEdge ot    = null;
		AbstractHalfEdge sym   = triangleList.iterator().next().getAbstractHalfEdge();
		AbstractHalfEdge temp0 = null;
		AbstractHalfEdge temp1 = null;

		for (Triangle t: triangleList)
		{
			ot = t.getAbstractHalfEdge(ot);
			if (t.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			int groupId = t.getGroupId();
			if (!groupSet.isEmpty() && !groupSet.contains(groupId))
				continue;
			for (int i = 0; i < 3; i++)
			{
				ot = ot.next();
				if (ot.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD))
					continue;
				sym = ot.sym(sym);
				int symGroupId = sym.getTri().getGroupId();
				if (groupId != symGroupId && (groupSet.isEmpty() || groupSet.contains(groupId)))
				{
					bindSymEdgesToVirtualTriangles(ot, sym, temp0, temp1, newTriangles);
					if (0 == ot.origin().getRef())
						setRefVertexOnBoundary(ot.origin());
					if (0 == ot.destination().getRef())
						setRefVertexOnBoundary(ot.destination());
				}
			}
		}
		int toReturn = newTriangles.size() / 2;
		triangleList.addAll(newTriangles);
		if (toReturn > 0 && logger.isLoggable(Level.CONFIG))
			logger.log(Level.CONFIG, "Add virtual boundaries for "+toReturn+" edges");
		rebuildVertexLinks();
		if (traitsBuilder.hasTrace())
		{
			traitsBuilder.getTrace(traits).println("groups = []");
			if (null != groups)
			{
				for (int i : groups)
					traitsBuilder.getTrace(traits).println("groups.append("+i+")");
			}
			traitsBuilder.getTrace(traits).println("self.m.buildGroupBoundaries(groups)");
			traitsBuilder.getTrace(traits).addAdjacentTriangles(this);
			traitsBuilder.getTrace(traits).println("# End: self.m.buildGroupBoundaries()");
		}
		return toReturn;
	}

	// This routine can be called when inverting triangles to have consistent normals
	final int scratchVirtualBoundaries()
	{
		if (triangleList.isEmpty())
			return 0;
		
		ArrayList<Triangle> removedTriangles = new ArrayList<Triangle>();
		AbstractHalfEdge ot    = null;
		AbstractHalfEdge sym   = triangleList.iterator().next().getAbstractHalfEdge();
		AbstractHalfEdge temp0 = null;
		AbstractHalfEdge temp1 = null;

		for (Triangle t: triangleList)
		{
			ot = t.getAbstractHalfEdge(ot);
			if (t.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			for (int i = 0; i < 3; i++)
			{
				ot = ot.next();
				if (!ot.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
					continue;

				Iterator<AbstractHalfEdge> it = ot.fanIterator();
				if (!it.hasNext())
					continue;
				temp0 = it.next();
				if (!it.hasNext())
					continue;
				temp1 = it.next();
				// If there are more than 2 fans, this edge is really
				// non-manifold
				if (it.hasNext())
					continue;
				if (temp0.origin() != temp1.destination() || temp0.destination() != temp1.origin())
					continue;
				// Remove inner boundary
				sym = temp0.sym(sym);
				removedTriangles.add(sym.getTri());
				sym = temp1.sym(sym);
				removedTriangles.add(sym.getTri());
				temp0.glue(temp1);
				ot.clearAttributes(AbstractHalfEdge.NONMANIFOLD);
				sym.clearAttributes(AbstractHalfEdge.NONMANIFOLD);
			}
		}
		if (removedTriangles.isEmpty())
			return 0;
		int toReturn = removedTriangles.size() / 2;
		if (triangleList instanceof Set)
			triangleList.removeAll(removedTriangles);
		else
		{
			// removeAll may be very slow on large lists
			ArrayList<Triangle> savedList = new ArrayList<Triangle>(triangleList);
			Set<Triangle> removedSet = HashFactory.createSet(removedTriangles);
			triangleList.clear();
			for (Triangle t : savedList)
			{
				if (!removedSet.contains(t))
					triangleList.add(t);
			}
		}
		if (logger.isLoggable(Level.CONFIG))
			logger.log(Level.CONFIG, "Remove virtual boundaries for "+toReturn+" edges");
		// Rebuild list of vertex links
		rebuildVertexLinks();
		// Make vertex manifold
		for (Triangle t : removedTriangles)
		{
			ot = t.getAbstractHalfEdge(ot);
			Vertex o = ot.origin();
			if (o.isManifold())
				continue;
			Triangle [] list = (Triangle[]) o.getLink();
			if (list.length == 1)
			{
				// Check that there is no non-manifold
				// incident edge
				Vertex d = ot.destination();
				boolean manifold = true;
				do
				{
					if (ot.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
					{
						manifold = false;
						break;
					}
					ot = ot.nextOriginLoop();
				}
				while (ot.destination() != d);
				if (manifold)
					o.setLink(list[0]);
			}
		}
		return toReturn;
	}

	/**
	 * }
	 * Sets an unused boundary reference on a vertex.
	 */
	public final void setRefVertexOnBoundary(Vertex v)
	{
		maxLabel++;
		v.setRef(maxLabel);
	}

	/**
	 * }
	 * Sets an unused boundary reference on a vertex.
	 */
	public final void setRefVertexOnInnerBoundary(Vertex v)
	{
		maxLabel++;
		v.setRef(-maxLabel);
	}

	/** Tag Vertices and HalfEdges which are in a give list of groups */
	public final void tagGroups(Collection<String> names, int attr)
	{
		boolean fixVertex  = (attr & AbstractHalfEdge.IMMUTABLE) != 0;
		if(fixVertex)
		{
			//node groups
			for(String gn:names)
			{
				Collection<Vertex> l = vertexGroups.get(gn);
				if(l != null)
					for(Vertex v:l)
						v.setMutable(false);
			}
		}

		//triangle groups
		if (hasAdjacency() && !triangleList.isEmpty())
		{
			names = new HashSet<String>(names);
			TIntHashSet groupIds = new TIntHashSet(names.size());
			for(Entry<Integer, String> e:groupNames.entrySet())
			{
				if(names.contains(e.getValue()))
					groupIds.add(e.getKey());
			}

			for (Triangle t: triangleList)
			{
				AbstractHalfEdge ot = t.getAbstractHalfEdge();
				if (t.hasAttributes(AbstractHalfEdge.OUTER))
					continue;
				if(groupIds.contains(t.getGroupId()))
				{
					for (int i = 0; i < 3; i++)
					{
						ot.setAttributes(attr);
						if (fixVertex)
						{
							if(ot.hasAttributes(AbstractHalfEdge.NONMANIFOLD)) {
								for (Iterator<AbstractHalfEdge> it = ot.fanIterator(); it.hasNext(); )
									it.next().setAttributes(attr);
							} else {
								ot.sym().setAttributes(attr);
							}
							ot.origin().setMutable(false);
							ot.destination().setMutable(false);
						}
						ot = ot.next();
					}
				}
			}
		}
	}
	/**
	 * Tag all edges on group boundaries with a given attribute.
	 */
	public final int tagGroupBoundaries(int attr)
	{
		if (triangleList.isEmpty())
			return 0;

		if (!hasAdjacency())
			throw new RuntimeException("tagGroupBoundaries called on a mesh without adjacency relations");

		AbstractHalfEdge ot  = null;
		AbstractHalfEdge sym = triangleList.iterator().next().getAbstractHalfEdge();
		boolean fixVertex  = (attr & AbstractHalfEdge.IMMUTABLE) != 0;

		int toReturn = 0;
		for (Triangle t: triangleList)
		{
			ot = t.getAbstractHalfEdge(ot);
			if (t.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			int groupId = t.getGroupId();
			for (int i = 0; i < 3; i++)
			{
				ot = ot.next();
				if (ot.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD))
					continue;
				sym = ot.sym(sym);
				if (groupId != sym.getTri().getGroupId())
				{
					ot.setAttributes(attr);
					if (fixVertex)
					{
						ot.origin().setMutable(false);
						ot.destination().setMutable(false);
					}
					toReturn++;
				}
			}
		}
		toReturn /= 2;
		if (toReturn > 0 && logger.isLoggable(Level.CONFIG))
			logger.log(Level.CONFIG, "Number of edges on group boundaries: "+toReturn);
		return toReturn;
	}

	/**
	 * Tag all free edges with a given attribute.
	 */
	public final int tagFreeEdges(int attr)
	{
		return tagIf(AbstractHalfEdge.BOUNDARY, attr);
	}

	public final int tagIf(int criterion, int attr)
	{
		if (triangleList.isEmpty())
			return 0;

		if (!hasAdjacency())
			throw new RuntimeException("tagIf called on a mesh without adjacency relations");

		AbstractHalfEdge ot = null;
		boolean pinVertices  = (attr & AbstractHalfEdge.IMMUTABLE) != 0;

		int toReturn = 0;
		for (Triangle t: triangleList)
		{
			ot = t.getAbstractHalfEdge(ot);
			if (t.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			for (int i = 0; i < 3; i++)
			{
				ot = ot.next();
				if (ot.hasAttributes(criterion))
				{
					ot.setAttributes(attr);
					if (pinVertices)
					{
						ot.origin().setMutable(false);
						ot.destination().setMutable(false);
					}
					toReturn++;
				}
			}
		}
		if (toReturn > 0 && logger.isLoggable(Level.CONFIG))
			logger.log(Level.CONFIG, "Number of tagged edges: "+toReturn);
		return toReturn;
	}

	public Metric getMetric(Location pt)
	{
		return euclidian_metric3d;
	}

	/**
	 * Returns square distance between 2 vertices.
	 */
	public final double distance2(double [] x1, double [] x2)
	{
		assert x1.length == 3;
		double dx = x1[0] - x2[0];
		double dy = x1[1] - x2[1];
		double dz = x1[2] - x2[2];
		return dx*dx + dy*dy + dz*dz;
	}

	/**
	 * Perform an automatic partitioning based on festure edges.
	 *
	 * @return number of partitions.
	 */
	public final int buildPartition()
	{
		int countPart = 0;
		if (!hasAdjacency())
		{
			logger.severe("Mesh data structure does not contain adjacency relations");
			return countPart;
		}

		Set<Triangle> unprocessedTriangles = new LinkedHashSet<Triangle>(triangleList);
		Set<Triangle> partTriangles = new LinkedHashSet<Triangle>();
		Triangle.List processedTriangles = new Triangle.List();

		if (unprocessedTriangles.isEmpty())
			return countPart;

		for (Triangle t : triangleList)
			t.setGroupId(0);

		TIntIntHashMap countTrianglesByPart = new TIntIntHashMap();
		AbstractHalfEdge ot  = unprocessedTriangles.iterator().next().getAbstractHalfEdge();
		AbstractHalfEdge sym = unprocessedTriangles.iterator().next().getAbstractHalfEdge();

		while(!unprocessedTriangles.isEmpty())
		{
			Triangle t1 = unprocessedTriangles.iterator().next();
			unprocessedTriangles.remove(t1);
			if (t1.hasAttributes(AbstractHalfEdge.OUTER))
				continue;

			partTriangles.clear();
			partTriangles.add(t1);
			countPart++;
			countTrianglesByPart.put(countPart, 0);
			while(!partTriangles.isEmpty())
			{
				Triangle t2 = partTriangles.iterator().next();
				partTriangles.remove(t2);
				if (t2.getGroupId() != 0 && countPart != t2.getGroupId())
				{
					logger.severe("Unexpected error");
				}
				if (processedTriangles.contains(t2))
					continue;

				t2.setGroupId(countPart);
				countTrianglesByPart.increment(countPart);
				unprocessedTriangles.remove(t2);
				processedTriangles.add(t2);

				ot = t2.getAbstractHalfEdge(ot);
				if (t2.hasAttributes(AbstractHalfEdge.OUTER))
					continue;
				for (int i = 0; i < 3; i++)
				{
					ot = ot.next();
					if (!(ot.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD | AbstractHalfEdge.SHARP)))
					{
						sym = ot.sym(sym);
						Triangle symTri = sym.getTri();
						if (!processedTriangles.contains(symTri))
							partTriangles.add(symTri);
					}
				}
			}
		}
		if (countPart > 0 && logger.isLoggable(Level.INFO))
		{
			logger.log(Level.INFO, "Found "+countPart+" part components");
			if (logger.isLoggable(Level.CONFIG))
			{
				for (int key : countTrianglesByPart.keys())
					logger.log(Level.CONFIG, " * Part id "+key+": "+countTrianglesByPart.get(key)+" triangles");
			}
		}

		if (traitsBuilder.hasTrace())
		{
			traitsBuilder.getTrace(traits).println("self.m.buildPartition()");
			traitsBuilder.getTrace(traits).addAdjacentTriangles(this);
			traitsBuilder.getTrace(traits).println("# End: self.m.buildPartition()");
		}

		return countPart;
	}

	/**
	 * Checks whether an edge can be contracted.
	 *
	 * @param e   edge to be checked
	 * @param v   the resulting vertex
	 * @return <code>true</code> if this edge can be contracted into the single vertex n, <code>false</code> otherwise.
	 */
	public final boolean canCollapseEdge(AbstractHalfEdge e, Location v)
	{
		return e.canCollapse(this, v);
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
	public final AbstractHalfEdge edgeCollapse(AbstractHalfEdge e, Vertex v)
	{
		if (traitsBuilder.hasTrace())
			traitsBuilder.getTrace(traits).edgeCollapse(e, v);
		return e.collapse(this, v);
	}
	
	/**
	 * Splits an edge.  This is the opposite of {@link #edgeCollapse}.
	 *
	 * @param e   edge being splitted
	 * @param v   the resulting vertex
	 */
	public final AbstractHalfEdge vertexSplit(AbstractHalfEdge e, Vertex v)
	{
		if (traitsBuilder.hasTrace())
			traitsBuilder.getTrace(traits).vertexSplitBefore(e, v);
		AbstractHalfEdge ret = e.split(this, v);
		if (traitsBuilder.hasTrace())
			traitsBuilder.getTrace(traits).vertexSplitAfter(ret, v);
		return ret;
	}
	
	/**
	 * Swaps an edge.
	 *
	 * @return swapped edge, origin and apical vertices are the same as in original edge
	 * @throws IllegalArgumentException if edge is on a boundary or belongs
	 * to an outer triangle.
	 */
	public final AbstractHalfEdge edgeSwap(AbstractHalfEdge e)
	{
		if (traitsBuilder.hasTrace())
			traitsBuilder.getTrace(traits).edgeSwap(e);
		return e.swap(this);
	}

	/**
	 * Checks whether origin point can be moved without introducing degenerated triangles.
	 *
	 * @param e   edge to be checked
	 * @param v   the destination vertex
	 * @return <code>true</code> if origin of this edge can be moved, false otherwise.
	 */
	public final boolean canMoveOrigin(AbstractHalfEdge e, Location pt)
	{
		return e.canMoveOrigin(this, pt);
	}

	/**
	 * Checks whether origin of an edge can be moved without inverting triangles.
	 *
	 * @param e   edge to be checked
	 * @param pt  coordinates where edge origin is to be moved
	 * @return <code>true</code> if edge origin can be moved without producing
	 *         inverted triangles, <code>false</code> otherwise.
	 */
	public final boolean checkNewRingNormals(AbstractHalfEdge e, Location pt)
	{
		return e.checkNewRingNormals(this, pt);
	}

	/**
	 * Checks whether this mesh is valid.
	 * This routine returns <code>isValid(true)</code>.
	 *
	 * @see #isValid(boolean)
	 */
	public final boolean isValid()
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
	protected boolean isValid(boolean constrained)
	{
		for (Triangle t: triangleList)
		{
			if (t.getV0() == t.getV1() || t.getV1() == t.getV2() || t.getV2() == t.getV0())
			{
				logger.severe("Duplicate vertices: "+t);
				return false;
			}
			if (t.getV0() == outerVertex || t.getV1() == outerVertex || t.getV2() == outerVertex)
			{
				if (constrained && factory.hasAdjacency())
				{
					if (!t.hasAttributes(AbstractHalfEdge.OUTER))
					{
						logger.severe("Triangle should be outer: "+t);
						return false;
					}
				}
			}
			for (int i = 0; i < 3; i++)
			{
				Vertex v = t.getV(i);
				if (v.getLink() == null)
					continue;
				if (v.isManifold())
				{
					Triangle t2 = (Triangle) v.getLink();
					if (t2.getV0() != v && t2.getV1() != v && t2.getV2() != v)
					{
						logger.severe("Vertex "+v+" linked to "+t2);
						return false;
					}
					if (!triangleList.contains(t2))
					{
						logger.severe("Vertex "+v+" linked to removed triangle: "+t2);
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
							logger.severe("Vertex "+v+" linked to removed triangle: "+t2);
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

	private boolean checkVirtualHalfEdges(Triangle t)
	{
		if (!traitsBuilder.getTriangleTraitsBuilder().hasCapability(TriangleTraitsBuilder.VIRTUALHALFEDGE))
			return true;
		VirtualHalfEdge ot = new VirtualHalfEdge();
		VirtualHalfEdge sym = new VirtualHalfEdge();
		ot.bind((TriangleVH) t);
		boolean isOuter = ot.hasAttributes(AbstractHalfEdge.OUTER);
		for (int i = 0; i < 3; i++)
		{
			ot.next();
			if (isOuter != ot.hasAttributes(AbstractHalfEdge.OUTER))
			{
				logger.severe("Inconsistent outer state: "+ot);
				return false;
			}
			if (!ot.hasSymmetricEdge())
				continue;
			sym = ot.sym(sym);
			sym.sym();
			if (sym.getTri() != ot.getTri())
			{
				logger.severe("Wrong adjacency relation: ");
				logger.severe(" adj1: "+ot);
				logger.severe(" adj2: "+sym);
				return false;
			}
			sym.sym();
			if (ot.hasAttributes(AbstractHalfEdge.BOUNDARY) != sym.hasAttributes(AbstractHalfEdge.BOUNDARY) || ot.hasAttributes(AbstractHalfEdge.NONMANIFOLD) != sym.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
			{
				logger.severe("Inconsistent boundary or nonmanifold flags");
				logger.severe(" "+ot);
				logger.severe(" "+sym);
				return false;
			}
			if (ot.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD) && sym.hasAttributes(AbstractHalfEdge.OUTER) != !isOuter)
			{
				logger.severe("Inconsistent outer flags");
				logger.severe(" "+ot);
				logger.severe(" "+sym);
				return false;
			}
			if (!triangleList.contains(sym.getTri()))
			{
				logger.severe("Triangle not present in mesh: "+sym.getTri());
				logger.severe("Linked from "+ot);
				return false;
			}
			Vertex v1 = ot.origin();
			Vertex v2 = ot.destination();
			if (!isOuter)
			{
				if (sym.origin() != v2 || sym.destination() != v1)
				{
					logger.severe("Vertex mismatch in adjacency relation: ");
					logger.severe(" "+ot);
					logger.severe(" "+sym);
					return false;
				}
				continue;
			}
			// triangle is outer
			if (ot.hasAttributes(AbstractHalfEdge.BOUNDARY) && !outerTrianglesAreConnected)
			{
				// Edge is manifold
				// next() and prev() must not be linked to other edges
				ot.next();
				if (ot.hasSymmetricEdge())
				{
					logger.severe("Outer edge: should not be linked to another edge: "+ot);
					return false;
				}
				ot.next();
				if (ot.hasSymmetricEdge())
				{
					logger.severe("Outer edge: should not be linked to another edge: "+ot);
					return false;
				}
				ot.next();
			}
			else if (ot.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
			{
				if (v1.isManifold())
				{
					logger.severe("Multiple edges: endpoint must be non-manifold: "+v1);
					logger.severe(" "+ot);
					return false;
				}
				if (v2.isManifold())
				{
					logger.severe("Multiple edges: endpoint must be non-manifold: "+v2);
					logger.severe(" "+ot);
					return false;
				}
				// next() and prev() must point to other non-manifold edges
				ot.next();
				if (!ot.hasSymmetricEdge())
				{
					logger.severe("Multiple edge: must be linked to another edge: "+ot);
					return false;
				}
				VirtualHalfEdge.symOTri(ot, sym);
				sym.next();
				if (!sym.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
				{
					logger.severe("Multiple edges: linked to a non-manifold edge");
					logger.severe(" "+ot);
					logger.severe(" "+sym);
					return false;
				}
				if (!ot.hasAttributes(AbstractHalfEdge.OUTER) || !sym.hasAttributes(AbstractHalfEdge.OUTER))
				{
					logger.severe("Multiple edges: linked to an inner edge");
					logger.severe(" "+ot);
					logger.severe(" "+sym);
					return false;
				}
				if (!triangleList.contains(sym.getTri()))
				{
					logger.severe("Multiple edges: Triangle not present in mesh: "+sym.getTri());
					logger.severe("Linked from "+this);
					return false;
				}
				if (!((sym.origin() == v1 && sym.destination() == v2) || (sym.origin() == v2 && sym.destination() == v1)))
				{
					logger.severe("Multiple edges: vertex mismatch in adjacency relation: ");
					logger.severe(" "+ot);
					logger.severe(" "+sym);
					return false;
				}
				ot.next();
				if (!ot.hasSymmetricEdge())
				{
					logger.severe("Multiple edge: must be linked to another edge: "+ot);
					return false;
				}
				VirtualHalfEdge.symOTri(ot, sym);
				sym.prev();
				if (!sym.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
				{
					logger.severe("Multiple edges: linked to a non-manifold edge");
					logger.severe(" "+ot);
					logger.severe(" "+sym);
					return false;
				}
				if (!ot.hasAttributes(AbstractHalfEdge.OUTER) || !sym.hasAttributes(AbstractHalfEdge.OUTER))
				{
					logger.severe("Multiple edges: linked to an inner edge");
					logger.severe(" "+ot);
					logger.severe(" "+sym);
					return false;
				}
				if (!triangleList.contains(sym.getTri()))
				{
					logger.severe("Multiple edges: Triangle not present in mesh: "+sym.getTri());
					logger.severe("Linked from "+this);
					return false;
				}
				if (!((sym.origin() == v1 && sym.destination() == v2) || (sym.origin() == v2 && sym.destination() == v1)))
				{
					logger.severe("Multiple edges: vertex mismatch in adjacency relation: ");
					logger.severe(" "+ot);
					logger.severe(" "+sym);
					return false;
				}
			}
		}
		return true;
	}

	private boolean checkHalfEdges(Triangle t)
	{
		if (!traitsBuilder.getTriangleTraitsBuilder().hasCapability(TriangleTraitsBuilder.HALFEDGE))
			return true;
		HalfEdge e = (HalfEdge) t.getAbstractHalfEdge();
		boolean isOuter = e.hasAttributes(AbstractHalfEdge.OUTER);
		for (int i = 0; i < 3; i++)
		{
			e = e.next();
			if (isOuter != e.hasAttributes(AbstractHalfEdge.OUTER))
			{
				logger.severe("Inconsistent outer state: "+e);
				return false;
			}
			if (!e.hasSymmetricEdge())
				continue;
			HalfEdge f = e.sym();
			if (f.sym() != e)
			{
				logger.severe("Wrong adjacency relation: ");
				logger.severe(" adj1: "+e);
				logger.severe(" adj2: "+f);
				return false;
			}
			if (f.hasAttributes(AbstractHalfEdge.BOUNDARY) != e.hasAttributes(AbstractHalfEdge.BOUNDARY) || f.hasAttributes(AbstractHalfEdge.NONMANIFOLD) != e.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
			{
				logger.severe("Inconsistent boundary or nonmanifold flags");
				logger.severe(" "+e);
				logger.severe(" "+f);
				return false;
			}
			if (e.hasAttributes(AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD) && f.hasAttributes(AbstractHalfEdge.OUTER) != !isOuter)
			{
				logger.severe("Inconsistent outer flags");
				logger.severe(" "+e);
				logger.severe(" "+f);
				return false;
			}
			if (!triangleList.contains(f.getTri()))
			{
				logger.severe("Triangle not present in mesh: "+f.getTri());
				logger.severe("Linked from "+e);
				return false;
			}
			Vertex v1 = e.origin();
			Vertex v2 = e.destination();
			if (!isOuter)
			{
				if (f.origin() != v2 || f.destination() != v1)
				{
					logger.severe("Vertex mismatch in adjacency relation: ");
					logger.severe(" "+e);
					logger.severe(" "+f);
					return false;
				}
				continue;
			}
			// triangle is outer
			if (e.hasAttributes(AbstractHalfEdge.BOUNDARY) && !outerTrianglesAreConnected)
			{
				// Edge e is manifold
				// next() and prev() must not be linked to other edges
				AbstractHalfEdge g = e.next();
				if (g.hasSymmetricEdge())
				{
					logger.severe("Outer edge: should not be linked to another edge: "+g);
					return false;
				}
				g = e.prev();
				if (g.hasSymmetricEdge())
				{
					logger.severe("Outer edge: should not be linked to another edge: "+g);
					return false;
				}
			}
			else if (e.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
			{
				if (v1.isManifold())
				{
					logger.severe("Multiple edges: endpoint must be non-manifold: "+v1);
					logger.severe(" "+e);
					return false;
				}
				if (v2.isManifold())
				{
					logger.severe("Multiple edges: endpoint must be non-manifold: "+v2);
					logger.severe(" "+e);
					return false;
				}
				// next() and prev() must point to other non-manifold edges
				AbstractHalfEdge g = e.next();
				if (!g.hasSymmetricEdge())
				{
					logger.severe("Multiple edge: must be linked to another edge: "+g);
					return false;
				}
				AbstractHalfEdge h = e.prev();
				if (!h.hasSymmetricEdge())
				{
					logger.severe("Multiple edge: must be linked to another edge: "+h);
					return false;
				}
				g = g.sym().next();
				h = h.sym().prev();
				if (!g.hasAttributes(AbstractHalfEdge.NONMANIFOLD) || !h.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
				{
					logger.severe("Multiple edges: linked to a non-manifold edge");
					logger.severe(" "+f);
					logger.severe(" "+g);
					logger.severe(" "+h);
					return false;
				}
				if (!g.hasAttributes(AbstractHalfEdge.OUTER) || !h.hasAttributes(AbstractHalfEdge.OUTER))
				{
					logger.severe("Multiple edges: linked to an inner edge");
					logger.severe(" "+f);
					logger.severe(" "+g);
					logger.severe(" "+h);
					return false;
				}
				if (!triangleList.contains(g.getTri()))
				{
					logger.severe("Multiple edges: Triangle not present in mesh: "+g.getTri());
					logger.severe("Linked from "+f);
					return false;
				}
				if (!triangleList.contains(h.getTri()))
				{
					logger.severe("Multiple edges: Triangle not present in mesh: "+h.getTri());
					logger.severe("Linked from "+f);
					return false;
				}
				if (!((g.origin() == v1 && g.destination() == v2) || (g.origin() == v2 && g.destination() == v1)))
				{
					logger.severe("Multiple edges: vertex mismatch in adjacency relation: ");
					logger.severe(" "+e);
					logger.severe(" "+g);
					return false;
				}
				if (!((h.origin() == v1 && h.destination() == v2) || (h.origin() == v2 && h.destination() == v1)))
				{
					logger.severe("Multiple edges: vertex mismatch in adjacency relation: ");
					logger.severe(" "+e);
					logger.severe(" "+h);
					return false;
				}
				for (Iterator<AbstractHalfEdge> it = e.fanIterator(); it.hasNext(); )
				{
					h = it.next();
					if (h.hasAttributes(AbstractHalfEdge.OUTER))
					{
						logger.severe("Multiple edges: fan iterator error: ");
						logger.severe(" "+e);
						logger.severe(" "+h);
						return false;
					}
				}
			}
		}
		return true;
	}
	
	public final boolean checkNoInvertedTriangles()
	{
		AbstractHalfEdge ot = null;
		AbstractHalfEdge sym = null;
		double [][] temp = new double[4][3];
		for (Triangle t : triangleList)
		{
			if (t.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			ot = t.getAbstractHalfEdge(ot);
			sym = t.getAbstractHalfEdge(sym);
			for (int i = 0; i < 3; i++)
			{
				ot = ot.next();
				if (ot.hasAttributes(AbstractHalfEdge.SHARP | AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD))
					continue;
				if (!ot.hasSymmetricEdge())
					continue;
				sym = ot.sym(sym);
				Vertex o = ot.origin();
				Vertex d = ot.destination();
				Vertex a = ot.apex();
				Vertex n = sym.apex();
				Matrix3D.computeNormal3D(o, d, a, temp[0], temp[1], temp[2]);
				Matrix3D.computeNormal3D(d, o, n, temp[0], temp[1], temp[3]);
				if (Matrix3D.prodSca(temp[2], temp[3]) < -0.6)
				{
					System.err.println("ERROR: dot product of normals of triangles below is: "+Matrix3D.prodSca(temp[2], temp[3]));
					System.err.println("T1: "+t);
					System.err.println("group name: "+getGroupName(t.getGroupId()));
					System.err.println("T2: "+sym.getTri());
					System.err.println("group name: "+getGroupName(sym.getTri().getGroupId()));
					return false;
				}
			}
		}
		return true;
	}

	public final boolean checkNoDegeneratedTriangles()
	{
		for (Triangle t : triangleList)
		{
			if (t.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			Vertex t0 = t.getV0();
			Vertex t1 = t.getV1();
			Vertex t2 = t.getV2();
			double a = t.getV0().sqrDistance3D(t.getV1());
			double c = t.getV0().sqrDistance3D(t.getV2());
			double b =
				(t1.getX() - t0.getX()) * (t2.getX() - t0.getX()) +
				(t1.getY() - t0.getY()) * (t2.getY() - t0.getY()) +
				(t1.getZ() - t0.getZ()) * (t2.getZ() - t0.getZ());
			if (a*c == b*b)
			{
				System.err.println("ERROR: degenerated triangle: "+t);
				System.err.println("group name: "+getGroupName(t.getGroupId()));
				return false;
			}
		}
		return true;
	}

	public final boolean checkVertexLinks()
	{
		AbstractHalfEdge ot = null;
		if (nodeList == null)
			return true;
		for (Vertex v : nodeList)
		{
			if (v.isManifold())
				continue;
			Triangle [] links = (Triangle []) v.getLink();
			// Fail gracefully if called before buildAdjacency()
			if (links == null)
				continue;
			HashSet triangles = new HashSet();
			for (Triangle t : links)
				triangles.add(t);
			for (Triangle t : links)
			{
				ot = t.getAbstractHalfEdge(ot);
				if (ot.destination() == v)
					ot = ot.next();
				else if (ot.apex() == v)
					ot = ot.next().next();
				Vertex d = ot.destination();
				do
				{
					if (ot.getTri() != t && triangles.contains(ot.getTri()))
					{
						System.err.println("ERROR: Triangles on the same fan found for vertex "+v);
						System.err.println(" Triangle "+t);
						System.err.println(" Triangle "+ot.getTri());
						return false;
					}
					ot = ot.nextOriginLoop();
				}
				while (ot.destination() != d);
			}
		}
		return true;
	}

	public Iterable<Vertex> getManifoldVertices()
	{
		return new Iterable<Vertex>(){
			public Iterator<Vertex> iterator() {
				return new Iterator<Vertex>(){
					private Iterator<Vertex> delegateIt = nodeList.iterator();
					private Vertex next;
					private void nextImpl()
					{
						if(next == null)
						{
							while(delegateIt.hasNext())
							{
								next = delegateIt.next();
								if(next.isManifold())
									break;
							}
						}
					}
					public boolean hasNext() {
						nextImpl();
						return next != null;
					}

					public Vertex next() {
						nextImpl();
						if(next == null)
							throw new NoSuchElementException();
						Vertex toReturn = next;
						next = null;
						return toReturn;
					}

					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}
		};
	}

	public int getNumberOfTrianglesByGroup(int group)
	{
		int n = 0;
		for(Triangle t:getTriangles())
		{
			if(t.getGroupId() == group)
				n ++;
		}
		return n;
	}

	public void checkVerticesRef()
	{
		for(Triangle t:getTriangles())
		{
			for(int i = 0; i < 3; i++)
			{
				Vertex v = t.getV(i);
				Iterator<AbstractHalfEdge> it = v.getNeighbourIteratorAbstractHalfEdge();
				boolean nonManifold = false;
				while(it.hasNext())
				{
					AbstractHalfEdge e = it.next();
					nonManifold = nonManifold || e.hasAttributes(
						AbstractHalfEdge.BOUNDARY |
						AbstractHalfEdge.NONMANIFOLD |
						AbstractHalfEdge.SHARP);
				}
				if((v.getRef() == 0) == nonManifold)
					throw new IllegalStateException("Invalid ref "+v.getRef()+
						" for "+v+". Manifold="+!nonManifold);
			}
		}
	}

	/**
	 * 
	 * @param bounds {xmin, ymin, zmin, xmax, ymax, zmax}
	 */
	public void getBoundingBox(double[] bounds)
	{
		bounds[0] = Double.POSITIVE_INFINITY;
		bounds[1] = Double.POSITIVE_INFINITY;
		bounds[2] = Double.POSITIVE_INFINITY;
		bounds[3] = Double.NEGATIVE_INFINITY;
		bounds[4] = Double.NEGATIVE_INFINITY;
		bounds[5] = Double.NEGATIVE_INFINITY;
		if(hasNodes())
		{
			for(Vertex v:getNodes())
			{
				bounds[0] = Math.min(bounds[0], v.getX());
				bounds[1] = Math.min(bounds[1], v.getY());
				bounds[2] = Math.min(bounds[2], v.getZ());
				bounds[3] = Math.max(bounds[3], v.getX());
				bounds[4] = Math.max(bounds[4], v.getY());
				bounds[5] = Math.max(bounds[5], v.getZ());
			}
		}
		else
		{
			for(Triangle t:getTriangles())
			{
				for(int i = 0; i < 3; i++)
				{
					Vertex v = t.getV(i);
					bounds[0] = Math.min(bounds[0], v.getX());
					bounds[1] = Math.min(bounds[1], v.getY());
					bounds[2] = Math.min(bounds[2], v.getZ());
					bounds[3] = Math.max(bounds[3], v.getX());
					bounds[4] = Math.max(bounds[4], v.getY());
					bounds[5] = Math.max(bounds[5], v.getZ());
				}
			}
		}
	}

	/** Return the number of fan for each neighbour edges */
	private List<Integer> getAdjacency(Vertex v)
	{
		Iterator<AbstractHalfEdge> it = v.getNeighbourIteratorAbstractHalfEdge();
		ArrayList<Integer> l = new ArrayList<Integer>();
		while(it.hasNext())
		{
			AbstractHalfEdge e = it.next();
			if(!e.hasAttributes(AbstractHalfEdge.OUTER))
			{
				if(e.hasAttributes(AbstractHalfEdge.BOUNDARY))
				{
					l.add(1);
				}
				else if(e.hasAttributes(AbstractHalfEdge.NONMANIFOLD))
				{
					int n = 0;
					Iterator<AbstractHalfEdge> it2 = e.fanIterator();
					while(it2.hasNext())
					{
						it2.next();
						n++;
					}
					l.add(n);
				}
				else
				{
					l.add(2);
				}
			}
		}
		Collections.sort(l);
		return l;
	}

	/** Clear the adjacency, rebuild it and compare both version */
	public boolean checkAdjacency()
	{
		Map<Vertex, List<Integer>> adjacency = HashFactory.createMap();
		for(Triangle t:getTriangles())
		{
			for(int i = 0; i < 3; i++)
			{
				Vertex v = t.getV(i);
				List<Integer> l = adjacency.get(v);
				if(l == null)
					adjacency.put(v, getAdjacency(v));
			}
		}
		clearAdjacency();
		buildAdjacency();
		for(Triangle t:getTriangles())
		{
			for(int i = 0; i < 3; i++)
			{
				Vertex v = t.getV(i);
				List<Integer> a1 = adjacency.get(v);
				List<Integer> a2 = getAdjacency(v);
				if(!a1.equals(a2))
				{
					new IllegalStateException(v + " " + a1 + " " + a2).printStackTrace();
					return false;
				}
			}
		}
		return true;
	}

	public void clearAdjacency()
	{
		Iterator<Triangle> itt = getTriangles().iterator();
		while(itt.hasNext())
		{
			Triangle t = itt.next();
			if(t.hasAttributes(AbstractHalfEdge.OUTER))
				itt.remove();
			else
			{
				AbstractHalfEdge e = t.getAbstractHalfEdge();
				for(int i = 0; i < 3; i++)
				{
					e.clearAttributes(0xFFFFFFFF);
					e.glue(null);
					e = e.next();
				}
			}
		}
		if(hasNodes())
		{
			for(Vertex v:getNodes())
				v.setLink(null);
		}
	}

	/**
	 * Merge an other mesh to this mesh.
	 * This method assuming that there are no group name conflict that is group
	 * names from the other mesh do not exists in this mesh
	 */
	public void merge(Mesh toMerge)
	{
		if(hasNodes())
			//I don't need it yet
			throw new UnsupportedOperationException();

		int groupOffset = getNumberOfGroups();
		for(Triangle t: toMerge.getTriangles())
		{
			add(t);
			t.setGroupId(t.getGroupId() + groupOffset);
		}
		int nbBeams = toMerge.getBeams().size() / 2;
		List<Vertex> otherBeams = toMerge.getBeams();
		for(int i = 0; i < nbBeams; i++)
		{
			addBeam(otherBeams.get(i * 2), otherBeams.get(i * 2 + 1),
				toMerge.getBeamGroup(i) + groupOffset);
		}
		for(Entry<Integer, String> e: toMerge.groupNames.entrySet())
			setGroupName(e.getKey()+groupOffset, e.getValue());
		vertexGroups.putAll(toMerge.getVertexGroup());
	}

	public void changeGroups(Map<Integer, Integer> map)
	{
		for(Triangle t: triangleList)
		{
			Integer newId = map.get(t.getGroupId());
			if(newId != null)
				t.setGroupId(newId);
		}
	}
	/**
	 * Return nodes even if hasNodes is false
	 * @triangles if not null, receive the triangle in the group
	 */
	public Collection<Vertex> getOrComputeNodes(int group, Collection<Triangle> triangles)
	{
		if(hasNodes() && group == -1)
			return getNodes();
		else
		{
			Set<Vertex> toReturn;
			if(group == -1)
				toReturn = HashFactory.createSet(triangleList.size() / 2 * 4 / 3);
			else
				toReturn = HashFactory.createSet();

			for(Triangle t: triangleList)
			{
				if(group == -1 || t.getGroupId() == group)
				{
					toReturn.add(t.getV0());
					toReturn.add(t.getV1());
					toReturn.add(t.getV2());
					if(triangles != null)
						triangles.add(t);
				}
			}
			return toReturn;
		}
	}

	/**
	 * Apply a geometrical transformation to all vertices of the triangles of
	 * a given group
	 */
	public void transform(int group, Matrix3D rotation, double[] translation)
	{
		double[] tmp1 = new double[3];
		double[] tmp2 = new double[3];
		for(Vertex v: getOrComputeNodes(group, null))
		{
			if(rotation != null)
			{
				v.get(tmp1);
				rotation.apply(tmp1, tmp2);
			}
			else
				v.get(tmp2);
			if(translation != null)
			{
				for(int i = 0; i < 3; i++)
					tmp2[i] += translation[i];
			}
			v.moveTo(tmp2[0], tmp2[1], tmp2[2]);
		}
	}

	/**
	 * Duplicate the vertices and triangles of a given group
	 */
	public void copy(int group, int toGroup)
	{
		ArrayList<Triangle> triangles = new ArrayList<Triangle>();
		Collection<Vertex> vertices = getOrComputeNodes(group, triangles);
		Map<Vertex, Vertex> map = HashFactory.createMap(vertices.size());
		for(Vertex v:vertices)
		{
			map.put(v, createVertex(v));
		}
		if(hasNodes())
			getNodes().addAll(map.values());
		for(Triangle t: triangles)
		{
			Triangle nt = createTriangle(map.get(t.v0), map.get(t.v1), map.get(t.v2));
			nt.setGroupId(toGroup);
			add(nt);
		}
	}

	public void reverse(int group)
	{
		for(Triangle t: getTriangles())
		{
			if(t.getGroupId() == group || group == -1)
			{
				Vertex v0 = t.getV0();
				Vertex v1 = t.getV1();
				t.setV(0, v1);
				t.setV(1, v0);
			}
		}
	}
	// Useful for debugging
	/*  Following imports must be moved at top.
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;
import gnu.trove.map.hash.TObjectIntHashMap;

	public void printMesh()
	{
		System.out.println("Mesh:");
		for (Triangle t: triangleList)
			System.out.println(""+t);
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
			LinkedHashSet<Vertex> nodeset = new LinkedHashSet<Vertex>();
			for (Triangle t: triangleList)
			{
				if (t.hasAttributes(AbstractHalfEdge.OUTER))
					continue;
				if (t.getV0() == outerVertex || t.getV1() == outerVertex || t.getV2() == outerVertex)
					continue;
				nodeset.add(t.getV0());
				nodeset.add(t.getV1());
				nodeset.add(t.getV2());
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
			for (Triangle t: triangleList)
			{
				if (t.hasAttributes(AbstractHalfEdge.OUTER))
					continue;
				if (t.getV0() == outerVertex || t.getV1() == outerVertex || t.getV2() == outerVertex)
					continue;
				count++;
				out.println(""+count+"        91         1         1         1         3");
				for(int i = 0; i < 3; i++)
				{
					int nodelabel = labels.get(t.getV(i));
					out.print(" "+nodelabel);
				}
				out.println("");
			}
			out.println("    -1");
			out.close();
		} catch (FileNotFoundException e)
		{
			logger.severe(e.toString());
			e.printStackTrace();
		} catch (IOException e)
		{
			logger.severe(e.toString());
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
			LinkedHashSet<Vertex> nodeset = new LinkedHashSet<Vertex>();
			for(Triangle t: triangleList)
			{
				if (t.hasAttributes(AbstractHalfEdge.OUTER))
					continue;
				if (t.getV0() == outerVertex || t.getV1() == outerVertex || t.getV2() == outerVertex)
					continue;
				nodeset.add(t.getV0());
				nodeset.add(t.getV1());
				nodeset.add(t.getV2());
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
			for(Triangle t: triangleList)
			{
				if (t.hasAttributes(AbstractHalfEdge.OUTER))
					continue;
				if (t.getV0() == outerVertex || t.getV1() == outerVertex || t.getV2() == outerVertex)
					continue;
				count++;
			}
			out.println(cr+"Triangles"+cr+count);
			count = 0;
			for(Triangle t: triangleList)
			{
				if (t.hasAttributes(AbstractHalfEdge.OUTER))
					continue;
				if (t.getV0() == outerVertex || t.getV1() == outerVertex || t.getV2() == outerVertex)
					continue;
				count++;
				for(int i = 0; i < 3; i++)
				{
					int nodelabel = labels.get(t.getV(i));
					out.print(nodelabel+" ");
				}
				out.println("1");
			}
			out.println(cr+"End");
			out.close();
		} catch (FileNotFoundException e)
		{
			logger.severe(e.toString());
			e.printStackTrace();
		} catch (IOException e)
		{
			logger.severe(e.toString());
			e.printStackTrace();
		}
	}
	*/
	
}
