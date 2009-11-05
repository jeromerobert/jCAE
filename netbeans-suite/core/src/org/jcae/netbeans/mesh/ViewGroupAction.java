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
 * (C) Copyright 2005-2009, by EADS France
 */

package org.jcae.netbeans.mesh;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import javax.xml.parsers.ParserConfigurationException;
import org.jcae.mesh.xmldata.Group;
import org.jcae.mesh.xmldata.Groups;
import org.jcae.netbeans.viewer3d.ViewManager;
import org.jcae.vtk.View;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;
import org.xml.sax.SAXException;

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
		try
		{
			HashMap<Groups, Collection<Group>> groups2Group =
				new HashMap<Groups, Collection<Group>>();
			for(int i=0; i<arg0.length; i++)
			{
				GroupNode gn=arg0[i].getCookie(GroupNode.class);
				Collection<Group> c=groups2Group.get(gn.getGroups());
				if(c==null)
				{
					c=new ArrayList<Group>();
					groups2Group.put(gn.getGroups(), c);
				}
				c.add(gn.getGroup());
			}

			View v = ViewManager.getDefault().getCurrentView();
			Iterator<Entry<Groups, Collection<Group>>> it =
				groups2Group.entrySet().iterator();

			while(it.hasNext())
			{
				Entry<Groups, Collection<Group>> e=it.next();
				AmibeNode.displayGroups(e.getKey(),
					arg0[0].getParentNode().getParentNode().getName(),
					e.getValue(), v, arg0[0].getParentNode().getParentNode().getParentNode());
			}
		}
		catch(IOException ex)
		{
			Exceptions.printStackTrace(ex);
		}
		catch(ParserConfigurationException ex)
		{
			Exceptions.printStackTrace(ex);
		}
		catch(SAXException ex)
		{
			Exceptions.printStackTrace(ex);
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

	@Override
	protected boolean asynchronous()
	{
		return false;
	}
}
