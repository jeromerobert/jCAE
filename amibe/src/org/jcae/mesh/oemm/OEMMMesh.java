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

package org.jcae.mesh.oemm;

import java.util.ArrayList;
import org.apache.log4j.Logger;

public class OEMMMesh
{
	private static Logger logger=Logger.getLogger(OEMMMesh.class);
	
	//  Triangle list
	private ArrayList triangleList = new ArrayList();

	/**
	 * Creates an empty mesh.
	 */
	public OEMMMesh()
	{
	}
	
	/**
	 * Adds an existing triangle to triangle list.
	 * This routine is useful when meshes are read from disk but not
	 * computed by node insertion.
	 *
	 * @param t  triangle being added.
	 */
	public void add(OEMMTriangle t)
	{
		triangleList.add(t);
	}
	
	/**
	 * Returns triangle list.
	 *
	 * @return triangle list.
	 */
	public ArrayList getTriangles()
	{
		return triangleList;
	}
}
