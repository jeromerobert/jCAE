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

package org.jcae.mesh.amibe.ds.tools;

import org.jcae.mesh.amibe.ds.*;
import java.util.Iterator;
import org.apache.log4j.Logger;

public class Calculus2D implements Calculus
{
	private static Logger logger=Logger.getLogger(Calculus2D.class);	

	//  The Mesh instance on which methods are applied
	protected Mesh mesh;
	
	/**
	 * Constructor.
	 *
	 * @param  m   the <code>Mesh</code> being modified.
	 */
	public Calculus2D(Mesh m)
	{
		mesh = m;
	}

	/**
	 * Returns the distance to another <code>Vertex</code> instance.
	 * Currently this routine returns the euclidian distance, but in
	 * a near future it has to be replaced by a metrics depending on
	 * local variations of <code>F</code>.
	 *
	 * @param start  the first node
	 * @param end  the node to which distance is computed.
	 * @return the distance to between the two nodes.
	 **/
	public double distance(Vertex start, Vertex end, Vertex vm)
	{
		return distance(start, end);
	}
	
	public double distance(Vertex start, Vertex end)
	{
		double [] x0 = start.getUV();
		double [] x1 = end.getUV();
		double dx = x0[0] - x1[0];
		double dy = x0[1] - x1[1];
		return Math.sqrt(dx*dx + dy*dy);
	}
	
	public double length(OTriangle ot)
	{
        	return distance(ot.origin(), ot.destination());
	}
	
	public double radius2d(Vertex vm)
	{
		return 1.0;
	}
	
}
