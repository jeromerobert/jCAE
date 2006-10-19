package test;

import java.io.PrintStream;
import java.util.Arrays;
import org.jcae.opencascade.jni.TopExp_Explorer;
import org.jcae.opencascade.jni.TopoDS_Iterator;
import org.jcae.opencascade.jni.TopoDS_Shape;

public class Utilities
{
	private static final int TAB=2;
	
	public static void dumpTopology(TopoDS_Shape shape)
	{
		dumpTopology(shape, System.out);
	}
	
	public static void dumpTopology(TopoDS_Shape shape, PrintStream out)
	{
		dumpTopology(shape, out, 0);
	}

	private static void dumpTopology(TopoDS_Shape shape, PrintStream out, int level)
	{		
		TopoDS_Iterator it=new TopoDS_Iterator(shape);
		char[] dots=new char[level*TAB];
		Arrays.fill(dots, '-');
		String dotss=new String(dots);
		String label=shape.toString().substring("org.jcae.opencascade.jni.TopoDS_".length());
		System.out.println("+"+dotss+label);
		while(it.more())
		{
			dumpTopology(it.value(), out, level+1);
			it.next();
		}
	}
	
	public static int numberOfShape(TopoDS_Shape shape, int type)
	{
		TopExp_Explorer exp=new TopExp_Explorer(shape, type);
		int n=0;
		while(exp.more())
		{
			n++;
			exp.next();
		}
		return n;
	}
	
	public static boolean isShapeInShape(TopoDS_Shape parent, TopoDS_Shape child)
	{
		TopExp_Explorer exp=new TopExp_Explorer(parent, child.shapeType());
		int n=0;
		while(exp.more())
		{
			if(exp.current().equals(child))
				return true;
			exp.next();
		}
		return false;		
	}
}
