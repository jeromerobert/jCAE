package test;

import org.jcae.opencascade.jni.*;

/**
 * Remove a face from a cube
 * @author Jerome Robert
 */
public class Test3
{
	public static void main(String[] args)
	{
		double[] p1=new double[]{0, 0, 0};
		double[] p2=new double[]{1, 1, 1};
		BRepPrimAPI_MakeBox makeBox=new BRepPrimAPI_MakeBox(p1, p2);
		TopoDS_Solid cube=(TopoDS_Solid) makeBox.shape();
		
		System.out.println("Number of face before: "+
			Utilities.numberOfShape(cube, TopAbs_ShapeEnum.FACE));		
		
		Utilities.dumpTopology(cube);
		TopExp_Explorer exp=new TopExp_Explorer(cube,TopAbs_ShapeEnum.SHELL);
		exp.next();
		exp.next();
		TopoDS_Shape ss=exp.current();
		System.out.println(ss);
		TopoDS_Shell shell=(TopoDS_Shell)ss;
		
		exp=new TopExp_Explorer(cube, TopAbs_ShapeEnum.FACE);
		exp.next();
		TopoDS_Shell face=(TopoDS_Shell) exp.current();

		if(!Utilities.isShapeInShape(shell, face))
			throw new IllegalStateException();
		
		BRep_Builder bb=new BRep_Builder();
		bb.remove(cube, face);
		
		System.out.println("Number of face after: "+
			Utilities.numberOfShape(cube, TopAbs_ShapeEnum.FACE));		
	}
}
