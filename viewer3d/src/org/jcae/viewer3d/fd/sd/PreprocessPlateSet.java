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

package org.jcae.viewer3d.fd.sd;
import java.util.*;

/**
 * Used to show a set of plates with colors associated with their attributs
 * @author  Jerome Robert
 */
public class PreprocessPlateSet
{
	final class Node3d
	{
		float x,y,z,value;
		public Node3d()
		{
			x=0; y=0; z=0;
		}

		public Node3d(float ax, float ay, float az)
		{
			x=ax;
			y=ay;
			z=az;
		}
		
		public int hashCode()
		{
			return Float.floatToRawIntBits(x)+Float.floatToRawIntBits(y)+Float.floatToRawIntBits(z);
		}
		
		public boolean equals(Object o)
		{
			if(o instanceof Node3d)
			{
				Node3d n=(Node3d)o;
				return (n.x==x)&&((n.y==y)&&(n.z==z));
			} else return false;
		}
	}

	private ArrayList plates;
	private float[][] grid;
	private HashMap coordinates=new HashMap();
	private int currentIndex=0;	
	private IntegerArrayList indices=new IntegerArrayList();	
	private boolean processed=false;
	
	/** Creates a new instance of ColoredPlateSet */
	public PreprocessPlateSet(ArrayList plates, float[][] grid)
	{
		this.plates=plates;
		this.grid=grid;
	}
	
	public float[] getCoordinates()
	{
		if(!processed) process();
		float[] r=new float[coordinates.size()*3];
		Iterator i=coordinates.entrySet().iterator();
		while(i.hasNext())
		{
			Map.Entry e=(Map.Entry)i.next();
			int id=((Integer)e.getValue()).intValue();
			Node3d n=(Node3d)(e.getKey());
			id=id*3;
			r[id]=n.x;
			r[id+1]=n.y;
			r[id+2]=n.z;
		}
		return r;
	}

	public int[] getCoordinateIndices()
	{
		if(!processed) process();
		return indices.toArray();
	}

	void process()
	{
		int n1,n2,n3,n4,valueIndex;
		for(int plateId=0;plateId<plates.size();plateId++)
		{
			Plate p=(Plate)(plates.get(plateId));
			valueIndex=0;
			n1=addNode(p.getCoordinates(grid,p.min1,p.min2));
			n2=addNode(p.getCoordinates(grid,p.max1,p.min2));
			n3=addNode(p.getCoordinates(grid,p.max1,p.max2));
			n4=addNode(p.getCoordinates(grid,p.min1,p.max2));
			addPlate(n1,n2,n3,n4);
		}
		processed=true;
	}
	
	private int addNode(float [] node)
	{
		Node3d nd=new Node3d(node[0],node[1],node[2]);
		if(coordinates.containsKey(nd))
			return  ((Integer)coordinates.get(nd)).intValue();
		coordinates.put(nd,new Integer(currentIndex));
		currentIndex++;
		return currentIndex-1;
	}

	private void addPlate(int p1, int p2, int p3, int p4)
	{
		indices.add(p1,p2,p3,p4);
	}	
}
