/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2009, by EADS France
 
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

package org.jcae.mesh.xmldata;

public interface FilterInterface
{
	/**
	 * Nodes on inner boundaries are usually written only once on disk.  A first
	 * pass is performed to find all boundary nodes so that they can be handled
	 * specifically by {@link #processOneShape(int, java.lang.String, int, org.jcae.mesh.cad.CADFace)}.
	 *
	 * @param faces  array of face number
	 */
	public void collectBoundaryNodes(int[] faces);

	/**
	 * Initialize members which cannot be instantiated by constructor because
	 * they depend on {@link #collectBoundaryNodesOneShape(iFace)}.
	 *
	 * @param writeNormal <code>true</code> if normals are written onto disk
	 */
	public void beforeProcessingAllShapes(boolean writeNormal);

	/**
	 * Convert mesh of a given face.
	 *
	 * @param groupId    group number
	 * @param groupName  group name
	 * @param iFace      shape number
	 */
	public void processOneShape(int groupId, String groupName, int iFace);

	/**
	 * This method is called when all shapes have been processed.
	 */
	public void afterProcessingAllShapes();
}

