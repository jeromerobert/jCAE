/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2003 Jerome Robert <jeromerobert@users.sourceforge.net>

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

package org.jcae.mesh.mesher.algos2d;

import org.jcae.mesh.cad.CADGeomSurface;
import org.jcae.mesh.mesher.ds.*;
import java.util.Iterator;
import org.apache.log4j.Logger;

/**
 * Smoothing performed by moving nodes to bissector angles.
 * This algorithm is described in Surazhsky &amp; Gotsman.
 */

public class SmoothAngles
{
	private static Logger logger=Logger.getLogger(SmoothAngles.class);
	private SubMesh2D submesh;
	private int nloop = 10;
	
	/**
	 * Creates a <code>SmoothAngles</code> instance.
	 *
	 * @param m  the <code>SubMesh2D</code> instance to refine.
	 */
	public SmoothAngles(SubMesh2D m)
	{
		submesh = m;
	}
	
	/**
	 * Creates a <code>SmoothAngles</code> instance.
	 *
	 * @param m  the <code>SubMesh2D</code> instance to refine.
	 * @param n  the number of iterations.
	 */
	public SmoothAngles(SubMesh2D m, int n)
	{
		submesh = m;
		nloop = n;
	}
	
	/**
	 * Moves all nodes until all iterations are done.
	 *
	 * @see #computeFace
	 */
	public void compute()
	{
		logger.debug("Running SmoothAngles");
		for (int i = 0; i < nloop; i++)
			computeFace(submesh);
		assert (submesh.isValid());
	}
	
	/**
	 * Moves all nodes along bissector angles.
	 * This algorithm is described in Surazhsky &amp; Gotsman.
	 * When neighbours do not form a convex hull, laplacian smoothing
	 * may induce inverted triangles or decrease mesh quality.
	 * Their algorithm moves each mesh node (called <code>M</code> below)
	 * in turn following these rules:
	 * <ul>
	 *   <li>For all neighbours <code>N<i>i</i></code> of <code>M</code>,
	 *       computes a new position <code>M<i>i</i></code> of <code>M</code>
	 *       such that <code>M<i>i</i></code> is on the bissector angle of
	 *       <code>N<i>i</i></code>, the distance to <code>N<i>i</i></code>
	 *       being unchanged.</li>
	 *   <li>Moves <code>M</code> to the centroid of <code>N<i>i</i></code>
	 *       points.  Weights are added to improve degenerated cases,
	 *       their value is <code>1/(A<i>i</i>^2)</code>, where
	 *       <code>A<i>i</i></code> is the angle at vertex
	 *       <code>N<i>i</i></code>.  Setting <code>alpha=1</code> below
	 *       suppresses these weights.</li>
	 * </ul>
	 *
	 * @param submesh2d  the mesh being updated.
	 */
	public static void computeFace(SubMesh2D submesh2d)
	{
		for (Iterator it=submesh2d.getNodesIterator(); it.hasNext(); )
		{
			MNode2D n = (MNode2D) it.next();
			if (!n.isMutable())
				continue;
			computeNode(submesh2d, n);
		}
		assert (submesh2d.isValid());
	}
	
	public static void computeNode(SubMesh2D submesh2d, MNode2D n)
	{
		CADGeomSurface surf = submesh2d.getGeomSurface();
		double[] oldp = n.getUV();
		double[] c = new double[2];
		c[0] = c[1] = 0.;
		double coefs = 0.0;
		for (Iterator itn=n.getNeighboursNodes().iterator(); itn.hasNext(); )
		{
			MNode2D neigh = (MNode2D) itn.next();
			MEdge2D edge = submesh2d.getEdgeDefinedByNodes(n, neigh);
			assert (null != edge);
			double neighp[] = neigh.getUV();
			Iterator itf = edge.getFacesIterator();
			MNode2D apex1 = ((MFace2D) itf.next()).apex(edge);
			MNode2D apex2 = ((MFace2D) itf.next()).apex(edge);
			double[] apex1p = apex1.getUV();
			double[] apex2p = apex2.getUV();
			double temp, alpha1, alpha2;
			double d = submesh2d.compGeom().distance(neigh, n);
			double d1 = d * submesh2d.compGeom().distance(neigh, apex1);
			double d2 = d * submesh2d.compGeom().distance(neigh, apex2);
			temp = ((oldp[0] - neighp[0])*(apex1p[0]-neighp[0]) +
			        (oldp[1] - neighp[1])*(apex1p[1]-neighp[1]));
			if (temp >= d1)
				alpha1 = 0.0;
			else if (temp <= -d1)
				alpha1 = Math.PI;
			else
				alpha1 = Math.acos(temp / d1);
			temp = ((oldp[0] - neighp[0])*(apex2p[0]-neighp[0]) +
			        (oldp[1] - neighp[1])*(apex2p[1]-neighp[1]));
			if (temp >= d2)
				alpha2 = 0.0;
			else if (temp <= -d2)
				alpha2 = Math.PI;
			else
				alpha2 = Math.acos(temp / d2);
			double delta;
			if (neigh.orient2d(apex1, apex2, surf) >= 0.0)
				delta = (alpha2 - alpha1) * 0.5;
			else
				delta = (alpha1 - alpha2) * 0.5;
			double alpha = Math.pow(1.0/(alpha1+alpha2), 2);
			coefs += alpha;
			c[0] += alpha * (neighp[0] +
				(oldp[0] - neighp[0]) * Math.cos(delta) -
				(oldp[1] - neighp[1]) * Math.sin(delta));
			c[1] += alpha * (neighp[1] +
				(oldp[1] - neighp[1]) * Math.cos(delta) +
				(oldp[0] - neighp[0]) * Math.sin(delta));
		}
		assert (coefs != 0.0);
		c[0] /= coefs;
		c[1] /= coefs;
		n.setUV(c[0], c[1]);
/*
		//  Check that this change does not create inverted triangles
		for (Iterator ite = n.getEdgesIterator(); ite.hasNext(); )
		{
			MEdge2D edge = (MEdge2D) ite.next();
			if (!edge.checkNoInvertedTriangles())
			{
				//  Restore old position
				n.setUV(oldp[0], oldp[1]);
				break;
			}
		}
*/
	}
}
