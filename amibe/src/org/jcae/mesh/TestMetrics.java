package org.jcae.mesh;

import org.jcae.mesh.mesher.ds.*;
import org.jcae.mesh.cad.*;
import org.jcae.mesh.cad.occ.OCCEdge;
import org.jcae.mesh.cad.occ.OCCFace;
import org.jcae.opencascade.jni.*;

public class TestMetrics
{
	public static void main(String args[])
	{
		String filename=args[0];
		CADShapeBuilder factory = CADShapeBuilder.factory;
		CADShape shape = factory.newShape(filename);
		CADExplorer expF = factory.newExplorer();
		CADExplorer expW = factory.newExplorer();
		CADExplorer expE = factory.newExplorer();
		
		for (expF.init(shape, CADExplorer.FACE); expF.more(); expF.next())
		{
			OCCFace face = (OCCFace) expF.current();
			TopoDS_Face fface = (TopoDS_Face) face.getShape();
			CADGeomSurface surface = face.getGeomSurface();
			for (expW.init(face, CADExplorer.WIRE); expW.more(); expW.next())
			{
				TopoDS_Wire W = (TopoDS_Wire) ((OCCEdge)expW.current()).getShape();
				BRepTools_WireExplorer wexp = new BRepTools_WireExplorer();
				double p2[], p3[];
				for (wexp.init(W, fface); wexp.more(); wexp.next())
				{
					TopoDS_Edge E = wexp.current();
					double range[] = new double[2];
					Geom2d_Curve curve = BRep_Tool.curveOnSurface(E, fface, range);
					Geom2dAdaptor_Curve C2d = new Geom2dAdaptor_Curve(curve);
					p2 = C2d.value((float)range[0]);
					p3 = surface.value(p2[0], p2[1]);
					System.out.println(p2[0]+" "+p2[1]+" -- "+p3[0]+" "+p3[1]+" "+p3[2]);
					p2 = C2d.value((float)range[1]);
					p3 = surface.value(p2[0], p2[1]);
					System.out.println(p2[0]+" "+p2[1]+" -- "+p3[0]+" "+p3[1]+" "+p3[2]);
				}
				MNode2D n2_1 = new MNode2D(1.0, 1.414213562373095);
				MNode2D n2_2 = new MNode2D(1.2, 1.414213562373095);
				MNode3D n3_1 = new MNode3D(n2_1, surface);
				MNode3D n3_2 = new MNode3D(n2_2, surface);
				System.out.println("Edge length: "+n3_1.distance(n3_2));
				SubMesh2D s = new SubMesh2D(face);
				s.pushCompGeom(3);
				System.out.println("Metrics: "+s.compGeom().distance(n2_1, n2_2));
			}
		}
	}

}
