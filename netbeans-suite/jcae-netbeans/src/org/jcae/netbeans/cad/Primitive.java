package org.jcae.netbeans.cad;

import org.jcae.opencascade.jni.TopoDS_Shape;

/**
 * @author Jerome Robert
 *
 */
public abstract class Primitive
{	
	public abstract TopoDS_Shape rebuild();
	
	private String name;

	public void setName(String name)
	{
		this.name = name;
	}

	public String toString()
	{
		return name;
	}
}
