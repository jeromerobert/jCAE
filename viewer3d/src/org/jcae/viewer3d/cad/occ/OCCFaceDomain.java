/*
 * Project Info:  http://jcae.sourceforge.net
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 *
 * (C) Copyright 2005, by EADS CRC
 */

package org.jcae.viewer3d.cad.occ;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import org.jcae.opencascade.jni.*;
import org.jcae.viewer3d.cad.CADDomainAdapator;
import org.jcae.viewer3d.cad.DefaultFaceMesh;

/**
 * Meshing parameters were taken from Opencascade sources. See
 * <ul>
 *  <li>src/AIS/AIS_TexturedShape.cxx</li>
 *  <li>src/AIS/AIS_Shape.cxx</li>
 *  <li>src/Prs3d/Prs3d_Drawer.cxx</li>
 *  <li>src/BRepMesh/BRepMesh.cxx</li>
 *  <li>inc/BRepMesh_IncrementalMesh.hxx</li>  
 * </ul>
 * @author Jerome Robert
 *
 */
public class OCCFaceDomain extends CADDomainAdapator
{
	private static int meshIter=3;
	private ArrayList faceMeshes;
	private Color[] facesColors;
	private static boolean debug=false;
	public OCCFaceDomain(TopoDS_Shape shape)
	{	
		TopExp_Explorer explorer = new TopExp_Explorer();
		TopLoc_Location loc = new TopLoc_Location();
		faceMeshes=new ArrayList();
				
		for (explorer.init(shape, TopAbs_ShapeEnum.FACE); explorer.more(); explorer.next())
		{						
			TopoDS_Shape s = explorer.current();
			if (!(s instanceof TopoDS_Face)) continue; // should not happen!
			TopoDS_Face face = (TopoDS_Face)s;
			Poly_Triangulation pt = BRep_Tool.triangulation(face,loc);
			
			float error=0.001f*getMaxBound(s)*4;
			//float error=0.0001f;
			int iter=0;
			while((pt==null)&(iter<meshIter)){
				new BRepMesh_IncrementalMesh(face,error, false);
				//new BRepMesh_IncrementalMesh(face,error, true);
				pt = BRep_Tool.triangulation(face,loc);				
				error/=10;
				iter++;
			}
						
			/*if(pt==null)
			{
				//System.err.println("Meshing "+face);
				//BRepMesh.mesh(face, boundingBoxDeflection);				
				new BRepMesh_IncrementalMesh(face, 0.1, true);
				pt = BRep_Tool.triangulation(face,loc);
			}	*/		
			
			if (pt==null)
			{
				System.err.println("Triangulation failed for face "+face+". Trying other mesh parameters.");
				faceMeshes.add(new DefaultFaceMesh(new float[0], new int[0]));
				continue;
				/*BRepMesh_IncrementalMesh mesh = new BRepMesh_IncrementalMesh();
				mesh.setAngle(angle);
				mesh.setRatio(ratio);
				mesh.update(face);
				pt = BRep_Tool.triangulation(face,loc);
				if (pt==null)
				{
					System.err.println("Cannot triangulate face "+face);
					continue;
				}*/
			}
			else if(debug){
				System.out.println("Triangulation succed for face "+face);
			}
			
			
			double[] dnodes = pt.nodes();
			final int[] itriangles = pt.triangles();						
			/*double[] point=new double[3];
			double[] meshNormal=computeMeshNormal(dnodes, itriangles, point);
			double[] cadNormal=getNormal(point, face);
			
			System.out.println("meshNormal "+meshNormal[0]+" "+meshNormal[1]+" "+meshNormal[2]);
			System.out.println("cadNormal "+cadNormal[0]+" "+cadNormal[1]+" "+cadNormal[2]);
			
			if(scalarProduct(meshNormal, cadNormal)<0)
			{				
				System.out.println("reverseMesh");
				reverseMesh(itriangles);
			}*/
			if(face.orientation()==TopAbs_Orientation.REVERSED)
			{
				reverseMesh(itriangles);
			}
			

			final float[] fnodes=new float[dnodes.length];			
			
			if(loc.isIdentity())
			{
				for(int i=0; i<dnodes.length; i++)
				{
					fnodes[i]=(float) dnodes[i];
				}				
			}
			else
				transformMesh(loc, dnodes, fnodes);
				
			faceMeshes.add(new DefaultFaceMesh(fnodes, itriangles));
		}
	}
	
	void transformMesh(TopLoc_Location loc, double[] src, float[] dst)
	{
		double[] matrix=new double[16];
		loc.transformation().getValues(matrix);
		Matrix4d m4d=new Matrix4d(matrix);
		Point3d p3d=new Point3d();
		for(int i=0; i<src.length; i+=3)
		{
			p3d.x=src[i+0];
			p3d.y=src[i+1];
			p3d.z=src[i+2];
			m4d.transform(p3d);
			dst[i+0]=(float) p3d.x;
			dst[i+1]=(float) p3d.y;
			dst[i+2]=(float) p3d.z;
		}		
	}
	
