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
import java.util.TreeSet;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

public class ShapeChildren extends Children.Array implements Node.Cookie
{			
	private static class SortedNode implements Comparable<SortedNode>
	{
		private final NbShape shape;
		private Node node;

		public SortedNode(Node node)
		{
			this.shape = GeomUtils.getShape(node);
			this.node = node;
		}

		public SortedNode(NbShape shape)
		{
			this.shape = shape;			
		}
		
		public int compareTo(SortedNode o)
		{
			return shape.compareTo(o.shape);
		}
		
		public Node getNode(FileObject fileObject)
		{
			if(node == null)
				node = ShapeNode.create(shape, fileObject);
			return node;
		}
	}

	private final FileObject fileObject;
	public ShapeChildren(FileObject fileObject)
	{
		this.fileObject = fileObject;
	}
	
	public void addShapes(Collection<NbShape> shapes)
	{
		TreeSet<SortedNode> set = new TreeSet<SortedNode>();
		for(Node n:getNodes())
			set.add(new SortedNode(n));

		for(NbShape s:shapes)
			set.add(new SortedNode(s));
				
		nodes.clear();
		for(SortedNode s:set)
			nodes.add(s.getNode(fileObject));
		
		refresh();
	}	
}
