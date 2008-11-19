/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2008, by EADS France

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

package org.jcae.mesh.amibe.projection;

import org.jcae.mesh.amibe.ds.Vertex;

/**
 * Interface to project vertices on a local surface.
 * 
 * @author Denis Barbier
 */
public interface LocalSurfaceProjection
{
	/**
	 * Flag to tell whether projection can be performed.
	 * 
	 * @return  <code>true</code> if local surface was successfully computed
	 *          and projection can be performed on it, <code>false</code>
	 *          otherwise.
	 */
	boolean canProject();
	
	/**
	 * Project a point on local surface.
	 *
	 * @param pt   point to project on the approximated surface.
	 * @return     <code>true</code> if projection has been performed
	 *             successfully, <code>false</code> otherwise.
	 */
	boolean project(Vertex v);
}
