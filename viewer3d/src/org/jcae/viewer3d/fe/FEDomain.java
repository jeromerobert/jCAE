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
 * (C) Copyright 2007, by EADS France
 */

package org.jcae.viewer3d.fe;

import org.jcae.viewer3d.ColoredDomain;
import org.jcae.viewer3d.MarkDomain;

/**
 * This interface represents a finite element mesh domain.
 * It extends MarkDomain to display mesh vertices.
 * @author Jerome Robert
 *
 */
public interface FEDomain extends MarkDomain, ColoredDomain
{
	/** Return the number of nodes in the mesh*/
	int getNumberOfNodes();
	
	/** Node coordinates*/
	float[] getNodes();
	
	int getNumberOfVertices();
	int[] getVertices();
	
	int getNumberOfBeam2();
	int[] getBeam2();

	int getNumberOfBeam3();	
	int[] getBeam3();
	
	int getNumberOfTria3();	
	int[] getTria3();

	int getNumberOfTria6();	
	int[] getTria6();
	
	int getNumberOfQuad4();	
	int[] getQuad4();

	int getNumberOfQuad8();	
	int[] getQuad8();

	int getNumberOfTetra4();	
	int[] getTetra4();

	int getNumberOfHexa8();	
	int[] getHexa8();

	int getID();
}
