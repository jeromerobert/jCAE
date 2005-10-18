package org.jcae.netbeans.mesh;

import java.util.ArrayList;
import java.util.Collection;
import org.openide.nodes.Children.Array;

public class GroupChildren extends Array {

	private Groups groups;

	public GroupChildren(Groups groups)
	{
		this.groups=groups;
	}

	protected Collection initCollection()
	{
		Group[] gps=groups.getGroups();
		ArrayList toReturn=new ArrayList(gps.length);
		for(int i=0; i<gps.length; i++)
		{
			toReturn.add(new GroupNode(gps[i], groups));
		}
		return toReturn;
	}
}
