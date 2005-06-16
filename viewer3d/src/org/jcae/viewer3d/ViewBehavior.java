package org.jcae.viewer3d;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.logging.Logger;
import javax.media.j3d.BoundingPolytope;
import javax.media.j3d.Transform3D;
import javax.vecmath.*;
import com.sun.j3d.utils.behaviors.vp.OrbitBehavior;
import com.sun.j3d.utils.picking.PickCanvas;
import com.sun.j3d.utils.picking.PickResult;
import com.sun.j3d.utils.picking.PickTool;

class ViewBehavior extends OrbitBehavior
{
	private Point anchor;
	private BufferedImage image;
	private View view;
	private Rectangle selectionRectangle;

	public ViewBehavior(View view)
	{
		super(view, OrbitBehavior.REVERSE_ALL);
		this.view = view;
	}

	protected void processMouseEvent(MouseEvent evt)
	{	
		if (evt.getID() == MouseEvent.MOUSE_CLICKED
			&& evt.getButton() == MouseEvent.BUTTON1)
		{
			pickPoint(evt);
		}
		// TODO
		// The picking on rectangle is currently disabled because it
		// has bugs.
		// It will only work with Java 1.3.2 (not realy sure of that)
		// http://javadesktop.org/java3d/javadoc/1.3.2/com/sun/j3d/utils/picking/PickTool.html
		// If the pick shape is a PickBounds, the pick result will contain only
		// the scene graph path, even if the mode is GEOMETRY_INTERSECT_INFO.
		// That mean that PickIntersection.getPrimitiveXXX are not availables.
		// The intersection between the real intersection between the PickShape
		// and Geometry returned by the PickIntersection must be computed by
		// the developper by testing each vertices with PickBounds.get().intersect()
		/*if (evt.isControlDown() && evt.getButton() == MouseEvent.BUTTON1
			&& evt.getID() == MouseEvent.MOUSE_PRESSED)
		{
			startRectangleSelection(evt);
		} else if (anchor != null)
		{
			if (evt.getButton() == MouseEvent.BUTTON1
				&& evt.getID() == MouseEvent.MOUSE_RELEASED)
			{
				endRectangleSelection(evt);
			} else
			{
				processRectangleSelection(evt);
			}
		}*/ else
		{
			super.processMouseEvent(evt);
			Transform3D t3d = new Transform3D();
			getViewingPlatform().getViewPlatformTransform().getTransform(t3d);
			fixOriginAxis(t3d);
			//notify slave view to change their position
			view.firePositionChanged();
			//TODO Center the behaviour on the center of the intersection between
			// frustrum and scene bounds.
		}
	}

	protected void pickPoint(MouseEvent evt)
	{
		Viewable cv = view.getCurrentViewable();
		if (cv == null) return;
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
		PickResult result = pickCanvas.pickClosest();
		long time2 = System.currentTimeMillis();
		Logger.global.finest("picked viewable is " + cv + " in "
			+ (time2 - time) + " ms");
		System.out.println(result);
		if (result != null) cv.pick(result, true);
	}

	protected void startRectangleSelection(MouseEvent evt)
	{
		anchor = evt.getPoint();
		image = view.getImage();
	}

	protected void endRectangleSelection(MouseEvent evt)
	{
		anchor = null;
		//delete the rectangle
		/*Graphics2D g2d=(Graphics2D) getGraphics();
		 g2d.drawImage(image, null, 0, 0);*/
		pickRectangle(evt);
		selectionRectangle = null;
	}

	protected void pickRectangle(MouseEvent evt)
	{
		Viewable cv = view.getCurrentViewable();
		if (cv == null) return;
		if (!evt.isControlDown())
		{
			Logger.global.finest("Ctrl is up so everything is unselected");
			cv.unselectAll();
		}
		PickCanvas pickCanvas = new PickCanvas(view, view.getBranchGroup(cv));
		Point3d startPoint = new Point3d();
		BoundingPolytope shape = computeRectangleProjection(selectionRectangle,
			startPoint);
		Vector4d[] v = new Vector4d[4];
		for (int i = 0; i < 4; i++)
			v[i] = new Vector4d();
		shape.getPlanes(v);
		pickCanvas.setMode(PickTool.GEOMETRY_INTERSECT_INFO);
		pickCanvas.setShapeBounds(shape, startPoint);
		long time = System.currentTimeMillis();
		PickResult result = pickCanvas.pickClosest();
		long time2 = System.currentTimeMillis();
		Logger.global.finest("picked viewable is " + cv + " in "
			+ (time2 - time) + " ms");
		System.out.println(Arrays.asList(pickCanvas.pickAll()));		
		if (result != null) cv.pick(result, true);
	}

	
	protected void processRectangleSelection(MouseEvent evt)
	{
		selectionRectangle = new Rectangle(anchor.x, anchor.y, 0, 0);
		selectionRectangle.add(evt.getPoint());
		Graphics2D g2d = (Graphics2D) view.getGraphics();
		g2d.drawImage(image, null, 0, 0);
		g2d.setColor(Color.WHITE);
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

	private BoundingPolytope computeRectangleProjection(Rectangle rectangle,
		Point3d startPoint)
	{
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
