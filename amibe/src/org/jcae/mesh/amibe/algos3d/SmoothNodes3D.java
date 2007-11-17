/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2003, 2005, by EADS CRC
    Copyright (C) 2007, by EADS France

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
import org.jcae.mesh.amibe.util.QSortedTree;
import org.jcae.mesh.amibe.util.PAVLSortedTree;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collection;
import java.util.Iterator;
import java.io.IOException;
import gnu.trove.TObjectDoubleHashMap;
import org.apache.log4j.Logger;

/**
 * 3D node smoothing.  Triangles are sorted according to their quality,
 * They are processed iteratively beginning with the worst triangle.
 * Its three vertices are moved if they have not already been moved
 * when processing a previous triangle.  A modified Laplacian smoothing
 * is performed, as briefly explained in
 * <a href="http://www.ann.jussieu.fr/~frey/publications/ijnme4198.pdf">Adaptive Triangular-Quadrilateral Mesh Generation</a>, by Houman Borouchaky and
 * Pascal J. Frey.
 * If the final position do not invert triangles, the point is moved.
 */
// Note 1: alternatives should be tested.
// Note 2: the point should be moved only if triangle quality is improved.
public class SmoothNodes3D
{
	private static Logger logger=Logger.getLogger(SmoothNodes3D.class);
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
	 *        <code>iterations</code> and <code>boundaries</code>.
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
		if (logger.isDebugEnabled())
		{
			if (sizeTarget > 0.0)
				logger.debug("Size: "+sizeTarget);
			logger.debug("Iterations: "+nloop);
			logger.debug("Refresh: "+refresh);
			logger.debug("Relaxation: "+relaxation);
			logger.debug("Tolerance: "+tolerance);
			logger.debug("Preserve boundaries: "+preserveBoundaries);
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
		// First compute triangle quality
		qualityMap = new TObjectDoubleHashMap<Triangle>(mesh.getTriangles().size());
		computeTriangleQuality();

		nodeset = mesh.getNodes();
		if (nodeset == null)
		{
			nodeset = new HashSet<Vertex>(mesh.getTriangles().size() / 2);
			for (Triangle f: mesh.getTriangles())
			{
				if (f.hasAttributes(AbstractHalfEdge.OUTER))
					continue;
				for (Vertex v: f.vertex)
					nodeset.add(v);
			}
		}
		for (int i = 0; i < nloop; i++)
			processAllNodes();
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
			if (!v.isManifold())
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
			if (!v.isManifold() || !v.isMutable())
			{
				notProcessed++;
				continue;
			}
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
					if (n == mesh.outerVertex || !n.isManifold() || !tree.contains(n))
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
					// Find the point on this edge which has the
					// desired length
					if (l <= 0.0)
					{
						nn--;
						continue;
					}
					l = sizeTarget / l;
					for (int i = 0; i < 3; i++)
						centroid3[i] += newp3[i] + l * (oldp3[i] - newp3[i]);
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
		if (!n.discreteProject(c))
			return false;
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
		Mesh mesh = new Mesh();
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
			MeshReader.readObject3D(mesh, args[argc], "jcae3d");
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		new SmoothNodes3D(mesh, opts).compute();			
		try
		{
			MeshWriter.writeObject3D(mesh, args[argc+1], "jcae3d", ".", "dummy.brep");
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
	}
}
