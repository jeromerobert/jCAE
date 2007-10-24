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
 * (C) Copyright 2007, by EADS France
 */

package org.jcae.viewer3d.fd.sd;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
/**
 * Used to show result with colored vertex on a set of plates
 * @author  Jerome Robert
 */
public class ColoredPlateSet
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
		
		@Override
		public int hashCode()
		{
			return Float.floatToRawIntBits(x)+Float.floatToRawIntBits(y)+Float.floatToRawIntBits(z);
		}
		
		@Override
		public boolean equals(Object o)
		{
			if(o instanceof Node3d)
			{
				Node3d n=(Node3d)o;
				return (n.x==x)&&((n.y==y)&&(n.z==z));
			}
			return false;
		}
	}

	private ArrayList<Plate> plates;
	private float[][] grid;
	private HashMap<Node3d, Integer> coordinates=new HashMap<Node3d, Integer>();
	private int currentIndex=0;	
	private IntegerArrayList indices=new IntegerArrayList();
	private IntegerArrayList colorIndices=new IntegerArrayList();
	private float minValue=0;
	private float maxValue=1f;
	private boolean processed=false;
	
	public void setMinValue(float minValue)
	{
		this.minValue=minValue;
	}

	public void setMaxValue(float maxValue)
	{
		this.maxValue=maxValue;
	}

	/** Creates a new instance of ColoredPlateSet */
	public ColoredPlateSet(ArrayList<Plate> plates, float[][] grid)
	{
		this.plates=plates;
		this.grid=grid;
	}
	
	public float[] getColoredPlateCoordinates()
	{
		if(!processed) process();
		float[] r=new float[coordinates.size()*3];
		Iterator<Map.Entry<Node3d, Integer>> i=coordinates.entrySet().iterator();
		while(i.hasNext())
		{
			Map.Entry<Node3d, Integer> e=i.next();
			int id=e.getValue().intValue();
			Node3d n=e.getKey();
			id=id*3;
			r[id]=n.x;
			r[id+1]=n.y;
			r[id+2]=n.z;
		}
		return r;
	}

	public int[] getColoredPlateCoordinatesIndices()
	{
		if(!processed) process();
		return indices.toArray();
	}

	public int[] getColoredPlateColorsIndices()
	{
		if(!processed) process();
		return colorIndices.toArray();
	}

	void process()
	{
		int n1,n2,n3,n4,valueIndex;
		float value;
		for(int plateId=0;plateId<plates.size();plateId++)
		{
			Plate p=plates.get(plateId);
			valueIndex=0;
			for(int j=p.min2;j<p.max2;j++)
			for(int i=p.min1;i<p.max1;i++)			
			{
				n1=addNode(p.getCoordinates(grid,i,j));
				n2=addNode(p.getCoordinates(grid,i+1,j));
				n3=addNode(p.getCoordinates(grid,i+1,j+1));
				n4=addNode(p.getCoordinates(grid,i,j+1));
				addPlate(n1,n2,n3,n4);
				value=p.values[valueIndex];
				if(value>maxValue) value=maxValue;
				if(value<minValue) value=minValue;
				int color=(int)((maxValue-value)/(maxValue-minValue)*1024);
				colorIndices.add(color,color,color,color);
				valueIndex++;
			}
		}
		processed=true;
	}
	
	private int addNode(float [] node)
	{
		Node3d nd=new Node3d(node[0],node[1],node[2]);
		if(coordinates.containsKey(nd))
			return  coordinates.get(nd).intValue();
		coordinates.put(nd,new Integer(currentIndex));
		currentIndex++;
		return currentIndex-1;
	}

	private void addPlate(int p1, int p2, int p3, int p4)
	{
		indices.add(p1,p2,p3,p4);
	}	
}
