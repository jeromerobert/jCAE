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

package org.jcae.viewer3d.fe.unv;

import java.awt.Color;
import java.io.*;
import org.jcae.viewer3d.Domain;
import org.jcae.viewer3d.Palette;
import org.jcae.viewer3d.fe.FEProvider;

public class UNVProvider implements FEProvider
{
	private static Palette palette = new Palette(Integer.MAX_VALUE);
	private UNVParser parser=new UNVParser();
	private Color[] colors;
		
	public UNVProvider()
	{
		//empty
	}
	
	public UNVProvider(File file) throws IOException
	{
		load(new BufferedReader(new FileReader(file)));
	}

	/** Set colors for domais (one color for each domain) */
	public void setColors(Color[] colors)
	{
		this.colors=colors;
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.DomainProvider#getDomain(int)
	 */
	public Domain getDomain(int id)
	{
		Color color;
		if(colors!=null)
			color=colors[id%colors.length];
		else
			color=palette.getColor(id);
		return new UNVDomain(parser, id, color);
	}
	
	/* (non-Javadoc)
	 * @see jcae.viewer3d.DomainProvider#getDomainIDs()
	 */
	public int[] getDomainIDs()
	{		
		int l=parser.getTria3GroupNames().length;
		int l4=parser.getQuad4GroupNames().length;
		int[] toReturn=new int[l+l4];

		for(int i=0; i<l; i++)
			toReturn[i]=i;

		for(int i=0; i<l4; i++)
			toReturn[l+i]=l+i;
				
		return toReturn;
	}

	public void load(BufferedReader stream) throws IOException
	{
		parser.parse(stream);
	}
	
	public void load(InputStream stream) throws IOException	
	{
		parser.parse(new BufferedReader(new InputStreamReader(stream)));
	}
}
