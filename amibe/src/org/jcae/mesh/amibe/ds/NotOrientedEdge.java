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

package org.jcae.mesh.amibe.ds;

public class NotOrientedEdge extends OTriangle
{
	public NotOrientedEdge()
	{
		super();
	}
	
	public NotOrientedEdge(OTriangle ot)
	{
		tri = (Triangle) ot.tri;
		localNumber = ot.localNumber;
		attributes = (tri.adjPos >> (8*(1+localNumber))) & 0xff;
	}

	public final int hashCode()
	{
		Triangle sym = (Triangle) tri.getAdj(localNumber);
		return tri.hashCode() + sym.hashCode();
	}
	
	public final boolean equals(Object o)
	{
		if (o == null)
			return false;
		NotOrientedEdge that = (NotOrientedEdge) o;
		return (that.tri == tri && that.localNumber == localNumber) ||
		       (that.tri == (Triangle) tri.getAdj(localNumber) && that.localNumber == ((tri.adjPos >> (2*localNumber)) & 3));
	}
	
	public final String toString()
	{
		return "Vertices: "+origin()+"\n"+destination();
	}
}
