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
 * (C) Copyright 2008, by EADS France
 */
package org.jcae.netbeans.cad;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.opencascade.Utilities;
import org.jcae.opencascade.jni.BRepTools;
import org.jcae.opencascade.jni.BRep_Builder;
import org.jcae.opencascade.jni.TopAbs_ShapeEnum;
import org.jcae.opencascade.jni.TopExp_Explorer;
import org.jcae.opencascade.jni.TopoDS_CompSolid;
import org.jcae.opencascade.jni.TopoDS_Compound;
import org.jcae.opencascade.jni.TopoDS_Edge;
import org.jcae.opencascade.jni.TopoDS_Face;
import org.jcae.opencascade.jni.TopoDS_Iterator;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.jcae.opencascade.jni.TopoDS_Shell;
import org.jcae.opencascade.jni.TopoDS_Solid;
import org.jcae.opencascade.jni.TopoDS_Vertex;
import org.jcae.opencascade.jni.TopoDS_Wire;

/**
 *
 * @author Jerome Robert
 */
public class Shape
{
	public static class Attribute
	{
		public String userTag;
		public String meshType;
		public String name;
	}
	
	private final static Map<Class, String> TYPE_MAP_XML =
		new HashMap<Class, String>();
	private final static Map<Class, String> TYPE_MAP_NAME =
		new HashMap<Class, String>();

	static
	{
		TYPE_MAP_XML.put(TopoDS_Compound.class, "co");
		TYPE_MAP_XML.put(TopoDS_CompSolid.class, "cs");
		TYPE_MAP_XML.put(TopoDS_Solid.class, "so");
		TYPE_MAP_XML.put(TopoDS_Shell.class, "sh");
		TYPE_MAP_XML.put(TopoDS_Face.class, "f");
		TYPE_MAP_XML.put(TopoDS_Wire.class, "w");
		TYPE_MAP_XML.put(TopoDS_Edge.class, "e");
		TYPE_MAP_XML.put(TopoDS_Vertex.class, "v");
		TYPE_MAP_NAME.put(TopoDS_Compound.class, "Compound");
		TYPE_MAP_NAME.put(TopoDS_CompSolid.class, "CompSolid");
		TYPE_MAP_NAME.put(TopoDS_Solid.class, "Solid");
		TYPE_MAP_NAME.put(TopoDS_Shell.class, "Shell");
		TYPE_MAP_NAME.put(TopoDS_Face.class, "Face");
		TYPE_MAP_NAME.put(TopoDS_Wire.class, "Wire");
		TYPE_MAP_NAME.put(TopoDS_Edge.class, "Edge");
		TYPE_MAP_NAME.put(TopoDS_Vertex.class, "Vertex");		
	}
	
	private TopoDS_Shape impl;
	private Shape[] children;
	private Attribute attribute;
	private Shape parent;
	
	public Shape(String fileName)
	{
		this(Utilities.readFile(fileName));
	}

	public Shape(TopoDS_Shape shape)
	{
		this(shape, new HashMap<TopoDS_Shape, Shape>(), null);
	}

	private Shape(TopoDS_Shape shape, Map<TopoDS_Shape, Shape> map, Shape parent)
	{
		this.impl = shape;
		this.parent = parent;
		List<Shape> cs = new ArrayList<Shape>();
		TopoDS_Iterator it = new TopoDS_Iterator(shape);
		while (it.more())
		{
			TopoDS_Shape tds = it.value();
			Shape css = map.get(tds);
			if (css == null)
				css = new Shape(tds, map, this);

			cs.add(css);
			it.next();
		}
		children = cs.toArray(new Shape[cs.size()]);
		map.put(shape, this);
	}

	public void add(Shape newShape)
	{
		new BRep_Builder().add(impl, newShape.impl);
		Shape[] nc = new Shape[children.length + 1];
		System.arraycopy(children, 0, nc, 0, children.length);
		nc[nc.length - 1] = newShape;
		children = nc;
		newShape.parent = this;
	}
	
	public void dump(PrintWriter writer)
	{
		int[] ids = new int[TopAbs_ShapeEnum.SHAPE];
		Arrays.fill(ids, 1);
		dump(writer, new HashSet<Shape>(), ids);
	}

	private void dump(PrintWriter writer, Set<Shape> shapeSet, int[] id)
	{
		if (!shapeSet.contains(this))
		{
			int type = getType();
			shapeSet.add(this);
			writer.println("<" + TYPE_MAP_XML.get(impl.getClass()) + " id=\"" +
				(id[type]++) + "\">");
			for (Shape s : children)
				s.dump(writer, shapeSet, id);
		}
	}

