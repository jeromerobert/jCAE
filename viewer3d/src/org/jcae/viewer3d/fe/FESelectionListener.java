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
 * (C) Copyright 2005, by EADS CRC
 */

package org.jcae.viewer3d.fe;

import java.util.Map;

/**
 * This class handle selection event in a finit element mesh.
 * A selection is a Map having the following structure
 * <code>
 * Selection(Map)
 * |
 * domainID (Integer) -> Map
 *                       |
 *                       element type (Short, FE_XXXX) -> id list int[] 
 * </code>
 * The id of an elements is its order when it is provided by the FEProvider.
 * example: The first provided elements for type FE_TRIA3 has the 0 id and the
 * last one have the (FEDomain.getNumberOfTria3()-1) id.
 * @author Jerome Robert
 * @deprecated
 */
public interface FESelectionListener
{
	final static short FE_VERTEX=0;
	final static short FE_BEAM2=1;
	final static short FE_BEAM3=2;
	final static short FE_TRIA3=3;
	final static short FE_TRIA6=4;
	final static short FE_QUAD4=5;
	final static short FE_QUAD8=6;
	final static short FE_POLY=7;
	final static short FE_TETRA4=8;
	final static short FE_TETRA10=9;
	final static short FE_PYRA5=10;
	final static short FE_PYRA13=11;
	final static short FE_HEXA8=12;
	final static short FE_HEXA20=13;
	final static short FE_PENTA6=14;
	final static short FE_PENTA15=15;
	
	/** Return ids of selected elements for each type of elements (FE_VERTEX, FE_BEAM2...).
	 * The id of an elements is its ordered when it is provided by the FEProvider.
	 * example: The first provided elements for type FE_TRIA3 has the 0 id.
	 * @param selection list elements id for each type of elements (map Short to int[]) */
	void elementsSelected(Map selection);
}
