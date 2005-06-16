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
import java.util.*;

/**
 * @author  Jerome Robert
 */
public class Palette
{
	HashSet colors=new HashSet();
	
	/** Create an empty palette. You must use addColor to add base color to the
	 palette */
	public Palette()
	{
	}
	
	/** Create a palette with n colors. The created palette will not include more
	 than 37 colors */
	public Palette(int n)
	{
		colors.add(new Color(0.7f,0.7f,0.7f));
		for(float b=1;b>=0.7;b-=0.15)
		{
			for(float s=0.5f;s>0f;s-=0.25f)
			{
				for(float h=0;h<1;h+=1.0/6.0)
				{
					colors.add(Color.getHSBColor(h, s, b));
					if(colors.size()>=n) return;
				}
			}
		}
	}
	
	public Color getColor(int i)
	{
		i=i%colors.size();
		return (Color)colors.toArray()[i];
	}
	
	public void addColor(Color c)
	{
		colors.add(c);
	}
	
	public void computeNewColors()
	{
		Color[] cs=new Color[colors.size()];
		cs=(Color[])colors.toArray(cs);
		for(int i=0;i<cs.length;i++)
		{
			for(int j=i+1;j<cs.length;j++)
			{
				/*float[] hsb1,hsb2;
				float h,s,b;
				hsb1=Color.RGBtoHSB(cs[i].getRed(), cs[i].getGreen(), cs[i].getBlue(),null);
				hsb2=Color.RGBtoHSB(cs[j].getRed(), cs[j].getGreen(), cs[j].getBlue(),null);
				h=(hsb1[0]+hsb2[0])/2;
				s=(hsb1[1]+hsb2[1])/2;
				b=(hsb1[2]+hsb2[2])/2;
				colors.add(Color.getHSBColor(h,s,b));*/
				int r=(cs[i].getRed()+cs[j].getRed())/2;
				int g=(cs[i].getGreen()+cs[j].getGreen())/2;
				int b=(cs[i].getBlue()+cs[j].getBlue())/2;
				colors.add(new Color(r,g,b));
			}
		}
	}
	
	public void computeNewColors(int n)
	{
		if(colors.size()<2)
			throw new IllegalStateException("There must be at least to color in the palette");
		while(colors.size()<n) computeNewColors();
	}
	
	public void removeDarkestColors(float s)
	{
		Iterator i=colors.iterator();
		while(i.hasNext())
		{
			Color c=(Color)i.next();
			float cs=(c.getRed()+c.getGreen()+c.getBlue())/3;
			if(cs<s) i.remove();
		}
	}
}
