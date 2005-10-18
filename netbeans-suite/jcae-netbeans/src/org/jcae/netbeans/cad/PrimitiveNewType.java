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
		ShapeCookie sc=(ShapeCookie) n.getCookie(ShapeCookie.class);
		return sc.getShape();
	}
	
	protected void insertPrimitive(TopoDS_Shape newShape, int id)
	{
		new BRep_Builder().add(getShape(node), newShape);		
		ShapePool sp=(ShapePool) node.getCookie(ShapePool.class);
		sp.putName(newShape, getName());
		ShapeChildren sc=(ShapeChildren) node.getCookie(ShapeChildren.class);
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

