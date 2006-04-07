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

package org.jcae.viewer3d;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.logging.Logger;

import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingPolytope;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.PointArray;
import javax.media.j3d.PointAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.vecmath.*;

import com.sun.j3d.utils.behaviors.vp.OrbitBehavior;
import com.sun.j3d.utils.picking.PickCanvas;
import com.sun.j3d.utils.picking.PickResult;
import com.sun.j3d.utils.picking.PickTool;

public  class ViewBehavior extends OrbitBehavior
{
	// dirty warkaround for bug
	// https://java3d.dev.java.net/issues/show_bug.cgi?id=179
	// because xtrans, ytrans and ztrans should be protected not private
	private final static Field FIELD_XTRANS;
	private final static Field FIELD_YTRANS;
	private final static Field FIELD_ZTRANS;
	private final static Field FIELD_ROTATETRANSFORM;
	static
	{
		Field xtrans=null, ytrans=null, ztrans=null, rotateTransform=null;
		try
		{
			xtrans = OrbitBehavior.class.getDeclaredField("xtrans");
			ytrans = OrbitBehavior.class.getDeclaredField("ytrans");
			ztrans = OrbitBehavior.class.getDeclaredField("ztrans");
			rotateTransform = OrbitBehavior.class.getDeclaredField("rotateTransform");
			xtrans.setAccessible(true);
			ytrans.setAccessible(true);
			ztrans.setAccessible(true);
			rotateTransform.setAccessible(true);
		}
		catch (SecurityException e)
		{			
			e.printStackTrace();
		}
		catch (NoSuchFieldException e)
		{		
			e.printStackTrace();
		}
		FIELD_XTRANS=xtrans;
		FIELD_YTRANS=ytrans;
		FIELD_ZTRANS=ztrans;
		FIELD_ROTATETRANSFORM=rotateTransform;
	}
	
	private Point anchor;
	private BufferedImage image;
	private View view;
	private Rectangle selectionRectangle;
	private boolean changeRotationCenter;
	
	//Viewer mouse modes
	public final static int DEFAULT_MODE=0;
	public final static int CLIP_RECTANGLE_MODE=1;
	public final static int CLIP_BOX_MODE=2;
	public final static int RECTANGLE_MODE=3;
	private int mouseMode=DEFAULT_MODE;
	
	private BranchGroup firstClipBoxPointGroup=null;
	private Point3d firstClipBoxPoint3d=null;
	
	public ViewBehavior(View view)
	{
		super(view, OrbitBehavior.REVERSE_ALL);
		this.view = view;
	}

	public void setChangeRotationCenter(boolean status)
	{
		view.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		changeRotationCenter=status;
	}
	
	protected void processMouseEvent(MouseEvent evt)
	{	
		
		switch(mouseMode){
		case CLIP_RECTANGLE_MODE :
			 rectangleClipMode(evt);
			break;
		case CLIP_BOX_MODE :
			clipBoxMode(evt);
			break;
		case RECTANGLE_MODE :
			//rectangleMode(evt);
			break;
		case DEFAULT_MODE:
		default :
			defaultMode(evt);
		}
	}
	
	private void clipBoxMode(MouseEvent evt) {
		if (evt.getID() == MouseEvent.MOUSE_CLICKED
				&& evt.getButton() == MouseEvent.BUTTON1)
		{
			if(firstClipBoxPointGroup==null){
				firstClipBoxPoint3d=getPickPoint3d(evt);
				if(firstClipBoxPoint3d==null) return;
				PointAttributes pa=new PointAttributes();
				pa.setPointSize(4);
				
				Appearance app=new Appearance();
				app.setPointAttributes(pa);
				
				PointArray pt=new PointArray(1,PointArray.COORDINATES | PointArray.COLOR_3);
				pt.setCoordinate(0,firstClipBoxPoint3d);
				pt.setColor(0,new Color3f(1,0,0));
				
				Shape3D s=new Shape3D();
				s.setAppearance(app);
				s.setGeometry(pt);
				
				firstClipBoxPointGroup=new BranchGroup();
				firstClipBoxPointGroup.addChild(s);
				view.addUnClipWidgetBranchGroup(firstClipBoxPointGroup);
				view.println("Select the second box point");
			
			}
			else {
				Point3d secondClipBoxPoint3d=getPickPoint3d(evt);
				view.setClipBox(new ClipBox(firstClipBoxPoint3d,secondClipBoxPoint3d));
				restoreDefaultMode();
			}
		}
		else 
			defaultMode(evt);
		
	}
	
