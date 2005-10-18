package org.jcae.netbeans.cad;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

public class ShapeChildren extends Children.Array implements Node.Cookie
{			
	public void addShapes(Collection shapes)
	{
		Node[] subNodes = getNodes();
		HashSet names=new HashSet();
		for(int i=0; i<subNodes.length; i++)
		{
			names.add(subNodes[i].getName());
		}

		ShapePool sp=(ShapePool) getNode().getCookie(ShapePool.class);
		Iterator it=shapes.iterator();
		while(it.hasNext())
		{
			TopoDS_Shape s=(TopoDS_Shape) it.next();
			String name=sp.getName(s);
			if(!names.contains(name))
				nodes.add(new ShapeNode(name, s, sp));
		}
		refresh();
	}	
}
