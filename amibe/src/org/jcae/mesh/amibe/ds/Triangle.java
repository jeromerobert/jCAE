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

package org.jcae.mesh.amibe.ds;

import org.apache.log4j.Logger;
import java.util.Random;

public class Triangle
{
	private static Logger logger = Logger.getLogger(Triangle.class);
	public static Triangle outer = null;
	public Vertex [] vertex = new Vertex[3];
	public Triangle [] adj = new Triangle[3];
	//  Byte 0 represents orientation of adjacent triangles:
	//     bits 0-1: adj[0]
	//     bits 2-3: adj[1]
	//     bits 4-5: adj[2]
	//  Bytes 1, 2 and 3 carry up attributes for edges 0, 1 and 2.
	public int adjPos = 0;
	
	public Triangle()
	{
	}

	public Triangle(Vertex a, Vertex b, Vertex c)
	{
		vertex[0] = a;
		vertex[1] = b;
		vertex[2] = c;
	}
	
	public void addToMesh()
	{
		Mesh m = vertex[0].mesh;
		if (Vertex.outer == vertex[0])
			m = vertex[1].mesh;
		assert null != m;
		m.add(this);
	}
	
	public Vertex centroid()
	{
		double [] p1 = vertex[0].getUV();
		double [] p2 = vertex[1].getUV();
		double [] p3 = vertex[2].getUV();
		return new Vertex(
			(p1[0]+p2[0]+p3[0])/3.0,
			(p1[1]+p2[1]+p3[1])/3.0
		);
	}
	
	public boolean isOuter()
	{
		return (adjPos & (OTriangle.OUTER << 8 | OTriangle.OUTER << 16 | OTriangle.OUTER << 24)) != 0;
	}
	
	public String toString()
	{
		String r = "Vertices:";
		for (int i = 0; i < 3; i++)
			r += "\n  "+vertex[i];
		r += "\nEdge attributes:";;
		for (int i = 0; i < 3; i++)
			r += " "+((adjPos >> (8*(1+i))) & 0xff);
		return r;
	}

}
