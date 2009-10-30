/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2006, by EADS CRC
    Copyright (C) 2007,2008,2009, by EADS France

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


package org.jcae.mesh.bora.ds;

import org.jcae.mesh.bora.algo.*;
import org.jcae.mesh.cad.CADShapeEnum;
import java.util.logging.Logger;
import java.lang.reflect.Constructor;
import java.util.logging.Level;

public class Hypothesis
{
	private static final Logger LOGGER = Logger.getLogger(Hypothesis.class.getName());

	private HypInterface hyp = HypNoneInstance;
	// Now, length is the target length and is the variable that is used;
	// In the near future we will want to enforce a maximum length lengthMax 
	private double length = -1.0, lengthMin = -1.0, lengthMax = -1.0;
	private double deflection = -1.0;
	private boolean lengthBool = false, numberBool = false;
	private int numberMin = -1, numberMax = -1;
	private boolean locked = false;

	// Unique identitier
	private final int id;
	private static int nextId = -1;

	private static final Class<?> [] innerClasses = Hypothesis.class.getDeclaredClasses();
	private static final HypNone HypNoneInstance = new HypNone();

	public Hypothesis()
	{
		nextId++;
		id = nextId;
	}

	public Hypothesis(int i)
	{
		id = i;
		if (i > nextId)
			nextId = i;
	}

	public int getId()
	{
		return id;
	}

	/**
	 * Sets element type.
	 *
	 * @param e  element type
	 */
	public void setElement(String e)
	{
		checkLock();
		hyp = getAlgo(e);
		if (LOGGER.isLoggable(Level.FINE))
			LOGGER.log(Level.FINE, "("+Integer.toHexString(this.hashCode())+") Setting element type to "+e+"  "+hyp.getClass().getName());
	}

	/**
	 * Gets element type.
	 *
	 * @return element type
	 */
	public String getElement()
	{
		return hyp.getType();
	}

	/**
	 * Checks compatibility with geometrical objects.
	 *
	 * @param cse geometrical object type
	 * @return <code>true</code> if this hypothesis can be appplied on
	 * geometrical objects of a given type, and <code>false</code>
	 * otherwise.
	 */
	boolean checkCompatibility(CADShapeEnum cse)
	{
		return hyp == null || hyp.getType() == null || hyp.dim() == cse;
	}

	/**
	 * Sets target length.
	 *
	 * @param l  length
	 */
	public void setLength(double l)
	{
		setLength(l, false);
	}

	/**
	 * Sets target length.
	 *
	 * @param l  length
	 * @param b  hard constraint
	 */
	private void setLength(double l, boolean b)
	{
		checkLock();
		if (LOGGER.isLoggable(Level.FINE))
			LOGGER.log(Level.FINE, "("+Integer.toHexString(this.hashCode())+") Setting length to "+l+"; strong constraint: "+b);
		length = l;
		lengthBool = b;
	}

	public double getLength()
	{
		return length;
	}

	public double getDeflection()
	{
		return deflection;
	}

	/**
	 * Sets length interval.
	 *
	 * @param l1  length min
	 * @param l2  length max
	 */
	@SuppressWarnings("unused")
	private void setLength(double l1, double l2)
	{
		setLength(l1, l2,false);
	}

	/**
	 * Sets length interval.
	 *
	 * @param l1  length min
	 * @param l2  length max
	 * @param b  hard constraint
	 */
	private void setLength(double l1, double l2, boolean b)
	{
		checkLock();
		if (LOGGER.isLoggable(Level.FINE))
			LOGGER.log(Level.FINE, "("+Integer.toHexString(this.hashCode())+") Setting length; min="+l1+" max="+l2+"; strong constraint: "+b);
		lengthMin = l1;
		lengthMax = l2;
		lengthBool = b;
	}

	/**
	 * Sets deflection.
	 *
	 * @param d  deflection
	 */
	public void setDeflection(double d)
	{
		checkLock();
		if (LOGGER.isLoggable(Level.FINE))
			LOGGER.log(Level.FINE, "("+Integer.toHexString(this.hashCode())+") Setting deflection to "+d);
		deflection = d;
	}

	/**
	 * Sets number of segments.
	 *
	 * @param n  number of segments.
	 */
	public void setNumber(int n)
	{
		setNumber(n, false);
	}

