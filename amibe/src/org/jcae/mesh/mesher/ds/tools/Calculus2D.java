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

package org.jcae.mesh.mesher.ds.tools;

import org.jcae.mesh.mesher.ds.*;
import java.util.Iterator;
import org.apache.log4j.Logger;

public class Calculus2D extends Calculus
{
	private static Logger logger=Logger.getLogger(Calculus2D.class);	

	//  The SubMesh2D instance on which methods are applied
	protected SubMesh2D submesh2d;
	
	/**
	 * Constructor.
	 *
	 * @param  m   the <code>SubMesh2D</code> being modified.
	 */
	public Calculus2D(SubMesh2D m)
	{
		submesh2d = m;
	}

	/**
	 * Returns the angle at which a segment is seen.
	 * Currently this routine performs computations in the 2D space using the
	 * euclidian distance, but in a near future it has to be replaced by a
	 * metrics depending on local variations of <code>F</code>.
	 *
	 * @param n0  view point
	 * @param n1  first node of the segment
	 * @param n2  second node of the segment
	 * @return the angle at which the segment is seen.
	 **/
	public double angle(MNode2D n0, MNode2D n1, MNode2D n2)
	{
		double normPn1 = distance(n0, n1);
		double normPn2 = distance(n0, n2);
		if ((normPn1 == 0.0) || (normPn2 == 0.0))
			return 0.0;
		double normPn3 = distance(n1, n2);
		double mu, alpha;
		if (normPn1 < normPn2)
		{
			double temp = normPn1;
			normPn1 = normPn2;
			normPn2 = temp;
		}
		if (normPn2 < normPn3)
			mu = normPn2 - (normPn1 - normPn3);
		else
			mu = normPn3 - (normPn1 - normPn2);
		double denom = (normPn1+(normPn2+normPn3))*((normPn1-normPn3)+normPn2);
		if (denom == 0.0)
			//  Sign does not matter because triangles are not oriented.
			//  The 0/0 case can only happen when all points are equal,
			//  which is caught at the beginning of this prtocedure.
			return Math.PI / 2.0;
		alpha = ((normPn1-normPn2)+normPn3)*mu / denom;
		if (alpha <= 0.0)
			return 0.0;
		return 2.0 * Math.atan(Math.sqrt(alpha));
	}
	
	/**
	 * Returns the distance to another <code>MNode2D</code> instance.
	 * Currently this routine returns the euclidian distance, but in
	 * a near future it has to be replaced by a metrics depending on
	 * local variations of <code>F</code>.
	 *
	 * @param start  the first node
	 * @param end  the node to which distance is computed.
	 * @return the distance to between the two nodes.
	 **/
	public double distance(MNode2D start, MNode2D end)
	{
		double [] x0 = start.getUV();
		double [] x1 = end.getUV();
		return Math.sqrt(
					Math.pow(x0[0] - x1[0], 2) +
					Math.pow(x0[1] - x1[1], 2)
		       );
	}
	
	/**
	 * Returns the minimal angle of a triangle.
	 * Currently this routine returns the euclidian distance, but in
	 * a near future it has to be replaced by a metrics depending on
	 * local variations of <code>F</code>.
	 *
	 * @param F  current topological face,
	 * @param n1  first node,
	 * @param n2  second node,
	 * @return the minimal angle of the triangle composed of this instance,
	 * <code>n1</code> and <code>n2</code>.
	 **/
	public double quality(MNode2D n0, MNode2D n1, MNode2D n2)
	{
		double Q = 2 * Math.PI;
		double a0 = 2 * Math.PI, a1 = 2 * Math.PI, a2 = 2 * Math.PI;
		
		a1 = Math.abs(angle(n0, n1, n2));
		a2 = Math.abs(angle(n1, n2, n0));
		a0 = Math.abs(angle(n2, n0, n1));
		// compute the triangle quality
		Q = Math.min(a0, a1);
		Q = Math.min(a2, Q);
		return Q;
	}
	
	//  This quality criterion can be used with non-Euclidian metrics.
	//  It is the ratio between radii of inner and outer circles.
	public double qualityAniso(MNode2D n0, MNode2D n1, MNode2D n2)
	{
		double l1 = distance(n0, n1);
		double l2 = distance(n0, n2);
		double l3 = distance(n1, n2);
		double p = (l1+l2+l3)*0.5;
		double ret = 1.0;
		if (l1 > p * 1.e-3)
			ret *= (p/l1) - 1.0;
		else
			ret = 0.0;
		if (l2 > p * 1.e-3)
			ret *= (p/l2) - 1.0;
		else
			ret = 0.0;
		if (l3 > p * 1.e-3)
			ret *= (p/l3) - 1.0;
		else
			ret = 0.0;
		if (ret <= 0.0)
			ret = 0.0;
		return ret;
	}
	
	public double quality(MFace2D f)
	{
		Iterator it = f.getNodesIterator();
		MNode2D n1 = (MNode2D) it.next();
		MNode2D n2 = (MNode2D) it.next();
		MNode2D n3 = (MNode2D) it.next();
		
		return quality(n1, n2, n3);
	}
	
	public double length(MEdge2D edge)
	{
        return distance(edge.getNodes1(), edge.getNodes2());
	}
	
}
