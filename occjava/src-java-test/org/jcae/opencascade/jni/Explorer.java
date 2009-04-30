package org.jcae.opencascade.jni;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.HashSet;
import org.hamcrest.core.IsInstanceOf;

public class Explorer
{
	@Test public void explorer()
	{
		// Create a cube
		double[] pt1=new double[]{0,0,0};
		double[] pt2=new double[]{1,3,2};
		TopoDS_Shape box = new BRepPrimAPI_MakeBox(pt1, pt2).shape();
		
		// The created shape is a solid.
		assertThat(box, new IsInstanceOf(TopoDS_Solid.class));
		
		// Display the child of our box
		TopoDS_Iterator it=new TopoDS_Iterator(box);
		
		// The first (and only) child is a shell
		TopoDS_Shell shell=(TopoDS_Shell) it.value();
		it.next();
		assertFalse(it.more());
		
		// Children of the shell are faces
		it=new TopoDS_Iterator(shell);
		while(it.more())
		{
			assertThat(it.value(), new IsInstanceOf(TopoDS_Face.class));
			it.next();
		}
		
		// TopExp_Explorer is easier to use if you are looking for shapes
		// of a give type. Let's count the number of edges in the box
		TopExp_Explorer exp=new TopExp_Explorer(box, TopAbs_ShapeEnum.EDGE);
		int counter=0;
		HashSet<TopoDS_Shape> set=new HashSet<TopoDS_Shape>();
		while(exp.more())
		{
			//Just to show that the type is TopoDS_Edge
			assertThat(exp.current(), new IsInstanceOf(TopoDS_Edge.class));
			set.add(exp.current());
			
			counter++;
			exp.next();
		}
		
		// the number of edges is 24 because each edge is shared by 2
		// faces.
		assertTrue(counter == 24);

		// the number of edges is 12
		assertTrue(12 == set.size());
	}
}