	/**
	 * Sets number of segments.
	 *
	 * @param n  number of segments.
	 * @param b  hard constraint
	 */
	private void setNumber(int n, boolean b)
	{
		checkLock();
		if (LOGGER.isLoggable(Level.FINE))
			LOGGER.log(Level.FINE, "("+Integer.toHexString(this.hashCode())+") Setting number of discretized points to "+n+"; strong constraint: "+b);
		numberMin = n;
		numberMax = n;
		numberBool = b;
	}

	/**
	 * Sets number of segments.
	 *
	 * @param n1  minimal number of segments
	 * @param n2  maximal number of segments
	 */
	@SuppressWarnings("unused")
	private void setNumber(int n1, int n2)
	{
		setNumber(n1, n2, false);
	}

	/**
	 * Sets number of segments.
	 *
	 * @param n1  minimal number of segments
	 * @param n2  maximal number of segments
	 * @param b  hard constraint
	 */
	private void setNumber(int n1, int n2, boolean b)
	{
		checkLock();
		if (LOGGER.isLoggable(Level.FINE))
			LOGGER.log(Level.FINE, "("+Integer.toHexString(this.hashCode())+") Setting number of discretized points; min="+n1+" max="+n2+"; strong constraint: "+b);
		numberMin = n1;
		numberMax = n2;
		numberBool = b;
	}

	/**
	 * Locks the hypothesis so that it cannot be modified.
	 */
	public void lock()
	{
		locked = true;
	}

	private void checkLock()
	{
		if (locked)
			throw new RuntimeException("Cannot modify an Hypothesis after it has been aplied!");
	}

	private static double combineDouble(double current, double that)
	{
		if (current < 0.0)
			return that;
		else if (that < 0.0 || that > current)
			return current;
		else
			return that;
	}

	/**
	 * Combines with another hypothesis.
	 *
	 * @return <code>true</code> if hypothesis have been successfully combined, <code>false</code> otherwise.
	 */
	boolean combine(Hypothesis that)
	{
		String elt = getElement();
		if (elt != null && that.getElement() != null && !elt.equals(that.getElement()))
			return false;

		length     = combineDouble(length, that.length);
		lengthMin  = combineDouble(lengthMin, that.lengthMin);
		lengthMax  = combineDouble(lengthMax, that.lengthMax);
		deflection = combineDouble(deflection, that.deflection);
		lengthBool |= that.lengthBool;
		return true;
	}

	@Override
	public String toString()
	{
		String ret = "Hyp. "+id+" elementType: "+hyp.getType();
		if (length >= 0.0)
			ret += " length: "+length;
		if (lengthMin >= 0.0)
			ret += " lengthMin: "+lengthMin;
		if (lengthMax >= 0.0)
			ret += " lengthMax: "+lengthMax;
		if (lengthBool)
			ret += " lengthBool: "+lengthBool;
		if (deflection >= 0.0)
			ret += " deflection: "+deflection;
		if (numberMin >= 0)
			ret += " numberMin: "+numberMin;
		if (numberMax >= 0)
			ret += " numberMax: "+numberMax;
		if (numberBool)
			ret += " numberBool: "+numberBool;
		return ret;
	}

	Hypothesis createInheritedHypothesis(CADShapeEnum cse)
	{
		Hypothesis ret = new Hypothesis();
		ret.length      = length;
		ret.lengthMin   = lengthMin;
		ret.lengthMax   = lengthMax;
		ret.lengthBool  = lengthBool;
		ret.deflection  = deflection;
		ret.hyp         = getAlgo(hyp.impliedType(cse));
		return ret;
	}

	AlgoInterface findAlgorithm(CADShapeEnum cse)
	{
		AlgoInterface ret = null;
		try {
			if (cse == CADShapeEnum.VERTEX)
			{
				Constructor<Vertex0d> cons = Vertex0d.class.getConstructor();
				ret = cons.newInstance();
			}
			else if (cse == CADShapeEnum.EDGE)
			{
				Constructor<UniformLengthDeflection1d> cons = UniformLengthDeflection1d.class.getConstructor(double.class, double.class, boolean.class);
				ret = cons.newInstance(length, deflection, true);
			}
			else if (cse == CADShapeEnum.FACE)
			{
				Constructor<Basic2d> cons = Basic2d.class.getConstructor(double.class, double.class, boolean.class, boolean.class);
				ret = cons.newInstance(length, deflection, true, true);
			}
			else if (cse == CADShapeEnum.SOLID)
			{
				Constructor<TetGen> cons = TetGen.class.getConstructor(double.class);
				ret = cons.newInstance(length);
				if (!ret.isAvailable())
					LOGGER.severe("TetGen not available!");
				/*
				Constructor cons = Netgen.class.getConstructor(double.class);
				ret = (AlgoInterface) cons.newInstance(length);
				if (!ret.isAvailable())
					LOGGER.severe("Netgen not available!");
				*/
			}
		} catch (Exception ex)
		{
			ex.printStackTrace();
			System.exit(1);
		}
		return ret;
	}

