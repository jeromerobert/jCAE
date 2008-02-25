/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2003,2005, by EADS CRC
    Copyright (C) 2007,2008, by EADS France

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
import org.jcae.mesh.amibe.metrics.Matrix3D;
import org.jcae.mesh.amibe.util.QSortedTree;
import org.jcae.mesh.amibe.util.PAVLSortedTree;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.Iterator;
import java.io.IOException;
import gnu.trove.TObjectDoubleHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

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
public class SmoothNodes3D
{
	private static Logger logger=Logger.getLogger(SmoothNodes3D.class.getName());
	private Mesh mesh;
	private double sizeTarget = -1.0;
	private int nloop = 10;
	private double tolerance = Double.MAX_VALUE / 2.0;
	private boolean preserveBoundaries = false;
	private int progressBarStatus = 10000;
	private static final double scaleFactor = 12.0 * Math.sqrt(3.0);
	private double relaxation = 0.6;
	private final Vertex c;
	private QSortedTree<Vertex> tree = new PAVLSortedTree<Vertex>();
	private boolean refresh = false;
	int processed = 0;
	int notProcessed = 0;
	TObjectDoubleHashMap<Triangle> qualityMap;
	Map<Vertex, QuadricProjection> nodeProjection;
	Collection<Vertex> nodeset;
	
	/**
	 * Creates a <code>SmoothNodes3D</code> instance.
	 *
	 * @param m  the <code>Mesh</code> instance to refine.
	 */
	public SmoothNodes3D(Mesh m)
	{
		mesh = m;
		c = mesh.createVertex(0.0, 0.0, 0.0);
	}
	
	/**
	 * Creates a <code>SmoothNodes3D</code> instance.
	 *
	 * @param m  the <code>Mesh</code> instance to refine.
	 * @param options  map containing key-value pairs to modify algorithm
	 *        behaviour.  Valid keys are <code>size</code>,
	 *        <code>iterations</code>, <code>boundaries</code>,
	 *        <code>tolerance</code>, <code>refresh</code> and
	 *        <code>relaxation</code>.
	 */
	public SmoothNodes3D(final Mesh m, final Map<String, String> options)
	{
		mesh = m;
		c = mesh.createVertex(0.0, 0.0, 0.0);
		for (final Map.Entry<String, String> opt: options.entrySet())
		{
			final String key = opt.getKey();
			final String val = opt.getValue();
			if (key.equals("size"))
				sizeTarget = Double.valueOf(val).doubleValue();
			else if (key.equals("iterations"))
				nloop = Integer.valueOf(val).intValue();
			else if (key.equals("boundaries"))
				preserveBoundaries = Boolean.valueOf(val).booleanValue();
			else if (key.equals("tolerance"))
				tolerance = Double.valueOf(val).doubleValue();
			else if (key.equals("refresh"))
				refresh = true;
			else if (key.equals("relaxation"))
				relaxation = Double.valueOf(val).doubleValue();
			else
				throw new RuntimeException("Unknown option: "+key);
		}
		if (logger.isLoggable(Level.FINE))
		{
			if (sizeTarget > 0.0)
				logger.fine("Size: "+sizeTarget);
			logger.fine("Iterations: "+nloop);
			logger.fine("Refresh: "+refresh);
			logger.fine("Relaxation: "+relaxation);
			logger.fine("Tolerance: "+tolerance);
			logger.fine("Preserve boundaries: "+preserveBoundaries);
		}
	}
	
