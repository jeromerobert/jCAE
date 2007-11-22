/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

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
import org.jcae.mesh.amibe.ds.AbstractHalfEdge;
import org.jcae.mesh.amibe.ds.HalfEdge;
import org.jcae.mesh.amibe.ds.Triangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import gnu.trove.TObjectIntHashMap;
import org.apache.log4j.Logger;

/**
 * Swaps edge to try to have all vertices have six neighbors.
 *
 * TODO:
 * In <a href="www.cs.technion.ac.il/~gotsman/AmendedPubl/SGP/SGP03.pdf">Explicit Surface Remeshing</a>, Vitaly Surazhsky and Craig Gotsman proposes an interesting algorithm.
 */

public class ImproveConnectivity extends AbstractAlgoHalfEdge
{
	private static Logger logger=Logger.getLogger(ImproveConnectivity.class);
	private double planarMin = 0.95;
	private TObjectIntHashMap<Vertex> map;
	
	/**
	 * Creates a <code>ImproveConnectivity</code> instance.
	 *
	 * @param m  the <code>Mesh</code> instance to modify
	 * @param options  map containing key-value pairs to modify algorithm
	 *        behaviour.  Valid key is <code>coplanar</code>.
	 */
	public ImproveConnectivity(final Mesh m, final Map<String, String> options)
	{
		super(m);
		for (final Map.Entry<String, String> opt: options.entrySet())
		{
			final String key = opt.getKey();
			final String val = opt.getValue();
			if (key.equals("coplanar"))
			{
				planarMin = new Double(val).doubleValue();
				logger.debug("Coplanar value: "+planarMin);
			}
			else
				throw new RuntimeException("Unknown option: "+key);
		}
		tolerance = - 0.01;
		// Do not let AbstractAlgoHalfEdge swap edges
		noSwapAfterProcessing = true;
	}
	
	@Override
	public Logger thisLogger()
	{
		return logger;
	}

	@Override
	public void preProcessAllHalfEdges()
	{
		map = new TObjectIntHashMap<Vertex>(mesh.getTriangles().size() / 2);
		for (Triangle t: mesh.getTriangles())
		{
			if (t.hasAttributes(AbstractHalfEdge.OUTER))
				continue;
			for (Vertex v: t.vertex)
				map.put(v, map.get(v) + 1);
			HalfEdge e = (HalfEdge) t.getAbstractHalfEdge();
			for (int i = 0; i < 3; i++)
			{
				e = e.next();
				if (e.checkSwap3D(planarMin) <= 0.0)
				{
					e.setAttributes(AbstractHalfEdge.MARKED);
					e.sym().setAttributes(AbstractHalfEdge.MARKED);
				}
			}
		}
	}

	@Override
	public double cost(final HalfEdge e)
	{
		Vertex o = e.origin();
		Vertex d = e.destination();
		Vertex a = e.apex();
		Vertex n = e.sym().apex();
		double before = vertexQuality(o, 0);
		double temp = vertexQuality(d, 0);
		if (temp < before)
			before = temp;
		temp = vertexQuality(a, 0);
		if (temp < before)
			before = temp;
		temp = vertexQuality(n, 0);
		if (temp < before)
			before = temp;

		double after = vertexQuality(o, -1);
		temp = vertexQuality(d, -1);
		if (temp < after)
			after = temp;
		temp = vertexQuality(a, 1);
		if (temp < after)
			after = temp;
		temp = vertexQuality(n, 1);
		if (temp < after)
			after = temp;
		return before - after;
	}
	
	private final double vertexQuality(Vertex v, int delta)
	{
		if (!v.isManifold())
			return 1.0;
		if (v.getRef() != 0)
		{
			int q = map.get(v);
			assert q > 0;
			if (q + delta <= 1)
				return -100.0;
			return 1.0;
		}
		int q = map.get(v) + delta;
		return connectivityQuality(q);
	}

	private final double connectivityQuality(int q)
	{
		if (q < 7)
			return q / 6.0;
		return 1.5 - q / 12.0;
	}

	@Override
	public boolean canProcessEdge(HalfEdge current)
	{
		if (current.hasAttributes(AbstractHalfEdge.OUTER | AbstractHalfEdge.BOUNDARY | AbstractHalfEdge.NONMANIFOLD))
			return false;
		if (current.hasAttributes(AbstractHalfEdge.MARKED) && current.sym().hasAttributes(AbstractHalfEdge.MARKED))
			return false;
		return current.checkSwap3D(planarMin) > 0.0;
	}

