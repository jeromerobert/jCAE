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
import java.util.Arrays;
import org.jcae.opencascade.jni.BRep_Builder;
import org.jcae.opencascade.jni.TopExp_Explorer;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;

public class ReverseAction extends CookieAction implements Node.Cookie
{
	private static Class[] COOKIE_CLASSES=new Class[]{ShapeCookie.class};
	
	protected int mode()
	{
		return CookieAction.MODE_ONE;
	}

	protected Class[] cookieClasses()
	{
		return COOKIE_CLASSES;
	}

	protected void performAction(Node[] arg0)
	{
		for(int i=0; i<arg0.length; i++)
		{
			TopoDS_Shape toRev = GeomUtils.getShape(arg0[i]);
			TopoDS_Shape parent = GeomUtils.getShape(arg0[i].getParentNode());
			parent=findParent(parent, toRev);			
			BRep_Builder bb = new BRep_Builder();
			bb.remove(parent, toRev);
			toRev.reverse();
			bb.add(parent, toRev);
			GeomUtils.getParentBrep(arg0[i]).getDataObject().setModified(true);
		}
	}

	protected TopoDS_Shape findParent(TopoDS_Shape where, TopoDS_Shape toRev)
	{
		if(where.shapeType()==(toRev.shapeType()-1))
		{			
			TopExp_Explorer exp=new TopExp_Explorer(where, toRev.shapeType());
			for(; exp.more(); exp.next())
			{
				if(exp.current().equals(toRev))
					return where;
			}
			return null;
		}
		else
		{
			TopExp_Explorer exp=new TopExp_Explorer(where, toRev.shapeType()-1);
			for(; exp.more(); exp.next())
			{
				TopoDS_Shape ssh = findParent(exp.current(), toRev);
				if(ssh!=null)
					return ssh;				
			}
			return null;
		}
	}
	
	public String getName()
	{
		return "Reverse";
	}

	public HelpCtx getHelpCtx()
	{
		return null;
	}
	
	protected boolean asynchronous()
	{
		return false;
	}
	
	protected String iconResource()
	{
		return "org/jcae/netbeans/cad/reverse.gif";
	}
}
