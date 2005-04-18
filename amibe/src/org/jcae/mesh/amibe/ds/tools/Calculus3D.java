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

package org.jcae.mesh.amibe.ds.tools;

import org.jcae.mesh.amibe.ds.*;
import org.jcae.mesh.amibe.metrics.Metric2D;
import java.util.Iterator;
import org.apache.log4j.Logger;

public class Calculus3D implements Calculus
{
	private static Logger logger=Logger.getLogger(Calculus3D.class);	

	//  The Mesh instance on which methods are applied
	protected Mesh mesh;
	
	/**
	 * Constructor.
	 *
	 * @param  m   the <code>Mesh</code> being modified.
	 */
	public Calculus3D(Mesh m)
	{
		mesh = m;
	}

	/**
	 * Returns the 3D-euclidian distance to another <code>Vertex</code> node.
	 * For both points, 3D coordinates are computed and the euclidian
	 * distance is returned.
	 *
	 * @param surf  the geometrical surface
	 * @param end  the node to which distance is computed.
	 * @return the distance to <code>end</code>.
	 **/
	public double distance(Vertex start, Vertex end)
	{
		return Math.max(distance(start, end, start), distance(start, end, end));
	}
	
	public double distance(Vertex start, Vertex end, Vertex vm)
	{
		double ret;
		Metric2D m = vm.getMetrics(mesh.getGeomSurface());
		ret = m.distance(start.getUV(), end.getUV());
		return ret;
	}
	
	public double radius2d(Vertex vm)
	{
		Metric2D m = vm.getMetrics(mesh.getGeomSurface());
		return 1.0 / Math.sqrt(m.minEV());
	}
	
	public double length(OTriangle ot)
	{
		return distance(ot.origin(), ot.destination());
	}
	
}
