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

import java.util.Iterator;
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
	
	/** getNumberOfNodes and getNodesIterator are deprecated... Iterators sucks !*/
	float[] getNodes();
	
	/** Return the number of result component.
	 * <ul>
	 * <li>0 mean no result. In this case the viewer will display a color from getColor</li>
	 * <li>1 mean a scalar value. In this case the viewer will compute a color
	 * map matching these values</li>
	 * <li>3 mean a vector value. In this case the viewer will display vectors</li>
	 * </ul>
	 * @return
	 */
	int getNumberOfNodeComp();
	
	/** Return an iterator on a Collection of float[getNumberOfNodeComp()] */
	Iterator getNodesRes();
	
	int getNumberOfVertices();
	int[] getVertices();
	int getNumberOfVerticesComp();
	Iterator getNumberOfVerticesRes();
			
	int getNumberOfBeam2();
	int[] getBeam2();
	int getNumberOfBeam2Comp();
	Iterator getBeam2Res();

	int getNumberOfBeam3();	
	int[] getBeam3();
	int getNumberOfBeam3Comp();
	Iterator getBeam3Res();
	
	int getNumberOfTria3();	
	int[] getTria3();
	int getNumberOfTria3Comp();
	Iterator getTria3Res();

	int getNumberOfTria6();	
	int[] getTria6();
	int getNumberOfTria6Comp();
	Iterator getTria6Res();
	
	int getNumberOfQuad4();	
	int[] getQuad4();
	int getNumberOfQuad4Comp();
	Iterator getQuad4Res();

	int getNumberOfQuad8();	
	int[] getQuad8();
	int getNumberOfQuad8Comp();
	Iterator getQuad8Res();

	int getNumberOfTetra4();	
	int[] getTetra4();
	int getNumberOfTetra4Comp();
	Iterator getTetra4Res();

	int getNumberOfHexa8();	
	int[] getHexa8();
	int getNumberOfHexa8Comp();
	Iterator getHexa8Res();

	int getID();
}
