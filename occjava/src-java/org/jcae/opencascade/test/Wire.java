package org.jcae.opencascade.test;

import org.jcae.opencascade.Utilities;
import org.jcae.opencascade.jni.*;

/**
 * Create a wire from 2 edges
 * @author Jerome Robert
 */
public class Wire
{
	public static void main(String[] args)
	{
		// The plate
		double[] p1=new double[]{0, 0, 0};
		double[] p2=new double[]{0, 1, 0};
		double[] p3=new double[]{1, 1, 0};
		double[] p4=new double[]{1, 0, 0};
		double[] p5=new double[]{0.5, 0.5, 0};
		double[] p6=new double[]{0.5, 0.5, 1};
		
		TopoDS_Edge edge1=(TopoDS_Edge) new BRepBuilderAPI_MakeEdge(p1,p2).shape();
		TopoDS_Edge edge2=(TopoDS_Edge) new BRepBuilderAPI_MakeEdge(p2,p3).shape();
		TopoDS_Edge edge3=(TopoDS_Edge) new BRepBuilderAPI_MakeEdge(p3,p4).shape();
		TopoDS_Edge edge4=(TopoDS_Edge) new BRepBuilderAPI_MakeEdge(p4,p1).shape();
		TopoDS_Edge freeEdge=(TopoDS_Edge) new BRepBuilderAPI_MakeEdge(p5,p6).shape();
		
		BRepBuilderAPI_MakeWire bb=new BRepBuilderAPI_MakeWire();
		bb.add(new TopoDS_Shape[]{edge1, edge2, edge3, freeEdge});
		TopoDS_Shape result=bb.shape();
		System.out.println(BRepAlgo.isValid(result));
		Utilities.dumpTopology(result, System.out);
	}
}
