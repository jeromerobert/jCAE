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
 * (C) Copyright 2008,2009, by EADS France
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
import org.jcae.opencascade.jni.TopoDS_Compound;
import org.jcae.opencascade.jni.TopoDS_Face;
import org.jcae.opencascade.jni.TopoDS_Iterator;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.jcae.opencascade.jni.TopoDS_Vertex;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * An abstraction level over TopoDS_Shape to easily decorate and serialize the
 * Opencascade shape graph.
 *
 * This class use the <a href="http://en.wikipedia.org/wiki/Curiously_Recurring_Template_Pattern">curiously
 * recurring template pattern</a> to enforce type safety; static polymorphism is
 * checked when compiling, there is no cast at run time.  In short, this recursive
 * definition <code>class Shape&lt;T extends Shape&lt;T>></code> means that derived
 * classes must be declared as <code>class Foo extends Shape&lt;Foo></code>,
 * one cannot declare <code>class Foo extends Shape&lt;Bar></code>.
 *
 * <p>
 * In this graph, we want parent and children nodes to be of the same class as
 * the current node.
 * Moreover, this class is a general framework and is meant to be subclassed,
 * so parent and children must be arrays of <code>T</code>.  Likewise, the map
 * used in Shape constructor must be of type <code>Map&lt;TopoDS_Shape, T></code>.
 * In order to build a node with the right type, it is obvious that a factory
 * is needed.  This factory is parameterized, and the parameter is the derived
 * class of the node being created.  Under some circumstances, we sometimes
 * need to downcast the current instance.  This could be achieved by a method like
 * </p>
 * <pre>
 *   public T downcast()
 *   {
 *       return (T) this;
 *   }
 * </pre>
 *
 * But there is a better alternative, declare
 * <pre>
 *   protected abstract T getDerived();
 * </pre>
 * in Shape, and let all derived classes declare
 * <pre>
 *   protected T getDerived()
 *   {
 *       return this;
 *   }
 * </pre>
 * @author Jerome Robert
 */
public abstract class Shape<T extends Shape<T>> implements Comparable<T>
{
	private static final Logger LOGGER = Logger.getLogger(Shape.class.getCanonicalName());

	private static enum ListOfShapes {
		COMPOUND (TopAbs_ShapeEnum.COMPOUND,  "Compound",  "co"),
		COMPSOLID(TopAbs_ShapeEnum.COMPSOLID, "CompSolid", "cs"),
		SOLID    (TopAbs_ShapeEnum.SOLID,     "Solid",     "so"),
		SHELL    (TopAbs_ShapeEnum.SHELL,     "Shell",     "sh"),
		FACE     (TopAbs_ShapeEnum.FACE,      "Face",      "f"),
		WIRE     (TopAbs_ShapeEnum.WIRE,      "Wire",      "w"),
		EDGE     (TopAbs_ShapeEnum.EDGE,      "Edge",      "e"),
		VERTEX   (TopAbs_ShapeEnum.VERTEX,    "Vertex",    "v"),
		SHAPE    (TopAbs_ShapeEnum.SHAPE,     "Shape",     "v");

		TopAbs_ShapeEnum type;
		String label;
		String xmlName;
		private ListOfShapes(TopAbs_ShapeEnum type, String label, String xmlName)
		{
			this.type = type;
			this.label = label;
			this.xmlName = xmlName;
		}
	};
	protected static GeomAPI_ProjectPointOnSurf projectPointOnSurf;
	/** map TopoDS_Compound.class to "co" */
	private final static Map<TopAbs_ShapeEnum, String> TYPE_MAP_XML;
	/** map "co" to TopoDS_Compound.class */
	private final static Map<String, TopAbs_ShapeEnum> TYPE_MAP_XML_INV;
	/** map TopoDS_Compound.class to "Compound" */
	private final static Map<TopAbs_ShapeEnum, String> TYPE_MAP_NAME;
	private final static TopAbs_ShapeEnum[] TYPE;
	private final static String[] TYPE_LABEL;
	
