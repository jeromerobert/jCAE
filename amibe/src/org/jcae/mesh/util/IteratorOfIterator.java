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
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */

package org.jcae.mesh.util;
import java.util.*;
import java.lang.reflect.*;
/**
 *
 * @author  Jerome Robert
 */
public class IteratorOfIterator implements Iterator
{
	private ArrayList iterators=new ArrayList();
	private int index;
	
	public void addIterator(Iterator it)
	{
		iterators.add(it);
	}
	
	public boolean hasNext()
	{
		if(iterators.size()==0) return false;
		Iterator ci=(Iterator)iterators.get(index);
		if(!ci.hasNext())
		{
			if(index<iterators.size()-1)
			{
				index++;
				return hasNext();
			}
			else return false;
		}
		return true;
	}

	public Object next()
	{
		Iterator ci=(Iterator)iterators.get(index);
		if(!ci.hasNext())
		{
			if(index<iterators.size()-1)
			{
				index++;
				ci=(Iterator)iterators.get(index);
			}
		}
		return ci.next();
	}

	public void remove()
	{
		Iterator ci=(Iterator)iterators.get(index);
		ci.remove();
	}	
}
