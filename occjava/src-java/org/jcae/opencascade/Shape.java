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
package org.jcae.opencascade;

import java.io.PrintWriter;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jcae.opencascade.jni.BRepBndLib;
import org.jcae.opencascade.jni.BRepBuilderAPI_MakeVertex;
import org.jcae.opencascade.jni.BRepBuilderAPI_Sewing;
import org.jcae.opencascade.jni.BRepTools;
import org.jcae.opencascade.jni.BRep_Builder;
import org.jcae.opencascade.jni.BRep_Tool;
import org.jcae.opencascade.jni.Bnd_Box;
import org.jcae.opencascade.jni.GeomAPI_ProjectPointOnSurf;
import org.jcae.opencascade.jni.Geom_Surface;
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
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A abstraction level over TopoDS_Shape to easily decorate and serialize the
 * Opencascade shape graph.
 * @author Jerome Robert
 */
public class Shape<T extends Shape> implements Comparable< Shape<T> >
{	
	/** map TopoDS_Compound.class to "co" */
	protected final static Map<Class, String> TYPE_MAP_XML;
	/** map "co" to TopoDS_Compound.class */
	protected final static Map<String, Class> TYPE_MAP_XML_INV;
	/** map TopoDS_Compound.class to "Compound" */
	protected final static Map<Class, String> TYPE_MAP_NAME;
	protected final static Class[] TYPE= new Class[]{
		TopoDS_Compound.class,
		TopoDS_CompSolid.class,
		TopoDS_Solid.class,
		TopoDS_Shell.class,
		TopoDS_Face.class,
		TopoDS_Wire.class,
		TopoDS_Edge.class,
		TopoDS_Vertex.class,
		TopoDS_Shape.class};

	public final static String[] TYPE_LABEL= new String[]{
		"Compound",
		"CompSolid",
		"Solid",
		"Shell",
		"Face",
		"Wire",
		"Edge",
		"Vertex",
		"Shape"};
	
	static
	{
		HashMap<Class, String> m = new HashMap<Class, String>();
		m.put(TopoDS_Compound.class, "co");
		m.put(TopoDS_CompSolid.class, "cs");
		m.put(TopoDS_Solid.class, "so");
		m.put(TopoDS_Shell.class, "sh");
		m.put(TopoDS_Face.class, "f");
		m.put(TopoDS_Wire.class, "w");
		m.put(TopoDS_Edge.class, "e");
		m.put(TopoDS_Vertex.class, "v");
		TYPE_MAP_XML=Collections.unmodifiableMap(m);
		HashMap<Class, String> mm = new HashMap<Class, String>();
		for(int i=0; i<TYPE.length; i++)
			mm.put(TYPE[i], TYPE_LABEL[i]);
		TYPE_MAP_NAME=Collections.unmodifiableMap(mm);
		
		HashMap<String, Class> mmi = new HashMap<String, Class>();
		for(Entry<Class, String> e:TYPE_MAP_XML.entrySet())
			mmi.put(e.getValue(), e.getKey());
		TYPE_MAP_XML_INV=Collections.unmodifiableMap(mmi);
	}
	
	protected interface Attributes
	{
		String toXML();
		void fromXML(Element node);
	}
	
	protected interface Factory<T>
	{
		T create(TopoDS_Shape shape, Map<TopoDS_Shape, Shape> map, Shape[] parents);
	}
	
	protected final static Factory DEFAULT_FACTORY=new Factory<Shape>()
	{
		public Shape create(TopoDS_Shape shape, Map<TopoDS_Shape, Shape> map, Shape[] parents)
		{
			return new Shape(shape, map, parents);
		}
	};
	
	protected TopoDS_Shape impl;
	private Shape[] children;
	private Shape[] parents;

	protected final static Shape[] NOPARENT=new Shape[0];

	public Shape(String fileName)
	{
		this(Utilities.readFile(fileName));
	}

	public Shape(TopoDS_Shape shape)
	{
		this(shape, new HashMap<TopoDS_Shape, Shape>(), NOPARENT);
	}
	
	protected Shape(TopoDS_Shape shape, Map<TopoDS_Shape, Shape> map,
		Shape[] parents)
	{
		if(shape == null)
		{
			TopoDS_Compound c =new TopoDS_Compound();
			new BRep_Builder().makeCompound(c);
			this.impl = c;
		}
		else
			this.impl = shape;
		this.parents = parents;
		List<Shape> cs = new ArrayList<Shape>();
		TopoDS_Iterator it = new TopoDS_Iterator(impl);
		while (it.more())
		{
			TopoDS_Shape tds = it.value();
			Shape css = map.get(tds);
			if (css == null)
				css = getFactory().create(tds, map, new Shape[]{this});

			cs.add(css);
			it.next();
		}
		children = cs.toArray(new Shape[cs.size()]);
		map.put(shape, this);
	}

