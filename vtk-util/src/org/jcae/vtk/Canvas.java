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

import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.SwingUtilities;
import vtk.vtkCanvas;
import vtk.vtkGlobalJavaHash;
import vtk.vtkInteractorStyleTrackballCamera;

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
	private Thread creationWindowThread = null;
	private static TimerTask garbageCall = new TimerTask() {
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
	private static Timer timerGC = new Timer();
	
	static {
		timerGC.schedule(garbageCall, 1000, 30000);
	}
	public Canvas()
	{
		setMinimumSize(new Dimension(0, 0));
		setPreferredSize(new Dimension(0, 0));
		vtkInteractorStyleTrackballCamera style =
			new vtkInteractorStyleTrackballCamera();
		style.AutoAdjustCameraClippingRangeOn();
		getIren().SetInteractorStyle(style);
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
		if(!isWindowSet())
			return;
		
		if (!SwingUtilities.isEventDispatchThread())
		{
			//Thread.dumpStack();
			/*System.err.println(
				"WARNING ! : you try to render on a different thread than the"+
				"thread that creates the renderView. Making an invokeLater to"+
				" render on the thread that creates the renderView");*/
			try{
			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					Render();
				}
			});
			}catch(Exception e)
			{
				System.out.println("Exception invokeAndWait : " + e.getLocalizedMessage());
			}
		}
		else Render();
	}

	/**
	 * JSplitPane with not continous layout make the vtkPanel undrawable if
	 * do not empty this method.
	 */
	@Override
	public void removeNotify()
	{
		// Do this workaround only for windows platforms
		String osName = System.getProperty("os.name");
		if(!osName.contains("Windows"));
			super.removeNotify();
		//Thread.dumpStack();
		//System.out.println("REMOVE NOTIFY "+getParent());
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
		if(isWindowSet())
			super.lock();
	}
	
	@Override
	public void unlock()
	{
		if(isWindowSet())
			super.unlock();
	}

	/** Disable keyboard event handling in VTK, use only Java*/
	@Override
	public void keyPressed(KeyEvent e)
	{
	}
}
