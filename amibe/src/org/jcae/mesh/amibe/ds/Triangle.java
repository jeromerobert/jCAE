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

import org.jcae.mesh.amibe.metrics.Metric3D;
import org.apache.log4j.Logger;
import java.util.Random;
import java.util.ArrayList;
import java.util.Iterator;

public class Triangle
{
	private static Logger logger = Logger.getLogger(Triangle.class);
	public static Triangle outer = null;
	public Vertex [] vertex = new Vertex[3];
	private Object [] adj = new Object[3];
	//  Byte 0 represents orientation of adjacent triangles:
	//     bits 0-1: adj[0]
	//     bits 2-3: adj[1]
	//     bits 4-5: adj[2]
	//  Other attributes:
	//     bit 6:  readable?
	//     bit 7:  writable?
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
	
	public void glue1(int num, Triangle that, int thatnum)
	{
		adj[num] = that;
		//  Clear previous adjacent position ...
		adjPos &= ~(3 << (2*num));
		//  ... and set it right
		adjPos |= (thatnum << (2*num));
	}
	
	public Object getAdj(int num)
	{
		return adj[num];
	}
	
	public void setAdj(int num, Object link)
	{
		adj[num] = link;
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
	
	public void setOuter()
	{
		adjPos |= (OTriangle.OUTER << 8 | OTriangle.OUTER << 16 | OTriangle.OUTER << 24);
	}
	
	public boolean isBoundary()
	{
		return (adjPos & (OTriangle.BOUNDARY << 8 | OTriangle.BOUNDARY << 16 | OTriangle.BOUNDARY << 24)) != 0;
	}
	
	public boolean isQuadrangle()
	{
		return (adjPos & (OTriangle.QUAD << 8 | OTriangle.QUAD << 16 | OTriangle.QUAD << 24)) != 0;
	}
	
	public void mark()
	{
		adjPos |= (OTriangle.MARKED << 8 | OTriangle.MARKED << 16 | OTriangle.MARKED << 24);
	}
	
	public void unmark()
	{
		adjPos &= ~(OTriangle.MARKED << 8 | OTriangle.MARKED << 16 | OTriangle.MARKED << 24);
	}
	
	public void swapAttributes12()
	{
		int byte0 = adjPos & 0xff;
		int byte0sw = (byte0 & 195) | ((byte0 & 12) << 2) | ((byte0 & 48) >> 2);
		adjPos = byte0sw | (adjPos & 0x0000ff00) | ((adjPos & 0x00ff0000) << 8) | ((adjPos & 0xff000000) >> 8);
	}
	
	public boolean isMarked()
	{
		return (adjPos & (OTriangle.MARKED << 8 | OTriangle.MARKED << 16 | OTriangle.MARKED << 24)) != 0;
	}
	
	public boolean isReadable()
	{
		return (adjPos & 0x40000000) != 0;
	}
	
	public boolean isWritable()
	{
		return (adjPos & 0x80000000) != 0;
	}
	
	public void setReadable(boolean b)
	{
		if (b)
			adjPos |= 0x40000000;
		else
			adjPos &= ~0x40000000;
	}
	
	public void setWritable(boolean b)
	{
		if (b)
			adjPos |= 0x80000000;
		else
			adjPos &= ~0x80000000;
	}
	
	private final String showAdj(int num)
	{
		String r = "";
		if (adj[num] == null)
			return "N/A";
		else if (adj[num] instanceof Triangle)
		{
			Triangle t = (Triangle) adj[num];
			if (t == null)
				r+= "null";
			else
				r+= t.hashCode()+"["+(((t.adjPos & (3 << (2*num))) >> (2*num)) & 3)+"]";
		}
		else
		{
			r+= "(";
			ArrayList a = (ArrayList) adj[num];
			boolean first = true;
			for (Iterator it = a.iterator(); it.hasNext(); )
			{
				Triangle t = (Triangle) it.next();
				Integer i = (Integer) it.next();
				if (!first)
					r+= ",";
				r+= t.hashCode()+"["+i+"]";
				first = false;
			}
			r+= ")";
		}
		return r;
	}
	
	public String toString()
	{
		String r = "";
		r += "hashcode: "+hashCode();
		r += "\nVertices:";
		for (int i = 0; i < 3; i++)
			r += "\n  "+vertex[i];
		r += "\nAdjacency: "+showAdj(0)+" "+showAdj(1)+" "+showAdj(2);
		r += "\nEdge attributes:";
		for (int i = 0; i < 3; i++)
			r += " "+Integer.toHexString((adjPos >> (8*(1+i))) & 0xff);
		return r;
	}

}