	private Point3d getPickPoint3d(MouseEvent evt){
		PickViewable result=basicPickPoint(evt);
		Point3d toReturn=null;
		if(result!=null)
			toReturn=result.getIntersection().getPointCoordinates();			
			
		return toReturn;
	}

	/** Defines what to do for the DEFAULT_MODE*/
	private void defaultMode(MouseEvent evt){
		if (evt.getID() == MouseEvent.MOUSE_CLICKED
				&& evt.getButton() == MouseEvent.BUTTON1)
		{
			if(changeRotationCenter)
				changeRotationCenter(evt);
			else
				pickPoint(evt);
		}
		else {
			super.processMouseEvent(evt);
			if(motion)
			{
				Transform3D t3d = new Transform3D();
				getViewingPlatform().getViewPlatformTransform().getTransform(t3d);
				fixOriginAxis(t3d);
				//notify slave view to change their position
				view.firePositionChanged();
			}
		}
	}
	
	/** Defines what to do for the BOX_MODE*/
	private void rectangleClipMode(MouseEvent evt){
		if (evt.getButton() == MouseEvent.BUTTON1
				&& evt.getID() == MouseEvent.MOUSE_PRESSED)
		{
			startRectangleDrawing(evt);
		}
		else if (anchor != null)
		{
			if (evt.getButton() == MouseEvent.BUTTON1
					&& evt.getID() == MouseEvent.MOUSE_RELEASED)
			{
				endRectangleDrawing(evt);
				createClipRectanglePlanes(evt);
			} else
			{
				processRectangleDrawing(evt,Color.MAGENTA);
			}
		}
	}
	
	/** Defines what to do for the RECTANGLE_MODE*/	
	private void rectangleMode(MouseEvent evt){
		if (evt.getButton() == MouseEvent.BUTTON1
				&& evt.getID() == MouseEvent.MOUSE_PRESSED)
		{
			startRectangleDrawing(evt);
		}
		else if (anchor != null)
		{
			if (evt.getButton() == MouseEvent.BUTTON1
					&& evt.getID() == MouseEvent.MOUSE_RELEASED)
			{
				endRectangleDrawing(evt);
				pickRectangle(evt);
			} else
			{
				processRectangleDrawing(evt,Color.WHITE);
			}
		}
	} 
	
	/** clear all variables needed for other modes*/
	private void restoreDefaultMode(){
		//for box and rectangle mode
		anchor=null;
		//for changeRotationCenter
		changeRotationCenter=false;
		view.setCursor(Cursor.getDefaultCursor());
		// for clipBox
		cancelClipBox();
		mouseMode=DEFAULT_MODE;
		
		view.println("> Default mode");
	}
	
	private void cancelClipBox() {
		if(firstClipBoxPointGroup==null) return;
		view.removeUnClipWidgetBranchGroup(firstClipBoxPointGroup);
		firstClipBoxPointGroup=null;
	}

	public void setMouseMode(int mode){
		restoreDefaultMode();
		switch(mode){
		case CLIP_RECTANGLE_MODE :
			mouseMode=CLIP_RECTANGLE_MODE;
			view.println("> Clip rectangle mode (press space key to escape)");
			break;
		case CLIP_BOX_MODE :
			view.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
			mouseMode=CLIP_BOX_MODE;
			view.println("> Clip box mode (press space key to escape)");
			view.println("Select the first box point");
			break;
		case RECTANGLE_MODE :
			mouseMode=RECTANGLE_MODE;
			view.println("> Rectangle Selection mode (press space key to escape)");
			break;
		}
	}
	
	public int getMouseMode(){
		return mouseMode;
	}
	
