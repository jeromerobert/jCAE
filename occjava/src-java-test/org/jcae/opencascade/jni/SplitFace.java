package org.jcae.opencascade.jni;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.HashSet;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

/*import org.jcae.viewer3d.View;
import org.jcae.viewer3d.cad.ViewableCAD;
import org.jcae.viewer3d.cad.occ.OCCProvider;*/

/**
 * Show how to split faces to make them smaller than a given
 * area. This is useful with Amibe as the size of the largest
 * face is the parameter which limit the size of possible meshes
 * @author Jerome Robert
 */
public class SplitFace
{	
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
	
	@Test public void sample()
	{
		TopoDS_Shape cuttedBox=createCuttedBox();
		BRepFilletAPI_MakeFillet fillet = new BRepFilletAPI_MakeFillet(cuttedBox);
		TopoDS_Edge[] edges = getEdges(cuttedBox);
		for(int i=0; i<edges.length; i++)
		{
			fillet.add(0.1, edges[i]);
		}
		
		ShapeUpgrade_ShapeDivideArea ss=new ShapeUpgrade_ShapeDivideArea(fillet.shape());
		ss.setMaxArea(0.5);
		ss.perform();
		//display(ss.getResult());
	}
	
	/*public static void display(TopoDS_Shape shape)
	{
		JFrame cadFrame=new JFrame("jcae-viewer3d-cad demo");			
		cadFrame.setSize(800,600);
		cadFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		final View cadView=new View(cadFrame);					
		
		ViewableCAD fcad=new ViewableCAD(new OCCProvider(shape));
		cadView.add(fcad);
		cadView.fitAll();
		cadFrame.getContentPane().add(cadView);
		cadFrame.setVisible(true);
		cadView.setOriginAxisVisible(true);
	}*/
}
