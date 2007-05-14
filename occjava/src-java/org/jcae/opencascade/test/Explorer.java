package org.jcae.opencascade.test;

import java.util.HashSet;
import org.jcae.opencascade.jni.*;

public class Explorer
{
	public static void main(String[] args)
	{
		// Create a cube
		double[] pt1=new double[]{0,0,0};
		double[] pt2=new double[]{1,3,2};
		TopoDS_Shape box = new BRepPrimAPI_MakeBox(pt1, pt2).shape();
		
		// The created shape is a solid. We can check it
		// by casting
		TopoDS_Solid solid = (TopoDS_Solid) box;
		
		// Display the child of our box
		TopoDS_Iterator it=new TopoDS_Iterator(solid);
		
		// The first (and only) child is a shell
		TopoDS_Shell shell=(TopoDS_Shell) it.value();
		
		// Children of the shell are faces
		it=new TopoDS_Iterator(shell);
		while(it.more())
		{
			// There should be only one shell (check by casting)
			System.out.println((TopoDS_Face)it.value());
			it.next();
		}
		
		// TopExp_Explorer is easier to use if you are looking for shapes
		// of a give type. Let's count the number of edges in the box
		TopExp_Explorer exp=new TopExp_Explorer(box, TopAbs_ShapeEnum.EDGE);
		int counter=0;
		while(exp.more())
		{
			//Just to show that the type is TopoDS_Edge
			TopoDS_Edge e=(TopoDS_Edge)exp.current();
			
			counter++;
			exp.next();
		}
		
		// the number of edges is 24 because each edge is shared by 2
		// faces.
		System.out.println("number of edges in a box: "+counter);
		
		// We can find the number of unique edges by using a Set
		exp=new TopExp_Explorer(box, TopAbs_ShapeEnum.EDGE);
		HashSet set=new HashSet();
		while(exp.more())
		{
			set.add(exp.current());
			exp.next();
		}
		// the number of edges is 12
		System.out.println("number of edges in a box: "+set.size());				
	}
}
