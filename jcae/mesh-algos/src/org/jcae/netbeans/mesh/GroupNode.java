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

import java.lang.reflect.InvocationTargetException;
import javax.swing.Action;
import org.jcae.mesh.xmldata.Group;
import org.jcae.mesh.xmldata.Groups;
import org.openide.actions.PropertiesAction;
import org.openide.actions.RenameAction;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node.Cookie;
import org.openide.nodes.PropertySupport;
import org.openide.util.actions.SystemAction;

public class GroupNode extends AbstractNode implements Cookie
{
	private class IDProperty extends PropertySupport.ReadOnly<String>
	{
		public IDProperty()
		{
			super("ID", String.class, "group ID", "group ID");
		}

		public String getValue() throws IllegalAccessException, InvocationTargetException {
			return group.getName();
		}
	}

	private class NumberProperty extends PropertySupport.ReadOnly<Integer>
	{
		public NumberProperty()
		{
			super("Number", Integer.class, "Number of elements", "Number of elements");
		}

		public Integer getValue() throws IllegalAccessException, InvocationTargetException {
			return group.getNumberOfElements();
		}
	}
	
	
	private Group group;
	private Groups groups;

	public GroupNode(Group group, Groups groups)
	{
		super(Children.LEAF);
		setName(group.getName());
		this.group=group;
		this.groups=groups;
		getCookieSet().add(this);
	}
	
	public boolean canRename()
	{
		return true;
	}
	
	public void setName(String arg0)
	{
		super.setName(arg0);
		if(group!=null)
			group.setName(arg0);
	}
	
	@Override
	public Action[] getActions(boolean arg0)
	{
		return new Action[]
		{
			SystemAction.get(RefineAction.class),
			SystemAction.get(SmoothAction.class),
			SystemAction.get(DecimateAction.class),
			SystemAction.get(SwapAction.class),
			null,
			SystemAction.get(RenameAction.class),
			SystemAction.get(ViewGroupAction.class),
			SystemAction.get(HideGroupAction.class),
			SystemAction.get(RefreshGroupAction.class),
			SystemAction.get(ExportGroupAction.class),
			SystemAction.get(FuseGroupAction.class),
			SystemAction.get(PropertiesAction.class)
		};
	}

	public Group getGroup()
	{
		return group;
	}

	public Groups getGroups()
	{
		return groups;
	}
	
	public PropertySet[] getPropertySets()
	{
		return new PropertySet[]{
			new PropertySet()
			{
				public Property[] getProperties() {
					return new Property[]{new IDProperty(), new NumberProperty()};
				}
				
				public String getName()
				{
					return "Mesh";
				}
			}
		};
	}	
}
