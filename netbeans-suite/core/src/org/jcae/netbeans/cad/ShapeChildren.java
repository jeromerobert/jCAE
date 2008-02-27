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

package org.jcae.netbeans.cad;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

public class ShapeChildren extends Children.Array implements Node.Cookie
{			
	public void addShapes(Collection shapes)
	{
		HashSet<String> names=new HashSet<String>();
		for(Node n:getNodes())
			names.add(n.getName());

		ShapePool sp = getNode().getCookie(ShapePool.class);
		Iterator it=shapes.iterator();
		while(it.hasNext())
		{
			TopoDS_Shape s=(TopoDS_Shape) it.next();
			String name=sp.getName(s);
			if(!names.contains(name))
				nodes.add(new ShapeNode(name, s, sp));
		}
		refresh();
	}	
}
