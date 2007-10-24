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
 * (C) Copyright 2007, by EADS France
 */

package org.jcae.viewer3d.post;

import java.awt.Image;
import java.awt.image.*;
import java.awt.image.Raster;
import java.net.URL;
import java.util.Map;
import javax.media.j3d.*;
import org.jcae.viewer3d.DomainProvider;
import org.jcae.viewer3d.PickViewable;
import org.jcae.viewer3d.SelectionListener;
import org.jcae.viewer3d.ViewableAdaptor;
import com.sun.j3d.utils.image.TextureLoader;
import com.sun.j3d.utils.picking.PickTool;

public class ImageViewable extends ViewableAdaptor
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
	private boolean interpolate;
	
	public ImageViewable(float[] coordinates)
	{
		this(coordinates, 0, 0, true);
	}
	
	public ImageViewable(float[] coordinates, int imageWidth, int imageHeight, boolean interpolate)
	{
		this.interpolate=interpolate;
		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;
		QuadArray qa = new QuadArray(coordinates.length / 3,			
			GeometryArray.COORDINATES|GeometryArray.TEXTURE_COORDINATE_2);
		qa.setCoordinates(0, coordinates);
		qa.setTextureCoordinates(0, 0, TEXT_COORD);
		appearance = new Appearance();
		appearance.setPolygonAttributes(POLYGON_ATTR);
		appearance.setCapability(Appearance.ALLOW_TEXTURE_READ);
		Shape3D shape3D = new Shape3D(qa, appearance);
		PickTool.setCapabilities(shape3D, PickTool.INTERSECT_COORD);
		branchGroup.addChild(shape3D);
	}

	@Override
	public void addSelectionListener(SelectionListener listener)
	{
		// nothing
	}

	@Override
	public void domainsChangedPerform(int[] domainId)
	{
		// nothing
	}

	@Override
	public DomainProvider getDomainProvider()
	{
		return null;
	}

	@Override
	public Node getJ3DNode()
	{
		return branchGroup;
	}

	@Override
	public void pick(PickViewable result)
	{
		// nothing
	}

	@Override
	public void removeSelectionListener(SelectionListener listener)
	{
		// nothing
	}

	public void setColorMapper(ColorMapper cm)
	{
		colorMapper = cm;
	}

	@Override
	public void setDomainVisible(Map<Integer, Boolean> map)
	{
		// nothing
	}

	public void setImage(Image image)
	{
		/*
		 * try{ new FileOutputStream("image"+(imagecounter++)).write(new
		 * com.keypoint.PngEncoderB(image).pngEncode()); } catch(Exception
		 * ex){ex.printStackTrace();}
		 */		
		TextureLoader tl = new TextureLoader(image, null);
		Texture t=tl.getTexture();
		t.setBoundaryModeS(Texture.CLAMP);
		t.setBoundaryModeT(Texture.CLAMP);
		if(!interpolate)
			t.setMagFilter(Texture.BASE_LEVEL_POINT);
		appearance.setTexture(t);
	}

	public void setImage(URL url)
	{
		TextureLoader tl = new TextureLoader(url, null);
		Texture t=tl.getTexture();
		t.setBoundaryModeS(Texture.CLAMP);
		t.setBoundaryModeT(Texture.CLAMP);
		if(!interpolate)
			t.setMagFilter(Texture.BASE_LEVEL_POINT);
		TransparencyAttributes ta=new TransparencyAttributes();
		ta.setTransparency(0.5f);
		ta.setTransparencyMode(TransparencyAttributes.BLENDED);
		appearance.setTransparencyAttributes(ta);
		appearance.setTexture(t);
	}

	public void setValues(float[] values)
	{
		long t1=System.currentTimeMillis();
		int[] arrayTexture = new int[values.length];		
		for (int i = 0; i < values.length; i++)
		{
			colorMapper.mapColor(values[i], arrayTexture, i);
		}
		long t2=System.currentTimeMillis();
		System.out.println("Texture computed in "+(t2-t1)+" ms");
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
		int x=ceilPower2(imageWidth);
		int y=ceilPower2(imageHeight);
		if(x>1024) x=1024;
		if(y>1024) y=1024;
		ImageComponent2D i2d = tl.getScaledImage(x, y);

		tl = new TextureLoader(i2d.getImage());
		
		Texture t=tl.getTexture();
		t.setBoundaryModeS(Texture.CLAMP);
		t.setBoundaryModeT(Texture.CLAMP);
		if(!interpolate)
			t.setMagFilter(Texture.BASE_LEVEL_POINT);
		appearance.setTexture(t);
	}
	
	private int ceilPower2(int imageHeight2)
	{
		double p=Math.log(imageHeight2)/Math.log(2);
		p=Math.round(p+2);
		return (int) Math.pow(2, p);
	}

	@Override
	public void unselectAll()
	{
		// nothing
	}
}
