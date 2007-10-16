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

import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntProcedure;
import gnu.trove.TFloatArrayList;
import org.jcae.mesh.amibe.ds.AbstractTriangle;
import org.jcae.mesh.amibe.ds.AbstractVertex;

/**
 * Compute node connectivity of 3D nodes.
 * This class implements the {@link QualityProcedure#quality(Object)}
 * method to compute node connectivity of 3D meshes.  As 3D meshes
 * have a simplistic data structure, computing node neighbours would
 * be very extensive.  Thus instead <code>quality</code> is called
 * with a {@link org.jcae.mesh.amibe.ds.AbstractTriangle} argument, this method
 * increments counters on its three vertices.  The {@link #finish}
 * method has to be called after looping on all faces.
 */

public class NodeConnectivity3D extends QualityProcedure
{
	private TObjectIntHashMap<AbstractVertex> nodeMap = new TObjectIntHashMap<AbstractVertex>();
	
	public NodeConnectivity3D()
	{
		setType(QualityProcedure.NODE);
	}
	
	@Override
	public float quality(Object o)
	{
		if (!(o instanceof AbstractTriangle))
			throw new IllegalArgumentException();
		AbstractTriangle f = (AbstractTriangle) o;
		for (int i = 0; i < 3; i++)
		{
			AbstractVertex n = f.vertex[i];
			if (!nodeMap.increment(n))
				nodeMap.put(n, 1);
		}
		return 0.0f;
	}
	
	/**
	 * Finish quality computations.  When all faces have been processed
	 * by {@link #quality}, this method rearrange result values to have
	 * the same node order as in mesh output.
	 */
	@Override
	public void finish()
	{
		data.clear();
		Count proc = new Count(data);
		nodeMap.forEachEntry(proc);
	}

	private static class Count implements TObjectIntProcedure<AbstractVertex>
	{
		private TFloatArrayList data;
		public Count(final TFloatArrayList d)
		{
			data = d;
		}
		public boolean execute(AbstractVertex a, int b)
		{
			float q = b / 6.0f;
			if (b <= 6)
				data.add(q);
			else if (b <= 12)
				data.add(2.0f - q);
			else
				data.add(0.0f);
			return true;
		}
	}
	
}
