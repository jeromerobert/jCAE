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

import java.util.ArrayList;
import java.util.Collection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class GeomMeta
{
	private Document document;
	private String geomName;
	
	public GeomMeta(Document dom, String geomName)
	{
		document=dom;
		this.geomName=geomName;
	}
	
	public void addFaceGroup(int[] ids)
	{
		Element e=getGeomMetaNode();
		Element g=document.createElement("group");
		for(int i=0; i<ids.length; i++)
		{
			Element f=document.createElement("f");
			f.setAttribute("id", ""+ids[i]);
			g.appendChild(f);
		}
		e.appendChild(g);		
	}

	private Element getGeomMetaNode()
	{
		Element jcaeNode=getJCAENode();
		Node e = jcaeNode.getFirstChild();
		while(e!=null && !isGeomMetaNode(e))
			e=e.getNextSibling();
		if(e==null)
		{
			e=document.createElement("geommeta");
			((Element)e).setAttribute("id", geomName);
			jcaeNode.appendChild(e);
		}
		return (Element) e;
	}

	private boolean isGeomMetaNode(Node n)
	{
		if(n.getNodeType()==Node.ELEMENT_NODE)
		{
			Element e=(Element)n;			
			String attr=e.getAttribute("id");
			return "geommeta".equals(e.getTagName()) && geomName.equals(attr);
		}
		return false;
	}

	private Element getJCAENode()
	{
		Element e=document.getDocumentElement();
		if(e==null || !"jcae".equals(e.getTagName()))
		{
			e=document.createElement("jcae");
			document.appendChild(e);
		}
		return e;
	}

	private int[] getFaces(Node n)
	{
		if(n.getNodeType()!=Node.ELEMENT_NODE)
			return null;
		Element e=(Element)n;
		NodeList nl = e.getElementsByTagName("f");
		int[] toReturn=new int[nl.getLength()];
		for(int i=0; i<nl.getLength(); i++)
		{
			toReturn[i]=Integer.parseInt(((Element)nl.item(i)).getAttribute("id"));
		}
		return toReturn;
	}
	
	/** 
	 * @return A Collection of int[]
	 */
	public Collection getFaceGroups()
	{
		ArrayList toReturn=new ArrayList();
		Element g=getGeomMetaNode();
		Node n=g.getFirstChild();
		while(n!=null)
		{
			int[] ids=getFaces(n);
			if(ids!=null)
				toReturn.add(ids);
			n=n.getNextSibling();
		}
		return toReturn;
	}
}
