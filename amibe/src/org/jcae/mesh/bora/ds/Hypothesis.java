/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

   (C) Copyright 2006, by EADS CRC
   (C) Copyright 2007, by EADS France

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
import org.apache.log4j.Logger;
import java.lang.reflect.Constructor;

public class Hypothesis
{
	private static Logger logger = Logger.getLogger(Hypothesis.class);
	protected HypInterface hyp = HypNoneInstance;
	// Now, length is the target length and is the variable that is used;
	// In the near future we will want to enforce a maximum length lengthMax 
	protected double length = -1.0, lengthMin = -1.0, lengthMax = -1.0;
	protected double deflection = -1.0;
	protected boolean lengthBool = false, numberBool = false;
	protected int numberMin = -1, numberMax = -1;
	private boolean locked = false;

	// Unique identitier
	private int id = -1;
	private static int nextId = -1;

	private static Class [] innerClasses = Hypothesis.class.getDeclaredClasses();
	private static HypNone HypNoneInstance = new HypNone();

	public Hypothesis()
	{
		nextId++;
		id = nextId;
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
		logger.debug("("+Integer.toHexString(this.hashCode())+") Setting element type to "+e+"  "+hyp.getClass().getName());
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
	public boolean checkCompatibility(CADShapeEnum cse)
	{
		return hyp.impliedType(cse) != null;
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
	public void setLength(double l, boolean b)
	{
		checkLock();
		logger.debug("("+Integer.toHexString(this.hashCode())+") Setting length to "+l+"; strong constraint: "+b);
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
	public void setLength(double l1, double l2)
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
	public void setLength(double l1, double l2, boolean b)
	{
		checkLock();
		logger.debug("("+Integer.toHexString(this.hashCode())+") Setting length; min="+l1+" max="+l2+"; strong constraint: "+b);
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
		logger.debug("("+Integer.toHexString(this.hashCode())+") Setting deflection to "+d);
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
	public void setNumber(int n, boolean b)
	{
		checkLock();
		logger.debug("("+Integer.toHexString(this.hashCode())+") Setting number of discretized points to "+n+"; strong constraint: "+b);
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
	public void setNumber(int n1, int n2)
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
	public void setNumber(int n1, int n2, boolean b)
	{
		checkLock();
		logger.debug("("+Integer.toHexString(this.hashCode())+") Setting number of discretized points; min="+n1+" max="+n2+"; strong constraint: "+b);
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

	public void combine(Hypothesis that)
	{
		String elt = getElement();
		if (elt == null || !elt.equals(that.getElement()))
			throw new RuntimeException();

		length     = combineDouble(length, that.length);
		lengthMin  = combineDouble(lengthMin, that.lengthMin);
		lengthMax  = combineDouble(lengthMax, that.lengthMax);
		deflection = combineDouble(deflection, that.deflection);
		lengthBool |= that.lengthBool;
	}

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

	public Hypothesis createInheritedHypothesis(CADShapeEnum cse)
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

	public AlgoInterface findAlgorithm(CADShapeEnum cse)
	{
		AlgoInterface ret = null;
		double targetLength = 0.5*(lengthMin+lengthMax);
		try {
			if (cse == CADShapeEnum.VERTEX)
			{
				Class [] typeArgs = new Class[0];
				Constructor cons = Vertex0d.class.getConstructor(typeArgs);
				ret = (AlgoInterface) cons.newInstance(new Object[0]);
			}
			else if (cse == CADShapeEnum.EDGE)
			{
				Class [] typeArgs = new Class[] {double.class, double.class, boolean.class};
				Constructor cons = UniformLengthDeflection1d.class.getConstructor(typeArgs);
				ret = (AlgoInterface) cons.newInstance(new Object [] {new Double(targetLength), new Double(deflection), Boolean.valueOf(true)});
			}
			else if (cse == CADShapeEnum.FACE)
			{
				Class [] typeArgs = new Class[] {double.class, double.class, boolean.class, boolean.class};
				Constructor cons = Basic2d.class.getConstructor(typeArgs);
				ret = (AlgoInterface) cons.newInstance(new Object [] {new Double(targetLength), new Double(deflection), Boolean.valueOf(true), Boolean.valueOf(true)});
			}
			else if (cse == CADShapeEnum.SOLID)
			{
				Class [] typeArgs = new Class[] {double.class};
				Constructor cons = TetGen.class.getConstructor(typeArgs);
				ret = (AlgoInterface) cons.newInstance(new Object [] {new Double(targetLength)});
				if (!ret.isAvailable())
					logger.error("TetGen not available!");
				/*
				Constructor cons = Netgen.class.getConstructor(typeArgs);
				ret = (AlgoInterface) cons.newInstance(new Object [] {new Double(targetLength)});
				if (!ret.isAvailable())
					logger.error("Netgen not available!");
				*/
			}
		} catch (Exception ex)
		{
			ex.printStackTrace();
			System.exit(1);
		}
		return ret;
	}

	public static interface HypInterface
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
		};
		return h;
	}

	public static class HypNone implements HypInterface
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
	public static class HypV1 implements HypInterface
	{
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
			else
				return null;
		}
	}
	public static class HypE2 implements HypInterface
	{
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
	public static class HypT3 implements HypInterface
	{
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
	public static class HypQ4 implements HypInterface
	{
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
	public static class HypT4 implements HypInterface
	{
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
