/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finite element mesher, Plugin architecture.
 
    Copyright (C) 2003,2004,2005, by EADS CRC
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
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.amibe.metrics;

import static org.junit.Assert.*;
import org.junit.Test;

public class Matrix2DTest
{
	@Test public void det()
	{
		Matrix2D A = new Matrix2D(4.0, 1.0, 1.0, 1.0);
		assertTrue(A.det() == 3.0);
	}
	@Test public void intersection()
	{
		Matrix2D A = new Matrix2D(4.0, 0.0, 0.0, 2.0);
		Matrix2D B = new Matrix2D(2.0, 0.0, 0.0, 1.0);
		Matrix2D C = B.intersection(A);
		double [][] c = new double[2][2];
		C.getValues(c);
		assertTrue(c[0][0] == 4.0 && c[0][1] == 0.0 && c[1][0] == 0.0 && c[1][1] == 2.0);
	}

}
