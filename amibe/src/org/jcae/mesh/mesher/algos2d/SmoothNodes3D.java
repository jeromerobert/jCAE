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

import org.jcae.mesh.mesher.ds.*;
import org.jcae.mesh.cad.CADGeomSurface;
import org.jcae.mesh.util.Calculs;
import java.util.Iterator;
import org.apache.log4j.Logger;

/**
 * Laplacian smoothing.
 */

public class SmoothNodes3D
{
	private static Logger logger=Logger.getLogger(SmoothNodes3D.class);
	private SubMesh2D submesh;
	private int nloop = 10;
	
	/**
	 * Creates a <code>SmoothNodes3D</code> instance.
	 *
	 * @param m  the <code>SubMesh2D</code> instance to refine.
	 */
	public SmoothNodes3D(SubMesh2D m)
	{
		submesh = m;
	}
	
	/**
	 * Creates a <code>SmoothNodes3D</code> instance.
	 *
	 * @param m  the <code>SubMesh2D</code> instance to refine.
	 * @param n  the number of iterations.
	 */
	public SmoothNodes3D(SubMesh2D m, int n)
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
		logger.debug("Running SmoothNodes3D");
		for (int i = 0; i < nloop; i++)
			computeFace(submesh);
		assert (submesh.isValid());
	}
	
	/**
	 * Moves all nodes using a laplacian smoothing.
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
		surf.dinit(1);

		double[] oldp2 = n.getUV();
		double[] oldp3 = surf.value(oldp2[0], oldp2[1]);
		
		//  Compute 3D coordinates centroid
		int nn = 0;
		double[] centroid3 = new double[3];
		centroid3[0] = centroid3[1] = centroid3[2] = 0.;
		for (Iterator itn=n.getNeighboursNodes().iterator(); itn.hasNext(); )
		{
			nn++;
			double[] newp2 = ((MNode2D) itn.next()).getUV();
			double[] newp3 = surf.value(newp2[0], newp2[1]);
			centroid3[0] += newp3[0];
			centroid3[1] += newp3[1];
			centroid3[2] += newp3[2];
		}
		assert (nn > 0);
		centroid3[0] /= nn;
		centroid3[1] /= nn;
		centroid3[2] /= nn;

		surf.dinit(1);
		surf.setParameter(oldp2[0], oldp2[1]);
		double a = Calculs.prodSca(surf.d1U(), surf.d1U());
		double b = Calculs.prodSca(surf.d1U(),surf.d1V());
		double c = 0.;
		double [] N = new double[3];
		N[0] = centroid3[0] - oldp3[0];
		N[1] = centroid3[1] - oldp3[1];
		N[2] = centroid3[2] - oldp3[2];
		double e = Calculs.prodSca(surf.d1V(),surf.d1V());
		double f = Calculs.prodSca(surf.d1V(),N);
		if ((a*e)-(b*b) != 0.)
		{
			double [] newp2 = new double[2];
			newp2[0] = oldp2[0] + ((c*e)-(b*f))/((a*e)-(b*b));
			newp2[1] = oldp2[1] + ((a*f)-(c*b))/((a*e)-(b*b));
			n.setUV(newp2[0], newp2[1]);
/*
			//  Check that this change does not create inverted triangles
			for (Iterator ite = n.getEdgesIterator(); ite.hasNext(); )
			{
				MEdge2D edge = (MEdge2D) ite.next();
				if (!edge.checkNoInvertedTriangles())
				{
					//  Restore old position
					n.setUV(oldp2[0], oldp2[1]);
					break;
				}
			}
*/
		}
	}
}
