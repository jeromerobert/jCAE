/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

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

package org.jcae.mesh.amibe.algos2d;

import org.jcae.mesh.amibe.ds.TriangleVH;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.amibe.patch.Mesh2D;
import org.jcae.mesh.amibe.patch.VirtualHalfEdge2D;
import org.jcae.mesh.amibe.patch.Vertex2D;
import org.jcae.mesh.amibe.metrics.Metric2D;
import org.jcae.mesh.amibe.util.QSortedTree;
import org.jcae.mesh.amibe.util.PAVLSortedTree;
import java.util.Map;
import java.util.HashSet;
import java.util.Collection;
import java.util.Iterator;
import gnu.trove.TObjectDoubleHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import org.jcae.mesh.amibe.ds.MMesh1D;
import org.jcae.mesh.amibe.ds.MeshParameters;
import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import org.jcae.mesh.cad.*;
import org.jcae.mesh.xmldata.MMesh1DReader;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;

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
public class SmoothNodes2D
{
	private static final Logger LOGGER=Logger.getLogger(SmoothNodes2D.class.getName());
	private final Mesh2D mesh;
	private boolean modifiedLaplacian = false;
	// If interpolate is false, distance(a,b) is computed with metric at point b.
	// Otherwise, it is computed with an interpolation of both metrics.
	private boolean interpolate = false;
	private int nloop = 5;
	private double tolerance = Double.MAX_VALUE / 2.0;
	private int progressBarStatus = 10000;
	private double relaxation = 0.6;
	private final Vertex2D c;
	private boolean refresh = false;
	private QSortedTree<Vertex2D> tree = new PAVLSortedTree<Vertex2D>();

	int processed = 0;
	int notProcessed = 0;
	TObjectDoubleHashMap<Triangle> qualityMap;
	Collection<Vertex> nodeset;
	
	/**
	 * Creates a <code>SmoothNodes2D</code> instance.
	 *
	 * @param m  the <code>Mesh2D</code> instance to refine.
	 */
	public SmoothNodes2D(Mesh2D m)
	{
		this(m, new HashMap<String, String>());
	}
	