	private static class QuadricProjection
	{
		final Matrix3D localFrameTransform;
		final double [] origin = new double[3];
		final double [] quadric;
		public QuadricProjection(double [] o, Matrix3D P, double [] q)
		{
			for (int i = 0; i < 3; i++)
				origin[i] = o[i];
			localFrameTransform = P;
			quadric = q;
		}
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
	public void compute()
	{
		logger.info("Run "+getClass().getName());
		if (nloop > 0)
		{
			// First compute triangle quality
			qualityMap = new TObjectDoubleHashMap<Triangle>(mesh.getTriangles().size());
			computeTriangleQuality();
	
			nodeset = mesh.getNodes();
			nodeProjection = new HashMap<Vertex, QuadricProjection>(mesh.getTriangles().size() / 2);
			if (nodeset == null)
			{
				nodeset = new LinkedHashSet<Vertex>(mesh.getTriangles().size() / 2);
				for (Triangle f: mesh.getTriangles())
				{
					if (f.hasAttributes(AbstractHalfEdge.OUTER))
						continue;
					for (Vertex v: f.vertex)
						nodeset.add(v);
				}
			}
			for (Vertex v: nodeset)
			{
				if (!v.isManifold() || !v.isMutable())
					continue;
				Matrix3D P = v.getMatrix3DLocalFrame();
				if (P == null)
					continue;
				double [] q = v.getLocalQuadric(P);
				if (q == null)
					continue;
				nodeProjection.put(v, new QuadricProjection(v.getUV(), P, q));
			}
			for (int i = 0; i < nloop; i++)
				processAllNodes();
		}
		logger.info("Number of moved points: "+processed);
		logger.info("Total number of points not moved during processing: "+notProcessed);
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
			if (!v.isManifold() || !v.isMutable())
				continue;
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
			tree.remove(v);
			if (v.getRef() != 0 && preserveBoundaries)
			{
				notProcessed++;
				continue;
			}
			if (smoothNode(v, ot, q.getValue()))
			{
				processed++;
				if (processed > 0 && (processed % progressBarStatus) == 0)
					logger.info("Vertices processed: "+processed);
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
 		ot = f.getAbstractHalfEdge(ot);
		if (ot.destination() == n)
			ot = ot.next();
		else if (ot.apex() == n)
			ot = ot.prev();
		assert ot.origin() == n;
		double [] oldp3 = n.getUV();
		
		//  Compute 3D coordinates centroid
		int nn = 0;
		double [] centroid3 = c.getUV();
		centroid3[0] = centroid3[1] = centroid3[2] = 0.0;
		assert n.isManifold();
		Vertex d = ot.destination();
		do
		{
			ot = ot.nextOriginLoop();
			Vertex v = ot.destination();
			if (v != mesh.outerVertex)
			{
				nn++;
				double l = n.distance3D(v);
				double[] newp3 = v.getUV();
				if (sizeTarget > 0.0)
				{
					if (l > 1.0)
					{
						// Find the point on this edge which has the
						// desired length
						l = sizeTarget / l;
						for (int i = 0; i < 3; i++)
							centroid3[i] += newp3[i] + l * (oldp3[i] - newp3[i]);
					}
					else
					{
						for (int i = 0; i < 3; i++)
							centroid3[i] += oldp3[i];
					}
				}
				else
				{
					for (int i = 0; i < 3; i++)
						centroid3[i] += newp3[i];
				}
			}
		}
		while (ot.destination() != d);
		assert (nn > 0);
		for (int i = 0; i < 3; i++)
			centroid3[i] /= nn;
		for (int i = 0; i < 3; i++)
			centroid3[i] = oldp3[i] + relaxation * (centroid3[i] - oldp3[i]);
		if (!ot.checkNewRingNormals(centroid3))
			return false;
		QuadricProjection tr = nodeProjection.get(n);
		if (tr == null)
			return false;
		c.projectQuadric(tr.origin, tr.localFrameTransform, tr.quadric);

		double saveX = oldp3[0];
		double saveY = oldp3[1];
		double saveZ = oldp3[2];
		n.moveTo(centroid3[0], centroid3[1], centroid3[2]);
		// Check that quality has not been degraded
		if (vertexQuality(ot) < quality)
		{
			n.moveTo(saveX, saveY, saveZ);
			return false;
		}
		return true;
	}
	
	private double triangleQuality(AbstractHalfEdge edge)
	{
		Triangle f = edge.getTri();
		assert f.vertex[0] != mesh.outerVertex && f.vertex[1] != mesh.outerVertex && f.vertex[2] != mesh.outerVertex : f;
		double p = f.vertex[0].distance3D(f.vertex[1]) + f.vertex[1].distance3D(f.vertex[2]) + f.vertex[2].distance3D(f.vertex[0]);
		double area = edge.area();
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
			double qt = triangleQuality(edge);
			if (qt < ret)
				ret = qt;
		}
		while (edge.destination() != d);
		return ret;
	}

	private static void usage(int rc)
	{
		System.out.println("Usage: SmoothNodes3D [options] xmlDir outDir");
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
		new SmoothNodes3D(mesh, opts).compute();			
		try
		{
			MeshWriter.writeObject3D(mesh, args[argc+1], null);
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
}
