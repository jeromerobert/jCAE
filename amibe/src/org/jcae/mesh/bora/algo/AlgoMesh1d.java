/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2003,2004,2005,2006, by EADS CRC
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

package org.jcae.mesh.bora.algo;

import org.jcae.mesh.amibe.algos1d.Compat1D2D;
import org.jcae.mesh.amibe.algos1d.UniformLength;
import org.jcae.mesh.amibe.algos1d.UniformLengthDeflection;
import org.jcae.mesh.amibe.ds.MMesh1D;
import org.jcae.mesh.amibe.ds.SubMesh1D;
import org.jcae.mesh.bora.ds.BDiscretization;
import java.util.Iterator;
import java.util.HashMap;
import org.apache.log4j.Logger;

/**
 * Computes a new discretization of the edges using the local definition
 * of target length and target deflection for each edge.
 */
public class AlgoMesh1d
{
	private static Logger logger=Logger.getLogger(AlgoMesh1d.class);
	private MMesh1D mesh1d;
	
	/**
	 * Creates a <code>AlgoMesh1d</code> instance.
	 *
	 * @param m  the <code>MMesh1D</code> instance to mesh.
	 */
	public AlgoMesh1d(MMesh1D m)
	{
		mesh1d = m;
	}

	/**
	 * Explores each edge discretization of the mesh and calls the 
	 * discretisation method.
	 */
	public void compute(boolean relDefl)
	{
		int nbTEdges = 0, nbNodes = 0, nbEdges = 0;
		double currentDiscrLength;
		double currentDiscrDeflec;
		/* Explore the shape for each edge */
		Iterator<BDiscretization> ite = mesh1d.getBEdgeIterator();
		/*  First compute current nbNodes and nbEdges  */
		while (ite.hasNext())
		{
		        BDiscretization discrE = ite.next();
			SubMesh1D submesh1d = mesh1d.getSubMesh1DFromMap(discrE);
			nbNodes += submesh1d.getNodes().size();
			nbEdges += submesh1d.getEdges().size();
		}
		ite = mesh1d.getBEdgeIterator();
		while (ite.hasNext())
		{
		        BDiscretization discrE = ite.next();
			SubMesh1D submesh1d = mesh1d.getSubMesh1DFromMap(discrE);
			nbNodes -= submesh1d.getNodes().size();
			nbEdges -= submesh1d.getEdges().size();

			currentDiscrLength = discrE.getConstraint().getHypothesis().getLength();
			currentDiscrDeflec = discrE.getConstraint().getHypothesis().getDeflection();
			HashMap<String, String> options1d = new HashMap<String, String>();
			options1d.put("size", ""+currentDiscrLength);
			options1d.put("deflection", ""+currentDiscrDeflec);
			options1d.put("relativeDeflection", ""+relDefl);

			if (currentDiscrDeflec <= 0.0)
			{
				if (new UniformLength(mesh1d, options1d).computeEdge(submesh1d))
					nbTEdges++;
			}
			else
			{
				if (new UniformLengthDeflection(mesh1d, options1d).computeEdge(submesh1d))
				{
					nbTEdges++;
					// Feature not available yet!
 					// if (isotropic)
 					// 	algoCompat1D2D.computeEdge(submesh1d, Set faceset, currentDiscrDeflec, relDefl);
					// The computation of faceset still has to be done.
				}
			}
			nbNodes += submesh1d.getNodes().size();
			nbEdges += submesh1d.getEdges().size();
			System.out.println(discrE+": nbNodes = "+submesh1d.getNodes().size()+" nbEdges = "+submesh1d.getEdges().size());
		}
		logger.debug("TopoEdges discretisees "+nbTEdges);
		logger.debug("Edges   "+nbEdges);
		logger.debug("Nodes   "+nbNodes);
		assert(mesh1d.isValid());
	}

}
