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

/*
 * IntegerArrayList.java
 *
 * Created on 3 d?cembre 2002, 18:46
 */

package org.jcae.util;
import java.util.*;
/**
 *
 * @author  robert
 */
public class IntegerArrayList
{
	private static int GRANULARITY=4*512;
	private int subLastIndex=0;
	LinkedList arrays;
	/** Creates a new instance of ArrayListExt */
	public IntegerArrayList()
	{
		arrays=new LinkedList();
		arrays.add(new int[GRANULARITY]);
	}
	
	public void add(int i1,int i2,int i3,int i4)
	{
		if(subLastIndex>=GRANULARITY)
		{
			arrays.add(new int[GRANULARITY]);
			subLastIndex=0;
		}
		int[] tf=(int[])arrays.getLast();
		tf[subLastIndex]=i1;
		tf[subLastIndex+1]=i2;
		tf[subLastIndex+2]=i3;
		tf[subLastIndex+3]=i4;
		subLastIndex+=4;		
	}

	public int getSize()
	{
		return GRANULARITY*(arrays.size()-1)+subLastIndex;
	}
	
	public int[] toArray()
	{
		int[] r=new int[getSize()];
		int j=0;
		for(int i=0; i<arrays.size()-1;j+=GRANULARITY,i++)
			System.arraycopy((int[])arrays.get(i),0,r,j,GRANULARITY);
		System.arraycopy((int[])arrays.getLast(),0,r,j,subLastIndex);
		return r;
	}
}
