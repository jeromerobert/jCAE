package test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import org.jcae.opencascade.jni.*;

/*
import java.awt.*;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import org.jcae.viewer3d.PositionListener;
import org.jcae.viewer3d.View;
import org.jcae.viewer3d.cad.ViewableCAD;
import org.jcae.viewer3d.cad.occ.OCCProvider;
*/

/**
 * A dirty static class to show what are holes, chamfer and fillet
 * @author Jerome Robert
 */
public class Test6
{
	/** Return the list of face owning the given edge */
	private static TopoDS_Face[] getFace(TopoDS_Shape shape, TopoDS_Edge edge)
	{
		ArrayList toReturn=new ArrayList();
		TopExp_Explorer exp=new TopExp_Explorer(shape, TopAbs_ShapeEnum.FACE);		
		while(exp.more())
		{
			TopoDS_Face face=(TopoDS_Face) exp.current();			
			HashSet edges = new HashSet(Arrays.asList(getEdges(face)));
			if(edges.contains(edge))
				toReturn.add(face);
			exp.next();
		}
		return (TopoDS_Face[]) toReturn.toArray(new TopoDS_Face[toReturn.size()]);		
	}
	
	private static TopoDS_Edge[] getEdges(TopoDS_Shape face)
	{
		HashSet toReturn=new HashSet();
		TopExp_Explorer exp=new TopExp_Explorer(face, TopAbs_ShapeEnum.EDGE);		
		while(exp.more())
		{
			toReturn.add(exp.current());
			exp.next();
		}
		return (TopoDS_Edge[]) toReturn.toArray(new TopoDS_Edge[toReturn.size()]);
	}
	
	private static TopoDS_Shape createCuttedBox()
	{
		TopoDS_Shape box1 = new BRepPrimAPI_MakeBox(
			new double[3], new double[]{4, 3, 2}).shape();

		TopoDS_Shape box2 = new BRepPrimAPI_MakeBox(
			new double[]{-1,-1,-1}, new double[]{3, 2, 1}).shape();
		
		return new BRepAlgoAPI_Cut(box1, box2).shape();		
	}
	
	private static TopoDS_Shape createVerticalCylinder(double radius, double x, double y, double height)
	{
		BRepPrimAPI_MakeCylinder cyl1=new BRepPrimAPI_MakeCylinder(
		new double[]{x,y,2,0,0,-1}, radius, height, Math.PI*2);	
		return cyl1.shape();
	}
	
	private static TopoDS_Shape makeHole(TopoDS_Shape shape)
	{
		TopoDS_Shape h1 = createVerticalCylinder(0.05, 0.5, 1.5, 2);
		TopoDS_Shape h2 = createVerticalCylinder(0.1, 2.5, 1.5, 0.5);
		TopoDS_Shape h3 = createVerticalCylinder(0.07, 3.5, 1.5, 1.9);
		TopoDS_Shape toReturn = new BRepAlgoAPI_Cut(shape, h1).shape();
		toReturn = new BRepAlgoAPI_Cut(toReturn, h2).shape();		
		toReturn = new BRepAlgoAPI_Cut(toReturn, h3).shape();		
		return toReturn;
	}
	
	public static void main(String[] args)
	{
		TopoDS_Shape cuttedBox=createCuttedBox();
		BRepFilletAPI_MakeFillet fillet = new BRepFilletAPI_MakeFillet(cuttedBox);
		TopoDS_Edge[] edges = getEdges(cuttedBox);
		for(int i=0; i<edges.length; i++)
		{
			fillet.add(0.1, edges[i]);
		}		
		
		BRepFilletAPI_MakeChamfer chamfer = new BRepFilletAPI_MakeChamfer(cuttedBox);
		for(int i=0; i<edges.length; i++)
		{
			chamfer.add(0.1, edges[i], getFace(cuttedBox, edges[i])[0]);
		}

		//displayAll(cuttedBox, fillet.shape(), chamfer.shape(), makeHole(cuttedBox));
	}

	/*private static View view1, view2, view3, view4;
	
	private static View createView(TopoDS_Shape shape, Window w)
	{
		final View cadView=new View(w);							
		ViewableCAD fcad=new ViewableCAD(new OCCProvider(shape));
		cadView.add(fcad);
		cadView.fitAll();
		cadView.addPositionListener(new PositionListener(){

			public void positionChanged()
			{
				view1.move(cadView.where());
				view2.move(cadView.where());
				view3.move(cadView.where());
				view4.move(cadView.where());
			}});
		
		return cadView;
	}
		
	private static void displayAll(TopoDS_Shape s1, TopoDS_Shape s2, TopoDS_Shape s3, TopoDS_Shape s4)
	{
		JFrame cadFrame=new JFrame();			
		cadFrame.setSize(800,600);
		cadFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);		
		Container p = cadFrame.getContentPane();
		p.setLayout(new GridLayout(2,2));
		
		view1=createView(s1, cadFrame);
		view2=createView(s2, cadFrame);
		view3=createView(s3, cadFrame);
		view4=createView(s4, cadFrame);
		p.add(view1);
		p.add(view2);
		p.add(view3);
		p.add(view4);		
		cadFrame.setVisible(true);
	}*/
}
