/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2008,2009,2010, by EADS France

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

package org.jcae.mesh.amibe.algos3d;

import org.jcae.mesh.amibe.ds.Mesh;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.projection.MeshLiaison;
import org.jcae.mesh.amibe.util.QSortedTree;
import org.jcae.mesh.amibe.util.PAVLSortedTree;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.Iterator;
import java.io.IOException;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.mesh.amibe.metrics.Location;
import org.jcae.mesh.amibe.metrics.MetricSupport;
import org.jcae.mesh.amibe.projection.MapMeshLiaison;

/**
 * Node smoothing.  Triangle quality is computed for all triangles,
 * and vertex quality is the lowest value of its incident triangles.
 * Vertices are sorted according to their quality, and processed
 * iteratively by beginning with worst vertex.  A modified Laplacian
 * smoothing is performed, as briefly explained in
 * <a href="http://www.ann.jussieu.fr/~frey/publications/ijnme4198.pdf">Adaptive Triangular-Quadrilateral Mesh Generation</a>, by Houman Borouchaky and
 * Pascal J. Frey.
 * If final position improves vertex quality, point is moved.
 */
public class SmoothNodes3DBg
{
	private final static Logger LOGGER = Logger.getLogger(SmoothNodes3DBg.class.getName());
	private final Mesh mesh;
	private final MeshLiaison liaison;
	private int nloop = 10;
	private double tolerance = Double.MAX_VALUE / 2.0;
	private double minCos = 0.95;
	private boolean preserveBoundaries = false;
	private boolean checkQuality = true;
	private int progressBarStatus = 10000;
	private static final double scaleFactor = 12.0 * Math.sqrt(3.0);
	private double relaxation = 0.6;
	private final QSortedTree<Vertex> tree = new PAVLSortedTree<Vertex>();
	private boolean refresh = false;
	int processed = 0;
	private int notProcessed = 0;
	private TObjectDoubleHashMap<Triangle> qualityMap;
	private Collection<Vertex> nodeset;
	private final Set<Vertex> immutableNodes = new LinkedHashSet<Vertex>();
	private MetricSupport metrics;
	/**
	 * Creates a <code>SmoothNodes3DBg</code> instance.
	 *
	 * @param m  the <code>Mesh</code> instance to refine.
	 */
	@Deprecated
	public SmoothNodes3DBg(Mesh m)
	{
		this(m, new HashMap<String, String>());
	}
	
	/**
	 * Creates a <code>SmoothNodes3DBg</code> instance.
	 *
	 * @param bgMesh  the <code>Mesh</code> instance to refine.
	 * @param options  map containing key-value pairs to modify algorithm
	 *        behaviour.  Valid keys are <code>size</code>,
	 *        <code>iterations</code>, <code>boundaries</code>,
	 *        <code>tolerance</code>, <code>refresh</code> and
	 *        <code>relaxation</code>.
	 */
	@Deprecated
	public SmoothNodes3DBg(final Mesh bgMesh, final Map<String, String> options)
	{
		this(new MapMeshLiaison(bgMesh), options);
	}

	public SmoothNodes3DBg(final MeshLiaison meshLiaison, final Map<String, String> options)
	{
		liaison = meshLiaison;
		mesh = liaison.getMesh();
		metrics = new MetricSupport(mesh, options);
		for (final Map.Entry<String, String> opt: options.entrySet())
		{
			final String key = opt.getKey();
			final String val = opt.getValue();
			if (key.equals("iterations"))
				nloop = Integer.valueOf(val).intValue();
			else if (key.equals("boundaries"))
				preserveBoundaries = Boolean.valueOf(val).booleanValue();
			else if (key.equals("tolerance"))
				tolerance = Double.valueOf(val).doubleValue();
			else if (key.equals("refresh"))
				refresh = true;
			else if (key.equals("check"))
				checkQuality = Boolean.valueOf(val).booleanValue();
			else if (key.equals("relaxation"))
				relaxation = Double.valueOf(val).doubleValue();
			else if (key.equals("coplanarity"))
			{
				minCos = Double.parseDouble(val);
				LOGGER.fine("Minimum dot product of face normals allowed for swapping an edge: "+minCos);
			}
			else if(!metrics.isKnownOption(key))
				throw new RuntimeException("Unknown option: "+key);
		}
		if (meshLiaison == null)
			mesh.buildRidges(minCos);
		if (LOGGER.isLoggable(Level.FINE))
		{
			LOGGER.fine("Iterations: "+nloop);
			LOGGER.fine("Refresh: "+refresh);
			LOGGER.fine("Relaxation: "+relaxation);
			LOGGER.fine("Tolerance: "+tolerance);
			LOGGER.fine("Preserve boundaries: "+preserveBoundaries);
		}
	}

