package org.jcae.viewer3d.post;

import java.awt.Color;

public class DefaultColorMapper implements ColorMapper
{
	private float min=0;
	private float max=1;
	private boolean haveInfinity=false;
	private float minInf=0;
	private int paletteSize=1;

	public DefaultColorMapper()
	{
		//nothing
	}
	
	public DefaultColorMapper(float[] values, int paletteSize)
	{
		ArrayTool at=new ArrayTool(values);
		setMin(at.getMin());
		setMax(at.getMax());
		setNegativeInfinity(at.haveNegativeInfinity());
		this.paletteSize=paletteSize;
	}
	
	/**
	 * @param max
	 * @param min
	 * @param infinity true if the values include Float.NEGATIVE_INFINITY
	 */
	public DefaultColorMapper( float min, float max, boolean infinity, int paletteSize)
	{
		setMin(min);
		setMax(max);
		setNegativeInfinity(infinity);
		this.paletteSize=paletteSize;
	}

	public byte[] getPalette()
	{		
		byte[] palette=new byte[paletteSize*3];
		if(paletteSize>1)
		for(int i=0; i<paletteSize; i++)
		{			
			Color c=Color.getHSBColor((1f-(float)i/(paletteSize-1))*2f/3f, 1f, 1f);
			palette[3*i]=(byte) c.getRed();
			palette[3*i+1]=(byte) c.getGreen();
			palette[3*i+2]=(byte) c.getBlue();
		}
		return palette;		
	} 
	
	public int map(float value)
	{
		if(value==Float.NEGATIVE_INFINITY)		
			value=minInf;
		int toReturn=(int) (paletteSize*(value-minInf)/(max-minInf));
		if(toReturn>=paletteSize)
			toReturn=paletteSize-1;
		if(toReturn<0)
			toReturn=0;
		//System.out.println(value +"=>"+ toReturn );
		return toReturn;
	}
	
	public Color mapColor(float value)
	{
		if(value==Float.NEGATIVE_INFINITY)		
			value=minInf;
		float v=(value-minInf)/(max-minInf);
		
		if(v<0f) v=0f;		
		if(v>1f) v=1f;
		
		return Color.getHSBColor((1f-v)*2f/3f, 1f, 1f);
	}
	
	public void mapColor(float value, int[] dst, int index)
	{
		if (value == Float.NEGATIVE_INFINITY) value = minInf;
		float v = (value - minInf) / (max - minInf);
		if (v < 0f) v = 0f;
		if (v > 1f) v = 1f;
		float hue = (1f - v) * 2f / 3f;
		int r = 0, g = 0, b = 0;
		float h = (hue - (float) Math.floor(hue)) * 6.0f;
		float f = h - (float) java.lang.Math.floor(h);
		float q = (1.0f - f);
		float t = f;
		switch ((int) h)
		{
			case 0 :
				r = 255;
				g = (int) (t * 255.0f + 0.5f);
				break;
			case 1 :
				r = (int) (q * 255.0f + 0.5f);
				g = 255;
				break;
			case 2 :
				g = 255;
				b = (int) (t * 255.0f + 0.5f);
				break;
			case 3 :
				g = (int) (q * 255.0f + 0.5f);
				b = 255;
				break;
			case 4 :
				r = (int) (t * 255.0f + 0.5f);
				b = 255;
				break;
			case 5 :
				r = 255;
				b = (int) (q * 255.0f + 0.5f);
				break;
		}
		dst[index] = 0xff000000 | (r << 16) | (g << 8) | (b << 0);
	}
	
	public void setMin(float min)
	{
		this.min=min;
		setNegativeInfinity(haveInfinity);
	}

	public void setMax(float max)
	{
		this.max=max;
	}
	
	public void setNegativeInfinity(boolean b)
	{
		haveInfinity=b;
		if(b)
		{
			minInf=min-(max-min)/10;
		}
		else
			minInf=min;
	}
}
