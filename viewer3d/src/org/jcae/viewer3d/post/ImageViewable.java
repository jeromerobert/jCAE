package org.jcae.viewer3d.post;

import java.awt.image.*;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.media.j3d.*;
import org.jcae.viewer3d.DomainProvider;
import org.jcae.viewer3d.SelectionListener;
import org.jcae.viewer3d.Viewable;
import com.sun.j3d.utils.image.TextureLoader;
import com.sun.j3d.utils.picking.PickResult;

public class ImageViewable implements Viewable
{
	private final static ColorModel COLOR_MODEL_RGB = new DirectColorModel(24,
		0x00ff0000, 0x0000ff00, 0x000000ff);
	private final static PolygonAttributes POLYGON_ATTR = new PolygonAttributes(
		PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE, 0);
	private Appearance appearance;
	private BranchGroup branchGroup = new BranchGroup();
	private ColorMapper colorMapper;
	private int imageHeight;
	private int imageWidth;
	private static float[] TEXT_COORD={0f,0f,1f,0f,1f,1f,0f,1f};
	
	public ImageViewable(float[] coordinates, int imageWidth, int imageHeight)
	{
		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;
		QuadArray qa = new QuadArray(coordinates.length / 3,			
			GeometryArray.COORDINATES|GeometryArray.TEXTURE_COORDINATE_2);
		qa.setCoordinates(0, coordinates);
		qa.setTextureCoordinates(0, 0, TEXT_COORD);
		appearance = new Appearance();
		appearance.setPolygonAttributes(POLYGON_ATTR);
		Shape3D shape3D = new Shape3D(qa, appearance);
		branchGroup.addChild(shape3D);
	}

	public void addSelectionListener(SelectionListener listener)
	{
		// nothing
	}

	public void domainsChanged(int[] domainId)
	{
		// nothing
	}

	public DomainProvider getDomainProvider()
	{
		return null;
	}

	public Node getJ3DNode()
	{
		System.out.println(branchGroup.getBounds());
		return branchGroup;
	}

	public void pick(PickResult result, boolean selected)
	{
		// nothing
	}

	public void removeSelectionListener(SelectionListener listener)
	{
		// nothing
	}

	public void setColorMapper(ColorMapper cm)
	{
		colorMapper = cm;
	}

	public void setDomainVisible(Map map)
	{
		// nothing
	}

	public void setValues(float[] values)
	{
		int[] arrayTexture = new int[values.length];
		for (int i = 0; i < values.length; i++)
		{
			arrayTexture[i] = colorMapper.mapColor(values[i]).getRGB();
		}
		DataBuffer dbuf = new DataBufferInt(arrayTexture, arrayTexture.length);
		SampleModel sampleModel = COLOR_MODEL_RGB.createCompatibleSampleModel(
			imageWidth, imageHeight);
		// Create a raster using the sample model and data buffer
		WritableRaster raster = Raster.createWritableRaster(sampleModel, dbuf,
			null);
		// Combine the color model and raster into a buffered image
		BufferedImage image = new BufferedImage(COLOR_MODEL_RGB, raster, false,
			null);
		/*
		 * try{ new FileOutputStream("image"+(imagecounter++)).write(new
		 * com.keypoint.PngEncoderB(image).pngEncode()); } catch(Exception
		 * ex){ex.printStackTrace();}
		 */
		TextureLoader tl = new TextureLoader(image);
		Texture t=tl.getTexture();
		t.setBoundaryModeS(Texture.CLAMP);
		t.setBoundaryModeT(Texture.CLAMP);
		appearance.setTexture(t);
	}

	public void unselectAll()
	{
		// nothing
	}
}
