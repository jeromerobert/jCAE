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

package org.jcae.viewer3d.fd;

import java.util.Iterator;
import org.jcae.viewer3d.ColoredDomain;
import org.jcae.viewer3d.MarkDomain;

/**
 * A domain of a finit difference mesh.
 * @author Jerome Robert
 */
public interface FDDomain extends ColoredDomain, MarkDomain
{
	public static final byte SLOT_FIRST=0;
	public static final byte XY_SLOT=0;
	public static final byte XZ_SLOT=1;
	public static final byte YX_SLOT=2;
	public static final byte YZ_SLOT=3;
	public static final byte ZX_SLOT=4;
	public static final byte ZY_SLOT=5;
	public static final byte SLOT_LAST=5;
	
	int getNumberOfXPlate();
	int getNumberOfYPlate();
	int getNumberOfZPlate();
	int getNumberOfXWire();
	int getNumberOfYWire();
	int getNumberOfZWire();
	/**
	 * 
	 * @param orientation XY_SLOT, XZ_SLOT...
	 * @return
	 */ 
	int getNumberOfSlot(byte orientation);
	int getNumberOfSolid();

	/** Return an iterator on {i, j1, k1, j2, k2} arrays, describing X plates */
	Iterator getXPlateIterator();
	
	/** Return an iterator on {j, i1, k1, i2, k2} arrays, describing Y plates */
	Iterator getYPlateIterator();
	
	/** Return an iterator on {k, i1, j1, i2, j2} arrays, describing Z plates */
	Iterator getZPlateIterator();

	/** Return an iterator on {i, j, k, i2} arrays, describing X wires */
	Iterator getXWireIterator();
	
	/** Return an iterator on {i, j, k, j2} arrays, describing Y wires */
	Iterator getYWireIterator();
	
	/** Return an iterator on {i, j, k, k2} arrays, describing Z wires */
	Iterator getZWireIterator();
	
	/**
	 * Return an iterator on int[]{i1, i2, j, k}, describing XY and XZ slots<br>	
	 * Return an iterator on int[]{j1, j2, i, k}, describing YX and YZ slots<br>	
	 * Return an iterator on int[]{k1, k2, i, j}, describing ZY and ZX slots or<br>
	 * float[]{x0, y0, z0, z1, y1, z1}
	 */	 
	Iterator getSlotIterator(byte type);
	
	/** Return an iteraotr on {i1, j1, k1, i2, j2, k2} arrays, describing volumes*/
	Iterator getSolidIterator();	
}
