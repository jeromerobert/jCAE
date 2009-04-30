package org.jcae.opencascade.jni;

import org.jcae.opencascade.jni.*;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Show how to use brep builder
 * @author Jerome Robert
 */
public class BRepBuilder
{
	@Test public void sample()
	{
		// Create a shape for the example (an half cone)
		double[] axis=new double[]{
			0,0,0, // position
			0,0,1  // direction
		};
		TopoDS_Shape cone=new BRepPrimAPI_MakeCone(axis, 2, 1, 1, Math.PI).shape();
		
		// Now we will remove one face of the shape. We need first to find
		// the direct parent of this face.
		TopoDS_Iterator it=new TopoDS_Iterator(cone);
		
		// The children of solids are shells
		TopoDS_Shell shell = (TopoDS_Shell) it.value();
		
		// Get a handle on the face to remove
		TopExp_Explorer exp=new TopExp_Explorer(cone, TopAbs_ShapeEnum.FACE);
		TopoDS_Face toRemove = (TopoDS_Face) exp.current();
		exp.next();
		TopoDS_Face anotherFace = (TopoDS_Face) exp.current();
		
		// Remove the face
		BRep_Builder bb=new BRep_Builder();
		bb.remove(shell, toRemove);
		
		// Let's revert the face and readd it to the cube
		bb.add(shell, toRemove.reversed());
		
		// We can also create a compound containing only the 2 first faces of
		// the cone
		TopoDS_Compound compound=new TopoDS_Compound();
		bb.makeCompound(compound);
		bb.add(compound, toRemove);
		bb.add(compound, anotherFace);
	}
}
