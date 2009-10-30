/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2005, by EADS CRC

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

package org.jcae.mesh.amibe.validation;

import org.jcae.mesh.amibe.traits.MeshTraitsBuilder;
import gnu.trove.TFloatArrayList;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Abstract class to compute quality criteria.
 * All functions implementing quality criteria have to inherit from
 * this class.  These functions can compute values either on elements
 * or nodes, and can work either on 2D or 3D meshes.
 *
 * For these reasons, <code>quality</code> method's argument is an
 * <code>Object</code>, and caller is responsible for passing the
 * right argument.
 */
public abstract class QualityProcedure
{
	protected static final int FACE = 1;
	protected static final int NODE = 2;
	protected static final int EDGE = 3;
	
	// By default, values are computed by faces.
	protected int type = FACE;
	protected String [] usageStr;
	protected TFloatArrayList data;
	
	private static final Class<QualityProcedure> [] subClasses = new Class[]{
		// AbsoluteDeflection2D.class,  Disabled for now
		Area.class,
		DihedralAngle.class,
		MaxAngleFace.class,
		MaxLengthFace.class,
		MinAngleFace.class,
		MinLengthFace.class,
		// NodeConnectivity.class,
		null
	};

	public static String [] getListSubClasses()
	{
		String [] ret = new String[2*subClasses.length-2];
		int idx = 0;
		try
		{
			for (Class<QualityProcedure> c: subClasses)
			{
				if (c == null)
					break;
				Constructor<QualityProcedure> cons = c.getConstructor();
				QualityProcedure qproc = cons.newInstance();
				String [] str = qproc.usageStr;
				ret[2*idx] = str[0];
				ret[2*idx+1] = str[1];
				idx++;
			}
		}
		catch (NoSuchMethodException ex)
		{
		}
		catch (IllegalAccessException ex)
		{
		}
		catch (InstantiationException ex)
		{
		}
		catch (InvocationTargetException ex)
		{
		}
		return ret;
	}

	protected abstract void setValidationFeatures();
	public QualityProcedure()
	{
		setValidationFeatures();
	}

	/**
	 * Return element type.
	 *
	 * @return element type
	 */
	protected final int getType()
	{
		return type;
	}
	
	/**
	 * Return the quality factor for a given object.
	 *
	 * @param o  entity at which quality is computed
	 * Returns quality factor.
	 */
	public abstract float quality(Object o);
	
	/**
	 * Returns default scale factor.
	 */
	protected final float getScaleFactor()
	{
		return 1.0f;
	}
	
	/**
	 * Returns <code>MeshTraitsBuilder</code> instance needed by this class.
	 */
	protected final MeshTraitsBuilder getMeshTraitsBuilder()
	{
		return new MeshTraitsBuilder();
	}
	
	/**
	 * Finish quality computations.
	 * By default, this method does nothing and can be overriden
	 * when post-processing is needed.
	 */
	public void finish()
	{
	}
	
	/**
	 * Make output array visible by the {@link #finish} method.
	 * @see QualityFloat#setQualityProcedure
	 *
	 * @param d  array containing output values
	 */
	public final void bindResult(TFloatArrayList d)
	{
		data = d;
	}
	
}
