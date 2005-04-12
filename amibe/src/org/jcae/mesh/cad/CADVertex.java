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
 * Describe a topological vertex
 */
public interface CADVertex extends CADShape
{
    /**
     * Return the 2D coordinates (u, v) of this vertex on a face
     * @return An array: {u, v}
     * @param that The face
     */    
	public double [] parameters(CADFace that);
    /**
     * Return the 3D coordinates of this vertex
     * @return An array: {x, y, z}
     */    
	public double [] pnt();
}