	static
	{
		HashMap<TopAbs_ShapeEnum, String> m = new HashMap<TopAbs_ShapeEnum, String>();
		HashMap<TopAbs_ShapeEnum, String> mm = new HashMap<TopAbs_ShapeEnum, String>();
		HashMap<String, TopAbs_ShapeEnum> mi = new HashMap<String, TopAbs_ShapeEnum>();
		ListOfShapes[] shapes = ListOfShapes.values();
		TYPE = new TopAbs_ShapeEnum[shapes.length];
		TYPE_LABEL = new String[shapes.length];
		int i = 0;
		for(ListOfShapes s : shapes)
		{
			if (s != ListOfShapes.SHAPE)
			{
				m.put(s.type, s.xmlName);
				mi.put(s.xmlName, s.type);
			}
			mm.put(s.type, s.label);
			TYPE[i] = s.type;
			TYPE_LABEL[i] = s.label;
			i++;
		}
		TYPE_MAP_XML=Collections.unmodifiableMap(m);
		TYPE_MAP_NAME=Collections.unmodifiableMap(mm);
		TYPE_MAP_XML_INV=Collections.unmodifiableMap(mi);
	}
	
	public interface Attributes
	{
		String toXML();
		void fromXML(Element node);
	}
	
	public interface Factory<T>
	{
		T create(TopoDS_Shape shape, Map<TopoDS_Shape, T> map, T[] parents);
		T[] createArray(int length);
	}
	
	protected TopoDS_Shape impl;
	private T[] children;
	private T[] parents;

