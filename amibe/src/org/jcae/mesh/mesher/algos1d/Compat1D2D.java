/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.
 
	Copyright (C) 2005 Jerome Robert <jeromerobert@users.sourceforge.net>
 
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

package org.jcae.mesh.mesher.algos1d;

import org.jcae.mesh.mesher.ds.*;
import org.jcae.mesh.cad.*;
import org.jcae.mesh.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import org.apache.log4j.Logger;

public class Compat1D2D
{
	private static Logger logger=Logger.getLogger(Compat1D2D.class);
	private MMesh1D mesh1d;
	
	/**
	 * Creates a <code>Compat1D2D</code> instance.
	 *
	 * @param m  the <code>MMesh1D</code> instance to refine.
	 */
	public Compat1D2D(MMesh1D m)
	{
		mesh1d = m;
	}

	/**
	 * Explores each edge of the mesh and calls the discretisation method.
	 */
	public void compute(boolean relDefl)
	{
		int nbTEdges = 0, nbNodes = 0, nbEdges = 0;
		/* Explore the shape for each edge */
		Iterator ite = mesh1d.getTEdgeList().iterator();
		while (ite.hasNext())
		{
			CADEdge E = (CADEdge) ite.next();
			SubMesh1D submesh1d = mesh1d.getSubMesh1DFromMap(E);
			if (null == submesh1d)
				continue;
			HashSet faceset = mesh1d.getAdjacentFaces(E);
			if (null != faceset && computeEdge(submesh1d, faceset, mesh1d.getMaxDeflection(), relDefl))
				nbTEdges++;
			nbNodes += submesh1d.getNodes().size();
			nbEdges += submesh1d.getEdges().size();
		}
		logger.debug("Discretized TopoEdges: "+nbTEdges);
		logger.debug("Edges   "+nbEdges);
		logger.debug("Nodes   "+nbNodes);
		assert(mesh1d.isValid());
	}

	private boolean computeEdge(SubMesh1D submesh1d, HashSet faceset, double deflection, boolean relDefl)
	{
		ArrayList edgelist = submesh1d.getEdges();
		ArrayList nodelist = submesh1d.getNodes();
		double [] curvmax = new double[nodelist.size()];
		for (int i = 0; i < curvmax.length; i++)
			curvmax[i] = 0.0;
		
		CADEdge E = submesh1d.getGeometry();
		CADGeomCurve3D curve3d = CADShapeBuilder.factory.newCurve3D(E);
		if (curve3d == null)
			return true;
		double [] coord = new double[3*curvmax.length];
		double [] paramOnEdge = new double[curvmax.length];
		int k = 0;
		for (Iterator itn = nodelist.iterator(); itn.hasNext(); k++)
		{
			MNode1D p1 = (MNode1D) itn.next();
			paramOnEdge[k] = p1.getParameter();
			double [] xyz = curve3d.value(paramOnEdge[k]);
			for (int j = 0; j < 3; j++)
				coord[3*k+j] = xyz[j];
		}
		curve3d.setDiscretization(paramOnEdge);
		
		for (Iterator itf = faceset.iterator(); itf.hasNext(); )
		{
			CADFace F = (CADFace) itf.next();
			CADGeomCurve2D curve2d = CADShapeBuilder.factory.newCurve2D(E, F);
			if (curve2d == null)
				continue;
			
			CADGeomSurface surface = F.getGeomSurface();
			surface.dinit(2);
			for (int i = 0; i < curvmax.length; i++)
			{
				double [] uv = curve2d.value(paramOnEdge[i]);
				surface.setParameter(uv[0], uv[1]);
				double cmin = Math.abs(surface.minCurvature());
				double cmax = Math.abs(surface.maxCurvature());
				if (Double.isNaN(cmin) || Double.isNaN(cmax))
				{
					logger.debug("Undefined curvature");
					continue;
				}
				if (cmin > cmax)
					cmax = cmin;
				curvmax[i] = Math.max(curvmax[i], cmax);
			}
		}
		int offset = 0;
		for (int i = 0; i < curvmax.length - 1; i++)
		{
			double meanCurv = Math.max(curvmax[i], curvmax[i+1]);
			double dist2 =
				(coord[3*i]   - coord[3*i+3]) * (coord[3*i]   - coord[3*i+3]) +
				(coord[3*i+1] - coord[3*i+4]) * (coord[3*i+1] - coord[3*i+4]) +
				(coord[3*i+2] - coord[3*i+5]) * (coord[3*i+2] - coord[3*i+5]);
			double epsilon = deflection;
			if (!relDefl)
				epsilon *= meanCurv;
			if (epsilon > 1.0)
				epsilon = 1.0;
			double alpha2 = 4.0 * epsilon * (2.0 - epsilon) / 2.0;
			if (dist2 * meanCurv * meanCurv > 4.0 * alpha2)
			{
				int nrsub = (int) (Math.sqrt(dist2 * meanCurv * meanCurv / alpha2) + 0.5);
				if (nrsub > 100)
					nrsub = 100;
				curve3d.splitSubsegment(i+offset, nrsub);
				offset += nrsub - 1;
			}
		}
		
		edgelist.clear();
		nodelist.clear();
		CADVertex[] V = E.vertices();
		boolean isDegenerated = false;
		
		MNode1D n1, n2;
		double param;

		//  First vertex
		CADVertex GPt = mesh1d.getGeometricalVertex(V[0]);
		MNode1D firstNode = new MNode1D(curve3d.parameter(1), GPt);
		n1 = firstNode;
		n1.isDegenerated(isDegenerated);
		nodelist.add(n1);
		if (!isDegenerated)
			GPt = null;

		//  Other points
		int nbPoints = curve3d.nbPoints();;
		for (int i = 0; i < nbPoints - 1; i++)
		{
			param = curve3d.parameter(i+2);
			if (i == nbPoints - 2)
				GPt = mesh1d.getGeometricalVertex(V[1]);
			n2 = new MNode1D(param, GPt);
			n2.isDegenerated(isDegenerated);
			nodelist.add(n2);
			MEdge1D e=new MEdge1D(n1, n2, false);
			edgelist.add(e);
			n1 = n2;
		}
		return true;
	}
}
