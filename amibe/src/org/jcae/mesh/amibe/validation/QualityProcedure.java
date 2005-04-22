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
 * Abstract class for to compute quality criteria.
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
	
	private int type = FACE;
	
	/**
	 * Return the quality factor for a given object.
	 * @param o  entity at which quality is computed
	 * Returns quality factor.
	 */
	public abstract float quality(Object o);
	
	/**
	 * Transform data values after all elements have been processed.
	 * This method do nothing by default.
	 */
	public void finish(TFloatArrayList data)
	{
	}
	
	/**
	 * Return element type.
	 *
	 * @return element type.
	 */
	protected int getType()
	{
		return type;
	}
	
	/**
	 * Set the element type.
	 *
	 * @param t  the element type.
	 */
	protected void setType(int t)
	{
		type = t;
	}
}
