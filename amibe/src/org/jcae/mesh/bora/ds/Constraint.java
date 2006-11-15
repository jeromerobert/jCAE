/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

   (C) Copyright 2006, by EADS CRC

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

import org.jcae.mesh.cad.CADShape;
import org.jcae.mesh.bora.algo.*;
import java.util.Iterator;
import java.util.Collection;
import java.lang.reflect.Constructor;
import org.apache.log4j.Logger;

public class Constraint extends Hypothesis
{
	private static Logger logger = Logger.getLogger(Constraint.class);
	protected int dimension = -1;
	protected boolean dirty = false;
	protected AlgoInterface algo = null;
	private static Class [] innerClasses = Constraint.class.getDeclaredClasses();

	// Is there a better way to do that?
	private void copyHypothesis(Hypothesis h, int d)
	{
		dimension   = d;
		lengthMin   = h.lengthMin;
		lengthMax   = h.lengthMax;
		deflection  = h.deflection;
		lengthBool  = h.lengthBool;
		numberBool  = h.numberBool;
		numberMin   = h.numberMin;
		numberMax   = h.numberMax;
		elementType = Constraint.impliedType(d, h.elementType);
		logger.debug("("+Integer.toHexString(h.hashCode())+") Dim: "+d+" Algo "+h.elementType+" mapped to "+elementType);
	}

	// Creates a Constraint derived from an Hypothesis
	private static Constraint createConstraint(Hypothesis h, int d)
	{
		Constraint ret = new Constraint();
		ret.copyHypothesis(h, d);
		if (ret.elementType == null)
			return null;
		return ret;
	}

	// Combines with an Hypothesis for a given dimension
	private void combine(BCADGraphCellHypothesis mh, int d)
	{
		Constraint that = createConstraint(mh.getHypothesis(), d);
		if (that == null)
			return;
		if (dimension == -1)
			copyHypothesis(that, d);
		if (elementType != that.elementType)
		{
			logger.debug("Element "+elementType+" and "+that.elementType+" differ and are not combined together");
		}
		double targetLengthMax = lengthMax;
		if (targetLengthMax > that.lengthMax)
			targetLengthMax = that.lengthMax;
		double targetLengthMin = lengthMin;
		if (targetLengthMin < that.lengthMin)
			targetLengthMin = that.lengthMin;
		if (lengthBool && that.lengthBool)
		{
			if (targetLengthMin > targetLengthMax)
			{
				dirty = true;
				throw new RuntimeException("length min > length max");
			}
			lengthMax = targetLengthMax;
			lengthMin = targetLengthMin;
		}
		else
		{
			lengthBool |= that.lengthBool;
			if (targetLengthMin > targetLengthMax)
			{
				dirty = true;
				printDirty(mh);
				lengthMax = targetLengthMin;
				lengthMin = targetLengthMax;
			}
		}
		int targetNumberMax = numberMax;
		if (targetNumberMax > that.numberMax)
			targetNumberMax = that.numberMax;
		int targetNumberMin = numberMin;
		if (targetNumberMin < that.numberMin)
			targetNumberMin = that.numberMin;
		if (numberBool && that.numberBool)
		{
			if (targetNumberMin > targetNumberMax)
			{
				dirty = true;
				throw new RuntimeException("number min > number max");
			}
			numberMax = targetNumberMax;
			numberMin = targetNumberMin;
		}
		else
		{
			numberBool |= that.numberBool;
			if (targetNumberMin > targetNumberMax)
			{
				dirty = true;
				printDirty(mh);
				numberMax = targetNumberMin;
				numberMin = targetNumberMax;
			}
		}
		double targetDefl = deflection;
		if (targetDefl > that.deflection)
			targetDefl = that.deflection;
		deflection = targetDefl;
	}

	private void printDirty(BCADGraphCellHypothesis mh)
	{
		logger.warn("Hypothesis not compatible: "+mh+": "+mh.getHypothesis()+" with "+this);
	}

