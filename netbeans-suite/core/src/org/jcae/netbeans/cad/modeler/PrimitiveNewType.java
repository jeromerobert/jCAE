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
 * (C) Copyright 2009, by EADS France
 */

package org.jcae.netbeans.cad.modeler;

import org.jcae.netbeans.cad.*;
import java.util.Collections;
import java.util.List;
import org.jcae.netbeans.Utilities;
import org.jcae.opencascade.jni.TopAbs_ShapeEnum;
import org.openide.nodes.Node;
import org.openide.util.datatransfer.NewType;

public class PrimitiveNewType extends NewType
{
	protected final Node node;
	private final Class primitiveClass;
	
	public PrimitiveNewType(Node n, Class primitiveClass)
	{
		this.node=n;
		this.primitiveClass=primitiveClass;
	}
	
	protected void insertPrimitive(NbShape newShape)
	{
		GeomUtils.getShape(node).add(newShape);
		newShape.setName(getName());		
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
				insertPrimitive(new NbShape(bean.rebuild()));
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
	
	public static void getNewType(Node node, List<NewType> list)
	{
		if(GeomUtils.getShape(node).getType().compareTo(TopAbs_ShapeEnum.COMPSOLID) <= 0)
		{
			list.add(new PrimitiveNewType(node, Cone.class));
			list.add(new PrimitiveNewType(node, Cube.class));
			list.add(new PrimitiveNewType(node, Cylinder.class));
			list.add(new PrimitiveNewType(node, Torus.class));
			list.add(new PrimitiveNewType(node, Sphere.class));
		}
	}
	
	@Override
	public String getName()
	{
		try
		{
			return  primitiveClass.newInstance().toString();
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

