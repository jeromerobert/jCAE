package org.jcae.mesh;

import org.jcae.mesh.mesher.ds.*;
import org.jcae.mesh.cad.*;

public class TestMetrics
{
	public static void main(String args[])
	{
		String filename=args[0];
		CADShapeBuilder factory = CADShapeBuilder.factory;
		CADShape shape = factory.newShape(filename);
		CADExplorer expF = factory.newExplorer();
		CADExplorer expW = factory.newExplorer();
		CADWireExplorer wexp = factory.newWireExplorer();
		
		for (expF.init(shape, CADExplorer.FACE); expF.more(); expF.next())
		{
			CADFace face = (CADFace) expF.current();
			CADGeomSurface surface = face.getGeomSurface();
			for (expW.init(face, CADExplorer.WIRE); expW.more(); expW.next())
			{
				double p2[], p3[];
				for (wexp.init((CADWire) expW.current(), face); wexp.more(); wexp.next())
				{
					CADEdge te = wexp.current();
					CADGeomCurve2D c2d = CADShapeBuilder.factory.newCurve2D(te, face);
					double range[] = c2d.getRange();
					p2 = c2d.value((float)range[0]);
					p3 = surface.value(p2[0], p2[1]);
					System.out.println(p2[0]+" "+p2[1]+" -- "+p3[0]+" "+p3[1]+" "+p3[2]);
					p2 = c2d.value((float)range[1]);
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
