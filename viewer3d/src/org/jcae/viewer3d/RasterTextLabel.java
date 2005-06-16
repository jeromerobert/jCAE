/*****************************************************************************
 *                      J3D.org Copyright (c) 2000
 *                              Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 ****************************************************************************/

//modified package for easier packaging
//package org.j3d.geom;

package org.jcae.viewer3d;

// Standard imports
import javax.media.j3d.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import javax.vecmath.Point3f;

// Application specific imports

/**
 * A text label for labelling objects on screen that uses a Java 3D Raster
 * to produce the overlay effect.
 * <p>
 *
 * If the label text is null, then no label will be displayed. All of the
 * setup will be done, but no raster will be created.
 * <p>
 *
 * The current implementation does not allow updating the text or colors
 *
 * @author Justin Couch, based on work from Corysia Taware
 * @version $Revision$
 */
public class RasterTextLabel extends Shape3D
{
    /** The inset between the text and the border if one is required. */
    private static final int BORDER_INSETS = 2;

    /** The current color of the text */
    private Color textColor;

    /** The current color of the border. Null if not in use */
    private Color borderColor;

    /** The font of the label. Null if using system default */
    private Font labelFont;

    /**
     * Create a new blank label with no text. It is located at the origin.
     */
    public RasterTextLabel()
    {
        this(null, null, 0, 0, 0, null, null);
    }

    /**
     * Create a new blank label with the given text located at the origin.
     * If the text color is not specified, white is used.
     *
     * @param label The string to use on the label
     * @param col The text color to be drawn in
     */
    public RasterTextLabel(String label, Color col)
    {
        this(label, col, 0, 0, 0, null, null);
    }

    /**
     * Create a new blank label with the given text located at a specific
     * point in 3D world coordinates.
     *
     * @param label The string to use on the label
     * @param col The text color to be drawn in
     * @param x The x world coordinate to place the label
     * @param y The y world coordinate to place the label
     * @param z The z world coordinate to place the label
     */
    public RasterTextLabel(String label, Color col, float x, float y, float z)
    {
        this(label, col, x, y, z, null, null);
    }

    /**
     * Create a new blank label with the given text located at a specific
     * point in 3D world coordinates and an option to show a border and
     * selected font. If the border color is specified, it will show a 1
     * pixel wide border in that color. If no font is defined, the system
     * default font will be used.
     *
     * @param label The string to use on the label
     * @param col The text color to be drawn in
     * @param x The x world coordinate to place the label
     * @param y The y world coordinate to place the label
     * @param z The z world coordinate to place the label
     * @param border The color to use for the border or null for none
     * @param font The font to draw the string in or null for default
     */
    public RasterTextLabel(String label,
                           Color col,
                           float x,
                           float y,
                           float z,
                           Color border,
                           Font font)
    {
        textColor = col;
        borderColor = border;
        labelFont = font;

        Appearance app = new Appearance();
        RenderingAttributes ra = new RenderingAttributes();
        //ra.setDepthBufferEnable(false);
        //ra.setDepthBufferWriteEnable(false);
        app.setRenderingAttributes(ra);
		TransparencyAttributes ta = new TransparencyAttributes(
			TransparencyAttributes.BLENDED, 0 );
		app.setTransparencyAttributes(ta);
        setAppearance(app);

        if(label == null)
            return;

        // create a disposable 1x1 image so that we can fetch the font
        // metrics associated with the font and text label. This will allow
        // us to determine the real image size. This is kludgy, but I can't
        // think of a better way of doing it!
        BufferedImage tmp_img =
            new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

        Graphics graphics = tmp_img.getGraphics();
        FontMetrics fm;

        if(font == null)
            fm = graphics.getFontMetrics();
        else
            fm = graphics.getFontMetrics(font);

        // now we have the metrics, let's work out how big the label is!
        Rectangle2D dimensions = fm.getStringBounds(label, graphics);

        graphics.dispose();
        tmp_img.flush();

        int width = (int)dimensions.getWidth()+5;
        int height = (int)dimensions.getHeight()+5;
        int ascent = fm.getMaxAscent();

        if(border != null)
        {
            width += BORDER_INSETS * 2 + 2;
            height += BORDER_INSETS * 2 + 2;
        }

        tmp_img = new BufferedImage(width,
                                height,
                                BufferedImage.TYPE_INT_ARGB);

        graphics = tmp_img.getGraphics();

        if(border != null)
        {
            graphics.setColor(borderColor);
            graphics.drawRect(0, 0, width - 1, height - 1);

            if(textColor == null)
                graphics.setColor(Color.white);
            else
                graphics.setColor(textColor);

            graphics.drawString(label,
                                BORDER_INSETS + 1,
                                ascent + BORDER_INSETS + 1);
        }
        else
        {
            if(textColor == null)
                graphics.setColor(Color.white);
            else
                graphics.setColor(textColor);

            graphics.drawString(label, 0, ascent);
        }

        graphics.dispose();

        ImageComponent2D img_comp =
            new ImageComponent2D(ImageComponent2D.FORMAT_RGBA, tmp_img);

        Point3f origin = new Point3f(x, y, z);

        Raster raster = new Raster(origin,
                                   Raster.RASTER_COLOR,
                                   0,
                                   0,
                                   width,
                                   height,
                                   img_comp,
                                   null);

        setGeometry(raster);
    }

    /**
     * Set the label string that is to be rendered. This maintains the
     * current text color.
     *
     * @param label to be used
     */
    public void setLabel(String label)
    {
    }


    /**
     * Set the label string that is to be rendered and changes the color
     * to the new value.
     *
     * @param label to be used
     * @param col The new color to be used or null for the default
     */
    public void setLabel(String label, Color col)
    {
    }

    /**
     * Set the condition of whether the implementation should resize the
     * canvas after each new label is set or just stick to a fixed size
     * canvas. A fixed size label is useful when you are making fast updates
     * such as a counter. When this is called, the label will not be resized
     * from it's current dimensions.
     *
     * @param fixed true if the label size should remain fixed
     */
    public void fixSize(boolean fixed)
    {
    }
}