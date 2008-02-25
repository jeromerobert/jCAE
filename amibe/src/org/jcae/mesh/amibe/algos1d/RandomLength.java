/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2006, by EADS CRC
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

package org.jcae.mesh.amibe.algos1d;

import org.jcae.mesh.amibe.ds.MEdge1D;
import org.jcae.mesh.amibe.ds.MMesh1D;
import org.jcae.mesh.amibe.ds.MNode1D;
import org.jcae.mesh.amibe.ds.SubMesh1D;
import org.jcae.mesh.cad.CADGeomCurve3D;
import org.jcae.mesh.cad.CADVertex;
import org.jcae.mesh.cad.CADEdge;
import org.jcae.mesh.cad.CADShapeFactory;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Computes a randomized discretization.
 * This is only useful when debugging, to check that adjacent faces
 * share the same 1D discretization.
 */
public class RandomLength
{
	private static Logger logger=Logger.getLogger(RandomLength.class.getName());
	private final MMesh1D mesh1d;
	private int divisions;
	
	/**
	 * Creates a <code>UniformLength</code> instance.
	 *
	 * @param m  the <code>MMesh1D</code> instance to refine.
	 */
	public RandomLength(MMesh1D m, final Map<String, String> options)
	{
		mesh1d = m;

		for (final Map.Entry<String, String> opt: options.entrySet())
		{
			final String key = opt.getKey();
			final String val = opt.getValue();
			if (key.equals("divisions"))
				divisions = Integer.valueOf(val).intValue();
			else
				throw new RuntimeException("Unknown option: "+key);
		}
	}

	/**
	 * Explores each edge of the mesh and calls the discretisation method.
	 */
	public void compute()
	{
		int nbTEdges = 0, nbNodes = 0, nbEdges = 0;
		/* Explore the shape for each edge */
		Iterator<CADEdge> ite = mesh1d.getTEdgeIterator();
		/*  First compute current nbNodes and nbEdges  */
		while (ite.hasNext())
		{
			CADEdge E = ite.next();
			SubMesh1D submesh1d = mesh1d.getSubMesh1DFromMap(E);
			nbNodes += submesh1d.getNodes().size();
			nbEdges += submesh1d.getEdges().size();
		}
		ite = mesh1d.getTEdgeIterator();
		while (ite.hasNext())
		{
			CADEdge E = ite.next();
			SubMesh1D submesh1d = mesh1d.getSubMesh1DFromMap(E);
			nbNodes -= submesh1d.getNodes().size();
			nbEdges -= submesh1d.getEdges().size();
			if (computeEdge(submesh1d))
				nbTEdges++;
			nbNodes += submesh1d.getNodes().size();
			nbEdges += submesh1d.getEdges().size();
		}
		logger.fine("TopoEdges discretisees "+nbTEdges);
		logger.fine("Edges   "+nbEdges);
		logger.fine("Nodes   "+nbNodes);
		assert(mesh1d.isValid());
	}

	private boolean computeEdge(SubMesh1D submesh1d)
	{
		int nbPoints;
		boolean isCircular = false;
		boolean isDegenerated = false;
		double[] paramOnEdge;
		double range[];
		CADEdge E = submesh1d.getGeometry();
		
		//  See also org.jcae.mesh.amibe.ds.Mesh.tooSmall()
		//if (BRep_Tool.degenerated(E))
		//	return false;
		
		List<MEdge1D> edgelist = submesh1d.getEdges();
		List<MNode1D> nodelist = submesh1d.getNodes();
		if (edgelist.size() != 1 || nodelist.size() != 2)
			return false;
		edgelist.clear();
		nodelist.clear();
		CADVertex[] V = E.vertices();
		if (V[0].isSame(V[1]))
			isCircular=true;
		
		CADGeomCurve3D curve = CADShapeFactory.getFactory().newCurve3D(E);
		if (curve == null)
		{
			if (!E.isDegenerated())
				throw new java.lang.RuntimeException("Curve not defined on edge, but this  edge is not degenerated.  Something must be wrong.");
			
			isDegenerated = true;
			range = E.range();
			nbPoints=2;
			paramOnEdge = new double[nbPoints];
			for (int i = 0; i < nbPoints; i++)
				paramOnEdge[i] = range[0] + (range[1] - range[0])*i/(nbPoints-1);
		}
		else
		{
			range = curve.getRange();
			Random rand = new Random(67L);
			nbPoints = divisions + 1;
			paramOnEdge = new double[nbPoints];
			paramOnEdge[0] = 0.0;
			for (int i = 1; i < nbPoints; i++)
				paramOnEdge[i] = paramOnEdge[i-1] + rand.nextDouble();
			double scale = (range[1] - range[0]) / paramOnEdge[nbPoints-1];
			for (int i = 0; i < nbPoints; i++)
				paramOnEdge[i] = range[0] + scale*paramOnEdge[i];
		}

		MNode1D n1, n2;
		double param;

		//  First vertex
		CADVertex GPt = mesh1d.getGeometricalVertex(V[0]);
		MNode1D firstNode = new MNode1D(paramOnEdge[0], GPt);
		n1 = firstNode;
		n1.isDegenerated(isDegenerated);
		nodelist.add(n1);
		if (!isDegenerated)
			GPt = null;

		//  Other points
		for (int i = 0; i < nbPoints - 1; i++)
		{
			param = paramOnEdge[i+1];
			if (i == nbPoints - 2)
				GPt = mesh1d.getGeometricalVertex(V[1]);
			n2 = new MNode1D(param, GPt);
			n2.isDegenerated(isDegenerated);
			nodelist.add(n2);
			MEdge1D e=new MEdge1D(n1, n2);
			edgelist.add(e);
			n1 = n2;
		}
		return true;
	}
}
