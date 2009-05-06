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

import java.util.HashMap;
import java.util.Map;
import org.jcae.opencascade.Shape;
import org.jcae.opencascade.Utilities;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.openide.nodes.Node;
import org.openide.nodes.Node.Cookie;

/**
 * A Shape which is easy to use in netbeans
 * @author Jerome Robert
 */
public class NbShape extends Shape<NbShape> implements Cookie
{
	public static class NbAttributes implements Attributes
	{
		public String userTag="";
		public String meshType;
		public String name="";

		public String toXML()
		{
			return "<tag>"+userTag+"</tag>\n"+
				"<name>"+name+"</name>";
		}

		public void fromXML(org.w3c.dom.Element element)
		{
			userTag = element.getElementsByTagName("tag").item(0).getTextContent();
			name = element.getElementsByTagName("name").item(0).getTextContent();
		}
	}
	
	private final static Factory<NbShape> FACTORY=new Factory<NbShape>()
	{
		public NbShape create(TopoDS_Shape shape,
			Map<TopoDS_Shape, Shape> map, Shape[] parents)
		{
			return new NbShape(shape, map, parents, this);
		}
	};

	private Node node;
	private NbAttributes attributes;
	
	public NbShape(String fileName)
	{
		this(Utilities.readFile(fileName));
	}

	public NbShape(TopoDS_Shape shape)
	{
		this(shape, new HashMap<TopoDS_Shape, Shape>(),
			NOPARENT, FACTORY);
	}

	@Override
	protected Factory<NbShape> getFactory()
	{
		return FACTORY;
	}
		
	protected NbShape(TopoDS_Shape shape, Map<TopoDS_Shape, Shape> map,
		Shape[] parents, Factory<NbShape> f)
	{
		super(shape, map, parents);
	}
	
	public void setNode(Node node)
	{
		this.node = node;	
	}
	
	public Node getNode()
	{
		return node;
	}
	
	/**
	 * 
	 * @return
	 * @deprecated  Use it with care. If you modify it the owning Shape object
	 * won't be aware of the change.
	 */
	@Deprecated
	public TopoDS_Shape getImpl()
	{
		return impl;
	}
	
	public String getName()
	{
		if(attributes == null || attributes.name.length()==0)
			return TYPE_MAP_NAME.get(impl.getClass())+getID();
		else
			return attributes.name+getID();
	}
	
	public void setName(String name)
	{
		if(attributes == null)
			attributes = new NbAttributes();
		attributes.name = name;
	}
	
	public String getTags()
	{
		if( attributes != null && attributes.userTag != null)
			return attributes.userTag;
		else return "";
	}
	
	public void setTags(String tags)
	{
		if(attributes == null)
			attributes = new NbAttributes();
		attributes.userTag = tags;
	}
	
	@Override
	protected NbAttributes getAttributes()
	{
		return attributes;
	}

	@Override
	protected void createAttributes()
	{
		attributes = new NbAttributes();
	}
}
