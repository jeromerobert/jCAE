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
 * (C) Copyright 2005-2010, by EADS France
 */

package org.jcae.netbeans.mesh;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Enumeration;
import javax.swing.Action;
import org.jcae.mesh.xmldata.Group;
import org.jcae.mesh.xmldata.Groups;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.loaders.InstanceDataObject;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node.Cookie;
import org.openide.nodes.PropertySupport;
import org.openide.util.Exceptions;

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
	
	@Override
	public boolean canRename()
	{
		return true;
	}
	
	@Override
	public final void setName(String arg0)
	{
		super.setName(arg0);
		if(group!=null)
			group.setName(arg0);
	}
	
	@Override
	public Action[] getActions(boolean arg0)
	{
		ArrayList<Action> toReturn = new ArrayList<Action>();
		Enumeration<DataObject> dobjs = DataFolder.findFolder(
			FileUtil.getConfigFile("NodeMenus/org-jcae-netbeans-mesh-GroupNode")).children();
		while(dobjs.hasMoreElements())
		{			
			try {
				InstanceDataObject ido =
					dobjs.nextElement().getLookup().lookup(InstanceDataObject.class);
				if(ido != null && ido.instanceOf(Action.class))
					toReturn.add((Action) ido.instanceCreate());
				else
					toReturn.add(null);				
			} catch (DataObjectNotFoundException ex) {
				Exceptions.printStackTrace(ex);
			} catch (IOException ex) {
				Exceptions.printStackTrace(ex);
			} catch (ClassNotFoundException ex) {
				Exceptions.printStackTrace(ex);
			}
		}
		return toReturn.toArray(new Action[toReturn.size()]);
	}

	public Group getGroup()
	{
		return group;
	}

	public Groups getGroups()
	{
		return groups;
	}
	
	@Override
	public PropertySet[] getPropertySets()
	{
		return new PropertySet[]{
			new PropertySet()
			{
				public Property[] getProperties() {
					return new Property[]{new IDProperty(), new NumberProperty()};
				}
				
				@Override
				public String getName()
				{
					return "Mesh";
				}
			}
		};
	}	
}
