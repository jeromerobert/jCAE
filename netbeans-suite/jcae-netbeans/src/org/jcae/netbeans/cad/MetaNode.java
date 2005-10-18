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
