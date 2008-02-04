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

package org.jcae.netbeans.mesh;

import java.util.*;
import java.util.Map.Entry;
import org.jcae.netbeans.viewer3d.View3D;
import org.jcae.netbeans.viewer3d.View3DManager;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;

public class ViewGroupAction extends CookieAction
{

	protected int mode()
	{
		return CookieAction.MODE_ALL;
	}

	protected Class[] cookieClasses()
	{
		return new Class[]{GroupNode.class};
	}

	protected void performAction(Node[] arg0)
	{
		HashMap groups2Group=new HashMap();
		for(int i=0; i<arg0.length; i++)
		{
			GroupNode gn=(GroupNode) arg0[i].getCookie(GroupNode.class);
			Collection c=(Collection) groups2Group.get(gn.getGroups());
			if(c==null)
			{
				c=new ArrayList();
				groups2Group.put(gn.getGroups(), c);
			}
			c.add(gn.getGroup());
		}
	
		View3D v = View3DManager.getDefault().getView3D();
		Iterator it=groups2Group.entrySet().iterator();
		while(it.hasNext())
		{
			Map.Entry e=(Entry) it.next();
			((Groups)e.getKey()).displayGroups(
				arg0[0].getParentNode().getParentNode().getName(),
				(Collection) e.getValue(), v);
		}
	}

	public String getName()
	{
		return "view";
	}

	public HelpCtx getHelpCtx()
	{
		return null;
	}

	protected boolean asynchronous()
	{
		return false;
	}
}
