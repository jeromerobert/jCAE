/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2004,2005
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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */

package org.jcae.mesh.cad;

/**
 * Describe a geometrical 2D curve
 */
public interface CADGeomCurve3D
{
	/**
	 * Return a point on this curve
	 * @param p The paramater
	 * @return an array {x, y, z}
	 */
	public double [] value(double p);
	
	/**
	 * Return the range of the parametrization of this edge
	 * @return an array {pMin, pMax}
	 */
	public double [] getRange();
	 
	/**
	 * Discretize this curve
	 * @param maxlen Maximum length of created arc
	 */
	public void discretize(double maxlen);
	
	/**
	 * Discretize this curve
	 * @param maxlen Maximum length of created arc
	 * @param deflection Maximum deflection
	 * @param relDefl set to true if deflective is relative
	 */
	public void discretize(double maxlen, double deflection, boolean relDefl);
	
	public void splitSubsegment(int numseg, int nrsub);
	
	/**
	 * Discretize this curve
	 * @param n Number of arcs to create
	 */
	public void discretize(int n);
	
	/**
	 * Set a discretization
	 * @param param 1D parameters
	 */
	public void setDiscretization(double [] param);
	
	/**
	 * Return the number of arc created after discretization
	 * @return Number of arc created after discretization
	 */
	public int nbPoints();
	
	/**
	 * Return the parameter of the created point after discretization
	 * @return The parameter matching this point
	 * @param index Point ID
	 */
	public double parameter(int index);
	
	/**
	 * Return the length of this curve
	 * @return the length of this curve
	 */
	public double length();
}
