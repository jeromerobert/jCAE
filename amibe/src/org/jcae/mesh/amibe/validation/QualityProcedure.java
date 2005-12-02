/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2005
                  Jerome Robert <jeromerobert@users.sourceforge.net>

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

import gnu.trove.TFloatArrayList;

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
	
	// By default, values are computed by faces.
	private int type = FACE;
	protected TFloatArrayList data;
	
	/**
	 * Return the quality factor for a given object.
	 *
	 * @param o  entity at which quality is computed
	 * Returns quality factor.
	 */
	public abstract float quality(Object o);
	
	/**
	 * Finish quality computations.
	 * By default, this method does nothing and can be overriden
	 * when post-processing is needed.
	 */
	public void finish()
	{
	}
	
	/**
	 * Return element type.
	 *
	 * @return element type.
	 */
	protected final int getType()
	{
		return type;
	}
	
	/**
	 * Set the element type.  Valid values are {@link #FACE} and
	 * {@link #NODE}.  By default, type is <code>FACE</code>.
	 * Quality procedures which compute values on nodes need to
	 * call this function, otherwise the data file written by
	 * {@link QualityFloat#printMeshBB(String)}
	 *
	 * @param t  the element type.
	 */
	protected final void setType(int t)
	{
		type = t;
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
