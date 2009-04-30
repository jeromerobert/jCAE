package org.jcae.opencascade.jni;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Example of BRepGProp and BRepBndLib
 * @author Jerome Robert
 */
public class Dimensions
{
	@Test public void torus()
	{
		// Create a shape for the example (a torus)
		double[] axis=new double[]{
			0,0,0,  // position
			0,0,1}; // direction
		double R = 3.0;
		double r = 1.0;
		TopoDS_Shape torus=new BRepPrimAPI_MakeTorus(axis, R, r).shape();

		// Compute the bounding box of the shape
		Bnd_Box bbox = new Bnd_Box(); 			
		BRepBndLib.add(torus, bbox);			
		double[] bboxValues = bbox.get();
		
		// Display the bounding box
		System.out.println("Xmin="+bboxValues[0]);
		System.out.println("Ymin="+bboxValues[1]);
		System.out.println("Zmin="+bboxValues[2]);
		System.out.println("Xmax="+bboxValues[3]);
		System.out.println("Ymax="+bboxValues[4]);
		System.out.println("Zmax="+bboxValues[5]);

		// Display various other properties
		GProp_GProps property=new GProp_GProps();
		BRepGProp.linearProperties(torus, property);
		System.out.println("length="+property.mass());
		assertEquals(property.mass(), 4.0*Math.PI*(R+2.0*r), 1.e-6);

		// If Eps is argument is absent, precision is quite poor
		BRepGProp.surfaceProperties(torus, property, 1.e-2);
		System.out.println("surface="+property.mass());
		assertEquals(property.mass(), 4.0*Math.PI*Math.PI*R*r, 1.e-6);

		BRepGProp.volumeProperties(torus, property);
		System.out.println("volume="+property.mass());
		assertEquals(property.mass(), 2.0*Math.PI*Math.PI*R*r*r, 1.e-6);

	}
}