	private static interface HypInterface
	{
		public String impliedType(CADShapeEnum d);
		public String getType();
		public CADShapeEnum dim();
	}

	private static HypInterface getAlgo(String elt)
	{
		HypInterface h = HypNoneInstance;
		if (elt == null)
			return h;
		try {
			for (int i = 0; i < innerClasses.length; i++)
			{
				if (innerClasses[i].getName().equals(Hypothesis.class.getName()+"$Hyp"+elt))
					h = (HypInterface) innerClasses[i].newInstance();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			h = HypNoneInstance;
		}
		return h;
	}

	private static class HypNone implements HypInterface
	{
		public CADShapeEnum dim()
		{
			return null;
		}
		public String getType()
		{
			return null;
		}
		public String impliedType(CADShapeEnum d)
		{
			return null;
		}
	}
	@SuppressWarnings("unused")
	private static class HypV1 implements HypInterface
	{
		// Hypothesis.getAlgo() needs access to this constructor
		HypV1() {}

		public CADShapeEnum dim()
		{
			return CADShapeEnum.VERTEX;
		}
		public String getType()
		{
			return "V1";
		}
		public String impliedType(CADShapeEnum d)
		{
			if (d == CADShapeEnum.VERTEX)
				return getType();
			return null;
		}
	}
	@SuppressWarnings("unused")
	private static class HypE2 implements HypInterface
	{
		// Hypothesis.getAlgo() needs access to this constructor
		HypE2() {}

		public CADShapeEnum dim()
		{
			return CADShapeEnum.EDGE;
		}
		public String getType()
		{
			return "E2";
		}
		public String impliedType(CADShapeEnum d)
		{
			if (d == CADShapeEnum.VERTEX)
				return "V1";
			else if (d == CADShapeEnum.EDGE)
				return getType();
			else
				return null;
		}
	}
	@SuppressWarnings("unused")
	private static class HypT3 implements HypInterface
	{
		// Hypothesis.getAlgo() needs access to this constructor
		HypT3() {}

		public CADShapeEnum dim()
		{
			return CADShapeEnum.FACE;
		}
		public String getType()
		{
			return "T3";
		}
		public String impliedType(CADShapeEnum d)
		{
			if (d == CADShapeEnum.VERTEX)
				return "V1";
			else if (d == CADShapeEnum.EDGE)
				return "E2";
			else if (d == CADShapeEnum.FACE)
				return getType();
			else
				return null;
		}
	}
	@SuppressWarnings("unused")
	private static class HypQ4 implements HypInterface
	{
		// Hypothesis.getAlgo() needs access to this constructor
		HypQ4() {}

		public CADShapeEnum dim()
		{
			return CADShapeEnum.FACE;
		}
		public String getType()
		{
			return "Q4";
		}
		public String impliedType(CADShapeEnum d)
		{
			if (d == CADShapeEnum.VERTEX)
				return "V1";
			else if (d == CADShapeEnum.EDGE)
				return "E2";
			else if (d == CADShapeEnum.FACE)
				return getType();
			else
				return null;
		}
	}
	@SuppressWarnings("unused")
	private static class HypT4 implements HypInterface
	{
		// Hypothesis.getAlgo() needs access to this constructor
		HypT4() {}

		public CADShapeEnum dim()
		{
			return CADShapeEnum.SOLID;
		}
		public String getType()
		{
			return "T4";
		}
		public String impliedType(CADShapeEnum d)
		{
			if (d == CADShapeEnum.VERTEX)
				return "V1";
			else if (d == CADShapeEnum.EDGE)
				return "E2";
			else if (d == CADShapeEnum.FACE)
				return "T3";
			else if (d == CADShapeEnum.SOLID)
				return getType();
			else
				return null;
		}
	}

}
