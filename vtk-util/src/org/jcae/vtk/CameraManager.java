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
 * (C) Copyright 2008-2009, by EADS France
 */

package org.jcae.vtk;


import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import vtk.vtkActor;
import vtk.vtkActorCollection;
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
	private static class ScreenshotCamera
	{
		private ImageIcon snapshot;
		private final static int height = 100;
		private final static int width = 100;

		public ImageIcon getSnapshot()
		{
			return snapshot;
		}

		public void shot(BufferedImage aSnapshot)
		{
			int h = aSnapshot.getHeight();
			int w = aSnapshot.getWidth();

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
			g2D.drawRenderedImage(aSnapshot, t);
			snapshot = new ImageIcon(buffer);
		}
	}	
	
	// Different classic cameras
	private static final vtkCamera[] defaultCameras =
		new vtkCamera[Orientation.values().length];
	private final Canvas canvas;
	private final vtkRenderer renderer;
	private final ArrayList<vtkCamera> cameras = new ArrayList<vtkCamera>();
	private final ArrayList<ScreenshotCamera> screenshots =
		new ArrayList<ScreenshotCamera>();
	private final vtkAxesActor originAxes;
	private boolean isVisibleRelativeAxis = true;
	private final vtkAxesActor relativeAxes;
	private final vtkOrientationMarkerWidget marker;
	private double originAxesFactor = 0.2;
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
		
		// FIXME: I do not know why, but originAxes has 6
		// pickable actors whereas relativeAxes has none.
		// As originAxes.PickableOff() does nothing, try
		// another way.
		vtkActorCollection actors = new vtkActorCollection();
		originAxes.GetActors(actors);
		actors.InitTraversal();
		for (vtkActor prop; (prop = actors.GetNextActor()) != null; )
			prop.PickableOff();
		actors.Delete();
		actors = null;
		
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
			{0., 0., 1.},
			{0., 1., 0.},
			// BOTTOM
			{0., 0., -1.},
			{0., 1., 0.},
			// LEFT
			{0., -1., 0.},
			{0., 0., 1.},
			// RIGHT
			{0., 1., 0.},
			{0., 0., 1.},
			// FRONT
			{-1., 0., 0.},
			{0., 0., 1.},
			// BACK
			{1., 0., 0.},
			{0., 0., 1.}};

		for (int i = 0; i < Orientation.values().length; ++i)
		{
			defaultCameras[i] = new vtkCamera();
			defaultCameras[i].SetPosition(cameraOrientation[i * 2]);
			defaultCameras[i].SetViewUp(cameraOrientation[i * 2 + 1]);
		}
	}

	/** This method is called by the VTK JNI code. Do not remove. */
	private void renderEvent()
	{
		scaleOriginaAxis();
	}
	
	/** scale the origin axis to make his screen size constant */
	private void scaleOriginaAxis()
	{
		// Find the distance from the camera of the origin axes
		vtkCamera camera = renderer.GetActiveCamera();				
		double zDistance;
		if(camera.GetParallelProjection()!=0)
			zDistance = camera.GetParallelScale()/2;
		else
		{
			vtkTransform modelView = camera.GetViewTransformObject();
			double[] point = modelView.TransformDoublePoint(0, 0, 0);
			zDistance = Math.abs(point[2]) * originAxesFactor;
		}
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

	public void setRotationCenter(double x, double y, double z) 
	{
		double[] position = new double[]{x,y,z};
		canvas.lock();
		canvas.GetRenderer().GetActiveCamera().SetFocalPoint(position);
		canvas.RenderSecured();
		canvas.unlock();
	}
	
	public void zoomTo(float x, float y, float z, float r) 
	{
		double[] pos = new double[]{x,y,z};
		canvas.lock();
		canvas.GetRenderer().GetActiveCamera().SetFocalPoint(pos);
		double[] n=canvas.GetRenderer().GetActiveCamera().GetDirectionOfProjection();
		pos[0]-=r*n[0];
		pos[1]-=r*n[1];
		pos[2]-=r*n[2];
		canvas.GetRenderer().GetActiveCamera().SetPosition(pos);
		canvas.RenderSecured();
		canvas.unlock();
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
		try {
			cameras.add(copy(renderer.GetActiveCamera()));
			ScreenshotCamera screenshot = new ScreenshotCamera();
			screenshot.shot(Utils.takeScreenshot(canvas));
			screenshots.add(screenshot);
		} catch (IOException ex) {
			Logger.getLogger(CameraManager.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public ImageIcon getScreenshotCamera(int index)
	{
		return screenshots.get(index).getSnapshot();
	}

	public void removeCamera(int index)
	{
		cameras.remove(index);
		screenshots.remove(index);
	}

	public int getNumberOfCameras()
	{
		return cameras.size();
	}
	
	public void setCameraOrientation(CameraManager.Orientation orientation)
	{
		canvas.lock();
		vtkCamera current = renderer.GetActiveCamera();
		int parallel = current.GetParallelProjection();
		current.Delete();
		vtkCamera c = copy(defaultCameras[orientation.ordinal()]);
		c.SetParallelProjection(parallel);
		renderer.SetActiveCamera(c);
		c.Delete();
		canvas.unlock();		
		refresh();
	}
		
	public void fitAll()
	{
		canvas.lock();		
		int visibility = originAxes.GetVisibility();
		originAxes.SetVisibility(0);
		double[] bounds = canvas.GetRenderer().ComputeVisiblePropBounds();
		if(visibility != 0)
			for(int i = 0; i<3; i++)
			{
				if(bounds[i*2]>0)
					bounds[i*2] = 0;
				if(bounds[i*2+1]<0)
					bounds[i*2+1] = 0;
			}
		canvas.GetRenderer().ResetCamera(bounds);
		originAxes.SetVisibility(visibility);
		scaleOriginaAxis();
		canvas.GetRenderer().ResetCameraClippingRange();
		canvas.RenderSecured();
		canvas.unlock();
	}
	
	/**
	 * Set the position rotation of the camera to the center of the bouding box
	 * of the cell selected
	 * 
	 * @param pickPosition 
	 */
	public void centerRotationSelection(Point pickPosition)
	{
		//backup actors pickable status and set it to true
		vtkActorCollection vtkActors = canvas.GetRenderer().GetActors();
		vtkActors.InitTraversal();
		int n = vtkActors.GetNumberOfItems();
		vtkActor[] actors = new vtkActor[n];
		int[] pickableBackup = new int[n];
		int k = 0;
		while(k < n)
		{
			actors[k] = vtkActors.GetNextActor();
			pickableBackup[k] = actors[k].GetPickable();
			actors[k].SetPickable(1);
			k++;
		}
		vtkActors.Delete();

		vtkCellPicker picker = new vtkCellPicker();
		canvas.lock();
		picker.Pick(pickPosition.getX(), pickPosition.getY(), 0., canvas.GetRenderer());
		double[] position = picker.GetPickPosition();
		picker.Delete();
		canvas.GetRenderer().GetActiveCamera().SetFocalPoint(position);
		canvas.getIren().GetInteractorStyle();
		canvas.RenderSecured();
		canvas.unlock();

		//restore actor pickable status
		for(int i = 0; i<n; i++)
			actors[i].SetPickable(pickableBackup[i]);
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

	public boolean isParallelProjection()
	{
		vtkCamera c = renderer.GetActiveCamera();
		boolean toReturn = c.GetParallelProjection() != 0;
		c.Delete();
		return toReturn;
	}

	public void setParallelProjection(boolean b)
	{
		vtkCamera c = renderer.GetActiveCamera();
		c.SetParallelProjection(b ? 1 : 0);
		c.Delete();
	}
}
