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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.openide.nodes.Node;

/** map shape to face nodes */
public class ShapePool implements Node.Cookie
{
	private final Map<TopoDS_Shape, Node> nodeMap=
		new WeakHashMap<TopoDS_Shape, Node>();
	private final Set<String> names=new HashSet<String>();
	private final Map<TopoDS_Shape, String> shapeToNames=
		new WeakHashMap<TopoDS_Shape, String>();
	
	public void putNode(TopoDS_Shape shape, Node node)
	{
		nodeMap.put(shape, node);
	}
	
	public Node getNode(TopoDS_Shape shape)
	{
		return nodeMap.get(shape);
	}

	public void removeNode(ShapeNode node)
	{
		nodeMap.remove(node);
	}
	
	public void putName(TopoDS_Shape key, String value)
	{
		int id=0;
		String name=value+id;
		while(names.contains(name))
		{
			id++;
			name=value+id;
		}
		shapeToNames.put(key, name);
		names.add(name);
	}

	public void putName(TopoDS_Shape key)
	{
		putName(key, SHAPE_LABEL[key.shapeType()]);
	}

	public String getName(TopoDS_Shape shape)
	{
		return shapeToNames.get(shape);
	}
	
	public final static String[] SHAPE_LABEL=
	{
		"Compound", "CompSolid", "Solid", "Shell", "Face", "Wire", "Edge", "Vertex", "Shape"
	};
}