	/**
	 * Combines all Hypothesis of a Collection.  In order to improve error
	 * reporting, BCADGraphCellHypothesis objects are passed as arguments instead
	 * of Hypothesis.
	 *
	 * @param mh  list of BCADGraphCellHypothesis objects.
	 * @param d   dimension
	 */
	public static Constraint combineAll(Collection mh, int d)
	{
		Constraint ret = null;
		if (mh.size() == 0)
			return null;
		ret = new Constraint();
		for (Iterator ita = mh.iterator() ; ita.hasNext(); )
			ret.combine((BCADGraphCellHypothesis) ita.next(), d);
		if (ret.dimension == -1)
			ret = null;
		return ret;
	}

	private static Hyp getAlgo(int d, String elt)
	{
		Hyp h = null;
		if (elt == null)
			return null;
		try {
			for (int i = 0; i < innerClasses.length; i++)
			{
				if (innerClasses[i].getName().equals(Constraint.class.getName()+"$Hyp"+elt))
					h = (Hyp) innerClasses[i].newInstance();
			}
		} catch (Exception ex) {ex.printStackTrace(); };
		return h;
	}

	private static String impliedType(int d, String elt)
	{
		Hyp h = getAlgo(d, elt);
		if (h == null)
			return null;
		return h.impliedType(d);
	}

	/**
	 * Finds the best algorithm suited to constraints defined on a submesh.
	 */
	public void findAlgorithm()
	{
		double targetLength = 0.5*(lengthMin+lengthMax);
		try {
			if (dimension == BCADGraph.DIM_EDGE)
			{
				Class [] typeArgs = new Class[] {double.class, double.class, boolean.class};
				Constructor cons = UniformLengthDeflection1d.class.getConstructor(typeArgs);
				algo = (AlgoInterface) cons.newInstance(new Object [] {new Double(targetLength), new Double(deflection), new Boolean(true)});
			}
			else if (dimension == BCADGraph.DIM_FACE)
			{
				Class [] typeArgs = new Class[] {double.class, double.class, boolean.class, boolean.class};
				Constructor cons = Basic2d.class.getConstructor(typeArgs);
				algo = (AlgoInterface) cons.newInstance(new Object [] {new Double(targetLength), new Double(deflection), new Boolean(true), new Boolean(true)});
			}
			else if (dimension == BCADGraph.DIM_SOLID)
			{
				Class [] typeArgs = new Class[] {double.class};
				Constructor cons = TetGen.class.getConstructor(typeArgs);
				algo = (AlgoInterface) cons.newInstance(new Object [] {new Double(targetLength)});
				if (!algo.isAvailable())
					logger.error("TetGen not available!");
				/*
				Constructor cons = Netgen.class.getConstructor(typeArgs);
				algo = (AlgoInterface) cons.newInstance(new Object [] {new Double(targetLength)});
				if (!algo.isAvailable())
					logger.error("Netgen not available!");
				*/
			}
		} catch (Exception ex)
		{
			ex.printStackTrace();
			System.exit(1);
		}
	}

	public void applyAlgorithm(BCADGraphCell m)
	{
		if (algo == null)
			findAlgorithm();
		if (!algo.compute(m))
			logger.warn("Failed! "+algo);
	}

	private interface Hyp
	{
		public String impliedType(int d);
		public int dim();
	}

	public static class HypE2 implements Hyp
	{
		public int dim()
		{
			return BCADGraph.DIM_EDGE;
		}
		public String impliedType(int d)
		{
			if (d == BCADGraph.DIM_EDGE)
				return "E2";
			else
				return null;
		}
	}
	public static class HypT3 implements Hyp
	{
		public int dim()
		{
			return BCADGraph.DIM_FACE;
		}
		public String impliedType(int d)
		{
			if (d == BCADGraph.DIM_EDGE)
				return "E2";
			else if (d == BCADGraph.DIM_FACE)
				return "T3";
			else
				return null;
		}
	}
	public static class HypT4 implements Hyp
	{
		public int dim()
		{
			return BCADGraph.DIM_SOLID;
		}
		public String impliedType(int d)
		{
			if (d == BCADGraph.DIM_EDGE)
				return "E2";
			else if (d == BCADGraph.DIM_FACE)
				return "T3";
			else if (d == BCADGraph.DIM_SOLID)
				return "T4";
			else
				return null;
		}
	}
}
