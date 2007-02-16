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

import org.apache.log4j.Logger;

/**
 * Abstract class ro implement raw OEMM traversal.
 * 
 * @see OEMM#walk
 */
public abstract class TraversalProcedure
{
	private static Logger logger = Logger.getLogger(TraversalProcedure.class);	
	
	protected static final int PREORDER  = 1;
	protected static final int POSTORDER = 2;
	protected static final int LEAF      = 3;
	
	public static final int OK  = 0;
	public static final int ABORT = 1;
	public static final int SKIPCHILD = 2;
	
	private int nrNodes = 0;
	private int nrLeaves = 0;
	
	/**
	 * Method called before traversing the OEMM.
	 */
	public void init(OEMM oemm)
	{
		nrNodes = 0;
		nrLeaves = 0;
	}
	
	/**
	 * Method called after traversing the OEMM.
	 */
	public void finish(OEMM oemm)
	{
	}
	
	/**
	 * Display statistics about this traversal.
	 */
	public void printStats()
	{
		logger.info("Leaves: "+nrLeaves+"   Octants: "+nrNodes);
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
	 */
	public abstract int action(OEMM o, OEMMNode c, int octant, int visit);
	
	/**
	 * This method is called when descending the tree.
	 * 
	 * @param  o       OEMM structure
	 * @param  c       the current node of the OEMM structure
	 * @param  octant  the octant number in parent node
	 */
	public int preorder(OEMM o, OEMMNode c, int octant)
	{
		int res = 0;
		nrNodes++;
		if (c.isLeaf)
		{
			nrLeaves++;
			if (logger.isDebugEnabled())
				logger.debug("Found LEAF: "+c);
			res = action(o, c, octant, LEAF);
		}
		else
		{
			if (logger.isDebugEnabled())
				logger.debug("Found PREORDER: "+c);
			res = action(o, c, octant, PREORDER);
		}
		logger.debug("  Res; "+res);
		return res;
	}
	
	/**
	 * This method is called when ascending the tree, which means that all children
	 * have been traversed.
	 * 
	 * @param  o       OEMM structure
	 * @param  c       the current node of the OEMM structure
	 * @param  octant  the octant number in parent node
	 */
	public int postorder(OEMM o, OEMMNode c, int octant)
	{
		if (logger.isDebugEnabled())
			logger.debug("Found POSTORDER: "+c);
		int res = action(o, c, octant, POSTORDER);
		logger.debug("  Res; "+res);
		return res;
	}
}
