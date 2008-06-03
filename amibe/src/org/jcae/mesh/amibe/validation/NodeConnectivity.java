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

package org.jcae.mesh.amibe.validation;

import org.jcae.mesh.amibe.ds.Vertex;
import java.util.Iterator;

public class NodeConnectivity extends QualityProcedure
{
	@Override
	protected void setValidationFeatures()
	{
		usageStr = new String[]{"NodeConnectivity", "node connectivity, normalized to 1"};
		type = QualityProcedure.NODE;
	}

	@Override
	public float quality(Object o)
	{
		if (!(o instanceof Vertex))
			throw new IllegalArgumentException();
		Vertex n = (Vertex) o;
		int count = 0;
		for (Iterator<Vertex> itnv = n.getNeighbourIteratorVertex(); itnv.hasNext(); itnv.next())
			count++;
		if (count <= 6)
			return (count / 6.0f);
		else if (count <= 11)
			return (2.0f - count / 6.0f);
		else
			return 0.0f;
	}
	
}
