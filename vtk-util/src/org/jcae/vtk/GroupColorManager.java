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
 * (C) Copyright 2008, by EADS France
 */
package org.jcae.vtk;

import java.awt.Color;
import java.util.HashSet;

/**
 *
 * @author ibarz
 */
public class GroupColorManager implements ColorManager
{
	private HashSet<Color> colors = new HashSet<Color>();

	public void setColor(Color color)
	{
		colors.add(color);
	}
	
	public Color getColor()
	{
		Color color;
		do
		{
			int r = (int) (255 * Math.random());
			int g = (int) (255 * Math.random());
			int b = (int) (255 * Math.random());
			color = new Color(r, g, b);
		} while (colors.contains(color));
		
		colors.add(color);
		
		return color;
	}
}
