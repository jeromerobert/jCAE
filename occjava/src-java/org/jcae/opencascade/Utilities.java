package org.jcae.opencascade;

import java.io.PrintStream;
import java.util.Arrays;
import org.jcae.opencascade.jni.*;

/**
 * Useful toolbox.
 * Note that this methods are not Open CASCADE binding and should probably
 * not be used outside of the jCAE project, as compatibility between versions
 * won't be garanty.
 */
public class Utilities
{
	private static final int TAB=2;
	
	/** Dump the topology of a shape (for debugging) */
	public static void dumpTopology(TopoDS_Shape shape)
	{
		dumpTopology(shape, System.out);
	}
	
	/** Dump the topology of a shape (for debugging) */
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
		System.out.print("+"+dotss+label);
		switch(shape.shapeType())
		{
			case TopAbs_ShapeEnum.FACE:
				out.print(" "+BRep_Tool.tolerance((TopoDS_Face)shape));
				break;
			case TopAbs_ShapeEnum.EDGE:
				out.print(" "+BRep_Tool.tolerance((TopoDS_Edge)shape));
				break;
			case TopAbs_ShapeEnum.VERTEX:
				out.print(" "+BRep_Tool.tolerance((TopoDS_Vertex)shape));
				break;
			default:
		}
		out.println();
		while(it.more())
		{
			dumpTopology(it.value(), out, level+1);
			it.next();
		}
	}
	
	/** Return the number of shapes in one shape */
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
	
	/** Test if a shape is part of another one  */
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
	
	/**
	 * Read a file guessing the format with the file extension
	 * Only .step, .igs and .brep are supported.
	 */
	public static TopoDS_Shape readFile(String fileName)
	{
        TopoDS_Shape brepShape;
        if (fileName.endsWith(".step"))
        {
            STEPControl_Reader aReader = new STEPControl_Reader();
            aReader.readFile(fileName);
            aReader.nbRootsForTransfer();
            aReader.transferRoots();
            brepShape = aReader.oneShape();
        }
        else if (fileName.endsWith(".igs"))
        {
            IGESControl_Reader aReader = new IGESControl_Reader();
            aReader.readFile(fileName);
            aReader.nbRootsForTransfer();
            aReader.transferRoots();
            brepShape = aReader.oneShape();
        }
        else
            brepShape = BRepTools.read(fileName, new BRep_Builder());
        return brepShape;
	}
	
	/**
	 * Return the face whose order is id in the given shape
	 */
	public static TopoDS_Face getFace(TopoDS_Shape shape, int id)
	{
		TopExp_Explorer exp=new TopExp_Explorer(shape, TopAbs_ShapeEnum.FACE);		
		int i=0;
		while(exp.more())
		{
			if(id==i)
				return (TopoDS_Face) exp.current();
			exp.next();
			i++;
		}
		throw new IndexOutOfBoundsException("Face "+id+" not found");		
	}

	/**
	 * Return the vertex whose order is id in the given shape
	 */
	public static TopoDS_Vertex getVertex(TopoDS_Shape shape, int id)
	{
		TopExp_Explorer exp=new TopExp_Explorer(shape, TopAbs_ShapeEnum.VERTEX);		
		int i=0;
		while(exp.more())
		{
			if(id==i)
				return (TopoDS_Vertex) exp.current();
			exp.next();
			i++;
		}
		throw new IndexOutOfBoundsException("Vertex "+id+" not found");		
	}
}
