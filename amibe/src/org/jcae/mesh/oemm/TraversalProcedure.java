/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.

    Copyright (C) 2005, by EADS CRC
    Copyright (C) 2007, by EADS France

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

package org.jcae.mesh.oemm;


/**
 * Abstract class to implement OEMM traversal.  Derived classes must define the
 * {@link #action} method.
 * 
 * @see OEMM#walk
 */
public abstract class TraversalProcedure
{
	/**
	 * Current node is not a leaf and its children have not been processed yet.
	 */
	static final int PREORDER  = 1;
	/**
	 * Current node is not a leaf and its children have already been processed.
	 */
	protected static final int POSTORDER = 2;
	/**
	 * Current node is a leaf
	 */
	protected static final int LEAF      = 3;
	
	/**
	 * Continue tree traversal.
	 */
	public static final int OK  = 0;
	/**
	 * Abort tree traversal immediately.
	 */
	public static final int ABORT = 1;
	/**
	 * Do not process children.
	 */
	public static final int SKIPCHILD = 2;
	
	/**
	 * Method called before traversing the OEMM.
	 */
	public void init(OEMM oemm)
	{
	}
	
	/**
	 * Method called after traversing the OEMM.
	 */
	public void finish(OEMM oemm)
	{
	}
	
	/**
	 * Abstract class which has to be overridden by subclasses.
	 * 
	 * @param  o       OEMM structure
	 * @param  c       the current node of the OEMM structure
	 * @param  octant  the octant number in parent node
	 * @param  visit   this parameter is set to <code>LEAF</code> if the current
	 *                 node is a leaf, <code>PREORDER</code> if children have not
	 *                 yet been traversed, and <code>POSTOREDER</code> otherwise.
	 * @return  ABORT      exit from {@link OEMM#walk} immediately
	 *          SKIPCHILD  skip current cell (ie do not process its children)
	 *          OK         process normally
	 * @see OEMM#walk
	 */
	public abstract int action(OEMM o, OEMM.Node c, int octant, int visit);
	
}
