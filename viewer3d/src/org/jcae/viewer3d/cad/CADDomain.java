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

import java.util.Iterator;
import org.jcae.viewer3d.MarkDomain;

/**
 * @author Jerome Robert
 *
 */
public interface CADDomain extends MarkDomain
{
	/** Iterator on array of float[3*number of nodes] */
	Iterator getEdgeIterator();
	
	/** Iterator on array float[3] */
	Iterator getVertexIterator();
	
	/** Iterator on FaceMesh objects*/
	Iterator getFaceIterator();
}
