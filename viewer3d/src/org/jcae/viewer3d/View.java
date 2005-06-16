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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.Map.Entry;
import javax.media.j3d.*;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.vecmath.*;
import org.jdesktop.j3d.utils.behaviors.vp.AxisBehavior;
import com.sun.j3d.utils.behaviors.vp.OrbitBehavior;
import com.sun.j3d.utils.picking.PickResult;
import com.sun.j3d.utils.universe.PlatformGeometry;
import com.sun.j3d.utils.universe.SimpleUniverse;
import com.sun.j3d.utils.universe.Viewer;
import com.sun.j3d.utils.universe.ViewingPlatform;

/**
 * An AWT component wich display Viewable in a Java3D canvas.
 * This class is responsible for handling picking and refresh events.
 * The expected navigation behavior is the one of the OrbitBehavior of Java3D.
 * Multiple selection is available using ctrl+Left click.
 * @author Jerome Robert
 * @todo all methods must be implemented. public methods may be added.
 */
public class View extends Canvas3D implements PositionListener
{
	static JTextPane textPane;
	private Switch originAxisSwitch=new Switch(Switch.CHILD_NONE);
	private Switch fixedAxisSwitch=new Switch(Switch.CHILD_NONE);
	private TransformGroup fixedAxisTransformGroup=new TransformGroup();
	private TransformGroup originAxisTransformGroup=new TransformGroup();
	private PickResult lastPickResult;
	
	private View navigationMaster;
	private List positionListeners=Collections.synchronizedList(new ArrayList());
	
	private BufferedImage snapShot;
	private Object snapShotLock=new Object();
	private boolean takeSnapShot;
	
	static private SimpleUniverse universe;
	static private Map viewableToViewSpecificGroup=Collections.synchronizedMap(new HashMap());
	private ViewingPlatform viewingPlatform;
	private BranchGroup axisBranchGroup=new BranchGroup();
	private Viewable currentViewable;
	private OrbitBehavior orbit= new ViewBehavior(this);
	private AxisBehavior axisBehavior;
	
	/** The constructor */	
	public View()
	{
		this(false);
	}

	/** The constructor */	
	public View(boolean offscreen)
	{		
		super(SimpleUniverse.getPreferredConfiguration(), offscreen);
		
		if(offscreen)
		{
			getScreen3D().setPhysicalScreenWidth(0.0254/90.0 * 1600);
			getScreen3D().setPhysicalScreenHeight(0.0254/90.0 * 1200); 
		}
		
		if(universe==null)
		{
			universe=new SimpleUniverse(this);
			viewingPlatform=universe.getViewingPlatform();			
			universe.addBranchGraph(createLights(new BoundingSphere(new Point3d(),Double.MAX_VALUE)));
		}
		else
		{
			viewingPlatform=createViewingPlatform();
			universe.getLocale().addBranchGraph(viewingPlatform);
		}

		orbit.setCapability(Node.ALLOW_BOUNDS_WRITE);
		viewingPlatform.setViewPlatformBehavior(orbit);		
		
		originAxisSwitch.setCapability(Switch.ALLOW_SWITCH_READ);
		originAxisSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
		fixedAxisSwitch.setCapability(Switch.ALLOW_SWITCH_READ);
		fixedAxisSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
		fixedAxisTransformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		
		// Create the origin axis
		createAxis(originAxisTransformGroup);
		originAxisSwitch.addChild(originAxisTransformGroup);			
		ViewSpecificGroup vsp=new ViewSpecificGroup();				
		vsp.addView(getView());		
		vsp.addChild(originAxisSwitch);
		axisBranchGroup.addChild(vsp);
		universe.addBranchGraph(axisBranchGroup);
		
		// Create the fixed axis		
		final TransformGroup tg=new TransformGroup();
		tg.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		tg.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		Transform3D t3d=new Transform3D();
		t3d.set(new Vector3d(-4, -4, -40));
		createAxis(fixedAxisTransformGroup);		
		tg.setTransform(t3d);
		tg.addChild(fixedAxisTransformGroup);
		fixedAxisSwitch.addChild(tg);
		PlatformGeometry pg=new PlatformGeometry();
		vsp=new ViewSpecificGroup();
		vsp.addView(getView());
		vsp.addChild(fixedAxisSwitch);
		pg.addChild(vsp);		
		axisBehavior=new AxisBehavior(fixedAxisTransformGroup,
			viewingPlatform.getViewPlatformTransform());
		pg.addChild(axisBehavior);
		viewingPlatform.setPlatformGeometry(pg);
		addComponentListener(new ComponentAdapter()
		{
			private Transform3D myT3d=new Transform3D();
			public void componentResized(ComponentEvent e)
			{
				tg.getTransform(myT3d);
				myT3d.set(new Vector3d(-4, -4*((float)getHeight())/getWidth(), -40));
				tg.setTransform(myT3d);
			}
		});

		zoomTo(0,0,0,1.0f);		
    }		
	
	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	protected void finalize() throws Throwable
	{
		Viewable[] vs=getViewables();
		for(int i=0; i<vs.length; i++)
		{
			remove(vs[i]);
		}
		universe.getLocale().removeBranchGraph(axisBranchGroup);
	}
	
