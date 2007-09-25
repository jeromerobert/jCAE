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

import org.jcae.mesh.bora.ds.BCADGraphCell;
import org.jcae.mesh.bora.ds.BDiscretization;
import org.jcae.mesh.mesher.ds.SubMesh1D;
import org.jcae.mesh.mesher.ds.MEdge1D;
import org.jcae.mesh.mesher.ds.MNode1D;
import org.jcae.mesh.cad.*;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.log4j.Logger;

/**
 * Computes a new discretization so that all edges have a uniform length.
 * On each edge, compute the number of subdivisions so that all segments
 * have the same length, which must be less than the given criterion.
 * The previous discretization nodes and edges are deleted, and replaced
 * by newer ones.
 */
public class UniformLengthDeflection1d implements AlgoInterface
{
	private static Logger logger=Logger.getLogger(UniformLengthDeflection1d.class);
	private double maxlen;
	private double deflection;
	private boolean relDefl;

	/**
	 * Discretizes a topological edge so that all edges have a uniform length.
	 * For a given topological edge, its previous discretization is first
	 * removed.  Then the number of segments is computed such that segment
	 * length is inferior to the desired length.  The geometrical edge is then
	 * divided into segments of uniform lengths.
	 *
	 * @param len  target length
	 * @param defl  target deflection
	 * @param rel  <code>true</code> if deflection is relative,
	 * <code>false</code> otherwise.
	 */
	public UniformLengthDeflection1d(double len, double defl, boolean rel)
	{
		maxlen = len;
		deflection = defl;
		relDefl = rel;
	}

	public boolean isAvailable()
	{
		return true;
	}

	public int getOrientation(int o)
	{
		return o;
	}

	/**
	 * @return <code>true</code> if this edge was successfully discretized,
	 * <code>false</code> otherwise.
	 */
	public boolean compute(BDiscretization d)
	{
		int nbPoints;
		boolean isCircular = false;
		boolean isDegenerated = false;
		double[] paramOnEdge;
		double range[];
		assert null == d.getMesh();
		BCADGraphCell cell = d.getGraphCell();
		CADEdge E = (CADEdge) cell.getShape();
		SubMesh1D submesh1d = new SubMesh1D(E);
		d.setMesh(submesh1d);
		logger.debug(""+this+"  shape: "+E);
		
		ArrayList<MEdge1D> edgelist = submesh1d.getEdges();
		ArrayList<MNode1D> nodelist = submesh1d.getNodes();
		if (edgelist.size() != 0 || nodelist.size() != 0)
			return false;
		edgelist.clear();
		nodelist.clear();
		BCADGraphCell [] child = new BCADGraphCell[2];
		{
			Iterator<BCADGraphCell> it = cell.shapesExplorer(CADShapeEnum.VERTEX);
			child[0] = it.next();
			child[1] = it.next();
		}
		CADVertex [] V = E.vertices();
		if (V[0].isSame(V[1]))
			isCircular=true;
		
		CADGeomCurve3D curve = CADShapeFactory.getFactory().newCurve3D(E);
		if (curve == null)
		{
			if (!E.isDegenerated())
				throw new java.lang.RuntimeException("Curve not defined on edge, but this  edhe is not degenrerated.  Something must be wrong.");
			
			isDegenerated = true;
			// Degenerated edges should not be discretized, but then
			// their vertices have very low connectivity.  So let
			// discretize them until a solution is found.
			range = E.range();
			nbPoints=2;
			paramOnEdge = new double[nbPoints];
			for (int i = 0; i < nbPoints; i++)
				paramOnEdge[i] = range[0] + (range[1] - range[0])*i/(nbPoints-1);
		}
		else
		{
			range = curve.getRange();
			if (deflection > 0.0)
				curve.discretize(maxlen, deflection, relDefl);
			else
				curve.discretize(maxlen);
			nbPoints = curve.nbPoints();
			int saveNbPoints =  nbPoints;
			if (nbPoints <= 2 && !isCircular)
			{
				//  Compute the deflection
				double mid1[], pnt1[], pnt2[];

				mid1 = curve.value((range[0] + range[1])/2.0);
				pnt1 = V[0].pnt();
				pnt2 = V[1].pnt();
				double d1 =
					(mid1[0] - 0.5*(pnt1[0]+pnt2[0])) * (mid1[0] - 0.5*(pnt1[0]+pnt2[0])) +
					(mid1[1] - 0.5*(pnt1[1]+pnt2[1])) * (mid1[1] - 0.5*(pnt1[1]+pnt2[1])) +
					(mid1[2] - 0.5*(pnt1[2]+pnt2[2])) * (mid1[2] - 0.5*(pnt1[2]+pnt2[2]));
				double d2 =
					(pnt1[0] - pnt2[0]) * (pnt1[0] - pnt2[0]) +
					(pnt1[1] - pnt2[1]) * (pnt1[1] - pnt2[1]) +
					(pnt1[2] - pnt2[2]) * (pnt1[2] - pnt2[2]);
				if (d1 > 0.01 * d2 && (deflection <= 0 || d1 > 1.e-6 * maxlen * maxlen)) {
					nbPoints=3;
				} else {
					nbPoints=2;
				}
			}
			else if (nbPoints <= 3 && isCircular)
				nbPoints=4;
			if (saveNbPoints != nbPoints)
				curve.discretize(nbPoints);
			paramOnEdge = new double[nbPoints];
			// GCPnts_UniformAbscissa is not very accurate, force paramOnEdge
			// to be in ascending order.
			int offset = 0;
			paramOnEdge[0] = curve.parameter(1);
			if (range[0] < range[1])
			{
				for (int i = 1; i < nbPoints; i++)
				{
					paramOnEdge[i-offset] = curve.parameter(i+1);
					if (paramOnEdge[i-offset] <= paramOnEdge[i-offset-1])
						offset++;
				}
			}
			else
			{
				for (int i = 1; i < nbPoints; i++)
				{
					paramOnEdge[i-offset] = curve.parameter(i+1);
					if (paramOnEdge[i-offset] >= paramOnEdge[i-offset-1])
						offset++;
				}
			}
			nbPoints -= offset;
		}

		MNode1D n1, n2;
		double param;

		//  First vertex
		BDiscretization dc = child[0].getDiscretizationSubMesh(d.getFirstSubMesh());
		CADVertex GPt = (CADVertex) dc.getMesh();
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
			{
				dc = child[1].getDiscretizationSubMesh(d.getFirstSubMesh());
				GPt = (CADVertex) dc.getMesh();
			}
			n2 = new MNode1D(param, GPt);
			n2.isDegenerated(isDegenerated);
			nodelist.add(n2);
			MEdge1D e=new MEdge1D(n1, n2);
			edgelist.add(e);
			n1 = n2;
		}
		if (logger.isDebugEnabled())
		{
			logger.debug("  Edges   "+edgelist.size());
			logger.debug("  Nodes   "+nodelist.size());
		}
		return true;
	}
	
	@Override
	public String toString()
	{
		String ret = "Algo: "+getClass().getName();
		ret += "\nTarget size: "+maxlen;
		ret += "\nDeflection: "+deflection;
		if (relDefl)
			ret += " (relative)";
		else
			ret += " (absolute)";
		return ret;
	}
}
