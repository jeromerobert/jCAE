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

package org.jcae.mesh.util;

import java.util.*;
import gnu.trove.THashSet;

public class HashSet extends THashSet
{
	public HashSet()
	{		
	}
	
	public HashSet(Collection c)
	{
		super(c);
	}
	
	public HashSet(int initialCapacity)
	{
		super(initialCapacity);
	}
	
	/** Add an object if it is not already present
	 * @param object The object to add
	 * @return If the object is alread present it is return, else the freshly added object is
	 * returned
	 */
	public final Object addIfNotPresent(Object object)
	{
		int i=index(object);
		if(i>=0) return _set[i];
		else
		{
			add(object);
			return object;
		}
	}
	/** Add an object without verifing if it is already in the set. This method
	 * is faster than add but must be used carefully.
	 */
	public final void addFast(Object obj)
	{
		int index = insertionIndexFast(obj);
		Object old = _set[index];
		_set[index] = obj;
		postInsertHook(old == null);
	}
	
	/**
	 * Locates the index at which <tt>obj</tt> can be inserted.  This method is
	 * a hack on the THashSet.insertionIndex method, which suppose that obj is
	 * not already in the set. It is fast but unsafe.
	 * @param obj an <code>Object</code> value
	 * @return the index of a FREE slot at which obj can be inserted
	 */
	private final int insertionIndexFast(Object obj)
	{
		int hash, probe, index, length;
		Object[] set;
		Object cur;
		
		set = _set;
		length = set.length;
		hash = _hashingStrategy.computeHashCode(obj) & 0x7fffffff;
		index = hash % length;
		cur = set[index];
		
		if (cur == null) return index;       // empty, all done
		else 
		{   // already FULL or REMOVED, must probe
			// compute the double hash
			probe = 1 + (hash % (length - 2));
			// starting at the natural offset, probe until we find an
			// offset that isn't full.
			do
			{
				index -= probe;
				if (index < 0) index += length;
				cur = set[index];
			}
			while (cur != null && cur != REMOVED);
			
			// if the index we found was removed: continue probing until we
			// locate a free location or an element which equal()s the
			// one we have.
			if (cur == REMOVED)
			{
				int firstRemoved = index;
				while (cur != null)
				{
					index -= probe;
					if (index < 0) index += length;
					cur = set[index];
				}
				return (cur != null
				&& cur != REMOVED) ? -index -1 : firstRemoved;
			}
			// if it's full, the key is already stored
			return (cur != null
			&& cur != REMOVED) ? -index -1 : index;
		}
	}
}