	public void setAnalyticMetric(MetricSupport.AnalyticMetricInterface m)
	{
		metrics.setAnalyticMetric(m);
	}

	public void setAnalyticMetric(int groupId, MetricSupport.AnalyticMetricInterface m)
	{
		metrics.setAnalyticMetric(groupId, m);
	}

	public final Mesh getOutputMesh()
	{
		return mesh;
	}
	
	public void setProgressBarStatus(int n)
	{
		progressBarStatus = n;
	}

	/**
	 * Moves all nodes until all iterations are done.
	 */
	private void computeTriangleQuality()
	{
 		AbstractHalfEdge ot = null;
		for (Triangle f: mesh.getTriangles())
		{
			if (f.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			ot = f.getAbstractHalfEdge(ot);
			double val = triangleQuality(ot);
			qualityMap.put(f, val);
		}
	}

	public final SmoothNodes3DBg compute()
	{
		long startTime = System.nanoTime();
		LOGGER.info("Run "+getClass().getName());
		if (nloop > 0)
		{
			metrics.compute();
			// First compute triangle quality
			qualityMap = new TObjectDoubleHashMap<Triangle>(mesh.getTriangles().size());
			computeTriangleQuality();

			nodeset = mesh.getNodes();
			if (nodeset == null)
			{
				nodeset = new LinkedHashSet<Vertex>(mesh.getTriangles().size() / 2);
				for (Triangle f: mesh.getTriangles())
				{
					if (f.hasAttributes(AbstractHalfEdge.OUTER))
						continue;
					f.addVertexTo(nodeset);
				}
			}
			// Detect immutable nodes
			AbstractHalfEdge ot = null;
			for (Triangle f: mesh.getTriangles())
			{
				if (f.hasAttributes(AbstractHalfEdge.OUTER))
					continue;
				ot = f.getAbstractHalfEdge(ot);
				for (int i = 0; i < 3; i++)
				{
					ot = ot.next();
					if (ot.hasAttributes(AbstractHalfEdge.IMMUTABLE | AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD | AbstractHalfEdge.SHARP))
					{
						immutableNodes.add(ot.origin());
						immutableNodes.add(ot.destination());
					}
				}
			}
			for (Vertex v: nodeset)
			{
				if (!v.isManifold() || (preserveBoundaries && v.getRef() != 0) || !v.isMutable())
					immutableNodes.add(v);
			}

			for (int i = 0; i < nloop; i++)
			{
				processAllNodes();
				postProcessIteration(mesh, i);
			}
		}
		LOGGER.info("Number of moved points: "+processed);
		LOGGER.info("Total number of points not moved during processing: "+notProcessed);
		assert mesh.checkNoDegeneratedTriangles();
		assert mesh.checkNoInvertedTriangles();
		long endTime = System.nanoTime();
		LOGGER.log(Level.INFO, "Computation time: {0}ms",
			Double.toString((endTime - startTime)/1E6));
		return this;
	}
	
	final void postProcessIteration(Mesh mesh, int i)
	{
		// Can be overridden
	}
	
	/*
	 * Moves all nodes using a modified Laplacian smoothing.
	 */
	private void processAllNodes()
	{
 		AbstractHalfEdge ot = null;
		// Compute vertex quality
		tree.clear();
		for (Vertex v: nodeset)
		{
			if (immutableNodes.contains(v))
			{
				notProcessed++;
				continue;
			}
			Triangle f = (Triangle) v.getLink();
			ot = f.getAbstractHalfEdge(ot);
			if (ot.destination() == v)
				ot = ot.next();
			else if (ot.apex() == v)
				ot = ot.prev();
			assert ot.origin() == v;
			double qv = vertexQuality(ot);
			if (qv <= tolerance)
				tree.insert(v, qv);
		}
		// Now smooth nodes iteratively
		while (!tree.isEmpty())
		{
			Iterator<QSortedTree.Node<Vertex>> itt = tree.iterator();
			QSortedTree.Node<Vertex> q = itt.next();
			if (q.getValue() > tolerance)
				break;
			Vertex v = q.getData();
			assert !immutableNodes.contains(v);
			tree.remove(v);
			if (smoothNode(v, ot, q.getValue()))
			{
				processed++;
				if (processed > 0 && (processed % progressBarStatus) == 0)
					LOGGER.info("Vertices processed: "+processed);
				if (!refresh)
					continue;
				assert ot != null;
				// Update triangle quality
				Vertex d = ot.destination();
				do
				{
					ot = ot.nextOriginLoop();
					if (ot.hasAttributes(AbstractHalfEdge.OUTER))
						continue;
					double qt = triangleQuality(ot);
					qualityMap.put(ot.getTri(), qt);
				}
				while (ot.destination() != d);
				// Update neighbor vertex quality
				do
				{
					ot = ot.nextOriginLoop();
					Vertex n = ot.destination();
					if (n == mesh.outerVertex || !tree.contains(n))
						continue;
					if (ot.hasAttributes(AbstractHalfEdge.OUTER))
						continue;
					ot = ot.next();
					double qv = vertexQuality(ot);
					ot = ot.prev();
					if (qv <= tolerance)
						tree.update(n, qv);
					else
					{
						tree.remove(n);
						notProcessed++;
					}
				}
				while (ot.destination() != d);
			}
			else
				notProcessed++;
		}
	}
	
	private boolean smoothNode(Vertex n, AbstractHalfEdge ot, double quality)
	{
		Triangle f = (Triangle) n.getLink();
		int group = f.getGroupId();
 		ot = f.getAbstractHalfEdge(ot);
		if (ot.destination() == n)
			ot = ot.next();
		else if (ot.apex() == n)
			ot = ot.prev();
		assert ot.origin() == n;
		//  Compute 3D coordinates centroid
		int nn = 0;
		Location centroid3 = new Location();
		assert n.isManifold();
		Vertex d = ot.destination();
		do
		{
			ot = ot.nextOriginLoop();
			Vertex v = ot.destination();
			if (v != mesh.outerVertex)
			{
				nn++;
				if (!metrics.isEmpty())
				{
					// Find the point on this edge which has the
					// desired length
					double p = metrics.interpolatedDistance(n, v);
					if(p < 1.0)
						centroid3.moveTo(
							centroid3.getX() + v.getX() + p * (n.getX() - v.getX()),
							centroid3.getY() + v.getY() + p * (n.getY() - v.getY()),
							centroid3.getZ() + v.getZ() + p * (n.getZ() - v.getZ()));
					else
						centroid3.add(v);
				}
				else
					centroid3.add(v);
			}
		}
		while (ot.destination() != d);
		assert (nn > 0);
		centroid3.scale(1.0/nn);
		centroid3.moveTo(
			n.getX() + relaxation * (centroid3.getX() - n.getX()),
			n.getY() + relaxation * (centroid3.getY() - n.getY()),
			n.getZ() + relaxation * (centroid3.getZ() - n.getZ()));
		double saveX = n.getX();
		double saveY = n.getY();
		double saveZ = n.getZ();
		if (!liaison.backupAndMove(n, centroid3, group))
		{
			LOGGER.finer("Point not moved, projection failed");
			liaison.backupRestore(n, true, group);
			return false;
		}
		// Temporarily reset n to its previous location, but do not
		// modify liaison, this is not
		centroid3.moveTo(n);
		n.moveTo(saveX, saveY, saveZ);
		if (!mesh.canMoveOrigin(ot, centroid3))
		{
			liaison.backupRestore(n, true, group);
			LOGGER.finer("Point not moved, some triangles would become inverted");
			return false;
		}
		n.moveTo(centroid3);

		if (checkQuality)
		{
			// Check that quality has not been degraded
			if (vertexQuality(ot) < quality)
			{
				n.moveTo(saveX, saveY, saveZ);
				liaison.backupRestore(n, true, group);
				LOGGER.finer("Point not moved, quality decreases");
				return false;
			}
		}
		liaison.backupRestore(n, false, group);
		if (!metrics.isEmpty())
			metrics.put(n, metrics.get(n, f));
		return true;
	}

	private double triangleQuality(AbstractHalfEdge edge)
	{
		Triangle f = edge.getTri();
		assert f.getV0() != mesh.outerVertex && f.getV1() != mesh.outerVertex && f.getV2() != mesh.outerVertex : f;
		double p = f.getV0().distance3D(f.getV1()) + f.getV1().distance3D(f.getV2()) + f.getV2().distance3D(f.getV0());
		double area = edge.area(mesh);
		double ret = scaleFactor * area / p / p;
		assert ret >= 0.0 && ret <= 1.01;
		return ret;
	}

	private double vertexQuality(AbstractHalfEdge edge)
	{
		Vertex d = edge.destination();
		double ret = Double.MAX_VALUE;
		do
		{
			edge = edge.nextOriginLoop();
			if (edge.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			assert qualityMap.containsKey(edge.getTri());
			double qt = qualityMap.get(edge.getTri());
			if (qt < ret)
				ret = qt;
		}
		while (edge.destination() != d);
		return ret;
	}

	private static void usage(int rc)
	{
		System.out.println("Usage: SmoothNodes3DBg [options] xmlDir outDir");
		System.out.println("Options:");
		System.out.println(" -h, --help         Display this message and exit");
		System.out.println(" --iterations <n>   Iterate <n> times over all nodes");
		System.out.println(" --size <s>         Set target size");
		System.out.println(" --tolerance <t>    Consider only nodes with quality lower than <t>");
		System.out.println(" --relaxation <r>   Set relaxation factor");
		System.out.println(" --refresh          Update vertex quality before each iteration");
		System.exit(rc);
	}

	/**
	 * 
	 * @param args [options] xmlDir outDir
	 */
	public static void main(String[] args)
	{
		org.jcae.mesh.amibe.traits.MeshTraitsBuilder mtb = org.jcae.mesh.amibe.traits.MeshTraitsBuilder.getDefault3D();
		mtb.addNodeList();
		Mesh mesh = new Mesh(mtb);
		Map<String, String> opts = new HashMap<String, String>();
		int argc = 0;
		for (String arg: args)
			if (arg.equals("--help") || arg.equals("-h"))
				usage(0);
		while (argc < args.length-1)
		{
			if (args[argc].length() < 2 || args[argc].charAt(0) != '-' || args[argc].charAt(1) != '-')
				break;
			if (args[argc].equals("--refresh") || args[argc].equals("--boundaries"))
			{
				opts.put(args[argc].substring(2), "true");
				argc++;
			}
			else
			{
				opts.put(args[argc].substring(2), args[argc+1]);
				argc += 2;
			}
		}
		if (argc + 2 != args.length)
			usage(1);
		try
		{
			MeshReader.readObject3D(mesh, args[argc]);
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		SmoothNodes3DBg smoother = new SmoothNodes3DBg(mesh, opts);
		smoother.compute();			
		try
		{
			MeshWriter.writeObject3D(smoother.getOutputMesh(), args[argc+1], null);
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
}
