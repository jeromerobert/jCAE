package org.jcae.netbeans.mesh;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import javax.swing.Action;
import org.jcae.netbeans.viewer3d.View3D;
import org.openide.actions.PropertiesAction;
import org.openide.actions.RenameAction;
import org.openide.actions.ViewAction;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node.Cookie;
import org.openide.nodes.PropertySupport;
import org.openide.util.actions.SystemAction;

public class GroupNode extends AbstractNode implements Cookie
{
	private class IDProperty extends PropertySupport.ReadOnly
	{
		public IDProperty()
		{
			super("ID", Integer.class, "group ID", "group ID");
		}

		public Object getValue() throws IllegalAccessException, InvocationTargetException {
			return new Integer(group.getId());
		}
	}

	private class NumberProperty extends PropertySupport.ReadOnly
	{
		public NumberProperty()
		{
			super("Number", Integer.class, "Number of elements", "Number of elements");
		}

		public Object getValue() throws IllegalAccessException, InvocationTargetException {
			return new Integer(group.getNumberOfElements());
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
	
	public Action[] getActions(boolean arg0)
	{
		return new Action[]
		{
			SystemAction.get(RenameAction.class),
			SystemAction.get(ViewGroupAction.class),
			SystemAction.get(ExportGroupAction.class),
			SystemAction.get(FuseGroupAction.class),
			SystemAction.get(PropertiesAction.class)
		};
	}

	public void view()
	{
		groups.displayGroups(Collections.singleton(group), View3D.getView3D());
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
