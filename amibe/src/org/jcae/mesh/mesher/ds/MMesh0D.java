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

import org.jcae.mesh.cad.*;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;

/**
 * List of vertices of the whole shape.
 * A <code>MMesh0D</code> instance is a list of vertices.  All topological
 * vertices which share the same location are merged into a unique vertex,
 * and <code>MMesh0D</code> contains the list of these unique vertices.
 */

public class MMesh0D
{
	private static Logger logger = Logger.getLogger(MMesh0D.class);	

	//  Array of distinct geometric nodes
	private CADVertex[] vnodelist;
	
	/**
	 * Creates a <code>MMesh0D</code> instance by merging all topological
	 * vertices which have the same location.
	 *
	 * @param shape  topological shape
	 */
	public MMesh0D(CADShape shape)
	{
		CADExplorer expV = CADShapeBuilder.factory.newExplorer();
		int nodes = 0;
		for (expV.init(shape, CADExplorer.VERTEX); expV.more(); expV.next())
			nodes++;

		//  Merge topological vertices found at the same geometrical point
		vnodelist = new CADVertex[nodes];
		for (expV.init(shape, CADExplorer.VERTEX); expV.more(); expV.next())
			addGeometricalVertex((CADVertex) expV.current());
	}
	
	//  Add a geometrical vertex.
	private void addGeometricalVertex(CADVertex V)
	{
		int i = 0;
		for (; i < vnodelist.length && null != vnodelist[i]; i++)
		{
			if (V.isSame(vnodelist[i]))
				return;  //  Vertex already exists in vnodelist
		}
		vnodelist[i] = V;
	}
	
	/**
	 * Returns the vertex which has the same location as the argument.
	 * This method must be used by 1D algos to ensure that vertices
	 * are unique.
	 *
	 * @param V  vertex
	 * @return the vertex which has the same location as V.
	 */
	public CADVertex getGeometricalVertex(CADVertex V)
	{
		for (int i = 0; i < vnodelist.length && null != vnodelist[i]; i++)
		{
			if (V.isSame(vnodelist[i]))
				return vnodelist[i];
		}
		throw new NoSuchElementException("TVertex : "+V);
	}
	
	/**
	 * Returns an index of the vertex which has the same location as the argument.
	 *
	 * @param V  vertex
	 * @return the index of the vertex which has the same location as V.
	 */
	public int getIndexGeometricalVertex(CADVertex V)
	{
		for (int i = 0; i < vnodelist.length && null != vnodelist[i]; i++)
		{
			if (V.isSame(vnodelist[i]))
				return i;
		}
		return -1;
	}
	
	/**
	 * Returns the vertex represented by its index.
	 *
	 * @param index  the index of the vertex.
	 * @return the geometrical vertex.
	 */
	public CADVertex getGeometricalVertex(int index)
	{
		return vnodelist[index];
	}
	
	/**
	 * Returns an iterator over unique vertices.
	 *
	 * @return an iterator over unique vertices.
	 */
	public Iterator getVerticesIterator()
	{
		return new Iterator()
		{
			private int pos = -1;
			public boolean hasNext()
			{
				return (pos+1 < vnodelist.length && null != vnodelist[pos+1]);
			}
			
			public Object next()
			{
				pos++;
				if (pos >= vnodelist.length || null == vnodelist[pos])
					return new NoSuchElementException();
				return vnodelist[pos];
			}
			public void remove()
			{
				//  Not needed
			}
		};
	}
}
