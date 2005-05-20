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
 * Describe a topological shape with an orientation, a location and a
 * geometry
 * @author Denis Barbier
 *
 */
public interface CADShape
{
    	/**
    	 * Return the bounding box of this shape in an array like {Xmin, Ymin, Zmin, Xmax,
    	 * Ymax, Zmax}.
    	 */    
	public double [] boundingBox();
	/** Return a reversed instance of this shape */
	public CADShape reversed();
	/** Return the orientation of the shape */
	public int orientation();
	/** Return true if and only if the face is forward oriented*/
	public boolean isOrientationForward();
	/** Return true if o have same orientation and geometry as this object */
	public boolean equals(Object o);
	/** Return true if o have same geometry as this object */
	public boolean isSame(Object o);
	/** Write shape into the native format */
	public void writeNative(String filename);
	/** Return a hash code matching the equals method */
	public int hashCode();
}
