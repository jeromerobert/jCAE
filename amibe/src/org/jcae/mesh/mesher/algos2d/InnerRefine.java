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
 * Refines all inner edges.
 * The initial triangulation may cause trouble in case of colocated
 * edges, because an edge may join 2 points which are identical
 * in the 3D space.  In order to prevent this situation, InnerRefine
 * can be applied twice.  The generated mesh can then be safely
 * converted to 3D.
 */

public class InnerRefine extends CutEdges
{
	private static Logger logger=Logger.getLogger(InnerRefine.class);
	protected SubMesh2D submesh2d = null;
	
	/**
	 * Creates a <code>InnerRefine</code> instance.
	 *
	 * @param m  the <code>SubMesh2D</code> instance to refine.
	 */
	public InnerRefine(SubMesh2D m)
	{
		submesh2d = m;
	}
	
	/**
	 * Refines all edges not on boundaries.  Middle points of all mutable edges
	 * are computed, and {@link CutEdges#computeFace} is called.
	 *
	 * @see CutEdges#computeFace
	 */
	public void compute()
	{
		//  Compute the mid point of all edges
		for(Iterator it=submesh2d.getEdgesIterator(); it.hasNext(); )
		{
			MEdge2D e = (MEdge2D) it.next();
			if (e.isMutable())
				e.setMidPoint();
			else
				e.resetMidPoint();
		}
		computeFace(submesh2d);
		assert(submesh2d.isValid());
	}
}
