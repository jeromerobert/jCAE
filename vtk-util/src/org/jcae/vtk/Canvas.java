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


import java.awt.Dimension;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Timer;
import java.util.TimerTask;
import vtk.vtkActorCollection;
import vtk.vtkCanvas;
import vtk.vtkInteractorStyleTrackballCamera;
import vtk.vtkMapper;

/**
 * This class is temporary. It permits to correct bugs on VTK.
 * See :
 * http://www.vtk.org/Bug/view.php?id=6268
 * http://ij-plugins.svn.sourceforge.net/viewvc/ij-plugins/trunk/VTK-Examples/Wrapping/Java/vtk/util/VtkPanelUtil.java?view=markup
 * @author ibarz
 */
//TODO manage 3 vtkRenderer/layer:
//1 - one for normal rendering
//2 - one for picking rendering
//3 - one for fast overlay rendering after picking
//vtkRenderer 2 and 3 should be dynamically created when needed and not keeped in
//the canvas. Beware to share vtkCamera. Beware of CameraClippingRange
public class Canvas extends vtkCanvas
{
	private static final TimerTask garbageCall = new TimerTask()
	{

		@Override
		public void run()
		{
			System.gc();
		/*System.out.println("GC ! NUMBER OF VTK OBJECTS : " + vtkGlobalJavaHash.PointerToReference.size());
		Map map = vtkGlobalJavaHash.PointerToReference;
		
		TObjectIntHashMap<String> mapper = new TObjectIntHashMap<String>();
		for(Object ob : map.values())
		{
		Object obj= ((WeakReference)ob).get();
		
		String name = "null";
		if(obj != null)
		{
		name = obj.getClass().getSimpleName();		
		int count = mapper.get(name);
		mapper.put(name, ++count);
		}
		// If the object was removed remove its entry from the global hash
		else map.remove(ob);
		}
		TObjectIntIterator<String> iter = mapper.iterator();
		while(iter.hasNext())
		{
		iter.advance();
		System.out.println(iter.key() + " : " + iter.value());
		}*/
		}
	};
	private static final Timer timerGC = new Timer();
	

	static
	{
		timerGC.schedule(garbageCall, 1000, 30000);
	}

	public Canvas()
	{
		addMouseWheelListener(new MouseWheelListener() {
			public void mouseWheelMoved(MouseWheelEvent e) {
				Lock();
				if(e.getWheelRotation() > 0)
					iren.MouseWheelForwardEvent();
				else
					iren.MouseWheelBackwardEvent();
				UnLock();
			}
		});
		setMinimumSize(new Dimension(0, 0));
		setPreferredSize(new Dimension(0, 0));
		vtkInteractorStyleTrackballCamera style =
				new vtkInteractorStyleTrackballCamera();
		style.AutoAdjustCameraClippingRangeOn();
		getIren().SetInteractorStyle(style);
		style.Delete();
	}

	/**
	 * Correct a bug : update the reference of camera.
	 * If we change the original camera the light does not follow the camera anymore.
	 * @see http://www.vtk.org/Bug/view.php?id=6913
	 */
	@Override
	public void UpdateLight()
	{
		if (LightFollowCamera == 0)
			return;

		cam = GetRenderer().GetActiveCamera();

		super.UpdateLight();
	}

	/**
	 * Override to correct the bug of the UpdateLight (the light position is not updated
	 * if the camera is moved by programming.
	 * @see http://www.vtk.org/Bug/view.php?id=6913
	 */
	public void RenderSecured()
	{
		if (!isWindowSet())
			return;

		Utils.goToAWTThread(new Runnable()
				{

					@Override
					public void run()
					{
						Render();
					}
				});
	}

	/**
	 * Override to correct the bug of the UpdateLight (the light position is not updated
	 * if the camera is moved by programming.
	 * @see http://www.vtk.org/Bug/view.php?id=6913 [^]
	 */
	@Override
	public void Render()
	{
		if (!rendering)
		{
			rendering = true;
			if (ren.VisibleActorCount() == 0)
			{
				rendering = false;
				return;
			}
			if (rw != null)
			{
				if (windowset == 0)
				{
					// set the window id and the active camera
					if (lightingset == 0)
					{
						ren.AddLight(lgt);
						lightingset = 1;
					}
					RenderCreate(rw);
					Lock();
					rw.SetSize(getWidth(), getHeight());
					UnLock();
					windowset = 1;
				}
				UpdateLight();
				Lock();
				rw.Render();
				UnLock();
				rendering = false;
			}
		}
	}

