package org.jcae.opencascade.test;

import org.jcae.opencascade.jni.*;

public class Curvature
{
	/** Example to show how to get the curvature of a surface */
	public static void main(String[] args)
	{
		//Create an cone for the example
		double[] axis=new double[]{
			0, 0, 0,
			1, 2, 3
		};		
		TopoDS_Shape shape=new BRepPrimAPI_MakeCone(
			axis, 3, 1, 4, Math.PI*2).shape();
		
		// Select the side of the cone
		TopExp_Explorer exp=new TopExp_Explorer(shape, TopAbs_ShapeEnum.FACE);
		TopoDS_Face face=(TopoDS_Face) exp.current();
		
		// Get the geometry associated to the previously selected face
		Geom_Surface geom = BRep_Tool.surface(face);
		
		// Get parameters intervals
		double[] paramBounds=new double[4];
		geom.bounds(paramBounds);		
		double umin=paramBounds[0];
		double umax=paramBounds[1];
		double vmin=paramBounds[2];
		double vmax=paramBounds[3];

		// Handle the case of infinite geometry
		if(!geom.isUClosed())
		{
			umin=0;
			umax=1;
		}

		if(!geom.isVClosed())
		{
			vmin=0;
			vmax=1;
		}
				
		// Create the object which allow to ask for various local geometry
		// properties
		GeomLProp_SLProps props=new GeomLProp_SLProps(2, 1E-7);
		props.setSurface(geom);
		
		
		// Iterate on u and v and display various properties at each point
		double ustep=(umax-umin)/10;
		double vstep=(vmax-vmin)/10;

		for(double u=umin; u<umax; u+=ustep)
		{
			for(double v=vmin; v<vmax; v+=vstep)
			{
				props.setParameters(u, v);
				//The 3D coordinate associated to the u,v coordinate
				double[] p = props.value();
				//The first derivative allong U
				double[] d1u = props.d1U();
				//The second derivative allong U
				double[] d2u = props.d2U();				
				
				System.out.println("M: ("+p[0]+", "+p[1]+", "+p[2]+")");
				System.out.println("dM/dU: ("+d1u[0]+", "+d1u[1]+", "+d1u[2]+")");
				System.out.println("d2M/d2U: ("+d2u[0]+", "+d2u[1]+", "+d2u[2]+")");
			}
		}
	}	
}

