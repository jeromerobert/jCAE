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

import java.util.Collections;
import org.jcae.netbeans.Utilities;
import org.jcae.opencascade.jni.BRep_Builder;
import org.jcae.opencascade.jni.TopAbs_ShapeEnum;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.openide.nodes.Node;
import org.openide.util.datatransfer.NewType;

public class PrimitiveNewType extends NewType
{
	protected Node node;
	private Class primitiveClass;
	static private int counter;
	public PrimitiveNewType(Node n, Class primitiveClass)
	{
		this.node=n;
		this.primitiveClass=primitiveClass;
	}
	
	protected static TopoDS_Shape getShape(Node n)
	{
		ShapeCookie sc=n.getCookie(ShapeCookie.class);
		return sc.getShape();
	}
	
	protected void insertPrimitive(TopoDS_Shape newShape, int id)
	{
		new BRep_Builder().add(getShape(node), newShape);		
		ShapePool sp=node.getCookie(ShapePool.class);
		sp.putName(newShape, getName());
		ShapeChildren sc=node.getCookie(ShapeChildren.class);
		sc.addShapes(Collections.singleton(newShape));
		GeomUtils.getParentBrep(node).getDataObject().setModified(true);
	}
	
	public void create()
	{
		try
		{
			Primitive bean = (Primitive) primitiveClass.newInstance();
			if(Utilities.showEditBeanDialog(bean))		
				insertPrimitive(bean.rebuild(), counter++);		
		}
		catch (InstantiationException e)
		{
			org.openide.ErrorManager.getDefault().notify(e);
		}
		catch (IllegalAccessException e)
		{
			org.openide.ErrorManager.getDefault().notify(e);
		}
	}
	
	public static NewType[] getNewType(Node node)
	{
		if(getShape(node).shapeType()<=TopAbs_ShapeEnum.COMPSOLID)
		{
			return new NewType[]{
				new PrimitiveNewType(node, Cone.class),
				new PrimitiveNewType(node, Cube.class),
				new PrimitiveNewType(node, Cylinder.class),
				new PrimitiveNewType(node, Torus.class),
				new PrimitiveNewType(node, Sphere.class),
			};
		}
		else
			return new NewType[0];
	}
	
	public String getName()
	{
		try
		{
			return  ((Primitive) primitiveClass.newInstance()).toString();
		}
		catch (InstantiationException e)
		{
			e.printStackTrace();
			return "error";
		}
		catch (IllegalAccessException e)
		{		
			e.printStackTrace();
			return "error";
		}
	}
}