	/**
	 * Workaround a bug with JSplitterPane on Windows.
	 * On Windows, JSplitterPane with no continous layout makes
	 * the vtkPanel undrawable when split bar is moved.
	 * As a workaround, do not call super.removeNotify() on
	 * Windows.  This method must be removed when vtk is fixed.
	 * @see http://www.vtk.org/Bug/view.php?id=7107
	 */
	@Override
	public void removeNotify()
	{
		String osName = System.getProperty("os.name");
		if (!osName.contains("Windows"))
			super.removeNotify();
	}

	/**
	 * Workaround for http://www.vtk.org/Bug/view.php?id=6268
	 * Pass through the case the rendering window is not linked to the canvas
	 * because it's created (see vtkPanel constructor). 
	 *
	 * @param width  the new width of this component in pixels.
	 * @param height the new height of this component in pixels
	 */
	@Override
	public void setSize(int x, int y)
	{
		super.setSize(x, y);
		Lock();
		rw.SetSize(x, y);
		iren.SetSize(x, y);
		iren.ConfigureEvent();
		UnLock();
	}

	@Override
	public void lock()
	{
		if (isWindowSet())
			super.lock();
	}

	@Override
	public void unlock()
	{
		if (isWindowSet())
			super.unlock();
	}

	/** Disable keyboard event handling in VTK, use only Java*/
	@Override
	public void keyPressed(KeyEvent e)
	{
	}

	/**
	 * Set the ImmediadeRenderingMode on the current view
	 * @param mode
	 */
	public void setImmediateRenderingMode(boolean mode) {
		
		vtkMapper mapper;
		vtkActorCollection listOfActors = GetRenderer().GetActors();
		int nbActors = listOfActors.GetNumberOfItems();

		listOfActors.InitTraversal();
		for (int i = 0; i < nbActors; ++i)
		{
			//browsing the list of actors and getting their associated mappers
			mapper = listOfActors.GetNextActor().GetMapper();
			mapper.SetImmediateModeRendering(Utils.booleanToInt(mode));
		}
		listOfActors.Delete();
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (ren.VisibleActorCount() == 0)
			return;
		
		Lock();
		rw.SetDesiredUpdateRate(5.0);
		lastX = e.getX();
		lastY = e.getY();

		ctrlPressed = (e.getModifiers() & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK ? 1 : 0;
		shiftPressed = (e.getModifiers() & InputEvent.SHIFT_MASK) == InputEvent.SHIFT_MASK ? 1 : 0;

		iren.SetEventInformationFlipY(e.getX(), e.getY(),
			ctrlPressed, shiftPressed, '0', 0, "0");

		if ((e.getModifiers() & InputEvent.BUTTON1_MASK) == InputEvent.BUTTON1_MASK)
			iren.LeftButtonPressEvent();
		else if ((e.getModifiers() & InputEvent.BUTTON2_MASK) == InputEvent.BUTTON2_MASK)
			iren.RightButtonPressEvent();
		else if ((e.getModifiers() & InputEvent.BUTTON3_MASK) == InputEvent.BUTTON3_MASK)
			iren.MiddleButtonPressEvent();
		
		UnLock();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		rw.SetDesiredUpdateRate(0.01);

		ctrlPressed = (e.getModifiers() & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK ? 1 : 0;
		shiftPressed = (e.getModifiers() & InputEvent.SHIFT_MASK) == InputEvent.SHIFT_MASK ? 1 : 0;

		iren.SetEventInformationFlipY(e.getX(), e.getY(),
			ctrlPressed, shiftPressed, '0', 0, "0");

		if ((e.getModifiers() & InputEvent.BUTTON1_MASK) ==
			InputEvent.BUTTON1_MASK) {
			Lock();
			iren.LeftButtonReleaseEvent();
			UnLock();
		}

		if ((e.getModifiers() & InputEvent.BUTTON2_MASK) ==
			InputEvent.BUTTON2_MASK) {
			Lock();
			iren.RightButtonReleaseEvent();
			UnLock();
		}

		if ((e.getModifiers() & InputEvent.BUTTON3_MASK) ==
			InputEvent.BUTTON3_MASK) {
			Lock();
			iren.MiddleButtonReleaseEvent();
			UnLock();
		}
	}
}
