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

import org.openide.filesystems.FileObject;
import org.openide.loaders.DataNode;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;

public class MetaNode extends DataNode implements EventListener
{
	public MetaNode(BrepDataObject mob, FileObject object)
	{
		super(mob, new Children.Array());
		refresh();
	}

	public String getDisplayName()
	{
		return "groups";
	}

	public void handleEvent(Event evt)
	{
		System.out.println(evt);
	}

	public void refresh()
	{
		Children c = getChildren();
		c.remove(c.getNodes());
		BrepDataObject dob=(BrepDataObject) getDataObject();
		GeomMeta gm=new GeomMeta(dob.getMetaDocument(), dob.getPrimaryFile().getNameExt());
		Object[] groups=gm.getFaceGroups().toArray();
		Node[] toAdd=new Node[groups.length];
		for(int i=0; i<groups.length; i++)
		{
			int[] ids=(int[]) groups[i];
			toAdd[i]=new AbstractNode(Children.LEAF);
			toAdd[i].setDisplayName(idsToString(ids));
		}
		c.add(toAdd);
	}	
	
	private static String idsToString(int[] ids)
	{
		String toReturn="";
		for(int i=0; i<ids.length-1; i++)
		{			
			toReturn=toReturn+ids[i]+", ";
		}
		
		toReturn+=toReturn+ids[ids.length-1];
		return toReturn;
	}
}
