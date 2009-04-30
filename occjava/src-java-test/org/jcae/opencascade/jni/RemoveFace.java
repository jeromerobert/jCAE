package org.jcae.opencascade.jni;

import static org.junit.Assert.*;
import org.junit.Test;

import org.jcae.opencascade.Utilities;

/**
 * Remove a face from a cube
 * @author Jerome Robert
 */
public class RemoveFace
{
	@Test public void removeFace()
	{
		double[] p1=new double[]{0, 0, 0};
		double[] p2=new double[]{1, 1, 1};
		BRepPrimAPI_MakeBox makeBox=new BRepPrimAPI_MakeBox(p1, p2);
		TopoDS_Solid cube=(TopoDS_Solid) makeBox.shape();
		
		assertTrue(6 == Utilities.numberOfShape(cube, TopAbs_ShapeEnum.FACE));
		
//		Utilities.dumpTopology(cube);
		TopExp_Explorer exp=new TopExp_Explorer(cube,TopAbs_ShapeEnum.SHELL);
		TopoDS_Shell shell=(TopoDS_Shell) exp.current();
		
		exp=new TopExp_Explorer(cube, TopAbs_ShapeEnum.FACE);
		exp.next();
		TopoDS_Face face=(TopoDS_Face) exp.current();

		assertTrue(Utilities.isShapeInShape(shell, face));
		
		BRep_Builder bb=new BRep_Builder();
		bb.remove(shell, face);
		
		assertTrue(5 == Utilities.numberOfShape(cube, TopAbs_ShapeEnum.FACE));
	}
}
