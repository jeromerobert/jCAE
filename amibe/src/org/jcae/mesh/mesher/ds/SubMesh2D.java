/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.
 
	Copyright (C) 2003 Jerome Robert <jeromerobert@users.sourceforge.net>
 
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

package org.jcae.mesh.mesher.ds;

import java.util.Iterator;
import java.util.HashSet;
import org.apache.log4j.Logger;

/**
 * 2D discretization of a single topological surface.
 * A <code>SubMesh2D</code> instance is created by projecting a
 * <code>MMesh1D</code> instance (which is the mesh boundary) to a 2D plane,
 * and by meshing its interior.
 */

public class SubMesh2D
{
	private static Logger logger=Logger.getLogger(SubMesh2D.class);	

	//  Set of faces
	private HashSet faceset = new HashSet();
	
	//  Set of nodes
	private HashSet nodeset = new HashSet();
	
	/**
	 * Returns an iterator over the set of faces.
	 *
	 * @return an iterator over the set of faces.
	 */
	public Iterator getFacesIterator()
	{
		return faceset.iterator();
	}
	
	/**
	 * Returns an iterator over the set of nodes.
	 *
	 * @return an iterator over the set of nodes.
	 */
	public Iterator getNodesIterator()
	{
		return nodeset.iterator();
	}
	
	/**
	 * Adds a triangle defined by its 3 vertices.
	 *
	 * @param n1  first node
	 * @param n2  second node
	 * @param n3  third node
	 * @return a newly created triangle
	 */
	public MFace2D addTriangle(MNode2D n1, MNode2D n2, MNode2D n3)
	{
		nodeset.add(n1);
		nodeset.add(n2);
		nodeset.add(n3);
		MFace2D f = new MFace2D(n1, n2, n3);
		faceset.add(f);
		return f;
	}
	
	public String toString()
	{
		String cr=System.getProperty("line.separator");
		String r="SubMesh2D";
		logger.debug("Printing "+r+"...");
		r+=cr;
		for(Iterator itn=nodeset.iterator();itn.hasNext();)
		{
			MNode2D node=(MNode2D)itn.next();
			r+=node+cr;
		}
		for(Iterator itf=faceset.iterator();itf.hasNext();)
		{
			MFace2D face=(MFace2D)itf.next();
			r+=face+cr;
		}
		logger.debug("...done");
		return r;
	}
}