	public TransformGroup getOriginAxisTransformGroup()
	{
		return originAxisTransformGroup;
	}
	/**
	 * @return
	 */
	private ViewingPlatform createViewingPlatform()
	{
		ViewingPlatform vp=new ViewingPlatform();
		vp.setUniverse(universe);
		Viewer viewer=new Viewer(this);
		viewer.setViewingPlatform(vp);
		return vp;		
	}

	/** Add a Viewable to the current view */
	public void add(Viewable viewable)
	{
		ViewSpecificGroup vsg=(ViewSpecificGroup) viewableToViewSpecificGroup.get(viewable);
		if(vsg==null)
		{
			Node node=viewable.getJ3DNode();
			BranchGroup parent=new BranchGroup();
			parent.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
			parent.setCapability(BranchGroup.ALLOW_DETACH);
			parent.setCapability(BranchGroup.ALLOW_BOUNDS_READ);
			parent.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);			
			vsg=new ViewSpecificGroup();
			vsg.setCapability(ViewSpecificGroup.ALLOW_VIEW_WRITE);
			vsg.setCapability(ViewSpecificGroup.ALLOW_VIEW_READ);
			vsg.setCapability(Node.ALLOW_BOUNDS_READ);
			vsg.setUserData(parent);
			viewableToViewSpecificGroup.put(viewable, vsg);
			vsg.addChild(node);
			parent.addChild(vsg);
			universe.getLocale().addBranchGraph(parent);
		}
		vsg.addView(getView());
		if(currentViewable==null)
			currentViewable=viewable;
	}

	/**
	 * Allow to had a custom branchgroup to the view
	 * @param branchGroup
	 */
	public void addBranchGroup(BranchGroup branchGroup)
	{
		universe.getLocale().addBranchGraph(branchGroup);
	}
	
	/**
	 * @param view
	 */
	public void addPositionListener(PositionListener listener)
	{
		positionListeners.add(listener);
	}
	
	private Node createAxis(TransformGroup transformGroup)
	{
		float[] f=new float[] {
			0, 0, 0,           1, 0, 0, // x line
			0.9f, 0.1f, 0,     1, 0, 0, // x arrow 1    
			0.9f, -0.1f, 0,    1, 0, 0, // x arrow 2    
			0.9f, 0, 0.1f,     1, 0, 0, // x arrow 3    
			0.9f, 0, -0.1f,    1, 0, 0, // x arrow 1    
			0, 0, 0,           0, 1, 0, // y line
			0.1f, 0.9f, 0,     0, 1, 0, // y arrow 1    
			-0.1f, 0.9f, 0,    0, 1, 0, // y arrow 2    
			0, 0.9f, 0.1f,     0, 1, 0, // y arrow 3    
			0, 0.9f, -0.1f,    0, 1, 0, // y arrow 4    
			0, 0, 0,           0, 0, 1, // z line
			0.1f, 0, 0.9f,     0, 0, 1, // z arrow 1    
			-0.1f, 0, 0.9f,    0, 0, 1, // z arrow 2    
			0, 0.1f, 0.9f,     0, 0, 1, // z arrow 3    
			0, -0.1f, 0.9f,    0, 0, 1 // z arrow 4    
		};
		LineArray la = new LineArray(f.length/3,
			LineArray.COORDINATES);
		la.setCoordinates(0, f);
		Appearance a = new Appearance();
		Color3f color=new Color3f(0.5f, 0.5f, 0.7f);
		ColoringAttributes ca = new ColoringAttributes(color,
			ColoringAttributes.FASTEST);
		a.setColoringAttributes(ca);
		Shape3D s3d = new Shape3D(la);
		s3d.setAppearance(a);
		transformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		transformGroup.addChild(s3d);		
		Font font=new JPanel().getFont();
		transformGroup.addChild(new RasterTextLabel("x", Color.WHITE, 1.1f, 0, 0));
		transformGroup.addChild(new RasterTextLabel("y", Color.WHITE, 0, 1.1f, 0));
		transformGroup.addChild(new RasterTextLabel("z", Color.WHITE, 0, 0, 1.1f));
		transformGroup.setCapability(Node.ALLOW_BOUNDS_READ);
		return transformGroup;		
	}
	
	private Node createLabel(String label, float x, float y, float z, Color color, Font font)
	{
		//Compute the size of the string
		/*BufferedImage image=new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d=(Graphics2D) image.getGraphics();
		Rectangle2D dim=font.getStringBounds(label, g2d.getFontRenderContext());		
		System.out.println(dim);*/
		//Create the image
		BufferedImage image=new BufferedImage(8, 15, BufferedImage.TYPE_INT_ARGB_PRE);		
		Graphics2D g2d=(Graphics2D) image.getGraphics();
		g2d.setFont(font);
		g2d.drawString(label, 0, 11);
		
		/*try
		{
			ImageIO.write(image, "png", new File("/tmp/raster"+label+".png"));
		} catch (IOException e)
		{
			e.printStackTrace();
		}*/
		
		float[] data=new float[image.getWidth()*image.getHeight()];
		Arrays.fill(data, 1.0f);		
		DepthComponentFloat dcf=new DepthComponentFloat(image.getWidth(), image.getHeight());
		dcf.setDepthData(data);
		
		Raster ras = new Raster();
		ras.setSize(image.getWidth(), image.getHeight());
		ras.setImage(new ImageComponent2D(ImageComponent.FORMAT_RGBA, image));			
		ras.setPosition(new Point3f(x,y,z));
		ras.setDepthComponent(dcf);
		Dimension dim=new Dimension();
		ras.getSize(dim);
		System.out.println(dim);
		Shape3D s3d=new Shape3D();
		TransparencyAttributes ta = new TransparencyAttributes(
			TransparencyAttributes.BLENDED, 0 );
		Appearance app=new Appearance();
		app.setTransparencyAttributes(ta);		
		s3d.addGeometry(ras);		
		s3d.setAppearance(app);
		return s3d;
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
		light2.setCapability(Light.ALLOW_INFLUENCING_BOUNDS_WRITE);
		light2.setCapability(Light.ALLOW_INFLUENCING_BOUNDS_READ);
		gp.addChild(light2);
		return gp;
	}
	private void displayTransform3d(Transform3D transform)
	{
		if(textPane==null)
		{
			JDialog d=new JDialog();
			textPane=new JTextPane();
			d.setContentPane(textPane);
			d.show();
		}	
		String cr=System.getProperty("line.separator");
		String s="scale="+transform.getScale()+cr;
		Matrix3f m=new Matrix3f();
		Vector3f v=new Vector3f();
		transform.get(m);
		transform.get(v);
		s+="rotation="+m+cr;
		s+="translation="+v+cr;
		textPane.setText(s);
	}

	protected void firePositionChanged()
	{
		Iterator it=positionListeners.iterator();
		while(it.hasNext())
		{
			PositionListener p=(PositionListener)it.next();
			p.positionChanged();
		}
	}
	
	/** Fit the view to show the specified viewable */
	public void fit(Viewable viewable)
	{
		BoundingSphere b= (BoundingSphere)getBranchGroup(viewable).getBounds();
		Point3d c=new Point3d();
		b.getCenter(c);
		zoomTo((float)c.x,(float)c.y,(float)c.z,(float)b.getRadius());
	}
	
	/** Fit the view to show all the Viewable */
	public void fitAll()
	{
		BoundingSphere bs=getBound();
		if(bs.getRadius()<=0)
			bs=new BoundingSphere();
		Point3d c=new Point3d();
		bs.getCenter(c);
		zoomTo((float)c.x,(float)c.y,(float)c.z,(float)bs.getRadius());
	}
	
	protected BoundingSphere getBound()
	{
		Iterator it=viewableToViewSpecificGroup.values().iterator();
		ArrayList bounds=new ArrayList();		
		while(it.hasNext())
		{
			ViewSpecificGroup bg=(ViewSpecificGroup) it.next();
			if(bg.indexOfView(getView())!=-1)
				bounds.add(bg.getBounds());
		}
		
		if(isOriginAxisVisible())			
			bounds.add(originAxisTransformGroup.getBounds());
		
		BoundingSphere bs;
		if(bounds.size()>0)
		{
			bs=(BoundingSphere) bounds.get(0);
			bs.combine((Bounds[]) bounds.toArray(new Bounds[bounds.size()]));
		}
		else
			bs=new BoundingSphere();
		return bs;
	}
	
	/**
	 * Get the cloned branchgroup of a viewable for this view.
	 * The viewables may used it to modify a branchgroup whithout rebuilding
	 * it entirely.
	 * @param view
	 * @return
	 * 
	 */
	protected BranchGroup getBranchGroup(Viewable viewable)
	{
		ViewSpecificGroup vsp=(ViewSpecificGroup) viewableToViewSpecificGroup.get(viewable);
		return (BranchGroup)vsp.getUserData();
	}	
	
	/** Return viewables shown in this view */
	public Viewable[] getViewables()
	{
		ArrayList toReturn=new ArrayList();
		Iterator it=viewableToViewSpecificGroup.entrySet().iterator();
		while(it.hasNext())
		{
			Map.Entry e=(Entry) it.next();
			Viewable v=(Viewable) e.getKey();
			ViewSpecificGroup vsg=(ViewSpecificGroup) e.getValue();
			if(vsg.indexOfView(getView())!=-1)
			{
				toReturn.add(v);
			}
		}		
		return (Viewable[])toReturn.toArray(new Viewable[toReturn.size()]);
	}
	
	public Viewable getCurrentViewable()
	{
		return currentViewable;
	}
	
	public void setCurrentViewable(Viewable v)
	{
		currentViewable=v;
	}
	
	/** Move the view to the specified position */
	public void move(Transform3D position)
	{
		viewingPlatform.getViewPlatformTransform().setTransform(position);
	}

	/**
	 * Implement PositionListener.
	 * This listener is fired when the navigation master move.
	 */
	public void positionChanged()
	{
		move(navigationMaster.where());
	}
    /**
     * Overloaded for to be able to take snapshots and draw overlays
     * (selection rectangle)
     * @see takeSnapshot 
     */
	public void postSwap()
	{
		super.postSwap();
		try
		{
			if (takeSnapShot)
			{
				snapShot = getImage();			
				takeSnapShot = false;
				synchronized(snapShotLock)
				{
					snapShotLock.notifyAll();
				}
			}
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	/**
	 * Return an Image representing the current Canvas3D
	 * This method is not synchronized with the Java3D rendering.
	 * It used by takeSnapShopt method (which add synchronization),
	 * and for the rendering of the rectangle selection.
	 */
	protected BufferedImage getImage()
	{
		Dimension dim=getSize();
		GraphicsContext3D ctx = getGraphicsContext3D();
		Raster ras=new Raster();
		ras.setSize(dim);
		ras.setImage(new ImageComponent2D(ImageComponent.FORMAT_RGB,dim.width, dim.height));
		ctx.readRaster(ras);		
		// Now strip out the image info
		return ras.getImage().getImage();					
	}
	
	/** Remove a viewable from this view */
	public void remove(Viewable viewable)
	{	
		ViewSpecificGroup vsg=(ViewSpecificGroup) viewableToViewSpecificGroup.get(viewable);
		if(vsg!=null)
		{
			vsg.removeView(getView());			
			if(vsg.numViews()==0)
			{				
				universe.getLocale().removeBranchGraph(getBranchGroup(viewable));
				viewableToViewSpecificGroup.remove(viewable);
				vsg.removeAllChildren();
			}			
		}
		if(currentViewable==viewable)
		{
			Viewable[] vs=getViewables();
			if(vs.length>0)
				currentViewable=vs[0];
			else
				currentViewable=null;
		}
	}
	
	/** Create a navigation link with the specified view.
	 * This will ensure that the current view show the same Position as the
	 * specified view.
	 * @param view the master view, null mean no master.
	 */
	public void setNavigationMaster(View view)
	{
		navigationMaster=view;
		view.addPositionListener(this);
	}	
	
	/**
	 * Set the list of Viewables to which picking events will be dispatched.
	 * The Viewable objects not in this list will not be pickable.
	 * @param viewables
	 */
	public void setPickableViewables(Collection viewables)
	{
		//TODO
	}
	
	/**
	 * Set the picking mode:
	 * <ul>
	 * <li>0, for single click picking</li>
	 * <li>1, for rectangular selection picking</li>
	 * <li>2, for polygonal selection picking</li>
	 * </ul>
	 * @param mode
	 */
	public void setPickingMode(short mode)
	{
		//TODO
	}

	/**
	 * Display 3D x-y-z cartesian, at the center of the view.
	 * These axis are visible whenever the 3D origin cannot be seen.
	 * Axis are identified with strings "x","y" and "z".
	 * The size of the axis do not depends on the zoom level.
	 * @param show
	 */
	public void setFixedAxisVisible(boolean show)
	{		
		if(show)
			fixedAxisSwitch.setWhichChild(0);
		else 
			fixedAxisSwitch.setWhichChild(Switch.CHILD_NONE);
	}
	/**
	 * Display 3D x-y-z cartesian at the 3D origin of the model.
	 * Axis are identified with strings "x","y" and "z".
	 * The size of the axis do not depends on the zoom level.
	 * @param show
	 */
	public void setOriginAxisVisible(boolean show)
	{
		if(show)
			originAxisSwitch.setWhichChild(0);
		else 
			originAxisSwitch.setWhichChild(Switch.CHILD_NONE);
	}
	
	public boolean isOriginAxisVisible()
	{
		return originAxisSwitch.getWhichChild()==0;		
	}
	
	/**
	 * Take a snapshot of the current view
	 * Do not use this for offscreen rendering. See "On-screen Rendering vs. Off-screen Rendering" in
	 * Canvas3D javadoc.
	 * @return
	 */
	public BufferedImage takeSnapshot()
	{
		takeSnapShot=true;
		//repaint();
		synchronized(snapShotLock)
		{
			try
			{
				snapShotLock.wait();
			} catch(InterruptedException ex)
			{
				ex.printStackTrace();
				return new BufferedImage(0, 0, BufferedImage.TYPE_BYTE_INDEXED);
			}
		}
		return snapShot;
	}

	/**
	 * Take a snapshot of the current view in Off-screen mode
	 * Do not use this for on-screen rendering. See "On-screen Rendering vs. Off-screen Rendering" in
	 * Canvas3D javadoc.	 
	 */
	public BufferedImage takeSnapshot(int w, int h)
	{		
		getScreen3D().setSize(w,h);
		BufferedImage image=new BufferedImage(w,h, ImageComponent.FORMAT_RGB);
		setOffScreenBuffer(new ImageComponent2D(ImageComponent.FORMAT_RGB, image));	
		renderOffScreenBuffer();
		waitForOffScreenRendering();
		return getOffScreenBuffer().getImage();
	}
	
	/** Return the current position of the view */
	public Transform3D where()
	{
		Transform3D t3d=new Transform3D();
		viewingPlatform.getViewPlatformTransform().getTransform(t3d);
		return t3d;
	}
		
	/** Modify the view to best see what is include a given sphere
	 * @param x x coordinate of the center of the sphere
	 * @param y y coordinate of the center of the sphere
	 * @param z z coordinate of the center of the sphere
	 * @param radius radius of the sphere
	 */
	public void zoomTo(float x, float y, float z, float radius)
	{	
		Point3d c=new Point3d(x,y,z);
		BoundingSphere b=new BoundingSphere(c,radius);
		orbit.setBounds(b);
		orbit.setRotationCenter(c);
		orbit.setZoomFactor(b.getRadius());
		orbit.setTransFactors(b.getRadius()/10,b.getRadius()/10);
		orbit.setRotFactors(0.5, 0.5);
		orbit.setSchedulingBounds(new BoundingSphere(c,b.getRadius()*100));
		axisBehavior.setSchedulingBounds(orbit.getSchedulingBounds());
		
		getView().setFrontClipDistance(0.001*radius);
		getView().setBackClipDistance(10*radius);
		//getView().setWindowResizePolicy(javax.media.j3d.View.VIRTUAL_WORLD);
		getView().setFieldOfView(Math.PI/12);
		
		Transform3D t3d = new Transform3D();
		viewingPlatform.getViewPlatformTransform().getTransform(t3d);
		//calculate the translation vector for a identity rotation matrix
		Vector3f correction=new Vector3f(0, 0, 5f*radius);
		t3d.setTranslation(new Vector3f());	
		//rotate the translation vector
		t3d.transform(correction);
		correction.add(new Vector3f(x,y,z));
		t3d.setTranslation(correction);		
		viewingPlatform.getViewPlatformTransform().setTransform(t3d);
		//orbit.setViewingPlatform(viewingPlatform);
	}
	
	public final static byte TOP	=0;
	public final static byte BOTTOM	=1;
	public final static byte LEFT	=2;
	public final static byte RIGHT	=3;
	public final static byte FRONT	=4;
	public final static byte BACK	=5;
	
	/** TOP, BOTTOM, LEFT, RIGHT, FRONT, BACK */
	public void setOrientation(byte orientation)
	{
		Point3d eye=null;
		Vector3d up=new Vector3d(0,1,0);
		switch(orientation)
		{
		case TOP:
			eye=new Point3d(0, 1, 0);
			up=new Vector3d(0,0,-1);
			break;
		case BOTTOM:
			eye=new Point3d(0, -1, 0);
			up=new Vector3d(0,0,1);
			break;
		case LEFT:
			eye=new Point3d(-1, 0, 0);			
			break;
		case RIGHT:
			eye=new Point3d(1, 0, 0);
			break;
		case FRONT:
			eye=new Point3d(0, 0, 1);
			break;
		case BACK:
			eye=new Point3d(0, 0, -1);
			break;
		default:
			throw new IllegalArgumentException();
		}
		
		BoundingSphere bs=getBound();
		Transform3D t3d=new Transform3D();		
		Point3d center=new Point3d();
		bs.getCenter(center);
		eye.scale(5f*bs.getRadius());
		t3d.lookAt(eye, center, up);
		t3d.invert();
		move(t3d);
	}	
}
