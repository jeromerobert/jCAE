/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2004 Jerome Robert <jeromerobert@users.sourceforge.net>

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
 * Describe a geometrical surface
 */
public interface CADGeomSurface
{
    /**
     * Initialize the degree of the surface
     * @param degree The degree of the surface
     */    
	public void dinit(int degree);
    
	/**
     * Set the u, v coordinates used for d1U, d1V and curvature operation
     * @param u The u coordinate
     * @param v The v coordinate
     */    
	public void setParameter(double u, double v);
    
	/**
     * Return the u first derivative vector at the coordinates set by setParameter method
     * @return the u first derivative
     */    
	public double [] d1U();
    
	/**
     * Return the v first derivative vector at the coordinates set by setParameter method
     * @return the v first derivative
     */    
	public double [] d1V();
    
	/**
     * Get 3D coordinates from (u, v) coordinates
     * @param u The u coordinate
     * @param v The v coordinate
     * @return A array {x, y, z}
     */    
	public double [] value(double u, double v);
    /**
     * Return the minimum curvature at the current point
     * @return the minimum curvature at the current point
     */    
	public double minCurvature();
    /**
     * Return the maximum curvature at the current point
     * @return The maximum curvature at the current point
     */    
	public double maxCurvature();
    /**
     * Return the direction of maximum and minimum curvature at the current point
     * @return An array: {Xmax, Ymax, Zmax, Xmin, Ymin, Zmin}
     */    
	public double [] curvatureDirections();
}
