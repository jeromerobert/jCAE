/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2006, by EADS CRC
    Copyright (C) 2007,2009, by EADS France

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

package org.jcae.mesh.amibe.traits;

public class TraitsBuilder
{
	private static final int BITNORMAL      = 0;
	private static final int BITCOLOR       = 1;
	private static final int BITVECTORFIELD = 2;
	private static final int BITSCALARFIELD = 3;

	public static final int NORMAL      = 1 << BITNORMAL;
	public static final int COLOR       = 1 << BITCOLOR;
	public static final int VECTORFIELD = 1 << BITVECTORFIELD;
	public static final int SCALARFIELD = 1 << BITSCALARFIELD;

	protected int attributes = 0;
	protected final int [] index = new int[32];

	/**
	 * Dummy constructor.
	 */
	public TraitsBuilder()
	{
	}

	/**
	 * Tells whether traits builder has a given feature.
	 *
	 * @param c  capability
	 * @return <code>true</code> if traits builder contains this feature,
	 * <code>false</code> otherwise.
	 */
	public boolean hasCapability(int c)
	{
		return ((attributes & c) != 0);
	}

	public void addNormal()
	{
		attributes |= NORMAL;
	}

	public double [] getNormal(Traits t)
	{
		return (double []) t.array[index[BITNORMAL]];
	}

	public void addColor()
	{
		attributes |= COLOR;
	}

	public double [] getColor(Traits t)
	{
		return (double []) t.array[index[BITCOLOR]];
	}

	public void addVectorField()
	{
		attributes |= VECTORFIELD;
	}

	public double [] getVectorField(Traits t)
	{
		return (double []) t.array[index[BITVECTORFIELD]];
	}

	public void addScalarField()
	{
		attributes |= SCALARFIELD;
	}

	public double [] getScalarField(Traits t)
	{
		return (double []) t.array[index[BITSCALARFIELD]];
	}

	/**
	 * Creates a {@link Traits} instance built from this traits builder.
	 *
	 * @return a {@link Traits} instance
	 */
	public Traits createTraits()
	{
		if (attributes == 0)
			return null;
		int n = 0;
		int k = 1;
		for (int i = 0; i < index.length; i++)
		{
			if ((attributes & k) != 0)
			{
				index[i] = n;
				n++;
			}
			else
				index[i] = -1;
			k <<= 1;
		}
		Traits t = new Traits(n);
		if ((attributes & NORMAL) != 0)
			t.array[index[BITNORMAL]] = new double[3];
		if ((attributes & COLOR) != 0)
			t.array[index[BITCOLOR]] = new double[3];
		if ((attributes & VECTORFIELD) != 0)
			t.array[index[BITVECTORFIELD]] = new double[3];
		if ((attributes & SCALARFIELD) != 0)
			t.array[index[BITSCALARFIELD]] = new Double(0.0);
		subInitTraits(t);
		return t;
	}

	/**
	 * Dummy method, overridden by subclasses.
	 * @param t  traits
	 */
	protected void subInitTraits(Traits t)
	{
	}

	@Override
	public String toString()
	{
		return ""+Integer.toHexString(attributes);
	}
}
