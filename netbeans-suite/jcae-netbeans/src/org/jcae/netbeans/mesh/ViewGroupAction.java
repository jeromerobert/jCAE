package org.jcae.netbeans.mesh;

import java.util.*;
import java.util.Map.Entry;
import org.jcae.netbeans.viewer3d.View3D;
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
	
		View3D v = View3D.getView3D();
		Iterator it=groups2Group.entrySet().iterator();
		while(it.hasNext())
		{
			Map.Entry e=(Entry) it.next();
			((Groups)e.getKey()).displayGroups((Collection) e.getValue(), v);
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
