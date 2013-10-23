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
 * (C) Copyright 2013, by EADS France
 */


package org.jcae.mesh.amibe.util;

import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Jerome Robert
 */
public class HashFactory {
	private static boolean sorted = false;
	static
	{
		//assert setAssertion();
		sorted = !Boolean.getBoolean("org.jcae.unsortedHash");
	}

	private static boolean setAssertion()
	{
		sorted = true;
		return true;
	}

	public static <E> Set<E> createSet()
	{
		assert sorted;
		if(sorted)
			return new LinkedHashSet<E>();
		else
			return new THashSet<E>();
	}

	public static <E> Set<E> createSet(int capacity)
	{
		if(sorted)
			return new LinkedHashSet<E>(capacity * 4 / 3);
		else
			return new THashSet<E>(capacity);
	}

	public static <E> Set<E> createSet(Collection<E> c)
	{
		if(sorted)
			return new LinkedHashSet<E>(c);
		else
			return new THashSet<E>(c);
	}
	
	public static <K, V> Map<K, V> createMap()
	{
		if(sorted)
			return new LinkedHashMap<K, V>();
		else
			return new THashMap<K, V>();
	}

	public static <K, V> Map<K, V> createMap(int capacity)
	{
		if(sorted)
			return new LinkedHashMap<K, V>(capacity * 4 / 3);
		else
			return new THashMap<K, V>(capacity);
	}	

	public static <K, V> void ensureCapacity(Map<K, V> map, int capacity)
	{
		if(map instanceof THashMap)
			((THashMap)map).ensureCapacity(capacity);
	}
}
