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
 * (C) Copyright 2008, by EADS France
 */

package org.jcae.vtk;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import vtk.vtkAxesActor;
import vtk.vtkCamera;
import vtk.vtkCellPicker;
import vtk.vtkOrientationMarkerWidget;
import vtk.vtkRenderer;
import vtk.vtkTransform;

/**
 * TODO : Integrate it directly in RubberBandHelper ?
 * @author ibarz
 */
public class CameraManager
{
	private static class ScreenShotCamera
	{
		private ImageIcon snapShot;
		private final static int height = 100;
		private final static int width = 100;

		public ImageIcon getSnapShot()
		{
			return snapShot;
		}

		public void shot(BufferedImage aSnapShot)
		{
			int h = aSnapShot.getHeight();
			int w = aSnapShot.getWidth();

			BufferedImage buffer = new BufferedImage(
					width, height, BufferedImage.TYPE_INT_RGB);
			double scaleH = ((double) height) / h;
			double scaleW = ((double) width) / w;
			double scale = Math.min(scaleH, scaleW);

			//scale transform to get always the same image size
			AffineTransform s = new AffineTransform();
			s.scale(scale, scale);
			//translate transform to center the image
			AffineTransform t = new AffineTransform();
			t.setToTranslation((width - scale * w) / 2.0, (height - scale * h) / 2.0);
			t.concatenate(s);

			Graphics2D g2D = buffer.createGraphics();
			g2D.drawRenderedImage(aSnapShot, t);
			snapShot = new ImageIcon(buffer);
		}
	}	
	
	// Different classic cameras
	private static final vtkCamera[] defaultCameras =
		new vtkCamera[Orientation.values().length];
	private Canvas canvas;
	private vtkRenderer renderer;
	private ArrayList<vtkCamera> cameras = new ArrayList<vtkCamera>();
	private ArrayList<ScreenShotCamera> screenShots =
		new ArrayList<ScreenShotCamera>();
	private vtkAxesActor originAxes;
	private boolean isVisibleRelativeAxis = true;
	private vtkAxesActor relativeAxes;
	private vtkOrientationMarkerWidget marker;
	private double originAxesFactor = 1. / 15.;
	public enum Orientation
	{
		TOP, BOTTOM, LEFT, RIGHT, FRONT, BACK
	}

	public CameraManager(Canvas canvas)
	{
		this.canvas = canvas;
		renderer = canvas.GetRenderer();
		
		originAxes = new vtkAxesActor();
		originAxes.AxisLabelsOff();
		renderer.AddActor(originAxes);
		
		// A display orientation
		relativeAxes = new vtkAxesActor();
		relativeAxes.AxisLabelsOn();
		
		marker = new vtkOrientationMarkerWidget();
		marker.SetOrientationMarker(relativeAxes);
		marker.SetViewport(0., 0., .3, .3);
		marker.SetInteractor(canvas.getIren());
		marker.EnabledOn();
		marker.InteractiveOff();
		
		// We connect the "RenderEvent" event to the renderEvent method of this 
		canvas.getIren().AddObserver("RenderEvent", this, "renderEvent");
		
		/**
		 * The camera orientation
		 * camera[i*2] is the vector position of camera i
		 * camera[i*2+1] is the vector up of camera i
		 * 
		 */
		double[][] cameraOrientation = {
			// TOP
			{0., 1., 0.},
			{0., 0., -1.},
			// BOTTOM
			{0., -1., 0.},
			{0., 0., 1.},
			// LEFT
			{-1., 0., 0.},
			{0., 1., 0.},
			// RIGHT
			{1., 0., 0.},
			{0., 1., 0.},
			// FRONT
			{0., 0., 1.},
			{0., 1., 0.},
			// BACK
			{0., 0., -1.},
			{0., 1., 0.}};

		for (int i = 0; i < Orientation.values().length; ++i)
		{
			defaultCameras[i] = new vtkCamera();
			defaultCameras[i].SetPosition(cameraOrientation[i * 2]);
			defaultCameras[i].SetViewUp(cameraOrientation[i * 2 + 1]);
		}
	}
	
	private void renderEvent()
	{
		// Find the distance from the camera of the origin axes
		vtkCamera camera = renderer.GetActiveCamera();
		vtkTransform modelView = camera.GetViewTransformObject();
		double[] point = modelView.TransformDoublePoint(0, 0, 0);
		// The distance is multiplied by K
		double zDistance = Math.abs(point[2]) * originAxesFactor;
		
		/*System.out.println("originAxesFactor " + originAxesFactor);
		System.out.println("z : " + point[2]);
		System.out.println("zDistance : " + zDistance);*/
		
		// If we are very close keep the original scale
		if(zDistance < 1.)
			zDistance = 1.;
		originAxes.SetTotalLength(zDistance, zDistance, zDistance);
	}
	
