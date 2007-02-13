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

import gnu.trove.TIntHashSet;
import java.awt.Rectangle;
import java.util.Enumeration;
import javax.media.j3d.*;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import javax.vecmath.Point3d;
import org.jcae.mesh.oemm.OEMM;
import org.jcae.mesh.oemm.OEMMViewer;
import org.jcae.mesh.oemm.Storage;
import org.jcae.viewer3d.bg.ViewableBG;
import org.jcae.viewer3d.cad.ViewableCAD;
import org.jcae.viewer3d.cad.occ.OCCProvider;

/**
 * Dynamically hide and show voxel in a OEMM viewer
 */
public class OEMMBehavior extends Behavior
{	
	interface ChangeListener
	{
		void stateChanged(OEMMBehavior behaviour);
	}
	
	/** Utility field holding list of ChangeListeners. */
	private transient java.util.ArrayList changeListenerList;
	
	/**
	 * The square of the minimal distance between the eye and a displayed
	 * OEMM voxel
	 */ 
	private double d2limit;
	private Viewable decMesh;
	private OEMM oemm;
	private boolean oemmActive;
	private View view;
	private Point3d[] voxels;
	
	private WakeupCriterion wakeupFrame;
	
	private WakeupCriterion wakeupTransf;

	public OEMMBehavior(View canvas, OEMM oemm)
	{
		setSchedulingBounds(new BoundingSphere(
			new Point3d(), Double.MAX_VALUE));
		double[] coords=oemm.getCoords(true);
		view=canvas;
		voxels=new Point3d[coords.length/6/4/3];
		for(int i=0; i<voxels.length; i++)
		{
			int n=6*4*3*i;
			voxels[i]=new Point3d(
				(coords[n+0]+coords[n+6*4*3-6])/2,
				(coords[n+1]+coords[n+6*4*3-5])/2,
				(coords[n+2]+coords[n+6*4*3-4])/2);
		}
		this.oemm=oemm;
		d2limit=2*(coords[0]-coords[6*4*3-6]);
		wakeupFrame=new WakeupOnElapsedFrames(60);
		wakeupTransf=new WakeupOnTransformChange(
			view.getViewingPlatform().getViewPlatformTransform());
	}
	
	/**
	 * Registers ChangeListener to receive events.
	 * @param listener The listener to register.
	 */
	public synchronized void addChangeListener(ChangeListener listener)
	{
		if (changeListenerList == null ) {
			changeListenerList = new java.util.ArrayList ();
		}
		changeListenerList.add (listener);
	}		

	/**
	 * Notifies all registered listeners about the event.
	 * @param object Parameter #1 of the <CODE>ChangeEvent<CODE> constructor.
	 */
	private void fireChangeListenerStateChanged()
	{
		java.util.ArrayList list;
		synchronized (this) {
			if (changeListenerList == null) return;
			list = (java.util.ArrayList)changeListenerList.clone ();
		}
		for (int i = 0; i < list.size (); i++) {
			((ChangeListener)list.get (i)).stateChanged(this);
		}
	}

	public void initialize()
	{
		d2limit=d2limit/Math.tan(view.getView().getFieldOfView()/2);
		d2limit=d2limit*d2limit;
		wakeupOn(wakeupTransf);			
	}

	public boolean isOemmActive()
	{
		return oemmActive;
	}

	public void processStimulus(Enumeration arg0)
	{
		if(arg0.nextElement() instanceof WakeupOnTransformChange)
		{
			wakeupOn(wakeupFrame);
			return;
		}
		
		ViewPyramid vp=new ViewPyramid(view, scaleRectangle(view.getBounds(), 2));
		
		TIntHashSet ids=new TIntHashSet();
		for(int i=0; i<voxels.length; i++)
		{
			if(vp.intersect(voxels[i]) &&
				voxels[i].distanceSquared(vp.getEye())<d2limit)
					ids.add(i);
		}

		view.remove(decMesh);

		oemmActive=ids.size()>0;
		if(ids.size()>0)
		{
			decMesh = new ViewableBG(OEMMViewer.meshOEMM(oemm, ids));                
			view.add(decMesh);
		}
		fireChangeListenerStateChanged();
		wakeupOn(wakeupTransf);
	}

	/**
	 * Removes ChangeListener from the list of listeners.
	 * @param listener The listener to remove.
	 */
	public synchronized void removeChangeListener(ChangeListener listener)
	{
		if (changeListenerList != null ) {
			changeListenerList.remove (listener);
		}
	}
	
	/** 
	 * @param rectangle The rectangle to scale
	 * @param factor The factor to apply
	 * @return The rectangle which was specified as input
	 */
	private Rectangle scaleRectangle(Rectangle rectangle, double factor)
	{
		double k=(1-factor)/2;
		rectangle.x += rectangle.width*k;
		rectangle.y += rectangle.height*k;
		rectangle.width = (int) (rectangle.width*factor);
		rectangle.height = (int) (rectangle.height*factor);
		return rectangle;
	}
	
	/** Test / example */
	public static void main(String[] args)
	{
		JFrame f=new JFrame("jcae-viewer3d-fd demo");
		f.setSize(800,600);
		f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		final View view=new View(f);
		f.getContentPane().add(view);		
		
		OEMM oemm = Storage.readOEMMStructure("/home/jerome/JCAEProject/new1.oemm");
		ViewableCAD vcad=new ViewableCAD(new OCCProvider("/home/jerome/Models/flight_solid.brep"));
		
		BranchGroup bg=new BranchGroup();
		OEMMBehavior oemmBehavior=new OEMMBehavior(view, oemm);		
		bg.addChild(oemmBehavior);
		
		view.addBranchGroup(bg);
		view.add(vcad);
		view.setOriginAxisVisible(true);
		view.fitAll();
		f.setVisible(true);
	}	
}