	protected Factory<T> getFactory()
	{
		return DEFAULT_FACTORY;
	}
	
	public void add(Shape newShape)
	{
		new BRep_Builder().add(impl, newShape.impl);
		Shape[] nc = new Shape[children.length + 1];
		System.arraycopy(children, 0, nc, 0, children.length);
		nc[nc.length - 1] = newShape;
		children = nc;		
		newShape.addParent(this);
	}
	
	/**
	 * Add a Vertex to this shape
	 * @return the created vertex
	 */
	public T addVertex(double x, double y, double z)
	{
		double[] coords = new double[]{x, y, z};
		TopoDS_Vertex v = (TopoDS_Vertex) new BRepBuilderAPI_MakeVertex(
			coords).shape();
		T vs = getFactory().create(v, new HashMap<TopoDS_Shape, Shape>(), NOPARENT);
		add(vs);
		if(impl instanceof TopoDS_Face)
		{
			TopoDS_Face face = (TopoDS_Face) impl;
			//Project p5 on surface
			double [] uv = new double[2];
			Geom_Surface surface = BRep_Tool.surface(face);
			GeomAPI_ProjectPointOnSurf proj = new GeomAPI_ProjectPointOnSurf(coords, surface);
			proj.lowerDistanceParameters(uv);
			double tolerance = proj.lowerDistance();
			new BRep_Builder().updateVertex(v, uv[0], uv[1], face, tolerance);
		}
		
		return vs;
	}
	
	/** Remove this shape from its parents */
	public void remove()
	{
		BRep_Builder bb = new BRep_Builder();
		for(Shape parent:parents)
		{
			ArrayList<Shape> set = new ArrayList<Shape>(Arrays.asList(parent.children));
			if(set.contains(this))
			{						
				bb.remove(parent.impl, impl);
				set.remove(this);
				parent.children = set.toArray(new Shape[set.size()]);
			}
		}
		parents=NOPARENT;
	}
	
	public void reverse()
	{
		Shape[] parentSav = parents.clone();
		remove();
		impl.reverse();
		for(Shape s: parentSav)
			s.add(this);
	}
		
	//TODO remplace this by a "sew" method which keep track of attributes. See
	// aseris-hf GUI for an example of that.
	public T sewed(double tolerance, boolean option, boolean cutting, boolean manifold)
	{
		BRepBuilderAPI_Sewing sewer=new BRepBuilderAPI_Sewing();
		sewer.init(tolerance, option, cutting, manifold);
		sewer.add(impl);
		sewer.perform();
		return getFactory().create(sewer.sewedShape(), new HashMap(), NOPARENT);
	}
	
	private void addParent(Shape parent)
	{
		Shape[] n = new Shape[parents.length+1];
		System.arraycopy(parents, 0, n, 0, parents.length);
		n[parents.length]=parent;
		parents = n;
	}
	
	public void dump(PrintWriter writer)
	{
		int[] ids = new int[TopAbs_ShapeEnum.SHAPE];
		Arrays.fill(ids, 1);
		writer.println("<geometry>");
		dump(writer, new HashSet<Shape>(), ids);
		writer.println("</geometry>");
	}

	private void dump(PrintWriter writer, Set<Shape> shapeSet, int[] id)
	{
		if (!shapeSet.contains(this))
		{
			int type = getType();
			shapeSet.add(this);
			if(getAttributes()!=null)
			{
				String e = TYPE_MAP_XML.get(impl.getClass());
				writer.println("<" + e + " id=\"" + id[type] + "\">");
				writer.println(getAttributes().toXML());
				writer.println("</"+e+">");
			}
			id[type]++;
			for (Shape s : children)
				s.dump(writer, shapeSet, id);
		}
	}

	public void load(Node node)
	{
		NodeList nodes = node.getChildNodes();
		for(int i = 0; i<nodes.getLength(); i++)
		{
			Node n = nodes.item(i);
			if(n.getNodeType() == Node.ELEMENT_NODE)
			{
				Class type = TYPE_MAP_XML_INV.get(n.getNodeName());
				Element e = (Element)n;
				int id = Integer.parseInt(e.getAttribute("id"));
				Shape s = getShapeFromID(id, type);
				if(s.getAttributes()==null)
					s.createAttributes();
				s.getAttributes().fromXML(e);
			}			
		}
	}
	
	/**
	 * Return the ID of this shape, concidering it's in a given root shape
	 * @return the ID of this shape or -1 if this shape is not a child of
	 * rootShape
	 */
	public int getID(Shape rootShape)
	{
		int[] ids = new int[]{0};
		if (getID(rootShape, new HashSet<Shape>(), ids, impl.getClass()))
			return ids[0];
		else
			return -1;
	}
	
