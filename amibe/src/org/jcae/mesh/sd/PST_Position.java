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

package org.jcae.mesh.sd;

import org.jcae.opencascade.jni.*;

/** An abstract class to federate all the different kinds of node position.
 *
 * \n This class entity allows a (1,1) relation with the MeshNode class.
 * \n From this class and by the way of an hetitage process, specific class are defined for each kind of nodes:
 * - on a vertex
 * - on an line
 * - on a surface
 * - into a volume
 * @see PST_VertexPosition class
 * @see PST_LinePosition class
 * @see PST_DegeneratedLinePosition class
 * @see PST_SurfacePosition class
 * \n
 * @author Cyril BRANDY & Marie-Helene GARAT.
 */
public abstract class PST_Position
{
	public final static int UNDEFINED = 0; /**< type value INDEFINI. */
	public final static int VERTEX = 1; /**< type value VERTEX. */
	public final static int EDGE = 2; /**< type value EDGE. */
	public final static int SURFACE = 3; /**< type value SURFACE. */
	public final static int DEGENERATEDLINE = 4; /**< type value DEGENERATEDLINE. */
	public final static PST_Position PST_UNDEFINED;
	
	/** Default constructor. */
	public PST_Position()
	{
	}
	
	/**
	 * Return the type of node position.
	 * @return int : the type of node position
	 */
	public int getType()
	{
		return UNDEFINED;
	}
	
	/** Test node position equality.
	 * @param o : an Object instance, the object to compare
	 * @return boolean : set to \c true if both node position are same, \c false if not.
	 */
	public boolean equals(Object o)
	{
		PST_Position p=(PST_Position)o;
		return getType()==p.getType();
	}
	
	/**
	 * Abstract method to return the topological entity related to the current node position.
	 * @return TopoDS_Shape : the topological entity related to the node
	 */
	public abstract TopoDS_Shape getShape();
	
	static
	{
		PST_UNDEFINED=new PST_Position()
		{
			public TopoDS_Shape getShape()
			{
				return null;
			}
		};
	}
}
