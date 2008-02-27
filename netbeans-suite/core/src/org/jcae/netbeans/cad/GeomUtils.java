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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import org.jcae.netbeans.Utilities;
import org.jcae.opencascade.jni.*;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.nodes.Node.Cookie;

public class GeomUtils
{
	/**
	 * Return the direct parent shape
	 * @param A root shape where to start the search
	 */
	static public TopoDS_Shape getParentShape(TopoDS_Shape where, TopoDS_Shape child)
	{		
		TopoDS_Iterator it=new TopoDS_Iterator(where);
		while(it.more())
		{
			TopoDS_Shape v=it.value();
			if(v.equals(child))
				return where;
			v=getParentShape(v, child);
			if(v!=null)
				return v;
			it.next();
		}
		return null;
	}
	
	/**
	 * Return the shape of a node or null the node do not have a ShapeCookie
	 * @param n
	 * @return
	 */
	static public TopoDS_Shape getShape(Node n)
	{
		TopoDS_Shape toReturn = null;
		if(n != null)
		{
			BrepDataObject b = n.getLookup().lookup(BrepDataObject.class);
			if(b != null)
			{
				if(b.isLoaded())
					toReturn = b.getShape();
			}
			else
			{
				ShapeCookie sc = n.getLookup().lookup(ShapeCookie.class);
				if(sc != null)
					toReturn = sc.getShape();
			}
		}
		return toReturn;
	}
	
	/**
	 * Find ShapeCookie nodes which have the same shapes than 'n' and
	 * which are under the node 'where'
	 */
	static public Collection<Node> findNode(Node where, Node n)
	{		
		ArrayList<Node> toReturn=new ArrayList<Node>();
		
		if(where.getClass().getName().equals("org.netbeans.modules.favorites.Favorites"))
		{
			return toReturn;
		}
		
		TopoDS_Shape ws = getShape(where);
		TopoDS_Shape nsh = getShape(n);
		if(ws!=null && nsh!=null && ws.equals(nsh))
			toReturn.add(where);
		
		Node[] ns=where.getChildren().getNodes();
		for(int i=0; i<ns.length; i++)
		{
			toReturn.addAll(findNode(ns[i], n));
		}
		
		return toReturn;
	}
	
	/**
	 * Return the closest parent node which is a Compound
	 * @param n
	 * @return
	 */
	static private Node getCompound(Node n)
	{
		if(n==null)
			return null;
		
		TopoDS_Shape s=getShape(n);
		if(s==null)
			return null;
		else if(s.shapeType()==TopAbs_ShapeEnum.COMPOUND)
			return n;
		else
			return getCompound(n.getParentNode());
	}
	
	/** Insert newShape under node */
	static public void insertShape(TopoDS_Shape newShape, String newName, Node node)
	{
		TopoDS_Shape shape = getShape(getCompound(node));

		if (shape != null)
		{
			new BRep_Builder().add(shape, newShape);
			ShapePool sp = node.getCookie(ShapePool.class);
			sp.putName(newShape, newName);
			ShapeChildren sc = node.getCookie(ShapeChildren.class);
			sc.addShapes(Collections.singleton(newShape));
		} else
		{
			FileObject objDir = Utilities.getProject(node).getProjectDirectory();
			String name = Utilities.getFreeName(objDir, newName,
				".brep");			
			String fn=new File(FileUtil.toFile(objDir),name).getPath();
			BRepTools.write(newShape, fn);
		}
	}
	
	/**
	 * Return the parent BrepNode
	 */
	static public BrepNode getParentBrep(Node node)
	{
		//System.out.println("getParentBrep: "+node);
		Cookie toReturn = node.getCookie(BrepNode.class);		
		if(! (toReturn instanceof BrepNode))
		{
			if(toReturn instanceof ShapeNode)
				node=(Node) toReturn;
			Node parent=node.getParentNode();
			if(parent!=null)
				toReturn=getParentBrep(parent);			
		}
		
		//System.out.println("getParentBrep return: "+toReturn);
		return (BrepNode) toReturn;
	}
	
	/**
	 * Return the IDs of each shape node in the parent shape. i.e first face have ID
	 * Support only face nodes.
	 * @param arg0 an array of ShapeCookie nodes under the same BrepNode
	 */
	static public int[] nodeToIDs(Node[] arg0)
	{
		int[] toReturn=new int[arg0.length];
		if(arg0.length==0)
			return toReturn;
		TopoDS_Shape rootShape=getShape(getParentBrep(arg0[0]));
		HashMap<TopoDS_Shape, Integer> map=new HashMap<TopoDS_Shape, Integer>();
		for(int i=0; i<arg0.length; i++)
		{
			map.put(getShape(arg0[i]), -1);
		}
		
		TopExp_Explorer exp=new TopExp_Explorer(rootShape, TopAbs_ShapeEnum.FACE);
		for(int k=0; exp.more(); exp.next(), k++)
		{
			TopoDS_Shape s = exp.current();
			Integer id=map.get(s);
			if(id!=null && id.intValue()==-1)
				map.put(s, k);
		}
		
		for(int i=0; i<arg0.length; i++)
		{
			toReturn[i]=map.get(getShape(arg0[i]));
		}
		
		return toReturn;
	}

}