	public int getID()
	{
		Shape r = getRootShape();
		int id = getID(r);
		if(id<0)
			throw new NoSuchElementException("Cannot find " + impl + " in " + r);
		return id;
	}	
	
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
	
	/**
	 * @param result will contains the found shapes. Use a HashSet to get unique
	 * shapes (getFromID) and ArrayList to get doublon (explore)
	 * @param wantedType the type of shape to return
	 * @param maxsize the maximum number of returned shapes
	 * @param shape exploration will end when this shape will be found
	 */
	private T explore(Collection<T> result, Class wantedType, int maxsize,
		TopoDS_Shape shape)
	{
		if(impl.getClass().equals(wantedType))
		{
			result.add((T)this);
			if(result.size()>=maxsize)
				return (T)this;
			if(impl.equals(shape))
				return (T)this;
		}
		
		for(Shape s:children)
		{
			T toReturn = (T) s.explore(result, wantedType, maxsize, shape);
			if(toReturn != null)
				return toReturn;
		}
		
		return null;
	}
	
	/**
	 * Return the children of this shape whose type is type
	 */
	public Collection<T> explore(int type)
	{
		ArrayList<T> toReturn = new ArrayList<T>();
		explore(toReturn, TYPE[type], Integer.MAX_VALUE, null);
		return toReturn;
	}
	
	public T getRootShape()
	{
		Shape aparent = this;
		while(aparent.parents.length!=0)
			aparent = aparent.parents[0];
		return (T)aparent;
	}
	
	/**
	 * @param id from 1 to n
	 * @param type
	 * @return
	 */
	public T getShapeFromID(int id, int type)
	{
		return getShapeFromID(id, TYPE[type]);
	}
	
	/**
	 * @param id from 1 to n
	 * @param type
	 * @return
	 */
	private T getShapeFromID(int id, Class type)
	{		
		Collection<T> result = new HashSet<T>();
		T toReturn = explore(result, type, id, null);
		if(toReturn == null)
		{
			throw new ArrayIndexOutOfBoundsException("This shape contains only "
				+result.size()+" elements of type "+type);
		}
		return toReturn;
	}
	
	public T getShapeFromImpl(TopoDS_Shape shape)
	{
		Collection<T> dummy = new AbstractList<T>()
		{
			public void add(int index, T element) {}			
			@Override
			public T get(int index)
			{
				throw new UnsupportedOperationException();
			}

			@Override
			public int size()
			{
				return 0;
			}
		};
		return explore(dummy, shape.getClass(), Integer.MAX_VALUE, shape);
	}
	
	/**
	 * Return the closest parent shape which is a Compound
	 * @param n
	 * @return
	 */
	public T getCompound()
	{
		if(impl instanceof TopoDS_Compound)
			return (T)this;
		else if(parents.length>0)
			return (T)parents[0].getCompound();
		else
			return null;
	}
	
	public double getTolerance()
	{
		return Utilities.tolerance(impl);
	}

	public int getType()
	{
		return impl.shapeType();
	}

	/**return {xmin, ymin, zmin, xmax, ymax, zmax} */ 
	public double[] getBounds()
	{
		Bnd_Box box = new Bnd_Box(); 			
		BRepBndLib.add(impl,box);			
		return box.get();
	}
	
	/**
	 * return the attributes of this nodes.
	 * Can be null if it as no attributes (default values)
	 */
	protected Attributes getAttributes()
	{
		return null;
	}

	/**
	 * Initialise the attributes. Ones called getAttributes will not return
	 * null.
	 */
	protected void createAttributes()
	{
	}

	public void saveImpl(String fileName)
	{
		BRepTools.write(impl, fileName);
	}
	
	public int compareTo(Shape<T> o)
	{
		int r = getType()-o.getType();
		if( r == 0 )
			r = getID() - o.getID();
		return r;
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
			TopoDS_Shape rootShape = Utilities.readFile("/home/jerome/Models/F1.brep");
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
			t1 = System.nanoTime();
			Shape s = rootShapeJ.getShapeFromID(330, TopAbs_ShapeEnum.EDGE);
			t2 = System.nanoTime();
			Shape ss = rootShapeJ.getShapeFromImpl(s.impl);
			long t3 = System.nanoTime();
			int id = ss.getID();
			long t4 = System.nanoTime();
			System.out.println("time for getShapeFromID: "+(t2-t1)/1E9);
			System.out.println("time for getShapeFromImpl: "+(t3-t2)/1E9);
			System.out.println("time for getID: "+(t4-t3)/1E9);
		}
		catch (Exception ex)
		{
			Logger.getLogger(Shape.class.getName()).log(Level.SEVERE, null, ex);
		}	
	}
}
