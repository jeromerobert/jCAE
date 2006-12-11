/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2003,2006 by EADS CRC

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
import org.jcae.mesh.amibe.ds.HalfEdge;
import org.jcae.mesh.amibe.ds.OTriangle;
import org.jcae.mesh.amibe.ds.Vertex;
import org.jcae.mesh.xmldata.MeshReader;
import org.jcae.mesh.xmldata.MeshWriter;
import java.io.File;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.log4j.Logger;

/**
 * Split long edges.  Edges are sorted and splitted in turn, the longest edge
 * being processed first.
 * Example:  if a mesh has already been performed with a target size
 * of discr, the following commands will split edges longer then discr/2
 * <pre>
 *   SplitEdge split = new SplitEdge(m, 0.5*discr);
 *   split.compute();
 * </pre>
 * TODO: currently edges longer than sqrt(2)*discr/2 will be splitted
 *
 * When an interior edge is splitted, its midpoint is projected onto
 * the surface by {@link Vertex#discreteProject(Vertex)}; if this
 * projection fails, this edge is not splitted.  It is removed from
 * the tree because there are few chances that this projection works
 * later.  This means in particular that an interior edge whose
 * endpoints are both on boundaries cannot be splitted, because
 * discrete projection cannot be performed on boundary nodes.
 * As for now, boundary edges are always splitted, and the new point
 * is in the middle of this edge.
 *
 * In all cases, the distance between the newly inserted point and
 * apical vertices is computed; if it is too low, the edge is not
 * splitted to avoid bad triangles.
 * TODO: edges should be swapped too to improve triangle quality.
 */
public class SplitEdge extends AbstractAlgoHalfEdge
{
	private static Logger logger=Logger.getLogger(SplitEdge.class);
	private double [] newXYZ = new double[3];
	private Vertex insertedVertex = null;
	
	/**
	 * Creates a <code>SplitEdge</code> instance.
	 *
	 * @param m  the <code>Mesh</code> instance to refine.
	 * @param options 
	 */
	public SplitEdge(Mesh m, Map options)
	{
		super(m, options);
		for (Iterator it = options.entrySet().iterator(); it.hasNext(); )
		{
			Map.Entry opt = (Map.Entry) it.next();
			String key = (String) opt.getKey();
			String val = (String) opt.getValue();
			if (key.equals("size"))
			{
				double sizeTarget = new Double(val).doubleValue();
				tolerance = 1.0 / (2.0 * sizeTarget * sizeTarget);
			}
			else if (key.equals("maxtriangles"))
				nrFinal = Integer.valueOf(val).intValue();
			else
				throw new RuntimeException("Unknown option: "+key);
		}
	}
	
	public Logger thisLogger()
	{
		return logger;
	}

	public void preProcessAllHalfEdges()
	{
		// Store triangles in a LinkedHashSet to speed up removal.
		LinkedHashSet newList = new LinkedHashSet(mesh.getTriangles());
		mesh.setTrianglesList(newList);
	}

	public double cost(HalfEdge e)
	{
		double [] p0 = e.origin().getUV();
		double [] p1 = e.destination().getUV();
		double l2 = (p1[0] - p0[0]) * (p1[0] - p0[0]) +
		            (p1[1] - p0[1]) * (p1[1] - p0[1]) +
		            (p1[2] - p0[2]) * (p1[2] - p0[2]);
		if (l2 == 0.0)
			return Double.MAX_VALUE;
		else
			return 1.0 / l2;
	}

	public boolean canProcessEdge(HalfEdge current)
	{
		// New point
		double [] p0 = current.origin().getUV();
		double [] p1 = current.destination().getUV();
		for (int i = 0; i < 3; i++)
			newXYZ[i] = 0.5*(p0[i]+p1[i]);
		insertedVertex = Vertex.valueOf(newXYZ);
		if (current.hasAttributes(OTriangle.BOUNDARY))
		{
			// FIXME: Check deflection
			mesh.setRefVertexOnboundary(insertedVertex);
			return true;
		}
		// Discrete differential operators, and thus
		// discreteProject, does not work on boundary
		// nodes.
		Vertex vm = current.origin();
		if (vm.getRef() != 0)
			vm = current.destination();
		if (vm.getRef() == 0 && !vm.discreteProject(insertedVertex))
			return false;

		double dapex = insertedVertex.distance3D(current.apex());
		if (!current.hasAttributes(OTriangle.BOUNDARY))
		{
			current = current.sym();
			dapex = Math.min(dapex, insertedVertex.distance3D(current.apex()));
		}
		return (dapex * dapex > tolerance / 16.0);
	}

	public HalfEdge processEdge(HalfEdge current)
	{
		if (logger.isDebugEnabled())
			logger.debug("Split edge: "+current+" by "+insertedVertex);
		tree.remove(current.notOriented());
		current.split(mesh, insertedVertex);
		assert current.destination() == insertedVertex : insertedVertex+" "+current;
		assert mesh.isValid();
		// Update edge length
		for (int i = 0; i < 4; i++)
		{
			addToTree(current);
			current = current.prevDest();
		}
		return current.next();
	}
	
	public void postProcessAllHalfEdges()
	{
		int cnt = 0;
		for (Iterator itt = tree.iterator(); itt.hasNext(); )
		{
			HalfEdge edge = (HalfEdge) itt.next();
			if (tree.getKey(edge) > tolerance)
				break;
			cnt++;
		}
		logger.info("Number of splitted edges: "+processed);
		logger.info("Total number of edges not splitted during processing: "+notProcessed);
		logger.info("Total number of edges swapped to increase quality: "+swapped);
		logger.info("Number of edges which could have been splitted: "+cnt);
		logger.info("Number of other edges not splitted: "+(tree.size() - cnt));
	}

	/**
	 * 
	 * @param args xmlDir, -t telerance | -n triangle, brepFile, output
	 */
	public static void main(String[] args)
	{
		HashMap options = new HashMap();
		if(args[1].equals("-n"))
			options.put("maxtriangles", args[2]);
		else if(args[1].equals("-t"))
			options.put("size", args[2]);
		else
		{
			System.out.println("<xmlDir> <-t telerance | -n triangle> <brepFile> <output>");
			return;
		}
		logger.info("Load geometry file");
		Mesh mesh=MeshReader.readObject3D(args[0], "jcae3d", -1);
		new SplitEdge(mesh, options).compute();
		File brepFile=new File(args[4]);
		MeshWriter.writeObject3D(mesh, args[4], "jcae3d", brepFile.getParent(), brepFile.getName(),1);
	}
}
