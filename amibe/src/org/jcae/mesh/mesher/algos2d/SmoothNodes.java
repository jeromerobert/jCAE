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
import java.util.Iterator;
import org.apache.log4j.Logger;

/**
 * Laplacian smoothing in the 2D space.
 */

public class SmoothNodes
{
	private static Logger logger=Logger.getLogger(SmoothNodes.class);
	private SubMesh2D submesh;
	private int nloop = 10;
	
	/**
	 * Creates a <code>SmoothNodes</code> instance.
	 *
	 * @param m  the <code>SubMesh2D</code> instance to refine.
	 */
	public SmoothNodes(SubMesh2D m)
	{
		submesh = m;
	}
	
	/**
	 * Creates a <code>SmoothNodes</code> instance.
	 *
	 * @param m  the <code>SubMesh2D</code> instance to refine.
	 * @param n  the number of iterations.
	 */
	public SmoothNodes(SubMesh2D m, int n)
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
		logger.debug("Running SmoothNodes");
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
		//  Relaxation factor;
		double lambda = 0.8;
		for (Iterator it=submesh2d.getNodesIterator(); it.hasNext(); )
		{
			MNode2D n = (MNode2D) it.next();
			if (!n.isMutable())
				continue;
			
			double[] oldp = n.getUV();
			int nn = 0;
			double[] c = new double[2];
			c[0] = c[1] = 0.;
			double[] newp;
			for (Iterator itn=n.getNeighboursNodes().iterator(); itn.hasNext(); )
			{
				nn++;
				newp = ((MNode2D) itn.next()).getUV();
				c[0] += newp[0];
				c[1] += newp[1];
			}
			assert (nn > 0);
			c[0] /= nn;
			c[1] /= nn;
			double deltaU = c[0] - oldp[0];
			double deltaV = c[1] - oldp[1];
			n.setUV(oldp[0] + lambda * deltaU, oldp[1] + lambda * deltaV);
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
		assert (submesh2d.isValid());
	}
}
