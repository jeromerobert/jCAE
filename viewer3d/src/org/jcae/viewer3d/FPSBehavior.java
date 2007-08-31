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

import javax.media.j3d.Behavior;
import javax.media.j3d.WakeupOnElapsedFrames;

/**
 * A behavior to measure FPS.
 * It's a very simplified version of the class found here
 * http://www.java-tips.org/other-api-tips/java3d/how-to-create-a-frames-per-second-counter.html
 * @author Jerome Robert
 */
public class FPSBehavior extends Behavior
{
	// Wakeup condition - framecount = 0 -> wakeup on every frame
	private WakeupOnElapsedFrames FPSwakeup = new WakeupOnElapsedFrames(0);
	// Counter for number of frames rendered
	private int numframes = 0;
	// Report frame rate after maxframe number of frames have been rendered
	private int maxframes = 50;
	private long lasttime = 0;

	private double currentFPS;

	public FPSBehavior()
	{
		setEnable(true);
	}
 
	/**
	 * Called to init the behavior
	 * @see Behavior.initialize
	 */
	public void initialize()
	{
		numframes=-1;
		// Set the trigger for the behavior to wakeup on every frame rendered
		wakeupOn(FPSwakeup);
	}
 
	/**
	 * Called every time the behavior is activated
	 * @see Behavior.processStimulus
	 */
	public void processStimulus(java.util.Enumeration critera)
	{	
		long currtime=System.currentTimeMillis();
		numframes++;
		
		//init
		if(numframes==0)
		{
			lasttime=currtime;
		}
		else
		{
			if(numframes>=maxframes)
			{			
				currentFPS=numframes/((double)currtime-lasttime)*1000;
				firePropertyChangeListenerPropertyChange(this, null, null, currentFPS);
				numframes=0;
				lasttime=currtime;
			}
		}
		// Set the trigger for the behavior
		wakeupOn(FPSwakeup);
	}

	/**
	 * Utility field holding list of PropertyChangeListeners.
	 */
	private transient java.util.ArrayList propertyChangeListenerList;

	/**
	 * Registers PropertyChangeListener to receive events.
	 * @param listener The listener to register.
	 */
	public synchronized void addPropertyChangeListener(java.beans.PropertyChangeListener listener)
	{
		if (propertyChangeListenerList == null ) {
			propertyChangeListenerList = new java.util.ArrayList ();
		}
		propertyChangeListenerList.add (listener);
	}

	/**
	 * Removes PropertyChangeListener from the list of listeners.
	 * @param listener The listener to remove.
	 */
	public synchronized void removePropertyChangeListener(java.beans.PropertyChangeListener listener)
	{
		if (propertyChangeListenerList != null ) {
			propertyChangeListenerList.remove (listener);
		}
	}

	/**
	 * Notifies all registered listeners about the event.
	 * 
	 * @param object Parameter #1 of the <CODE>PropertyChangeEvent<CODE> constructor.
	 * @param string Parameter #2 of the <CODE>PropertyChangeEvent<CODE> constructor.
	 * @param object0 Parameter #3 of the <CODE>PropertyChangeEvent<CODE> constructor.
	 * @param object1 Parameter #4 of the <CODE>PropertyChangeEvent<CODE> constructor.
	 */
	private void firePropertyChangeListenerPropertyChange(java.lang.Object object, java.lang.String string, java.lang.Object object0, java.lang.Object object1)
	{
		java.util.ArrayList list;
		java.beans.PropertyChangeEvent e = new java.beans.PropertyChangeEvent (object, string, object0, object1);
		synchronized (this) {
			if (propertyChangeListenerList == null) return;
			list = (java.util.ArrayList)propertyChangeListenerList.clone ();
		}
		for (int i = 0; i < list.size (); i++) {
			((java.beans.PropertyChangeListener)list.get (i)).propertyChange (e);
		}
	}

	/**
	 * Return the last measurement 
	 */
	public double getFPS()
	{
		return currentFPS;
	}
}
