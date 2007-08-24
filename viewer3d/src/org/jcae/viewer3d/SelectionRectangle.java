package org.jcae.viewer3d;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;

import javax.media.j3d.*;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import com.sun.j3d.utils.universe.PlatformGeometry;

public abstract class SelectionRectangle
{
	/** Display the selection rectangle on a 2D context */
	public static class SelectionRectangle2D extends SelectionRectangle implements Runnable
	{
		private boolean visible;
		private Color color;

		public SelectionRectangle2D(View  view)
		{
			super(view);
		}
		
		public void setColor(Color color)
		{
			this.color=color;
		}

		protected void draw(Point anchor, Point point)
		{
			//nothing
		}

		public void setVisible(boolean b)
		{
			this.visible=b;
			if(b)
				view.addPostRenderer(this);
			else
				view.removePostRenderer(this);
		}

		public void run()
		{
			J3DGraphics2D g = view.getGraphics2D();
			g.setColor(color);
			g.drawRect(rectangle2D.x, rectangle2D.y, rectangle2D.width, rectangle2D.height);
			g.flush(true);
		}

		public boolean isVisible()
		{
			return visible;
		}		
	}
	
	/**
	 * Display the selection rectangle on a 3D context.
	 * It doesn't work because we don't know where to set the rectangle
	 * on the platform geometry. We keep it as a remind.
	 */
	public static class SelectionRectangle3D extends SelectionRectangle
	{
		private BranchGroup branchGroup=new BranchGroup();
		private PlatformGeometry platformGeometry;
		private ColoringAttributes coloringAttributes=new ColoringAttributes();
		
		private IndexedQuadArray quad;
		private double[] coordinates = new double[12];
		private Point3d p3d1=new Point3d();
		private Point3d p3d2=new Point3d();	

		public SelectionRectangle3D(View  view)
		{
			super(view);
			branchGroup.setCapability(BranchGroup.ALLOW_DETACH);
			platformGeometry=view.getViewingPlatform().getPlatformGeometry();
			
			quad= new IndexedQuadArray(4, GeometryArray.COORDINATES, 4);
			quad.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);
			quad.setCoordinates(0, coordinates);
			quad.setCoordinateIndices(0, new int[]{0,1,2,3});
			Appearance appearance=new Appearance();
			PolygonAttributes pa=new PolygonAttributes();
			pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
			pa.setCullFace(PolygonAttributes.CULL_NONE);
			appearance.setColoringAttributes(coloringAttributes);
			appearance.setPolygonAttributes(pa);
			Shape3D s3d=new Shape3D();
			s3d.addGeometry(quad);
			s3d.setAppearance(appearance);
			branchGroup.addChild(s3d);
			coloringAttributes.setCapability(ColoringAttributes.ALLOW_COLOR_WRITE);		
		}
		
		public void setVisible(boolean b)
		{
			if(b && !isVisible())
				platformGeometry.addChild(branchGroup);
			else if(!b && isVisible())
				platformGeometry.removeChild(branchGroup);
		}
		
		public boolean isVisible()
		{
			return platformGeometry.indexOfChild(branchGroup)>=0;
		}

		public void setColor(Color color)
		{
			coloringAttributes.setColor(new Color3f(color));
		}

		protected void draw(Point anchor, Point point)
		{
			view.getPixelLocationInImagePlate(anchor.x, anchor.y, p3d1);
			view.getPixelLocationInImagePlate(point.x, point.y, p3d2);		
			Transform3D t3d=new Transform3D();
			/*view.getImagePlateToVworld(t3d);
			t3d.transform(p3d1);
			t3d.transform(p3d2);*/
			
			System.out.println(point.x + " " + point.y);
			System.out.println(p3d1 +" "+ p3d2+ " " + view.where().getScale()+ " "+view.getView().getFrontClipDistance()+" "+view.getView().getFieldOfView()*180/Math.PI);
			for(int i=0; i<coordinates.length; i++)
				coordinates[i] = -1;
			/*p3d1.x=-0.8;
			p3d1.y=-0.8;
			p3d2.x=0.8;
			p3d2.y=0.8;*/

			coordinates[0]=p3d1.x;
			coordinates[1]=p3d1.y;		
			coordinates[3]=p3d2.x;
			coordinates[4]=p3d1.y;
			coordinates[6]=p3d2.x;
			coordinates[7]=p3d2.y;		
			coordinates[9]=p3d1.x;
			coordinates[10]=p3d2.y;			
			quad.setCoordinates(0, coordinates);			
		}		
	}
	
	protected View view;
	protected Rectangle rectangle2D=new Rectangle();
	
	public SelectionRectangle(View  view)
	{
		this.view=view;
	}
	
	public Rectangle getGeometry2D()
	{
		return rectangle2D;
	}
	
	public abstract void setVisible(boolean b);
	public abstract boolean isVisible();
	public abstract void setColor(Color color);
	protected abstract void draw(Point anchor, Point point);
	
	public void setGeometry(Point anchor, Point point)
	{
		rectangle2D.setFrameFromDiagonal(anchor, point);
	}
}
