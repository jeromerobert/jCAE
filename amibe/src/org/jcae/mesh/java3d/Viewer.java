/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2003,2004,2005
                  Jerome Robert <jeromerobert@users.sourceforge.net>

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */

package org.jcae.mesh.java3d;

import java.awt.event.*;
import java.awt.Frame;
import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.Graphics2D;
import java.util.*;
import javax.swing.*;
import javax.media.j3d.*;
import javax.vecmath.*;
import com.sun.j3d.utils.universe.*;
import com.sun.j3d.loaders.*;
import com.sun.j3d.utils.geometry.*;
import com.sun.j3d.utils.picking.*;
import com.sun.j3d.utils.behaviors.vp.*;
import org.jcae.mesh.*;
import org.apache.log4j.Logger;

/** This is the panel which include the J3DCanvas for 3D display. It should be set
 * in an independant Windows (JDialog, JFrame), to avoid averlaping with swing
 * component.
 * @author Jerome Robert
 */

public class Viewer extends JFrame
{	
	private static Logger logger=Logger.getLogger(Viewer.class);
	private BorderLayout borderLayout = new BorderLayout();
	protected Canvas3D canvas3D;
	private SimpleUniverse universe;
	private BranchGroup mainBranchGroup;
	private double [] lastClick = null;
	private char lastKey = 0;
	
	/** The constructor */	
	public Viewer()
	{
		getContentPane().setLayout(borderLayout);
		canvas3D=new Canvas3D(SimpleUniverse.getPreferredConfiguration());
		canvas3D.addMouseListener(new java.awt.event.MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				canvas3D_mouseClicked(e);
			}
		});
		canvas3D.addKeyListener(new java.awt.event.KeyAdapter()
		{
			public void keyPressed(KeyEvent e)
			{
				canvas3D_keyPressed(e);
			}
		});
		getContentPane().add(canvas3D,BorderLayout.CENTER);
		universe=new SimpleUniverse(canvas3D);
		mainBranchGroup=new BranchGroup();		
		mainBranchGroup.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		mainBranchGroup.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		mainBranchGroup.setCapability(BranchGroup.ALLOW_DETACH);
		mainBranchGroup.setCapability(BranchGroup.ALLOW_BOUNDS_READ);
		universe.addBranchGraph(createLights(new BoundingSphere(new Point3d(),Double.MAX_VALUE)));		
		universe.addBranchGraph(mainBranchGroup);
		setSize(800,600);
    }		

	public void addBranchGroup(BranchGroup branchGroup)
	{
		branchGroup.setCapability(BranchGroup.ALLOW_DETACH);
		mainBranchGroup.addChild(branchGroup);
	}
	
	public void removeAllBranchGroup()
	{
		mainBranchGroup.removeAllChildren();
	}

	public Runnable callBack = null;
	void canvas3D_mouseClicked(MouseEvent e)
	{
		try
		{
			PickCanvas pickCanvas = new PickCanvas(canvas3D,universe.getLocale());
			pickCanvas.setTolerance(0);
			pickCanvas.setShapeLocation(e);
			pickCanvas.setMode(PickTool.GEOMETRY_INTERSECT_INFO);
			PickResult pr=pickCanvas.pickClosest();
			if(pr!=null)
			{
				double distmin=pr.getIntersection(0).getDistance();
				int closest=0;
				for (int t=1;t<pr.numIntersections();t++)
				{
					double dist = pr.getIntersection(t).getDistance();
					if (dist < distmin)
					{
						dist = distmin;
						closest = t;
					}
				}
				PickIntersection pi=pr.getIntersection(closest);
				logger.debug("closest vertex index="+closest);
				Point3d pickPoint=pi.getPointCoordinates();
				lastClick = new double[3];
				lastClick[0] = pickPoint.x;
				lastClick[1] = pickPoint.y;
				lastClick[2] = pickPoint.z;
				if (callBack != null)
					callBack.run();
				lastClick = null;
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace(System.out);
		}
	}
	
	/** Return the position of last click */
	public double [] getLastClick()
	{
		return lastClick;
	}
	
	void canvas3D_keyPressed(KeyEvent e)
	{
		try
		{
			lastKey = e.getKeyChar();
			if (callBack != null)
				callBack.run();
			lastKey = 0;
		}
		catch(Exception ex)
		{
			ex.printStackTrace(System.out);
		}
	}
	
	public char getLastKey()
	{
		return lastKey;
	}
	
	/** Fit all the scenes in the current view */	
	public void zoomTo()
	{
		BoundingSphere b= (BoundingSphere)mainBranchGroup.getBounds();
		Point3d c=new Point3d();
		b.getCenter(c);
		zoomTo((float)c.x,(float)c.y,(float)c.z,(float)b.getRadius());
	}
	
	/** Modify the view to best see what is include a given sphere
	 * @param x x coordinate of the center of the sphere
	 * @param y y coordinate of the center of the sphere
	 * @param z z coordinate of the center of the sphere
	 * @param radius radius of the sphere
	 */
	public void zoomTo(float x, float y, float z, float radius)
	{
		OrbitBehavior orbit;
		Point3d c=new Point3d(x,y,z);
		BoundingSphere b=new BoundingSphere(c,radius);
		orbit = new OrbitBehavior(canvas3D, OrbitBehavior.REVERSE_ALL);
		orbit.setBounds(b);
		orbit.setRotationCenter(c);
		orbit.setZoomFactor(b.getRadius());
		orbit.setTransFactors(b.getRadius(),b.getRadius());
		orbit.setSchedulingBounds(new BoundingSphere(c,b.getRadius()*100));
		universe.getViewingPlatform().setViewPlatformBehavior(orbit);
		
		View view=universe.getViewer().getView();
		view.setFrontClipDistance(0.001*radius);
		view.setBackClipDistance(10*radius);

		Transform3D t3d = new Transform3D();
		universe.getViewingPlatform().getViewPlatformTransform().getTransform(t3d);
		//calculate the translation vector for a identity rotation matrix
		Vector3f v=new Vector3f(x,y,3f*radius+z);
		//rotate the translation vector
		t3d.transform(v);
		t3d.setTranslation(v);
		universe.getViewingPlatform().getViewPlatformTransform().setTransform(t3d);
		orbit.setViewingPlatform(universe.getViewingPlatform());		
	}
	
	private BranchGroup createLights(Bounds bounds)
	{
	    BranchGroup gp=new BranchGroup();
		// Set up the ambient light
		Color3f	ambientColor = new Color3f(0.1f, 0.1f, 0.1f);
		AmbientLight ambientLightNode = new AmbientLight(ambientColor);
		ambientLightNode.setInfluencingBounds(bounds);
		gp.addChild(ambientLightNode);

		// Set up the directional lights
		Color3f	light1Color = new Color3f(1.0f, 1.0f, 0.9f);
		Vector3f light1Direction = new Vector3f(1.0f, 1.0f, 1.0f);
		DirectionalLight light1 = new DirectionalLight(light1Color, light1Direction);
		light1.setInfluencingBounds(bounds);
		gp.addChild(light1);

		Color3f	light2Color = new Color3f(1.0f, 1.0f, 1.0f);
		Vector3f light2Direction = new Vector3f(-1.0f, -1.0f, -1.0f);
		DirectionalLight light2 = new DirectionalLight(light2Color, light2Direction);
		light2.setInfluencingBounds(bounds);
		light2.setCapability(light2.ALLOW_INFLUENCING_BOUNDS_WRITE);
		light2.setCapability(light2.ALLOW_INFLUENCING_BOUNDS_READ);
		gp.addChild(light2);
		return gp;
	}		
}