	public int getID(Shape rootShape)
	{
		int[] ids = new int[]{0};
		if (getID(rootShape, new HashSet<Shape>(), ids, impl.getClass()))
			return ids[0];
		else
			throw new NoSuchElementException("Cannot find " + impl + " in " +
				rootShape.impl);
	}

	/**
	 * @param rootShape the shape in witch we look for this
	 * @param shapeSet keep track of already visited shape
	 * @param number of shape of the same type already visited
	 * @param wantedType optimisation proxy for this.getType()
	 * @return true if we found this
	 */
	private boolean getID(Shape rootShape, Set<Shape> shapeSet, int[] number,
		Class wantedType)
	{	
		if (!shapeSet.contains(rootShape))
		{
			shapeSet.add(rootShape);
			
			//check if the root shape have the good type
			if (rootShape.impl.getClass().equals(wantedType))
			{
				number[0]++;	
				if(this.equals(rootShape))
					return true;
				//A compound can include another compound
				else if(rootShape.impl instanceof TopoDS_Compound)
				{
					//So we don't give up but iterate on children
					for (Shape s : rootShape.children)
						//only on TopoDS_Compound children
						if (s.impl instanceof TopoDS_Compound &&
							getID(s, shapeSet, number, wantedType))
							return true;					
				}
			}
			else
			//look for this shape in children
				for (Shape s : rootShape.children)
					if (getID(s, shapeSet, number, wantedType))
						return true;
		}
		return false;
	}

	public String getName()
	{
		return getName(getRootShape());
	}
	
	private String getName(Shape rootShape)
	{
		if(attribute == null || attribute.name == null)
			return TYPE_MAP_NAME.get(impl.getClass())+getID(rootShape);
		else
			return attribute.name;
	}
	
	public void setName(String name)
	{
		if(attribute == null)
			attribute = new Attribute();
		attribute.name = name;
	}
	
	private Shape getRootShape()
	{
		Shape aparent = this;
		while(aparent.parent!=null)
			aparent = aparent.parent;
		return aparent;
	}
	
	public double getTolerance()
	{
		return Utilities.tolerance(impl);
	}

	public int getType()
	{
		return impl.shapeType();
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		toString("", sb);
		return sb.toString();
	}
	
	private void toString(String spacer, StringBuilder sb)
	{
		sb.append(spacer);
		sb.append(getName());
		for (Shape s : children)
		{
			sb.append('\n');
			sb.append(spacer);
			s.toString(spacer+" ", sb);
		}
	}
	
	public void saveImpl(String fileName)
	{
		BRepTools.write(impl, fileName);
	}
	
	/** For debugging */
	private static void dumpTopExp(TopoDS_Shape shape)
	{
		for (int i = TopAbs_ShapeEnum.COMPOUND; i < TopAbs_ShapeEnum.SHAPE; i++)
		{
			TopExp_Explorer ex = new TopExp_Explorer(shape, i);
			while (ex.more())
			{
				System.out.println(ex.current());
				ex.next();
			}
		}
	}

	/** For debugging */
	public static void main(final String[] args)
	{
		try
		{
			Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
			long t1 = System.nanoTime();
			TopoDS_Shape rootShape = Utilities.readFile("/home/jerome/Models/ecoul2.brep");
			long t2 = System.nanoTime();
			logger.info("Time to load brep: " + (t2 - t1) / 1E9);
			System.gc();
			logger.info("Used memory :" +
				(Runtime.getRuntime().totalMemory() -
				Runtime.getRuntime().freeMemory()) / 1E6 + " Mb");			
			t1 = System.nanoTime();
			Shape rootShapeJ = new Shape(rootShape);
			t2 = System.nanoTime();
			logger.info("Time to create dual graph: " + (t2 - t1) / 1E9);
			System.gc();
			logger.info("Used memory :" +
				(Runtime.getRuntime().totalMemory() -
				Runtime.getRuntime().freeMemory()) / 1E6 + " Mb");
			logger.info(rootShapeJ.toString());
			/*PrintWriter pw = new PrintWriter(System.out);
			rootShapeJ.dump(pw);
			pw.flush();*/
		}
		catch (Exception ex)
		{
			Logger.getLogger(Shape.class.getName()).log(Level.SEVERE, null, ex);
		}	
	}
}