	/**
	 * Compute the bounding box of the shape and
	 * return the maximum bound value
	 * @param shape
	 * @return
	 */
	private static float getMaxBound(TopoDS_Shape shape){
		Bnd_Box box = new Bnd_Box(); 
		BRepBndLib.add(shape,box);
		double[] bbox = box.get();
		double minBoundingBox=
			Math.max(Math.max(bbox[3]-bbox[0], bbox[4]-bbox[1]), bbox[5]-bbox[2]);
		return (float)minBoundingBox;
	}
	
	/**
	 * Create a Colored Shape
	 * @param shape
	 * @param facesColors
	 */
	public OCCFaceDomain(TopoDS_Shape shape,Color[] facesColors){
		this(shape);
		this.facesColors=facesColors;
	}
	
	/**
	 * @param itriangles
	 */
	static private void reverseMesh(int[] itriangles)
	{
		int tmp;
		for(int i=0; i<itriangles.length; i+=3)
		{
			tmp=itriangles[i];
			itriangles[i]=itriangles[i+1];
			itriangles[i+1]=tmp;
		}
	}

	static private double scalarProduct(double[] v1, double[] v2)
	{
		return v1[0]*v2[0]+v1[1]*v2[1]+v1[2]*v2[2];
	}
	/**
	 * 
	 * @param nodes the nodes of the mesh
	 * @param trias the triangles of the mesh
	 * @param point double[3] array which will receive the point were
	 * the normal is computed
	 * @return The normal a the point <code>point</code>
	 */
	static private double[] computeMeshNormal(double[] nodes, int[] trias, double[] point)
	{
		double[] toReturn=new double[3];
		int index=(trias.length/6)*3;
		double[] v1=new double[3];
		double[] v2=new double[3];
		
		v1[0]=nodes[trias[index]*3]-nodes[trias[index+1]*3];
		v1[1]=nodes[trias[index]*3+1]-nodes[trias[index+1]*3+1];
		v1[2]=nodes[trias[index]*3+2]-nodes[trias[index+1]*3+2];

		v2[0]=nodes[trias[index+2]*3]-nodes[trias[index+1]*3];
		v2[1]=nodes[trias[index+2]*3+1]-nodes[trias[index+1]*3+1];
		v2[2]=nodes[trias[index+2]*3+2]-nodes[trias[index+1]*3+2];
		
		//compute center of the triangle
		point[0]=nodes[trias[index]*3]+nodes[trias[index]*3+1]+nodes[trias[index]*3+2];
		point[1]=nodes[trias[index+1]*3]+nodes[trias[index+1]*3+1]+nodes[trias[index+1]*3+2];
		point[2]=nodes[trias[index+2]*3]+nodes[trias[index+2]*3+1]+nodes[trias[index+2]*3+2];
		
		/*System.out.println("v1 "+v1[0]+" "+v1[1]+" "+v1[2]);
		System.out.println("v2 "+v2[0]+" "+v2[1]+" "+v2[2]);
		System.out.println("point "+point[0]+" "+point[1]+" "+point[2]);*/
		//compute the normal
		toReturn[0]=(v1[1]*v2[2])-(v1[2]*v2[1]);
		toReturn[1]=(v2[0]*v1[2])-(v2[2]*v1[0]);
		toReturn[2]=(v1[0]*v2[1])-(v1[1]*v2[0]);
		
		return toReturn;
	}
	
	static private double[] getNormal(double[] point, TopoDS_Face face)
	{
		Geom_Surface gs=BRep_Tool.surface(face);
		GeomLProp_SLProps myLprop = new GeomLProp_SLProps(2, 0.0001);
		myLprop.setSurface(gs);
		GeomAPI_ProjectPointOnSurf pof=new GeomAPI_ProjectPointOnSurf(point, gs);
		double[] uv=new double[2];
		pof.lowerDistanceParameters(uv);
		myLprop.setParameter(uv[0], uv[1]);
		double[] toReturn=myLprop.normal();
		if(face.orientation()==TopAbs_Orientation.REVERSED)
		{
			toReturn[0]*=-1;
			toReturn[1]*=-1;
			toReturn[2]*=-1;
		}
		return toReturn;
	}
	
	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.cad.CADDomainAdapator#getFaceIterator()
	 */
	public Iterator getFaceIterator()
	{
		return faceMeshes.iterator();
	}
	/**
	 * Return the Color of the ith Face of the iterator 
	 * @param i
	 * @return
	 */
	public Color getFaceColor(int i){
		if(facesColors==null) return null;
		return facesColors[i];
	}
}
