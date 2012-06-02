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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.jcae.netbeans.Utilities;
import org.jcae.opencascade.jni.*;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.nodes.Node;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;

public class GeomUtils
{	
	/**
	 * Return the shape of a node or null the node do not have a ShapeCookie
	 * @param n
	 * @return
	 */
	static public NbShape getShape(Node n)
	{
		return n.getLookup().lookup(NbShape.class);
	}
	
	/**
	 * Find ShapeCookie nodes which have the same shapes than 'n' and
	 * which are under the node 'where'
	 * @param where
	 * @param n
	 * @return
	 */
	public static Collection<Node> findNode(Node where, Node n)
	{		
		ArrayList<Node> toReturn=new ArrayList<Node>();		
		NbShape ws = getShape(where);
		NbShape nsh = getShape(n);
		if(ws!=null && nsh!=null && ws.equals(nsh))
			toReturn.add(where);
		
		for (Node n1 : where.getChildren().getNodes())
			toReturn.addAll(findNode(n1, n));
		
		return toReturn;
	}
	
	/**
	 * Insert newShape under node
	 * @param newShape The shape to insert
	 * @param newName The name of the shape to insert
	 * @param node The node where it should be inserted
	 */
	public static void insertShape(TopoDS_Shape newShape, String newName, Node node)
	{
		
		NbShape shape = getShape(node);		
		if (shape == null)
		{
			FileObject objDir = node.getLookup().lookup(FileObject.class);
			String name = Utilities.getFreeName(objDir, newName, ".brep");			
			String fn=new File(FileUtil.toFile(objDir),name).getPath();
			BRepTools.write(newShape, fn);
		}
		else
		{
			NbShape s = new NbShape(newShape);
			s.setName(newName);
			shape.getCompound().add(s);
			ShapeChildren sc = node.getCookie(ShapeChildren.class);
			sc.addShapes(Collections.singleton(s));
		}
	}
	
	/**
	 * Return the parent BrepNode
	 * @param node
	 * @return
	 */
	public static BrepNode getParentBrep(Node node)
	{
		BrepNode toReturn = node.getLookup().lookup(BrepNode.class);
		if(toReturn == null && node.getParentNode() != null)
			toReturn = getParentBrep(node.getParentNode());
		return toReturn;
	}
	
	public static Sheet.Set createSheetSet(final Node node)
	{
		Sheet.Set set=new Sheet.Set();
		final NbShape shape = GeomUtils.getShape(node);
		set.put(new PropertySupport.ReadWrite<String> (
			"userTags", String.class, "User tags", "User tags")
		{
			@Override
			public String getValue() throws IllegalAccessException,
				InvocationTargetException
			{
				return shape.getTags();
			}

			@Override
			public void setValue(String val) throws IllegalAccessException,
				IllegalArgumentException, InvocationTargetException
			{
				shape.setTags(val);
				GeomUtils.getParentBrep(node).getDataObject().setModified(true);
			}
		});
		
		set.put(new PropertySupport.ReadOnly<Double>(
			"tolerance", Double.class, "Tolerance", "Tolerance")
			{	
				public Double getValue()
				{
					return shape.getTolerance();
				}
			});			
		set.setName("Geometry");
		return set;
	}	
}
