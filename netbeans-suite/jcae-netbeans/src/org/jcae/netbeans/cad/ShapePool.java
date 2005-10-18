package org.jcae.netbeans.cad;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.openide.nodes.Node;

/** map shape to face nodes */
public class ShapePool implements Node.Cookie
{
	private Map nodeMap=new WeakHashMap();
	private Set names=new HashSet();
	private Map shapeToNames=new WeakHashMap();
	
	public void putNode(TopoDS_Shape shape, Node node)
	{
		nodeMap.put(shape, node);
	}
	
	public Node getNode(TopoDS_Shape shape)
	{
		return (Node) nodeMap.get(shape);
	}

	public void removeNode(ShapeNode node)
	{
		nodeMap.remove(node);
	}
	
	public void putName(TopoDS_Shape key, String value)
	{
		int id=0;
		String name=value.toString()+id;
		while(names.contains(name))
		{
			id++;
			name=value.toString()+id;
		}
		shapeToNames.put(key, name);
		names.add(name);
	}

	public void putName(TopoDS_Shape key)
	{
		putName(key, SHAPE_LABEL[key.shapeType()]);
	}

	public String getName(TopoDS_Shape shape)
	{
		return (String) shapeToNames.get(shape);
	}
	
	public final static String[] SHAPE_LABEL=
	{
		"Compound", "CompSolid", "Solid", "Shell", "Face", "Wire", "Edge", "Vertex", "Shape"
	};
}
