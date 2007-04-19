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

import java.util.Iterator;
import org.jcae.viewer3d.ColoredDomain;
import org.jcae.viewer3d.Domain;
import org.jcae.viewer3d.MarkDomain;

/**
 * This interace represent a finit element mesh domain.
 * It inherit MarkDomain to display vertices of the mesh.
 * @author Jerome Robert
 *
 */
public interface FEDomain extends MarkDomain, ColoredDomain
{
	/** Return the number of nodes in the mesh*/
	int getNumberOfNodes();
	
	/** Return an iterator on a Collection of float[3] */ 
	Iterator getNodesIterator();
	
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
	Iterator getVerticesIterator();
	int getNumberOfVerticesComp();
	Iterator getNumberOfVerticesRes();
			
	int[] getBeam2Indices();
	int getNumberOfBeam2();
	int getNumberOfBeam2Comp();
	Iterator getBeam2Res();

	int getNumberOfBeam3();	
	Iterator getBeam3Iterator();
	int getNumberOfBeam3Comp();
	Iterator getBeam3Res();
	
	/** Return the number of 3 nodes triangles in the mesh*/
	int getNumberOfTria3();	
	/** Return an iterator on a Collection of int[3] */
	Iterator getTria3Iterator();
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
	Iterator getQuad8Iterator();
	int getNumberOfQuad8Comp();
	Iterator getQuad8Res();

	int getNumberOfPoly();	
	Iterator getPolyIterator();
	int getNumberOfPolyComp();
	Iterator getPolyRes();
	
	int getNumberOfTetra4();	
	Iterator getTetra4Iterator();
	int getNumberOfTetra4Comp();
	Iterator getTetra4Res();

	int getNumberOfTetra10();	
	Iterator getTetra10Iterator();
	int getNumberOfTetra10Comp();
	Iterator getTetra10Res();
	
	int getNumberOfPyra5();	
	Iterator getPyra5Iterator();
	int getNumberOfPyra5Comp();
	Iterator getPyra5Res();

	int getNumberOfPyra13();	
	Iterator getPyra13Iterator();
	int getNumberOfPyra13Comp();
	Iterator getPyra13Res();

	int getNumberOfHexa8();	
	Iterator getHexa8Iterator();
	int getNumberOfHexa8Comp();
	Iterator getHexa8Res();

	int getNumberOfHexa20();	
	Iterator getHexa20Iterator();
	int getNumberOfHexa20Comp();
	Iterator getHexa20Res();

	int getNumberOfPenta6();	
	Iterator getPenta6Iterator();
	int getNumberOfPenta6Comp();
	Iterator getPenta6Res();

	int getNumberOfPenta15();	
	Iterator getPenta15Iterator();
	int getNumberOfPenta15Comp();
	Iterator getPenta15Res();
	
	int getID();
}