	public void setRelativeAxisVisible(boolean visibility)
	{
		this.isVisibleRelativeAxis = visibility;		
		canvas.lock();
		marker.SetEnabled(Utils.booleanToInt(visibility));
		canvas.unlock();
		relativeAxes.SetVisibility(Utils.booleanToInt(visibility));
		canvas.RenderSecured();
	}
	
	public void setOriginAxisVisible(boolean visibility)
	{
		int iVisibility = (visibility) ? 1 : 0;
		
		originAxes.SetVisibility(iVisibility);
		canvas.RenderSecured();
	}
	
	public boolean isOriginAxisVisible()
	{
		return (originAxes.GetVisibility() == 0) ? false : true;
	}
	
	public boolean isRelativeAxisVisible()
	{
		return (relativeAxes.GetVisibility() == 0) ? false : true;
	}
	
	/**
	 * There is no function in VTK to copy a camera so created our proper copy of camera.
	 * WARNING : this function is not complete and function only if we manipulate the parameters that the function copy.
	 * @param c
	 * @return
	 */
	vtkCamera copy(vtkCamera c)
	{
		vtkCamera rep = new vtkCamera();
		rep.SetPosition(c.GetPosition());
		rep.SetFocalPoint(c.GetFocalPoint());
		rep.SetViewUp(c.GetViewUp());

		return rep;
	}

	/**
	 * Set the factor of the size of the origin axes. When the axes is very far
	 * it is scaled to the originAxesFactor where originAxesFactor define the size of
	 * the actor in function of the size of the viewport. A value of 1 will make
	 * the axes as large as the size of the viewport. By default this value is set to 1/15
	 * @param originAxesSizeFactor
	 */
	public void setOriginAxesSizeFactor(double originAxesSizeFactor)
	{
		this.originAxesFactor = originAxesSizeFactor;
	}
	
	private void refresh()
	{
		canvas.lock();
		renderer.ResetCamera();
		canvas.unlock();
		
		//RenderSecured with the new position of the camera
		canvas.RenderSecured();
	}

	/**
	 * Save the current camera
	 */
	public void saveCurrentCamera()
	{
		cameras.add(copy(renderer.GetActiveCamera()));
		ScreenShotCamera screenShot = new ScreenShotCamera();
		
		screenShot.shot(Utils.takeScreenShot(canvas));
		screenShots.add(screenShot);
	}

	public ImageIcon getScreenShotCamera(int index)
	{
		return screenShots.get(index).getSnapShot();
	}

	public void removeCamera(int index)
	{
		cameras.remove(index);
		screenShots.remove(index);
	}

	public int getNumberOfCameras()
	{
		return cameras.size();
	}
	
	public void setCameraOrientation(CameraManager.Orientation orientation)
	{
		canvas.lock();
		renderer.SetActiveCamera(copy(defaultCameras[orientation.ordinal()]));
		canvas.unlock();
		
		refresh();
	}
		
	public void fitAll()
	{
		canvas.lock();
		canvas.GetRenderer().ResetCamera();
		canvas.unlock();
		if(canvas.isWindowSet())
			canvas.RenderSecured();
	}
	
	/**
	 * Set the position rotation of the camera to the center of the bouding box
	 * of the cell selected
	 * 
	 * @param pickPosition 
	 */
	public void centerRotationSelection(Point pickPosition)
	{
		vtkCellPicker picker = new vtkCellPicker();
		canvas.getIren().SetPicker(picker);
		
		// TODO : calculate the diagonal in pixel of the renderer
		picker.SetTolerance(0.005);
		canvas.lock();
		picker.Pick(pickPosition.getX(), pickPosition.getY(), 0., canvas.GetRenderer());
		canvas.unlock();
		double[] position = picker.GetPickPosition();
		
		canvas.lock();
		canvas.GetRenderer().GetActiveCamera().SetFocalPoint(position);
		canvas.unlock();
	}

	public Canvas getCanvas()
	{
		return canvas;
	}

	public void setCamera(int index)
	{
		canvas.lock();
		renderer.SetActiveCamera(copy(cameras.get(index)));
		canvas.unlock();
		refresh();
	}
}
