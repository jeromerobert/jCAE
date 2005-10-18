package org.jcae.netbeans.cad;

import org.jcae.opencascade.jni.TopoDS_Shape;
import org.openide.nodes.Node;

public interface ShapeCookie  extends Node.Cookie
{
	TopoDS_Shape getShape();
}
