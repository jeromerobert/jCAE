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

import gnu.trove.THashSet;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 * @author Jerome Robert
 */
public class HashFactory {
	private static boolean haveAssertion = false;
	static
	{
		assert setAssertion();
	}
	private static boolean setAssertion()
	{
		haveAssertion = true;
		return true;
	}

	public static <E> Set<E> createHashSet()
	{
		assert haveAssertion;
		if(haveAssertion)
			return new LinkedHashSet<E>();
		else
			return new THashSet<E>();
	}

	public static <E> Set<E> createHashSet(int capacity)
	{
		if(haveAssertion)
			return new LinkedHashSet<E>(capacity * 4 / 3);
		else
			return new THashSet<E>(capacity);
	}

	public static <E> Set<E> createHashSet(Collection<E> c)
	{
		if(haveAssertion)
			return new LinkedHashSet<E>(c);
		else
			return new THashSet<E>(c);
	}
}