	/**
	 * Creates a <code>SmoothNodes2D</code> instance.
	 *
	 * @param m  the <code>Mesh2D</code> instance to refine.
	 * @param options  map containing key-value pairs to modify algorithm
	 *        behaviour.  Valid keys are <code>size</code>,
	 *        <code>iterations</code> and <code>boundaries</code>.
	 */
	public SmoothNodes2D(final Mesh2D m, final Map<String, String> options)
	{
		mesh = m;
		c = (Vertex2D) mesh.createVertex(0.0, 0.0);
		for (final Map.Entry<String, String> opt: options.entrySet())
		{
			final String key = opt.getKey();
			final String val = opt.getValue();
			if (key.equals("modifiedLaplacian"))
				modifiedLaplacian = Boolean.valueOf(val).booleanValue();
			else if (key.equals("iterations"))
				nloop = Integer.valueOf(val).intValue();
			else if (key.equals("tolerance"))
				tolerance = Double.valueOf(val).doubleValue();
			else if (key.equals("refresh"))
				refresh = Boolean.valueOf(val).booleanValue();
			else if (key.equals("relaxation"))
				relaxation = Double.valueOf(val).doubleValue();
			else if (key.equals("interpolate"))
				interpolate = Boolean.valueOf(val).booleanValue();
			else
				throw new RuntimeException("Unknown option: "+key);
		}
		if (LOGGER.isLoggable(Level.FINE))
		{
			if (modifiedLaplacian)
				LOGGER.fine("Modified Laplacian smoothing");
			LOGGER.fine("Iterations: "+nloop);
			LOGGER.fine("Refresh: "+refresh);
			LOGGER.fine("Relaxation: "+relaxation);
			LOGGER.fine("Tolerance: "+tolerance);
			LOGGER.fine("Interpolate: "+interpolate);
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
		VirtualHalfEdge2D ot = new VirtualHalfEdge2D();
		for (Triangle f: mesh.getTriangles())
		{
			if (f.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			ot.bind((TriangleVH) f);
			double val = triangleQuality(ot);
			qualityMap.put(f, val);
		}
	}
	public void compute()
	{
		LOGGER.fine("Run "+getClass().getName());
		if (nloop > 0)
		{
			mesh.pushCompGeom(3);
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
			mesh.popCompGeom(3);
		}
		LOGGER.fine("Number of moved points: "+processed);
		LOGGER.fine("Total number of points not moved during processing: "+notProcessed);
	}
	
	/*
	 * Moves all nodes using a modified Laplacian smoothing.
	 */
	private void processAllNodes()
	{
		VirtualHalfEdge2D ot = new VirtualHalfEdge2D();
		// Compute vertex quality
		tree.clear();
		for (Vertex av: nodeset)
		{
			Vertex2D v = (Vertex2D) av;
			if (!v.isMutable())
				continue;
			TriangleVH f = (TriangleVH) v.getLink();
			if (f.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			ot.bind(f);
			if (ot.destination() == v)
				ot.next();
			else if (ot.apex() == v)
				ot.prev();
			assert ot.origin() == v;
			double qv = vertexQuality(ot);
			if (qv <= tolerance)
				tree.insert(v, qv);
		}
		// Now smooth nodes iteratively
		while (!tree.isEmpty())
		{
			Iterator<QSortedTree.Node<Vertex2D>> itt = tree.iterator();
			QSortedTree.Node<Vertex2D> q = itt.next();
			if (q.getValue() > tolerance)
				break;
			Vertex2D v = q.getData();
			tree.remove(v);
			if (!v.isMutable())
				continue;
			if (smoothNode(v, ot, q.getValue()))
			{
				processed++;
				if (processed > 0 && (processed % progressBarStatus) == 0)
					LOGGER.fine("Vertices processed: "+processed);
				if (!refresh)
					continue;
				// Update triangle quality
				Vertex2D d = (Vertex2D) ot.destination();
				do
				{
					ot.nextOrigin();
					if (ot.hasAttributes(AbstractHalfEdge.OUTER))
						continue;
					double qt = triangleQuality(ot);
					qualityMap.put(ot.getTri(), qt);
				}
				while (ot.destination() != d);
				// Update neighbor vertex quality
				do
				{
					ot.nextOrigin();
					Vertex2D n = (Vertex2D) ot.destination();
					if (!tree.contains(n))
						continue;
					ot.next();
					double qv = vertexQuality(ot);
					ot.prev();
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
	
	private boolean smoothNode(Vertex2D n, VirtualHalfEdge2D ot, double quality)
	{
		TriangleVH f = (TriangleVH) n.getLink();
		ot.bind(f);
		if (ot.destination() == n)
			ot.next();
		else if (ot.apex() == n)
			ot.prev();
		assert ot.origin() == n;
		double [] oldp2 = n.getUV();
		
		//  Compute 2D coordinates centroid.
		//  Metrics are not interpolated; these computations
		//  are not accurate, but we check that quality is improved
		//  before moving vertices, so we are safe.
		int nn = 0;
		double [] centroid2 = c.getUV();
		centroid2[0] = centroid2[1] = 0.0;
		Vertex2D d = (Vertex2D) ot.destination();
		Metric2D m0 = n.getMetrics(mesh);
		Metric2D mInterpolate = null;
		Metric2D mInv0 = null;
		if (interpolate)
		{
			// If interpolate is true, distance is computed with metric
			//    M = inv((inv(metric(n)) + inv(metric(d)))/2)
			// otherwise
			//    M = metric(d)
			// First, compute mInv0 = inv(metric(n))
			mInv0 = new Metric2D();
			if (m0.inv(mInv0))
				mInterpolate = new Metric2D();
		}
		do
		{
			ot.nextOrigin();
			assert !ot.hasAttributes(AbstractHalfEdge.OUTER);
			Metric2D m1;
			Vertex2D v = (Vertex2D) ot.destination();
			Metric2D m2 = v.getMetrics(mesh);
			if (mInterpolate != null)
			{
				if (Metric2D.interpolateSpecial(mInv0, m2, mInterpolate))
					m1 = mInterpolate;
				else
					m1 = m2;
			}
			else
				m1 = m2;
			nn++;
			double l = mesh.compGeom().distance2(n, v, m1);
			double[] newp2 = v.getUV();
			if (modifiedLaplacian)
			{
				if (l > 1.0)
				{
					// Find the point on this edge which has the
					// desired length
					l = 1.0 / Math.sqrt(l);
					for (int i = 0; i < 2; i++)
						centroid2[i] += newp2[i] + l * (oldp2[i] - newp2[i]);
				}
				else
				{
					for (int i = 0; i < 2; i++)
						centroid2[i] += oldp2[i];
				}
			}
			else
			{
				for (int i = 0; i < 2; i++)
					centroid2[i] += newp2[i];
			}
		}
		while (ot.destination() != d);
		assert (nn > 0);
		for (int i = 0; i < 2; i++)
			centroid2[i] /= nn;
		for (int i = 0; i < 2; i++)
			centroid2[i] = oldp2[i] + relaxation * (centroid2[i] - oldp2[i]);
		do
		{
			ot.nextOrigin();
			if (c.onLeft(mesh, (Vertex2D) ot.destination(), (Vertex2D) ot.apex()) < 0L)
				return false;
		}
		while (ot.destination() != d);

		double saveX = oldp2[0];
		double saveY = oldp2[1];
		n.moveTo(centroid2[0], centroid2[1]);
		// Check that quality has not been degraded
		double newQuality = vertexQuality(ot);
		if (newQuality < quality)
		{
			n.moveTo(saveX, saveY);
			return false;
		}
		return true;
	}
	
	private double triangleQuality(VirtualHalfEdge2D edge)
	{
		Triangle f = edge.getTri();
		assert f.vertex[0] != mesh.outerVertex && f.vertex[1] != mesh.outerVertex && f.vertex[2] != mesh.outerVertex : f;
		Vertex2D v0 = (Vertex2D) f.vertex[0];
		Vertex2D v1 = (Vertex2D) f.vertex[1];
		Vertex2D v2 = (Vertex2D) f.vertex[2];
		Metric2D m0 = v0.getMetrics(mesh);
		Metric2D m1 = v1.getMetrics(mesh);
		Metric2D m2 = v2.getMetrics(mesh);

		double l01 = mesh.compGeom().distance2(v0, v1, m0);
		double l12 = mesh.compGeom().distance2(v1, v2, m1);
		double l20 = mesh.compGeom().distance2(v2, v0, m2);

		double lmin, lmax;
		if (l01 > l12)
		{
			lmin = l12;
			lmax = l01;
		}
		else
		{
			lmax = l12;
			lmin = l01;
		}
		if (l20 < lmin)
			lmin = l20;
		else if (l20 > lmax)
			lmax = l20;
		assert lmax > 0.0;
		return lmin / lmax;
	}

	private double vertexQuality(VirtualHalfEdge2D edge)
	{
		Vertex2D d = (Vertex2D) edge.destination();
		double ret = Double.MAX_VALUE;
		do
		{
			edge.nextOrigin();
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
		System.out.println("Usage: SmoothNodes2D [options] xmlDir outDir");
		System.out.println("Options:");
		System.out.println(" -h, --help         Display this message and exit");
		System.out.println(" --iterations <n>   Iterate <n> times over all nodes");
		System.out.println(" --tolerance <t>    Consider only nodes with quality lower than <t>");
		System.out.println(" --relaxation <r>   Set relaxation factor");
		System.out.println(" --interpolate      Interpolate metrics");
		System.out.println(" --refresh          Update vertex quality before each iteration");
		System.exit(rc);
	}

	/**
	 * 
	 * @param args [options] xmlDir outDir
	 */
	public static void main(String[] args)
	{
		Map<String, String> opts = new HashMap<String, String>();
		int argc = 0;
		for (String arg: args)
			if (arg.equals("--help") || arg.equals("-h"))
				usage(0);
		while (argc < args.length-1)
		{
			if (args[argc].length() < 2 || args[argc].charAt(0) != '-' || args[argc].charAt(1) != '-')
				break;
			opts.put(args[argc].substring(2), args[argc+1]);
			argc += 2;
		}
		if (argc + 2 != args.length)
			usage(1);

		HashMap<String, String> options2d = new HashMap<String, String>();

		String inputDir = args[argc];
		String outputDir = args[argc+1];
		MMesh1D mesh1D = MMesh1DReader.readObject(inputDir);
		CADShape shape = mesh1D.getGeometry();
		CADExplorer expF = CADShapeFactory.getFactory().newExplorer();
		String brepFile = mesh1D.getGeometryFilename();

		MeshTraitsBuilder mtb = MeshTraitsBuilder.getDefault2D();

		int iFace = 0;
		for (expF.init(shape, CADShapeEnum.FACE); expF.more(); expF.next())
		{
			CADFace face = (CADFace) expF.current();
			iFace++;
			MeshParameters mp = new MeshParameters(options2d);
			Mesh2D mesh = new Mesh2D(mtb, mp, face);
			try
			{
				MeshReader.readObject(mesh, inputDir, iFace);
				new SmoothNodes2D(mesh, opts).compute();			
				MeshWriter.writeObject(mesh, outputDir, brepFile, iFace);

				// Copy geometry file
				FileInputStream is = new FileInputStream(inputDir+File.separator+brepFile);
				FileChannel iChannel = is.getChannel();
				FileOutputStream os = new FileOutputStream(new File(outputDir, brepFile), false);
				FileChannel oChannel = os.getChannel();
				oChannel.transferFrom(iChannel, 0, iChannel.size());
				is.close();
				os.close();
			}
			catch(IOException ex)
			{
				ex.printStackTrace();
			}
		}
	}
}
