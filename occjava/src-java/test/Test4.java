package test;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import org.jcae.opencascade.jni.*;
/*import org.jcae.viewer3d.View;
import org.jcae.viewer3d.cad.ViewableCAD;
import org.jcae.viewer3d.cad.occ.OCCProvider;*/

/** Test circles, extrude, BRepTools_Quilt */
public class Test4
{
	/** Easy creation of faces */
	private static TopoDS_Face createFace(TopoDS_Edge e1)
	{
		TopoDS_Wire wirePlate=
			(TopoDS_Wire) new BRepBuilderAPI_MakeWire(e1).shape();
		return (TopoDS_Face) new BRepBuilderAPI_MakeFace(wirePlate, true).shape();
	}

	private static TopoDS_Edge getLastEdge(TopoDS_Shape shape)
	{
		TopExp_Explorer exp=new TopExp_Explorer(shape, TopAbs_ShapeEnum.EDGE);
		TopoDS_Edge lastEdge=null;
		while(exp.more())
		{			
			lastEdge=(TopoDS_Edge) exp.current();
			exp.next();
		}
		return lastEdge;
	}
	
	private static TopoDS_Edge createCircle(double cx, double cy, double cz, double dx, double dy, double dz, double radius)
	{
		GP_Circ circleB=new GP_Circ(new double[]{cx, cy, cz, dx, dy, dz}, radius);
		return (TopoDS_Edge) new BRepBuilderAPI_MakeEdge(circleB).shape();
	}

	private static TopoDS_Face createFace(TopoDS_Wire wire1, TopoDS_Wire wire2)
	{
		TopoDS_Face face=(TopoDS_Face) new BRepBuilderAPI_MakeFace(wire1, true).shape();
		return (TopoDS_Face) new BRepBuilderAPI_MakeFace(face, wire2).shape();		
	}
	
	private static TopoDS_Shell[] createShell(TopoDS_Face[] faces)
	{
		BRepTools_Quilt quil=new BRepTools_Quilt();
		for(int i=0; i<faces.length; i++)
		{
			quil.add(faces[i]);
		}
		TopExp_Explorer exp=new TopExp_Explorer(quil.shells(), TopAbs_ShapeEnum.SHELL);
		ArrayList list=new ArrayList();
		while(exp.more())
		{
			list.add(exp.current());
			exp.next();
		}
		return (TopoDS_Shell[]) list.toArray(new TopoDS_Shell[0]);
	}
	
	private static TopoDS_Face extrude(TopoDS_Shape shape, double vx, double vy, double vz)
	{
		return (TopoDS_Face) new BRepPrimAPI_MakePrism(
			shape, new double[]{vx, vy, vz}).shape();
	}
	
	private static TopoDS_Face[] getFaces(TopoDS_Shape shape)
	{
		TopExp_Explorer exp=new TopExp_Explorer(shape, TopAbs_ShapeEnum.FACE);
		ArrayList list=new ArrayList();
		while(exp.more())
		{
			list.add(exp.current());
			exp.next();
		}
		return (TopoDS_Face[]) list.toArray(new TopoDS_Face[0]);
	}
	
	private static TopoDS_Wire[] getWires(TopoDS_Shape shape)
	{
		TopExp_Explorer exp=new TopExp_Explorer(shape, TopAbs_ShapeEnum.WIRE);
		ArrayList list=new ArrayList();
		while(exp.more())
		{
			System.out.println(exp.current());
			list.add(exp.current());
			exp.next();
		}
		return (TopoDS_Wire[]) list.toArray(new TopoDS_Wire[0]);
	}
	
	private static TopoDS_Wire createWire(TopoDS_Edge edge)
	{
		return (TopoDS_Wire) new BRepBuilderAPI_MakeWire(edge).shape();
	}
	
	public static void main(String[] args)
	{
		//Create a holed cylinder
		TopoDS_Edge circleB=createCircle(0, 0, -1E-3, 0, 0, 1, 4E-3);
		TopoDS_Edge circleSP1=createCircle(0, 0, 0, 0, 0, 1, 1E-3);
		TopoDS_Face cylinderS=extrude(circleSP1, 0, 0, -0.016);		
		TopoDS_Face cylinderBzM=(TopoDS_Face) extrude(circleB, 0, 0, -0.015).reversed();		
		TopoDS_Face cylinderBzP=extrude(circleB, 0, 0, 1E-3);
		
		TopoDS_Face hdSurface=(TopoDS_Face) createFace(
			createWire(getLastEdge(cylinderBzM)),
			createWire(getLastEdge(cylinderS))).reversed();		
		
		//Create a cylinder with a large disc sewed to it's base
		
		TopoDS_Face cylinderDiscP2=createFace(
			createWire(getLastEdge(cylinderBzP)), createWire(circleSP1));
		TopoDS_Face cylinderSzP=extrude(circleSP1, 0, 0, 7.5E-3);
		TopoDS_Face cylinderDiscSzP=createFace(getLastEdge(cylinderSzP));
		
		//Create a box
		TopoDS_Solid box=(TopoDS_Solid) new BRepPrimAPI_MakeBox(new double[]{4.5E-2,0.03,-0.001}, new double[]{-4.5E-2,-0.03,0}).shape();
		//Make a hole in the box
		TopoDS_Face[] boxFaces=getFaces(box);
		TopoDS_Face squareZm=boxFaces[4];
		TopoDS_Face squareZp=boxFaces[5];
		squareZm=createFace(getWires(squareZm)[0], (TopoDS_Wire) createWire(circleB).reversed());
		squareZp=createFace(getWires(squareZp)[0], getWires(cylinderDiscP2)[0]);
		TopoDS_Face[] holedBoxFaces=new TopoDS_Face[]{
			boxFaces[0],
			boxFaces[1],
			boxFaces[2],
			boxFaces[3],
			squareZm,
			squareZp,
			cylinderBzP};		
		TopoDS_Solid holedBox=(TopoDS_Solid) new BRepBuilderAPI_MakeSolid(createShell(holedBoxFaces)[0]).shape();
		
		//Put it all together
		BRep_Builder bb=new BRep_Builder();
		TopoDS_Compound compound=new TopoDS_Compound();
		bb.makeCompound(compound);
		bb.add(compound, holedBox);
		bb.add(compound, cylinderBzM);
		bb.add(compound, cylinderS);
		bb.add(compound, hdSurface);
		bb.add(compound, cylinderDiscP2);
		bb.add(compound, cylinderSzP);
		bb.add(compound, cylinderDiscSzP);
		bb.add(compound, cylinderBzP);		
		
		//save it
		String file="/tmp/toto.brep";
		BRepTools.write(compound, file);

		//display(file);
		//display("/tmp/Solid_1.brep");
	}
	
	/**
	 * Display the geometry (viewer3d required)
	 */
	/*public static void display(String file)
	{
		JFrame cadFrame=new JFrame("jcae-viewer3d-cad demo");			
		cadFrame.setSize(800,600);
		cadFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		final View cadView=new View(cadFrame);					
		
		ViewableCAD fcad=new ViewableCAD(new OCCProvider(file));
		cadView.add(fcad);
		cadView.fitAll();
		cadFrame.getContentPane().add(cadView);
		cadFrame.getContentPane().add(new JButton(new AbstractAction(){

			public void actionPerformed(ActionEvent e)
			{
				cadView.fitAll();
			}}), BorderLayout.NORTH);
		
		cadFrame.setVisible(true);
		cadView.setOriginAxisVisible(true);
	}*/
}
