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

import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.logging.Logger;

import javax.media.j3d.*;
import javax.vecmath.*;

import com.sun.j3d.utils.behaviors.vp.OrbitBehavior;
import com.sun.j3d.utils.picking.PickCanvas;
import com.sun.j3d.utils.picking.PickResult;
import com.sun.j3d.utils.picking.PickTool;

public  class ViewBehavior extends OrbitBehavior
{
	private static Logger logger=Logger.getLogger("global");
	// dirty workaround for bug
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
	private View view;
	private SelectionRectangle selectionRectangle3D; 
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
		selectionRectangle3D = new SelectionRectangle.SelectionRectangle2D(view);
	}

	public void setChangeRotationCenter(boolean status)
	{
		view.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		changeRotationCenter=status;
	}
	
	@Override
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
			rectangleMode(evt);
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
				
				PointArray pt=new PointArray(1,GeometryArray.COORDINATES | GeometryArray.COLOR_3);
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
			startRectangleDrawing(evt, Color.MAGENTA);
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
				selectionRectangle3D.setGeometry(anchor, evt.getPoint());
			}
		}
	}
	
	/** Defines what to do for the RECTANGLE_MODE*/	
	private void rectangleMode(MouseEvent evt){
		if (evt.getButton() == MouseEvent.BUTTON1
				&& evt.getID() == MouseEvent.MOUSE_PRESSED)
		{
			startRectangleDrawing(evt, Color.WHITE);
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
				selectionRectangle3D.setGeometry(anchor, evt.getPoint());
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
			logger.finest("Ctrl is up so everything is unselected");
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
		logger.finest("picked viewable is " + cv + " in "
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
			view.getCurrentViewable().pick(result);
	}

	protected void startRectangleDrawing(MouseEvent evt, Color color)
	{
		anchor = evt.getPoint();
		selectionRectangle3D.setGeometry(anchor, anchor);
		selectionRectangle3D.setColor(color);
		selectionRectangle3D.setVisible(true);
	}
	
	protected void endRectangleDrawing(MouseEvent evt)
	{
		anchor = null;
		selectionRectangle3D.setVisible(false);
	}
	
	protected void createClipRectanglePlanes(MouseEvent evt)
	{
		Viewable cv = view.getCurrentViewable();
		if (cv == null) return;
//		if (!evt.isControlDown())
//		{
//			Logger.global.finest("Ctrl is up so everything is unselected");
//			cv.unselectAll();
//		}
		Vector4d[] planes=new Vector4d[4];
		new ViewPyramid(view, selectionRectangle3D.getGeometry2D()).getSidePlanes(planes);
		view.setClipPlanes(planes);
	}

	protected void pickRectangle(MouseEvent evt)
	{
		Viewable cv = view.getCurrentViewable();
		if (cv == null)
			return;

		com.sun.j3d.utils.pickfast.PickCanvas pickCanvas =
			new com.sun.j3d.utils.pickfast.PickCanvas(
			view, view.getBranchGroup(cv));
		
		ViewPyramid shape =
			new ViewPyramid(view, selectionRectangle3D.getGeometry2D());
		
		Vector4d[] v = new Vector4d[4];
		shape.getPlanes(v);
		pickCanvas.setMode(PickInfo.PICK_GEOMETRY);		
		pickCanvas.setShapeBounds(shape, shape.getStartPoint());
		PickInfo[] result = pickCanvas.pickAllSorted();
		if (result != null && result.length > 0)
			cv.pickArea(result, shape);
	}

	private void fixOriginAxis(Transform3D t3d)
	{
		Vector3f translation = new Vector3f();
		t3d.get(translation);
		Transform3D t3d2 = new Transform3D();
		double scale = translation.length() / 10 * Math.tan(view.getView().getFieldOfView());
		t3d2.setScale(scale);
		view.getOriginAxisTransformGroup().setTransform(t3d2);
	}
}
