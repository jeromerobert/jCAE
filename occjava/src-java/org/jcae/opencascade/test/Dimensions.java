package org.jcae.opencascade.test;

import org.jcae.opencascade.jni.*;

/**
 * Example of BRepGProp and BRepBndLib
 * @author Jerome Robert
 */
public class Dimensions
{
	public static void main(String[] args)
	{
		// Create a shape for the example (a torus)
		double[] axis=new double[]{
			0,0,0,  // position
			0,0,1}; // direction
		TopoDS_Shape torus=new BRepPrimAPI_MakeTorus(axis, 3, 1).shape();

		// Compute the bounding box of the shape
		Bnd_Box bbox = new Bnd_Box(); 			
		BRepBndLib.add(torus, bbox);			
		double[] bboxValues = bbox.get();
		
		// Display the bounding box
		System.out.println("Xmin="+bboxValues[0]);
		System.out.println("Ymin="+bboxValues[0]);
		System.out.println("Zmin="+bboxValues[0]);
		System.out.println("Xmax="+bboxValues[0]);
		System.out.println("Ymax="+bboxValues[0]);
		System.out.println("Zmax="+bboxValues[0]);
		
		// Display various other properties
		GProp_GProps property=new GProp_GProps();
		BRepGProp.linearProperties(torus, property);
		System.out.println("length="+property.mass());
		
		BRepGProp.surfaceProperties(torus, property);
		System.out.println("surface="+property.mass());
		
		BRepGProp.volumeProperties(torus, property);
		System.out.println("volume="+property.mass());

	}
}