	private Tuple3d getTranslation()
	{
		Vector3d toReturn=null;
		try
		{
			toReturn = new Vector3d(
				FIELD_XTRANS.getDouble(this),
				FIELD_YTRANS.getDouble(this),
				FIELD_ZTRANS.getDouble(this)
			);
		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{		
			e.printStackTrace();
		}		
		return toReturn;
	}

	private void setTranslation(Tuple3d vector)
	{
		try
		{
			FIELD_XTRANS.setDouble(this, vector.x);
			FIELD_YTRANS.setDouble(this, vector.y);
			FIELD_ZTRANS.setDouble(this, vector.z);
		}
		catch (IllegalArgumentException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{		
			e.printStackTrace();
		}
	}
	
	private Transform3D getRotateTransform()
	{
		try
		{
			return (Transform3D) FIELD_ROTATETRANSFORM.get(this);
		}
		catch (IllegalArgumentException e)
		{	
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{		
			e.printStackTrace();
		}
		return null;
	}
	
	private void changeRotationCenter(MouseEvent evt)
	{
		PickViewable result=basicPickPoint(evt);
		if(result!=null)
		{
			
			Point3d newCenter=result.getIntersection().getPointCoordinates();			
			Point3d oldCenter=new Point3d();
			getRotationCenter(oldCenter);
			Tuple3d trans = getTranslation();						
			Point3d centerDelta=new Point3d();			
			centerDelta.sub(oldCenter, newCenter);
			Transform3D invRot=new Transform3D(getRotateTransform());
			invRot.invert();
			invRot.transform(centerDelta);
			trans.add(centerDelta);
			setTranslation(trans);
			setRotationCenter(newCenter);
		}
		changeRotationCenter=false;
		view.setCursor(Cursor.getDefaultCursor());
	}

	protected PickViewable basicPickPoint(MouseEvent evt)
	{
		Viewable cv = view.getCurrentViewable();
		if (cv == null)
			return null;
		if (!evt.isControlDown())
		{
			Logger.global.finest("Ctrl is up so everything is unselected");
			cv.unselectAll();
		}
		PickCanvas pickCanvas = new PickCanvas(view, view.getBranchGroup(cv));
		pickCanvas.setMode(PickTool.GEOMETRY_INTERSECT_INFO);
		pickCanvas.setTolerance(5.0f);
		pickCanvas.setShapeLocation(evt.getX(), evt.getY());
		long time = System.currentTimeMillis();
		PickViewable result = pickPoint(pickCanvas.pickAllSorted());
		//PickViewable result=new PickViewable(pickCanvas.pickClosest(), 0);
		long time2 = System.currentTimeMillis();
		Logger.global.finest("picked viewable is " + cv + " in "
			+ (time2 - time) + " ms");
		return result;
	}
	
	
	/** returns the first PickViewable in the modelClip and
	 * between the front and back clip planes*/
	protected PickViewable  pickPoint(PickResult[] result){
		if(result==null) return null;
		PickViewable toReturn=null;
		
		//Front and back clip distance are not related to picking distance,
		//neither in PHYSICAL_EYE neither in VIRTUAL_EYE modes, so they
		//cannot be used to filter picking. We keep this code as a remind.
		//double minDistance=view.getFrontClipDistance();
		//double maxDistance=view.getBackClipDistance();
		
		double minDistance=-Double.MAX_VALUE;
		double maxDistance=Double.MAX_VALUE;
		double d;
		
		for(int i=0;i<result.length;i++){
			PickResult pr=result[i];			
			for(int j=0;j<pr.numIntersections();j++){
				Point3d pt=pr.getIntersection(j).getPointCoordinatesVW();
				if(view.isInModelClip(pt)){
					d=pr.getIntersection(j).getDistance();
					if( (d>minDistance)&(d<maxDistance)){
						toReturn=new PickViewable(pr,j);
						maxDistance=d;
					}
				}
			}
		}
		
		return toReturn;		
	}
	
	protected void pickPoint(MouseEvent evt)
	{
		PickViewable result = basicPickPoint(evt);
		if (result != null)
			view.getCurrentViewable().pick(result, true);
	}

	protected void startRectangleDrawing(MouseEvent evt)
	{
		anchor = evt.getPoint();
		image = view.getImage();
	}
	
	protected void endRectangleDrawing(MouseEvent evt)
	{
		anchor = null;
		//delete the rectangle
		Graphics2D g2d=(Graphics2D) view.getGraphics();
		g2d.drawImage(image, null, 0, 0);
	}
	
	protected void createClipRectanglePlanes(MouseEvent evt){
		Viewable cv = view.getCurrentViewable();
		if (cv == null) return;
//		if (!evt.isControlDown())
//		{
//			Logger.global.finest("Ctrl is up so everything is unselected");
//			cv.unselectAll();
//		}
		Vector4d[] planes=computeClipPlanes(selectionRectangle);
		if(planes!=null)
			view.setClipPlanes(planes);
		selectionRectangle = null;
	}
	
	

	protected void pickRectangle(MouseEvent evt)
	{
		Viewable cv = view.getCurrentViewable();
		if (cv == null) return;
//		if (!evt.isControlDown())
//		{
//			Logger.global.finest("Ctrl is up so everything is unselected");
//			cv.unselectAll();
//		}
		PickCanvas pickCanvas = new PickCanvas(view, view.getBranchGroup(cv));
		Point3d startPoint = new Point3d();
		BoundingPolytope shape = computeRectangleProjection(selectionRectangle,
			startPoint);
		if(shape!=null){
		Vector4d[] v = new Vector4d[4];
		for (int i = 0; i < 4; i++)
			v[i] = new Vector4d();
		shape.getPlanes(v);
		pickCanvas.setMode(PickTool.GEOMETRY_INTERSECT_INFO);
		pickCanvas.setShapeBounds(shape, startPoint);
		long time = System.currentTimeMillis();
		PickViewable result = pickPoint(pickCanvas.pickAllSorted());
		long time2 = System.currentTimeMillis();
		Logger.global.finest("picked viewable is " + cv + " in "
			+ (time2 - time) + " ms");		
		if (result != null) cv.pick(result, true);
		}
		selectionRectangle = null;
	}

	
	protected void processRectangleDrawing(MouseEvent evt,Color rectangleColor)
	{
		selectionRectangle = new Rectangle(anchor.x, anchor.y, 0, 0);
		selectionRectangle.add(evt.getPoint());
		Graphics2D g2d = (Graphics2D) view.getGraphics();
		g2d.drawImage(image, null, 0, 0);
		g2d.setColor(rectangleColor);
		g2d.draw(selectionRectangle);
	}

	private void fixOriginAxis(Transform3D t3d)
	{
		Vector3f translation = new Vector3f();
		t3d.get(translation);
		Transform3D t3d2 = new Transform3D();
		double scale = translation.length() / 20;
		t3d2.setScale(scale);
		view.getOriginAxisTransformGroup().setTransform(t3d2);
	}

	
	private Vector4d[] computeClipPlanes(Rectangle rectangle){
		
		Point3d[] pyramVertex=getPyramVertex(rectangle);
		if(pyramVertex==null) return null;
		//Compute rectangle center
		Point3d center=new Point3d(pyramVertex[0]);
		center.add(pyramVertex[2]);
		center.scale(0.5);
		
		Vector3d screenNormal;
		Point3d eye=new Point3d(pyramVertex[5]);
		Vector4d[] toReturn=new Vector4d[4];
		Vector3d n=new Vector3d();
		
		for(int ii=0;ii<3;ii++){
			//Compute the screen normal
			screenNormal=new Vector3d();	
			screenNormal.sub(eye,pyramVertex[ii]);
			n.sub(pyramVertex[ii],pyramVertex[ii+1]);
			toReturn[ii]=computePlane(n,screenNormal,pyramVertex[ii],center);
			toReturn[ii].scale(-1);
		}
		
		//Compute the screen normal
		screenNormal=new Vector3d();	
		screenNormal.sub(eye,pyramVertex[3]);
		n.sub(pyramVertex[3],pyramVertex[0]);
		toReturn[3]=computePlane(n,screenNormal,pyramVertex[3],center);
		toReturn[3].scale(-1);
		return toReturn;
	}
	
	/** returns the { {minX,minY},{minX,maxY},{maxX,maxY},{maxX,minY},{midle},{eye} }
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
		java.awt.Dimension dim = view.getSize();
		rectPoint[4] = new Point2d(dim.width / 2.0, dim.height / 2.0);
		//rectPoint[4] = new Point2d(orgX + (newX - orgX)/2.0,
		//                         orgY + (newY - orgY)/2.0);
		for (int ii = 0; ii < rectPoint.length; ii++)
			view.getPixelLocationInImagePlate(rectPoint[ii], pyramVertex[ii]);
		view.getCenterEyeInImagePlate(pyramVertex[5]);
		Transform3D trans = new Transform3D();
		view.getImagePlateToVworld(trans);
		for (int ii = 0; ii < pyramVertex.length; ii++)
			trans.transform(pyramVertex[ii], pyramVertex[ii]);
		
		return pyramVertex;
	}
	
	/** returns the plane equation defined by tow vectors a point going throw and a point
	 * pointed by the plane normal*/
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
	
	private BoundingPolytope computeRectangleProjection(Rectangle rectangle,
		Point3d startPoint)
	{
		Point3d[] pyramVertex=getPyramVertex(rectangle);
		if(pyramVertex==null) return null;
		//Compute the plane function of the bottom face of the pyramid bounds
		double farClipLength = getView().getBackClipDistance();
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
		return new BoundingPolytope(planeFunc)
		{
			//Fix a bug in PickResult line 1819
			public void getPlanes(Vector4d[] planes)
			{
				for(int i=0;i<planes.length;i++)
				{
					planes[i]=new Vector4d();
				}
				super.getPlanes(planes);				
			}
		};		
	}

	/**
	 * Compute the plane function, return it
	 * @param p1 The 1rst point
	 * @param p2 The 2nd point
	 * @param p3 The 3rd point
	 * @return An object of Vector4d, which represents a plane function
	 */
	private Vector4d getPlaneFunc(Point3d p1, Point3d p2, Point3d p3)
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
}
