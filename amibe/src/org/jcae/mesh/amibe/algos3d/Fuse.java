/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2003,2004,2005
                  Jerome Robert <jeromerobert@users.sourceforge.net>

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

import org.jcae.mesh.amibe.ds.MMesh3D;
import org.jcae.mesh.amibe.ds.MFace3D;
import org.jcae.mesh.amibe.ds.MNode3D;
import org.jcae.mesh.amibe.util.Octree;
import java.util.Iterator;
import java.util.HashMap;
import org.apache.log4j.Logger;

/**
 * (Obsolete) Fuse near nodes in a <code>MMesh3D</code> instance.
 */
public class Fuse
{
	private static Logger logger=Logger.getLogger(Fuse.class);
	private MMesh3D mesh;
	private double tolerance = 0.0;
	
	/**
	 * Creates a <code>Fuse</code> instance.
	 *
	 * @param m  the <code>MMesh3D</code> instance to fuse.
	 */
	public Fuse(MMesh3D m)
	{
		mesh = m;
	}
	
	/**
	 * Creates a <code>Fuse</code> instance.
	 *
	 * @param m  the <code>MMesh3D</code> instance to refine.
	 * @param eps  tolerance.
	 */
	public Fuse(MMesh3D m, double eps)
	{
		mesh = m;
		tolerance = eps;
	}
	
	/**
	 * Fuse boundary nodes which are closer than a given bound.
	 */
	public void compute()
	{
		logger.debug("Running Fuse");
		double [] bmin = new double[3];
		double [] bmax = new double[3];
		for (int i = 0; i < 3; i++)
		{
			bmin[i] = Double.MAX_VALUE;
			bmax[i] = Double.MIN_VALUE;
		}
		for (Iterator it=mesh.getNodesIterator(); it.hasNext(); )
		{
			MNode3D n = (MNode3D) it.next();
			double [] oldp = n.getXYZ();
			for (int i = 0; i < 3; i++)
			{
				bmin[i] = Math.min(bmin[i], oldp[i]);
				bmax[i] = Math.max(bmax[i], oldp[i]);
			}
		}
		//  Enlarge the bounding box
		for (int i = 0; i < 3; i++)
		{
			if (bmin[i] > 0.0)
				bmin[i] *= 0.99;
			else
				bmin[i] *= 1.01;
			if (bmax[i] > 0.0)
				bmax[i] *= 1.01;
			else
				bmax[i] *= 0.99;
		}
		Octree octree = new Octree(bmin, bmax);
		HashMap map = new HashMap();
		int nSubst = 0;
		for (Iterator it = mesh.getNodesIterator(); it.hasNext(); )
		{
			MNode3D n = (MNode3D) it.next();
			if (n.isMutable())
				continue;
			MNode3D p = octree.getNearestVertex(n);
			if (p == null || p.isMutable() || n.distance(p) > tolerance)
				octree.add(n);
			else
			{
				logger.debug("Node "+n+" is removed, it is too close from "+p);
				nSubst++;
				map.put(n, p);
				n.clearRef();
			}
		}
		logger.debug(""+nSubst+" node(s) are removed");
		for (Iterator it = mesh.getFacesIterator(); it.hasNext(); )
		{
			MFace3D face = (MFace3D) it.next();
			for (Iterator itn = face.getNodesIterator(); itn.hasNext(); )
			{
				MNode3D n = (MNode3D) itn.next();
				MNode3D p = (MNode3D) map.get(n);
				if (p != null)
				{
					face.substNode(n, p);
					mesh.removeNode(n);
				}
			}
		}
	}
}
