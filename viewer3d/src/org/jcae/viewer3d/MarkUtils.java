package org.jcae.viewer3d;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import javax.media.j3d.*;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;
import com.sun.j3d.utils.picking.PickTool;

/**
 * @author Jerome Robert
 *
 */
public class MarkUtils
{
	/** dummy object to indentify marks in the java3d tree */
	public final static Object MARK_IDENTIFIER=new Object();
	
	/** The inset between the text and the border if one is required. */
    private static final int BORDER_INSETS = 2;
	private static final Appearance LABEL_APPEARANCE=new Appearance();
	
	public static class MarkID
	{
		private int domainID;
		private int typeID;
		/**
		 * @param domainID
		 * @param typeID
		 */
		public MarkID(int domainID, int typeID)
		{
			this.domainID = domainID;
			this.typeID = typeID;
		}
		public int getDomainID()
		{
			return domainID;
		}
		public int getTypeID()
		{
			return typeID;
		}
	}
	static
	{
	    RenderingAttributes ra = new RenderingAttributes();
	    //ra.setDepthBufferEnable(false);
	    //ra.setDepthBufferWriteEnable(false);
	    LABEL_APPEARANCE.setRenderingAttributes(ra);
		TransparencyAttributes ta = new TransparencyAttributes(
			TransparencyAttributes.BLENDED, 0 );
		LABEL_APPEARANCE.setTransparencyAttributes(ta);		
	}
    
	/**
	 * @param domain
	 * @return
	 */
	static public Node createMarkNode(MarkDomain domain, int domainID)
	{
		Object[] marks=domain.getMarksTypes();
		BranchGroup toReturn=new BranchGroup();
		for(int i=0; i<marks.length; i++)
		{
			float[] coords=domain.getMarks(marks[i]);
			if(coords==null) continue;
			if(coords.length==0) continue;
			Shape3D s3d;
			if(marks[i] instanceof PointAttributes)
			{
				PointArray pa=new PointArray(coords.length/3, PointArray.COORDINATES);
				pa.setCoordinates(0, coords);
				Appearance a=new Appearance();
				a.setPointAttributes((PointAttributes) marks[i]);
				s3d=new Shape3D(pa, a);
			}
			else
			{
				s3d=createLabelShape(marks[i], coords);
				s3d.setPickable(false);
				toReturn.addChild(s3d);

				PointArray pa=new PointArray(coords.length/3, PointArray.COORDINATES);
				pa.setCoordinates(0, coords);
				Appearance a=new Appearance();
				a.setColoringAttributes(new ColoringAttributes(
					new Color3f(Color.DARK_GRAY), ColoringAttributes.FASTEST));
				s3d=new Shape3D(pa, a);
			}
			s3d.setCapability(Node.ALLOW_PICKABLE_WRITE);
			PickTool.setCapabilities(s3d, PickTool.INTERSECT_FULL);
			s3d.setUserData(new MarkID(domainID, i));
			toReturn.addChild(s3d);

		}
		toReturn.setCapability(Node.ALLOW_PICKABLE_WRITE);
		toReturn.setCapability(Group.ALLOW_CHILDREN_READ);
		toReturn.setUserData(MARK_IDENTIFIER);
		return toReturn;
	}
	
	/**
	 * @param object
	 * @param coords
	 * @return
	 */
	static protected Shape3D createLabelShape(Object object, float[] coords)
	{
		Shape3D toReturn=new Shape3D();
		ImageComponent2D img=createImageComponent2D(object.toString(), null, null, null);
		toReturn.setAppearance(LABEL_APPEARANCE);
		for(int i=0; i<coords.length; i+=3)
		{
	        Raster raster = new Raster(new Point3f(coords[i], coords[i+1], coords[i+2]),
                Raster.RASTER_COLOR,
                0,
                0,
                img.getWidth(),
                img.getHeight(),
                img,
                null);
	        toReturn.addGeometry(raster);
		}
		return toReturn;
	}

	/**
	 * J3D.org Copyright (c) 2000 Java Source
	 * This source is licensed under the GNU LGPL v2.1
	 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
	 */
	static protected ImageComponent2D createImageComponent2D(String label, Font font, Color borderColor, Color textColor)
	{
		// create a disposable 1x1 image so that we can fetch the font
		// metrics associated with the font and text label. This will allow
		// us to determine the real image size. This is kludgy, but I can't
		// think of a better way of doing it!
		BufferedImage tmp_img = new BufferedImage(1, 1,
			BufferedImage.TYPE_INT_ARGB);
		Graphics graphics = tmp_img.getGraphics();
		FontMetrics fm;
		if (font == null) fm = graphics.getFontMetrics();
		else fm = graphics.getFontMetrics(font);
		// now we have the metrics, let's work out how big the label is!
		Rectangle2D dimensions = fm.getStringBounds(label, graphics);
		graphics.dispose();
		tmp_img.flush();
		int width = (int) dimensions.getWidth()+5;
		int height = (int) dimensions.getHeight()+5;
		int ascent = fm.getMaxAscent();
		if (borderColor != null)
		{
			width += BORDER_INSETS * 2 + 2;
			height += BORDER_INSETS * 2 + 2;
		}
		tmp_img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		graphics = tmp_img.getGraphics();
		if (borderColor != null)
		{
			graphics.setColor(borderColor);
			graphics.drawRect(0, 0, width - 1, height - 1);
			if (textColor == null) graphics.setColor(Color.white);
			else graphics.setColor(textColor);
			graphics.drawString(label, BORDER_INSETS + 1, ascent
				+ BORDER_INSETS + 1);
		} else
		{
			if (textColor == null) graphics.setColor(Color.white);
			else graphics.setColor(textColor);
			graphics.drawString(label, 0, ascent);
		}
		graphics.dispose();
		ImageComponent2D img_comp = new ImageComponent2D(
			ImageComponent2D.FORMAT_RGBA, tmp_img);
		return img_comp;
	}

	/**
	 * Change the pickable status of a node created with createMarkNode.
	 * @param marks
	 */
	public static void setPickable(Node marks, boolean enable)
	{
		Group g=(Group) marks;
		for(int i=0; i<g.numChildren(); i++)
		{
			Node n=g.getChild(i);
			if(n.getUserData() instanceof MarkID)
			{
				n.setPickable(enable);
			}
		}
	}
	
}
