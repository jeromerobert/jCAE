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

import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingBox;
import javax.media.j3d.BoundingPolytope;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.IndexedQuadArray;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Tuple3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector4d;


/**
 * A Bound object representing the visible part of the space.
 * It's computed by cutting the infinite pyramid made by the eye and
 * the screen, by the back and front clipping planes.
 */
public class ViewPyramid extends BoundingPolytope
{
	private Canvas3D canvas;
	/**
	 * Point where start rays when doing picking
	 */
	private Point3d startPoint=new Point3d();
	private Point3d eye;
	private Rectangle rectangle;

	public ViewPyramid(Canvas3D canvas)
	{
		this(canvas, canvas.getBounds());
	}

	public ViewPyramid(Canvas3D canvas, Rectangle rectangle)
	{
		this.canvas=canvas;
		this.rectangle=rectangle;
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
		//Compute origin
		double[] plane=new double[4];
		n.get(plane);
		plane[3]=-1*n.dot(new Vector3d(goesThrow));
		
		return new Vector4d(plane);	
	}
	
	private Vector4d[] computeRectangleProjection(Rectangle rectangle)
	{
		Point3d[] p=getPrismVertices(rectangle);
		//Define an array of Vector4d for all the planes of the pickBounds
		Vector4d[] planeFunc = new Vector4d[6];
		planeFunc[0] = getPlaneFunc(p[0], p[1], p[2]);
		planeFunc[1] = getPlaneFunc(p[4], p[6], p[5]);
		planeFunc[2] = getPlaneFunc(p[1], p[0], p[4]);
		planeFunc[3] = getPlaneFunc(p[1], p[6], p[2]);
		planeFunc[4] = getPlaneFunc(p[2], p[6], p[3]);
		planeFunc[5] = getPlaneFunc(p[0], p[3], p[4]);
		
		//Define the BoundingPolytope bounds object for picking
		return planeFunc;
	}

	//Fix a bug in PickResult line 1819
	@Override
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
	 * on the canvas3d. This is the pyramid between the eye and the screen.
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
			(rectangle.getMaxX()+rectangle.getMinX()) / 2.0,
			(rectangle.getMaxY()+rectangle.getMinY()) / 2.0);

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
	 * Return
	 * {{minX,minY,frontZ},{minX,maxY,frontZ},{maxX,maxY,fontZ},{maxX,minY,frontZ},
	 * {minX,minY,backZ},{minX,maxY,backZ},{maxX,maxY,backZ},{maxX,minY,backZ}}
	 * @param rectangle
	 * @return
	 */
	private Point3d[] getPrismVertices(Rectangle rectangle)
	{
		double farClip = canvas.getView().getBackClipDistance();
		double frontClip = canvas.getView().getFrontClipDistance();		
		Point3d[] pyramid = getPyramVertex(rectangle);
		eye = pyramid[5];
		startPoint = pyramid[4];
		double frontFactor = computeClipFactor(eye, startPoint, frontClip);
		double backFactor = computeClipFactor(eye, startPoint, farClip);
		Point3d[] toReturn = new Point3d[8];
		for(int i=0; i<4; i++)
		{
			toReturn[i] = computePrismPoint(frontFactor, eye, pyramid[i]);
			toReturn[i+4] = computePrismPoint(backFactor, eye, pyramid[i]);
		}
		return toReturn;
	}	
	
	private Point3d computePrismPoint(double factor, Point3d pyramidTop,
		Point3d pyramidBasePoint)
	{
		Point3d p3d=new Point3d(pyramidTop);
		p3d.scale(1-factor);
		Point3d toReturn=new Point3d(pyramidBasePoint);
		toReturn.scaleAdd(factor, p3d);
		return toReturn;
	}

	/** Compute ||v1-v2||+d / ||v1-v2|| **/
	private double computeClipFactor(Tuple3d v1, Tuple3d v2, double d)
	{
		Vector3d p=new Vector3d(v1);
		p.sub(v2);
		double l=p.length();
		return (l+d)/l;
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
	
	/**
	 * Return a shape3D representing the current pyramid.
	 * This method aims to be used for debugging.
	 */
	public Shape3D getShape3D()
	{
		IndexedQuadArray geom=new IndexedQuadArray(8,GeometryArray.COORDINATES,24);
		geom.setCoordinates(0, getPrismVertices(rectangle));
		geom.setCoordinateIndices(0, new int[]{
			0,1,2,3,
			4,5,6,7,
			1,2,6,5,
			0,1,5,4,
			2,3,7,6,
			0,3,7,4});
		Shape3D s3d = new Shape3D(geom);
		Appearance app=new Appearance();
		PolygonAttributes pa=new PolygonAttributes();
		pa.setCullFace(PolygonAttributes.CULL_NONE);
		pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
		app.setPolygonAttributes(pa);
		s3d.setAppearance(app);
		System.out.println(new BoundingBox(s3d.getBounds()));
		System.out.println(new BoundingBox(this));
		return s3d;
	}
}
