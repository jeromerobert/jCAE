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
 * (C) Copyright 2007, by EADS France
 */

package org.jcae.viewer3d;

import java.awt.Rectangle;
import javax.media.j3d.BoundingPolytope;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.Transform3D;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector4d;


public class ViewPyramid extends BoundingPolytope
{
	private Canvas3D canvas;
	/**
	 * Point where start rays when doing picking
	 */
	private Point3d startPoint=new Point3d();
	private Point3d eye;

	public ViewPyramid(Canvas3D canvas)
	{
		this(canvas, canvas.getBounds());
	}

	public ViewPyramid(Canvas3D canvas, Rectangle rectangle)
	{
		this.canvas=canvas;
		
		setPlanes(computeRectangleProjection(rectangle));
	}
	
	/**
	 * returns the plane equation defined by tow vectors a point going throw
	 * and a point pointed by the plane normal
	 */
	private Vector4d computePlane(Vector3d n1,Vector3d n2,Point3d goesThrow,Point3d pointed){
		Vector3d n=new Vector3d();
		//Compute normal
		n.cross(n1,n2);
		n.normalize();
		Point3d p=new Point3d();
		p.sub(pointed,goesThrow);
		if(n.dot(new Vector3d(p))<0)
			n.scale(-1);
		//Compute origine
		double[] plane=new double[4];
		n.get(plane);
		plane[3]=-1*n.dot(new Vector3d(goesThrow));
		
		return new Vector4d(plane);	
	}
	
	private Vector4d[] computeRectangleProjection(Rectangle rectangle)
	{
		Point3d[] pyramVertex=getPyramVertex(rectangle);
		//Compute the plane function of the bottom face of the pyramid bounds
		double farClipLength = canvas.getView().getBackClipDistance();
		Vector3d farClipVect = new Vector3d();
		farClipVect.sub(pyramVertex[4], pyramVertex[5]);
		farClipVect.normalize();
		Vector3d farClipPt = new Vector3d();
		farClipPt.scale(farClipLength, farClipVect);
		double d0 = -farClipVect.dot(farClipPt);
		//Define an array of Vector4d for all the planes of the pickBounds
		Vector4d[] planeFunc = new Vector4d[5];
		planeFunc[0] = new Vector4d(farClipVect.x, farClipVect.y,
			farClipVect.z, d0);
		planeFunc[1] = getPlaneFunc(pyramVertex[0], pyramVertex[1],
			pyramVertex[5]);
		planeFunc[2] = getPlaneFunc(pyramVertex[1], pyramVertex[2],
			pyramVertex[5]);
		planeFunc[3] = getPlaneFunc(pyramVertex[2], pyramVertex[3],
			pyramVertex[5]);
		planeFunc[4] = getPlaneFunc(pyramVertex[3], pyramVertex[0],
			pyramVertex[5]);
		//Define the BoundingPolytope bounds object for picking
		startPoint = pyramVertex[4];
		eye = pyramVertex[5];
		return planeFunc;
	}

	//Fix a bug in PickResult line 1819
	public void getPlanes(Vector4d[] planes)
	{
		for(int i=0;i<planes.length;i++)
		{
			planes[i]=new Vector4d();
		}
		super.getPlanes(planes);				
	}
	
	
	/**
	 * Compute the plane function, return it
	 * @param p1 The 1rst point
	 * @param p2 The 2nd point
	 * @param p3 The 3rd point
	 * @return An object of Vector4d, which represents a plane function
	 */
	private static Vector4d getPlaneFunc(Point3d p1, Point3d p2, Point3d p3)
	{
		Point3d pt = new Point3d();
		pt.sub(p1, p2);
		Vector3d v1 = new Vector3d(pt);
		pt.sub(p3, p2);
		Vector3d v2 = new Vector3d(pt);
		Vector3d planeVec = new Vector3d();
		planeVec.cross(v2, v1);
		planeVec.normalize();
		//double d0 = -planeVec.dot(new Vector3d(p1));
		double d1 = -planeVec.dot(new Vector3d(p2));
		return new Vector4d(planeVec.x, planeVec.y, planeVec.z, d1);
	}
	
	/**
	 * returns the { {minX,minY},{minX,maxY},{maxX,maxY},{maxX,minY},{midle},{eye} }
	 * positions in the Vworld coordinates corresponding to the rectangle parameter
	 * on the canvas3d.
	 * @param rectangle
	 * @return
	 */
	private Point3d[] getPyramVertex(Rectangle rectangle){
		if(rectangle==null) return null;
		Point3d[] pyramVertex = new Point3d[6];
		Point2d[] rectPoint = new Point2d[5];
		for (int ii = 0; ii < pyramVertex.length; ii++)
			pyramVertex[ii] = new Point3d();
		rectPoint[0] = new Point2d(rectangle.getMinX(), rectangle.getMinY());
		rectPoint[1] = new Point2d(rectangle.getMinX(), rectangle.getMaxY());
		rectPoint[2] = new Point2d(rectangle.getMaxX(), rectangle.getMaxY());
		rectPoint[3] = new Point2d(rectangle.getMaxX(), rectangle.getMinY());		
		rectPoint[4] = new Point2d(
			(rectangle.getMaxX()-rectangle.getMinX()) / 2.0,
			(rectangle.getMaxY()-rectangle.getMinY()) / 2.0);

		for (int ii = 0; ii < rectPoint.length; ii++)
			canvas.getPixelLocationInImagePlate(rectPoint[ii], pyramVertex[ii]);
		canvas.getCenterEyeInImagePlate(pyramVertex[5]);
		Transform3D trans = new Transform3D();
		canvas.getImagePlateToVworld(trans);
		for (int ii = 0; ii < pyramVertex.length; ii++)
			trans.transform(pyramVertex[ii], pyramVertex[ii]);
		
		return pyramVertex;
	}

	/**
	 * Return a point from where to start when picking
	 * @todo Probably useless, must be checked
	 */
	public Point3d getStartPoint()
	{
		return startPoint;
	}

	public void getSidePlanes(Vector4d[] planes)
	{
		Vector4d[] vs=new Vector4d[getNumPlanes()];
		getPlanes(vs);
		planes[0]=vs[1];
		planes[1]=vs[2];
		planes[2]=vs[3];
		planes[3]=vs[4];
	}

	/**
	 * Return the position of the eye in the VWorld
	 * @todo Probably useless, must be checked
	 */	
	public Point3d getEye()
	{
		return eye;
	}
}