	@Override
	public HalfEdge processEdge(HalfEdge current, double costCurrent)
	{
		Vertex o = current.origin();
		Vertex d = current.destination();
		Vertex a = current.apex();
		Vertex n = current.sym().apex();

		if (logger.isDebugEnabled())
		{
			logger.debug("Swap edge: "+current+"  cost="+costCurrent);
			logger.debug(" Connections before swap: o="+map.get(o)+" d="+map.get(d)+" a="+map.get(a)+" n="+map.get(n));
		}

		assert costCurrent == cost(current) : costCurrent+" != "+cost(current);
		removeAllEdgesIncidentTo(o);
		removeAllEdgesIncidentTo(d);
		removeAllEdgesIncidentTo(a);
		removeAllEdgesIncidentTo(n);
		// mesh.edgeSwap() transforms (oda) into (ona)
		current = (HalfEdge) mesh.edgeSwap(current);
		// Update vertex costs
		int oVal = map.get(o);
		int dVal = map.get(d);
		int aVal = map.get(a);
		int nVal = map.get(n);
		map.put(o, oVal - 1);
		map.put(d, dVal - 1);
		map.put(a, aVal + 1);
		map.put(n, nVal + 1);

		addAllEdgesIncidentTo(o);
		addAllEdgesIncidentTo(d);
		addAllEdgesIncidentTo(a);
		addAllEdgesIncidentTo(n);
		return current.prev();
	}
	
	private final void removeAllEdgesIncidentTo(Vertex v)
	{
		Triangle t = (Triangle) v.getLink();
		HalfEdge current = (HalfEdge) t.getAbstractHalfEdge();
		if (current.destination() == v)
			current = current.next();
		else if (current.apex() == v)
			current = current.prev();
		assert current.origin() == v;
		Vertex d = current.destination();
		do
		{
			HalfEdge h = uniqueOrientation(current);
			if (!tree.remove(h))
				notInTree++;
			assert !tree.contains(h);
			h = uniqueOrientation(current.next());
			if (!tree.remove(h))
				notInTree++;
			assert !tree.contains(h);
			current = current.nextOriginLoop();
		}
		while (current.destination() != d);
	}

	private final void addAllEdgesIncidentTo(Vertex v)
	{
		Triangle t = (Triangle) v.getLink();
		HalfEdge current = (HalfEdge) t.getAbstractHalfEdge();
		if (current.destination() == v)
			current = current.next();
		else if (current.apex() == v)
			current = current.prev();
		assert current.origin() == v;
		Vertex d = current.destination();
		do
		{
			HalfEdge h = uniqueOrientation(current);
			if (!tree.contains(h))
			{
				double val = cost(h);
				if (val <= tolerance)
				{
					tree.insert(h, val);
					h.setAttributes(AbstractHalfEdge.MARKED);
				}
			}
			h = uniqueOrientation(current.next());
			if (!tree.contains(h))
			{
				double val = cost(h);
				if (val <= tolerance)
				{
					tree.insert(h, val);
					h.setAttributes(AbstractHalfEdge.MARKED);
				}
			}
			current = current.nextOriginLoop();
		}
		while (current.destination() != d);
	}

	@Override
	public void postProcessAllHalfEdges()
	{
		logger.info("Number of swapped edges: "+processed);
		//logger.info("Number of edges which were not in the binary tree before being removed: "+notInTree);
		logger.info("Number of edges still present in the binary tree: "+tree.size());
	}

	private static void usage(int rc)
	{
		System.out.println("Usage: ImproveConnectivity [options] xmlDir outDir");
		System.out.println("Options:");
		System.out.println(" -h, --help      display this message and exit");
		System.out.println(" --coplanar <d>  adjacent triangles for which dot product of normals > d are coplanar");
		System.exit(rc);
	}


	/**
	 * 
	 * @param args xmlDir, -t tolerance | -n triangle, brepFile, output
	 */
	public static void main(final String[] args)
	{
		final HashMap<String, String> options = new HashMap<String, String>();
		int argc = 0;
		for (String arg: args)
			if (arg.equals("--help") || arg.equals("-h"))
				usage(0);
		while (argc < args.length-1)
		{
			if (args[argc].length() < 2 || args[argc].charAt(0) != '-' || args[argc].charAt(1) != '-')
				break;
			options.put(args[argc].substring(2), args[argc+1]);
			argc += 2;
		}
		if (argc + 2 != args.length)
			usage(1);
		logger.info("Load geometry file");
		final Mesh mesh = new Mesh();
		try
		{
			MeshReader.readObject3D(mesh, args[argc], "jcae3d");
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			throw new RuntimeException(ex);
		}
		new ImproveConnectivity(mesh, options).compute();
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
