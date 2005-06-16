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

package org.jcae.viewer3d.cad;

/**
 * A selection in a ViewableCAD.
 * If faceIDs, edgesIDs and vertexIDs are null, the whole domain is concidered as selected.
 * @author Jerome Robert
 */
public class CADSelection
{
	private int domainID;
	private int[] faceIDs;
	private int[] edgeIDs;
	private int[] vertexIDs;
	
	/**
	 * @param domainID
	 * @param faceIDs
	 * @param edgeIDs
	 * @param vertexIDs
	 */
	public CADSelection(int domainID, int[] faceIDs, int[] edgeIDs,
		int[] vertexIDs)
	{
		this.domainID = domainID;
		this.faceIDs = faceIDs;
		this.edgeIDs = edgeIDs;
		this.vertexIDs = vertexIDs;
	}
	
	/** Return the domain ID of the this selection.
	 * The first domain returned by the CADProvider get the 0 ID, and so on.
	 */
	public int getDomainID()
	{
		return domainID;
	}
	
	/** Return the edges IDs of the this selection.
	 * The first edge returned by the CADDomain get the 0 ID, and so on.
	 */
	public int[] getEdgeIDs()
	{
		return edgeIDs;
	}

	/** Return the faces IDs of the this selection.
	 * The first face returned by the CADDomain get the 0 ID, and so on.
	 */	
	public int[] getFaceIDs()
	{
		return faceIDs;
	}

	/** Return the vertices IDs of the this selection.
	 * The first vertex returned by the CADDomain get the 0 ID, and so on.
	 */	
	public int[] getVertexIDs()
	{
		return vertexIDs;
	}
}
