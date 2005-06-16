/*
 * Project Info:  http://jcae.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * (C) Copyright 2005, by EADS CRC
 */

package org.jcae.viewer3d.cad;

/**
 * This class describe the mesh of CAD surface.
 * This mesh is intended for shaded visualisation.
 * @author Jerome Robert
 */
public interface FaceMesh
{
	/** Return the nodes of the mesh.
	 * The array contains {x1, y1, z1, x2, y2, z2 ...} 
	 */
	float[] getNodes();
	/** Return the triangles of the mesh.
	 * The array contains nodes id, {triangle1_node1, t1_n2, t1_n3, t2_n1...} 
	 */
	int[] getMesh();
}
