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
 * Explores edges of a wire
 */
public interface CADWireExplorer
{
    /**
     * Initialize the explorer
     * @param wire The wire to explore
     * @param face The underlying face (used for 2D analysis)
     */    
	public void init(CADWire wire, CADFace face);
    /**
     * Return true if there are more edges to explore
     * @return true if there are more edges to explore
     */    
	public boolean more();
    /**
     * Moves on to the next edge in the exploration
     */    
	public void next();
    /**
     * Return the current edge in the exploration
     * @return The current edge in the exploration
     */    
	public CADEdge current();
}