	protected Shape(TopoDS_Shape shape, Map<TopoDS_Shape, T> map, T[] parents)
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
		List<T> cs = new ArrayList<T>();
		TopoDS_Iterator it = new TopoDS_Iterator(impl);
		while (it.more())
		{
			TopoDS_Shape tds = it.value();
			T css = map.get(tds);
			if (css == null)
			{
				T[] pArray = getFactory().createArray(1);
				pArray[0] = getDerived();
				css = getFactory().create(tds, map, pArray);
			}

			cs.add(css);
			it.next();
		}
		children = cs.toArray(getFactory().createArray(cs.size()));
		map.put(shape, getDerived());
	}

	private static TopoDS_Compound createCompound()
	{
		TopoDS_Compound toReturn = new TopoDS_Compound();
		BRep_Builder bb=new BRep_Builder();
		bb.makeCompound(toReturn);	
		return toReturn;
	}

	public static String[] getLabels(TopAbs_ShapeEnum from)
	{
		int startIndex = from.ordinal();
		String[] toReturn = new String[TopAbs_ShapeEnum.values().length - 1 - startIndex];
		System.arraycopy(TYPE_LABEL, startIndex, toReturn, 0, toReturn.length);
		return toReturn;
	}

	protected abstract Factory<T> getFactory();
	protected abstract T getDerived();
	
	public void add(T newShape)
	{
		new BRep_Builder().add(impl, newShape.impl);
		T[] nc = getFactory().createArray(children.length + 1);
		System.arraycopy(children, 0, nc, 0, children.length);
		nc[nc.length - 1] = newShape;
		children = nc;
		newShape.addParent(getDerived());
	}
	
	/**
	 * Add a Vertex to this shape
	 * @return the created vertex
	 */
	public T addVertex(double[] coords)
	{
		TopoDS_Vertex v = (TopoDS_Vertex) new BRepBuilderAPI_MakeVertex(
			coords).shape();
		T vs = getFactory().create(v, new HashMap<TopoDS_Shape, T>(), getFactory().createArray(0));
		add(vs);
		if(impl instanceof TopoDS_Face)
		{
			TopoDS_Face face = (TopoDS_Face) impl;
			//Project p5 on surface
			double [] uvTol = new double[3];
			projectPoint(coords, uvTol);
			new BRep_Builder().updateVertex(v, uvTol[0], uvTol[1], face, uvTol[2]);
		}		
		return vs;
	}

	/**
	 * Project a point on surface.
	 * result[2] will contains Double.POSITIVE_INFINITY if no projection are
	 * found.
	 * @param result {u, v, distance, x, y, z}
	 * @throws ClassCastException if this shape is not a surface
	 */
	public void projectPoint(double[] coords, double[] result)
	{
		TopoDS_Face face = (TopoDS_Face) impl;
		Geom_Surface surface = BRep_Tool.surface(face);
		if(projectPointOnSurf == null)
			projectPointOnSurf = new GeomAPI_ProjectPointOnSurf(coords, surface);
		else
			projectPointOnSurf.init(coords, surface);
		if(projectPointOnSurf.nbPoints()>0)
		{
			projectPointOnSurf.lowerDistanceParameters(result);
			double[] p = projectPointOnSurf.nearestPoint();
			result[2] = projectPointOnSurf.lowerDistance();
			System.arraycopy(p, 0, result, 3, 3);
		}
		else
			result[2] = Double.POSITIVE_INFINITY;
	}
	
	/** Remove this shape from its parents */
	public void remove()
	{
		BRep_Builder bb = new BRep_Builder();
		T shape = getDerived();
		for(T parent : parents)
		{
			ArrayList<T> set = new ArrayList<T>(Arrays.asList(parent.children));
			if(set.contains(shape))
			{						
				bb.remove(parent.impl, impl);
				set.remove(shape);
				parent.children = set.toArray(getFactory().createArray(set.size()));
			}
		}
		parents = getFactory().createArray(0);
	}
	
	public void reverse()
	{
		T[] parentSav = parents.clone();
		remove();
		impl.reverse();
		for(T s : parentSav)
			s.add(getDerived());
	}
		
	//TODO remplace this by a "sew" method which keep track of attributes. See
	// aseris-hf GUI for an example of that.
	public T sewed(double tolerance, boolean option, boolean cutting, boolean manifold)
	{
		BRepBuilderAPI_Sewing sewer=new BRepBuilderAPI_Sewing();
		sewer.init(tolerance, option, cutting, manifold);
		sewer.add(impl);
		sewer.perform();
		return getFactory().create(sewer.sewedShape(), new HashMap<TopoDS_Shape, T>(), getFactory().createArray(0));
	}
	
	private void addParent(T parent)
	{
		T[] n = getFactory().createArray(parents.length+1);
		System.arraycopy(parents, 0, n, 0, parents.length);
		n[parents.length]=parent;
		parents = n;
	}
	
	public void dump(PrintWriter writer)
	{
		int[] ids = new int[TopAbs_ShapeEnum.values().length - 1];
		Arrays.fill(ids, 1);
		writer.println("<geometry>");
		dump(writer, new HashSet<T>(), ids);
		writer.println("</geometry>");
	}

	private void dump(PrintWriter writer, Set<T> shapeSet, int[] id)
	{
		T shape = getDerived();
		if (!shapeSet.contains(shape))
		{
			int type = getType().ordinal();
			shapeSet.add(shape);
			if(getAttributes()!=null)
			{
				String e = TYPE_MAP_XML.get(impl.shapeType());
				writer.println("<" + e + " id=\"" + id[type] + "\">");
				writer.println(getAttributes().toXML());
				writer.println("</"+e+">");
			}
			id[type]++;
			for (T s : children)
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
				TopAbs_ShapeEnum type = TYPE_MAP_XML_INV.get(n.getNodeName());
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
	public int getID(T rootShape)
	{
		int[] ids = new int[]{0};
		if (getID(rootShape, new HashSet<T>(), ids, impl.shapeType()))
			return ids[0];
		else
			return -1;
	}
	
	public int getID()
	{
		T r = getRootShape();
		int id = getID(r);
		if(id<0)
			throw new NoSuchElementException("Cannot find " + impl + " in " + r);
		return id;
	}	
	
	private boolean getID(T rootShape, Set<T> shapeSet, int[] number,
		TopAbs_ShapeEnum wantedType)
	{	
		if (!shapeSet.contains(rootShape))
		{
			shapeSet.add(rootShape);
			
			//check if the root shape have the good type
			if (rootShape.impl.shapeType().equals(wantedType))
			{
				number[0]++;	
				if(this.equals(rootShape))
					return true;
				//A compound can include another compound
				else if(rootShape.impl instanceof TopoDS_Compound)
				{
					//So we don't give up but iterate on children
					for (T s : rootShape.children)
						//only on TopoDS_Compound children
						if (s.impl instanceof TopoDS_Compound &&
							getID(s, shapeSet, number, wantedType))
							return true;					
				}
			}
			else
			//look for this shape in children
				for (T s : rootShape.children)
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
	private T explore(Collection<T> result, TopAbs_ShapeEnum wantedType, int maxsize,
		TopoDS_Shape shape)
	{
		if(impl.shapeType().equals(wantedType))
		{
			result.add(getDerived());
			if(result.size()>=maxsize)
				return getDerived();
			if(impl.equals(shape))
				return getDerived();
		}
		
		for(T s : children)
		{
			T toReturn = s.explore(result, wantedType, maxsize, shape);
			if(toReturn != null)
				return toReturn;
		}
		
		return null;
	}
	
	/**
	 * Return the children of this shape whose type is type
	 */
	public Collection<T> explore(TopAbs_ShapeEnum type)
	{
		ArrayList<T> toReturn = new ArrayList<T>();
		explore(toReturn, type, Integer.MAX_VALUE, null);
		return toReturn;
	}
	
	public T getRootShape()
	{
		T aparent = getDerived();
		while(aparent.parents.length!=0)
			aparent = aparent.parents[0];
		return aparent;
	}
	
	/**
	 * @param id from 1 to n
	 * @param type shape type
	 * @return the shape of the given type with this id
	 */
	public T getShapeFromID(int id, TopAbs_ShapeEnum type)
	{
		if(id<1)
			throw new IllegalArgumentException("Shape ID must be greater than 1");

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
			@Override
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
		return explore(dummy, shape.shapeType(), Integer.MAX_VALUE, shape);
	}
	
	/**
	 * Return the closest parent shape which is a Compound
	 * @return the closest parent shape which is a Compound
	 */
	public T getCompound()
	{
		if(impl instanceof TopoDS_Compound)
			return getDerived();
		else if(parents.length>0)
			return parents[0].getCompound();
		else
			return null;
	}
	
	public double getTolerance()
	{
		return Utilities.tolerance(impl);
	}

	public TopAbs_ShapeEnum getType()
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

	public String getName()
	{
		return TYPE_MAP_NAME.get(impl.shapeType());
	}

	public void saveImpl(String fileName)
	{
		BRepTools.write(impl, fileName);
	}
	
	public int compareTo(T o)
	{
		int r = getType().compareTo(o.getType());
		if( r == 0 )
			r = getID() - o.getID();

		return r;
	}	
	
	/** For debugging */
	private static void dumpTopExp(TopoDS_Shape shape)
	{
		for (TopAbs_ShapeEnum type : TopAbs_ShapeEnum.values())
		{
			if (type.equals(TopAbs_ShapeEnum.SHAPE))
				continue;
			TopExp_Explorer ex = new TopExp_Explorer(shape, type);
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
			long t1 = System.nanoTime();
			TopoDS_Shape rootShape = Utilities.readFile("/home/jerome/Models/F1.brep");
			long t2 = System.nanoTime();
			LOGGER.info("Time to load brep: " + (t2 - t1) / 1E9);
			System.gc();
			LOGGER.info("Used memory :" +
				(Runtime.getRuntime().totalMemory() -
				Runtime.getRuntime().freeMemory()) / 1E6 + " Mb");			
			t1 = System.nanoTime();
			ShapeImpl rootShapeJ = new ShapeImpl(rootShape, new HashMap<TopoDS_Shape, ShapeImpl>(), new ShapeImpl[0]);
			t2 = System.nanoTime();
			LOGGER.info("Time to create dual graph: " + (t2 - t1) / 1E9);
			System.gc();
			LOGGER.info("Used memory :" +
				(Runtime.getRuntime().totalMemory() -
				Runtime.getRuntime().freeMemory()) / 1E6 + " Mb");
			LOGGER.info(rootShapeJ.toString());
			t1 = System.nanoTime();
			ShapeImpl s = rootShapeJ.getShapeFromID(330, TopAbs_ShapeEnum.EDGE);
			t2 = System.nanoTime();
			ShapeImpl ss = rootShapeJ.getShapeFromImpl(s.impl);
			long t3 = System.nanoTime();
			int id = ss.getID();
			long t4 = System.nanoTime();
			System.out.println("time for getShapeFromID: "+(t2-t1)/1E9);
			System.out.println("time for getShapeFromImpl: "+(t3-t2)/1E9);
			System.out.println("time for getID: "+(t4-t3)/1E9);
		}
		catch (Exception ex)
		{
			LOGGER.log(Level.SEVERE, null, ex);
		}	
	}

	private static final Factory<ShapeImpl> DEFAULT_FACTORY=new Factory<ShapeImpl>()
	{
		public ShapeImpl create(TopoDS_Shape shape, Map<TopoDS_Shape, ShapeImpl> map, ShapeImpl[] parents)
		{
			return new ShapeImpl(shape, map, parents);
		}

		public ShapeImpl[] createArray(int length)
		{
			return new ShapeImpl[length];
		}
	};

	private static class ShapeImpl extends Shape<ShapeImpl>
	{
		public ShapeImpl(TopoDS_Shape shape, Map<TopoDS_Shape, ShapeImpl> map, ShapeImpl[] parents)
		{
			super(shape, map, parents);
		}

		protected Factory<ShapeImpl> getFactory()
		{
			return DEFAULT_FACTORY;
		}

		protected ShapeImpl getDerived()
		{
			return this;
		}
	}
}
