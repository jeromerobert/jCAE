/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2005
                  Jerome Robert <jeromerobert@users.sourceforge.net>

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
import gnu.trove.TFloatArrayList;
import org.jcae.mesh.amibe.ds.MMesh3D;
import org.jcae.mesh.amibe.ds.MFace3D;
import org.jcae.mesh.amibe.ds.MNode3D;
import java.util.Iterator;

public class NodeConnectivity3D extends QualityProcedure
{
	private TObjectIntHashMap nodeMap = new TObjectIntHashMap();
	private TFloatArrayList result = new TFloatArrayList();
	
	public NodeConnectivity3D()
	{
		setType(QualityProcedure.NODE);
	}
	
	public float quality(Object o)
	{
		if (!(o instanceof MFace3D))
			throw new IllegalArgumentException();
		MFace3D f = (MFace3D) o;
		Iterator itn = f.getNodesIterator();
		for (int i = 0; i < 3; i++)
		{
			MNode3D n = (MNode3D) itn.next();
			if (!nodeMap.increment(n))
				nodeMap.put(n, 1);
		}
		return 0.0f;
	}
	
	public void finish(TFloatArrayList data)
	{
		data.clear();
		data.add(result.toNativeArray());
	}
	
	public void postProcess3D(MMesh3D mesh3D)
	{
		result.clear();
		for (Iterator itn = mesh3D.getNodesIterator(); itn.hasNext(); )
		{
			MNode3D n = (MNode3D) itn.next();
			int cnt = nodeMap.get(n);
			float q = ((float) cnt) / 6.0f;
			if (cnt <= 6)
				result.add(q);
			else if (cnt <= 12)
				result.add(2.0f - q);
			else
				result.add(0.0f);
		}
	}
	
}
