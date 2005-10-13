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
import java.util.Map;

/**
 * This is a selection of finit elements in a domain. This can be a compressed
 * selection or a selection of elementary cells.
 * @author Jerome Robert
 * @todo implement
 */
public class FDSelection
{
	private int domainID;
	private int[][] slotCells=new int[6][];
	private int[] xWireCells, yWireCells, zWireCells;
	
	public FDSelection(int domainID)
	{
		this.domainID=domainID;
	}
	
	public int getDomainID()
	{
		return domainID;
	}
	
	/** Return the array of selected X plate ID
	 * The first X plate returned by the FDDomain X plate iterator get the 0 ID
	 * and so on.
	 */
	public int[] getXPlates()
	{
		//TODO
		return null;
	}

	/** Return the array of selected Y plate ID
	 * The first Y plate returned by the FDDomain Y plate iterator get the 0 ID
	 * and so on.
	 */
	public int[] getYPlates()
	{
		//TODO
		return null;
	}

	/** Return the array of selected Z plate ID
	 * The first Z plate returned by the FDDomain Z plate iterator get the 0 ID
	 * and so on.
	 */
	public int[] getZPlates()
	{
		//TODO
		return null;
	}

	/** Return the array of selected X wire ID
	 * The first X plate returned by the FDDomain X wire iterator get the 0 ID
	 * and so on.
	 */
	public int[] getXWires()
	{
		//TODO
		return null;
	}

	/** Return the array of selected Y wire ID
	 * The first Y plate returned by the FDDomain Y wire iterator get the 0 ID
	 * and so on.
	 */
	public int[] getYWires()
	{
		//TODO
		return null;
	}

	/** Return the array of selected Z wire ID
	 * The first Z plate returned by the FDDomain Z wire iterator get the 0 ID
	 * and so on.
	 */
	public int[] getZWires()
	{
		//TODO
		return null;
	}	
	
	/**
	 * 
	 * @param FDDomain.XY_SLOT, FDDomain.XZ_SLOT...
	 * @return
	 */
	public int[] getSlots(byte type)
	{			
		//TODO
		return null;
	}
	
	public int[] getSolids()
	{
		//TODO
		return null;
	}
	
	/**
	 * Return a map matching mark type to list of mark id. The id of a mark
	 * is its order in the array returned by <code>MarkDomain.getMarks</code>
	 * @return
	 */
	public Map getMarks()
	{
		//TODO
		return null;
	}
	
	/**
	 * Return a {i1, j1, k1, i2, j2, k2...} array containing the list of selected
	 * elementary X wire.
	 * @return
	 */
	int[] getX1DCells()
	{
		//TODO
		return null;
	}
	
	int[] getY1DCells()
	{
		//TODO
		return null;
	}
	
	int[] getZ1DCells()
	{
		//TODO
		return null;
	}

	int[] getX2DCells()
	{
		//TODO
		return null;
	}
		
	int[] getY2DCells()
	{
		//TODO
		return null;
	}

	int[] getZ2DCells()
	{
		//TODO
		return null;
	}
	
	int[] get3DCells()
	{
		//TODO
		return null;
	}

	int[] getXYSlotCells()
	{
		//TODO
		return null;
	}

	int[] getXZSlotCells()
	{
		//TODO
		return null;
	}

	int[] getYXSlotCells()
	{
		//TODO
		return null;
	}

	int[] getYZSlotCells()
	{
		//TODO
		return null;
	}

	int[] getZXSlotCells()
	{
		//TODO
		return null;
	}

	int[] getZYSlotCells()
	{
		//TODO
		return null;
	}	

	final private static String CR=System.getProperty("line.separator");  
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return "Domain id: "+domainID+CR+
			"X plates: "+Utils.intArrayToCollection(getXPlates())+CR+
			"Y plates: "+Utils.intArrayToCollection(getYPlates())+CR+
			"Z plates: "+Utils.intArrayToCollection(getZPlates())+CR;
	}

	void setSlotCells(byte type, int[] cells)
	{
		slotCells[type]=cells;
	}
	
	public int[] getSlotCells(byte type)
	{
		return slotCells[type];
	}

	public int[] getXWireCells()
	{
		return xWireCells;
	}

	void setXWireCells(int[] wireCells)
	{
		xWireCells = wireCells;
	}

	public int[] getYWireCells()
	{
		return yWireCells;
	}

	void setYWireCells(int[] wireCells)
	{
		yWireCells = wireCells;
	}

	public int[] getZWireCells()
	{
		return zWireCells;
	}

	void setZWireCells(int[] wireCells)
	{
		zWireCells = wireCells;
	} 

}