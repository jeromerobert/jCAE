/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2003,2004,2005,2006, by EADS CRC
    Copyright (C) 2007, by EADS France

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

package org.jcae.mesh.amibe.algos2d;

import org.jcae.mesh.amibe.patch.Mesh2D;

/**
 * Meshes a patch.
 * <ol>
 *   <li>Insert interior nodes (see {@link Insertion}) to
 *       have a mesh with target size of 16.</li>
 *   <li>Check compatibility between triangle normals and normals to
 *       the surface (see {@link ConstraintNormal3D}).  If triangle
 *       inversion gives better result, edges are swapped.</li>
 *   <li>Insert interior nodes to have a mesh with target size of 4.</li>
 *   <li>Check compatibility between triangle normals and normals to
 *       the surface.</li>
 *   <li>Insert interior nodes to have a mesh with target size of 1.</li>
 *   <li>Check compatibility between triangle normals and normals to
 *       the surface.</li>
 * </ol>
 */
public class BasicMesh
{
	private final Mesh2D mesh;
	
	/**
	 * Creates a <code>BasicMesh</code> instance.
	 *
	 * @param m  the data structure in which the mesh will be stored.
	 */
	public BasicMesh(Mesh2D m)
	{
		mesh = m;
	}
	
	/**
	 * Launch method to mesh a surface.
	 */
	public void compute()
	{
		mesh.pushCompGeom(3);
		new Insertion(mesh, 16.0 / Math.sqrt(2.0), 16.0 * Math.sqrt(2.0)).compute();
		new ConstraintNormal3D(mesh).compute();
		new Insertion(mesh, 4.0 / Math.sqrt(2.0), 4.0 * Math.sqrt(2.0)).compute();
		new ConstraintNormal3D(mesh).compute();
		new Insertion(mesh, 1.0 / Math.sqrt(2.0), Math.sqrt(2.0)).compute();
		mesh.popCompGeom(3);
		
		assert (mesh.isValid());
	}
	
}
