package org.jcae.viewer3d.post;

import java.awt.Color;

/**
 * Map colors on values.
 * This class properly handle set of values including Float.NEGATIVE_INFINITY,
 * but positive infinity will lead to unexpected behavior.
 * @author Jerome Robert
 */
public class DefaultColorMapper implements ColorMapper
{
	private float min=0;
	private float max=1;
	private boolean haveInfinity=false;
	private float minInf=0;
	private int paletteSize=1;

	/**
	 * Create a color mapper with the following caracteristics:
	 * <ul>
	 * 	<li>min=0</li>
	 * 	<li>max=1</li>
	 * 	<li>palletteSize=1</li>
	 * </ul>
	 */
	public DefaultColorMapper()
	{
		//nothing
	}
	
	/**
	 * Create a mapper for a given set of values
	 * @param values Extract the min and max value from this array
	 * @param paletteSize The number of color to generate
	 * @todo Call DefaultColorMapper(float, float, boolean, int)
	 */
	public DefaultColorMapper(float[] values, int paletteSize)
	{
		ArrayTool at=new ArrayTool(values);
		setMin(at.getMin());
		setMax(at.getMax());
		setNegativeInfinity(at.haveNegativeInfinity());
		this.paletteSize=paletteSize;
	}
	
	/**
	 * Create a mapper for a set of values whose only min and max are known
	 * @param max greated value
	 * @param min smallest <strong>finit</strong> value
	 * @param infinity true if the values include Float.NEGATIVE_INFINITY
	 */
	public DefaultColorMapper( float min, float max, boolean infinity, int paletteSize)
	{
		setMin(min);
		setMax(max);
		setNegativeInfinity(infinity);
		this.paletteSize=paletteSize;
	}

	/**
	 * Return the palette in the RGB format.
	 * The size of the return array is 3 times the size of the palette.
	 * Note that this method do not use min and max values as it always
	 * return an HSB palette from red to blue through green.
	 */
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
	
	/**
	 * Return the color index in the palette for the given value.
	 * This is a discret mapping.
	 */
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
	
	/**
	 * Return the color for the given value.
	 * This is a continues mapping (i.e. the returned value may not be in
	 * the palette).
	 */
	public Color mapColor(float value)
	{
		if(value==Float.NEGATIVE_INFINITY)		
			value=minInf;
		float v=(value-minInf)/(max-minInf);
		
		if(v<0f) v=0f;		
		if(v>1f) v=1f;
		
		return Color.getHSBColor((1f-v)*2f/3f, 1f, 1f);
	}
	
	/**
	 * Set the color associated to the value into the destination array
	 * This is a continues mapping (.e. the returned value may not be in
	 * the palette).
	 * This methods aims at being use in loop to fill large destination
	 * arrays. Each element of the destination array is a ARGB color
	 * encoded on one integer: 0xffRRGGBB.
	 */
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
	
	/**
	 * Set the <strong>finit</strong> smallest value
	 */
	public void setMin(float min)
	{
		this.min=min;
		setNegativeInfinity(haveInfinity);
	}

	/**
	 * Set the greatest value
	 */
	public void setMax(float max)
	{
		this.max=max;
	}
	
	/**
	 * Specifiy that the working set of value contains Float.NEGATIVE_INFINITY.
	 */
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
	
	/**
	 * Get the <strong>finit</strong> smallest value
	 */	
	public float getMin()
	{
		return min;
	}

	/**
	 * Get the greatest value
	 */	
	public float getMax()
	{
		return min;
	}
	
	/**
	 * Return true is the set of value contains Float.NEGATIVE_INFINITY
	 */
	public boolean isNegativeInfinity()
	{
		return haveInfinity;
	}
}
