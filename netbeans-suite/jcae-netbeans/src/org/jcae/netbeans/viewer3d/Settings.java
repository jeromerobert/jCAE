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
 * (C) Copyright 2004, by EADS CRC
 */

package org.jcae.netbeans.viewer3d;

import org.openide.options.SystemOption;

/**
 * @author Jerome Robert
 *
 */
public class Settings extends SystemOption
{
	private static final String ZFACTORABS = "javax.media.j3d.zFactorAbs";
	private static final String ZFACTORREL = "javax.media.j3d.zFactorRel";
	/**
	 * @return Returns the polygonOffset.
	 */
	public float getPolygonOffset()
	{
		String s=System.getProperty(ZFACTORABS, "20.0f");
		return Float.parseFloat(s);
	}
	/**
	 * @param polygonOffset The polygonOffset to set.
	 */
	public void setPolygonOffset(float polygonOffset)
	{
		System.setProperty(ZFACTORABS, ""+polygonOffset);
	}
	/**
	 * @return Returns the polygonOffsetFactor.
	 */
	public float getPolygonOffsetFactor()
	{
		String s=System.getProperty(ZFACTORREL, "2.0f");
		return Float.parseFloat(s);
	}
	/**
	 * @param polygonOffsetFactor The polygonOffsetFactor to set.
	 */
	public void setPolygonOffsetFactor(float polygonOffsetFactor)
	{
		System.setProperty(ZFACTORREL, ""+polygonOffsetFactor);
	}
	/* (non-Javadoc)
	 * @see org.openide.options.SystemOption#displayName()
	 */
	public String displayName()
	{
		return "viewer 3d settings";
	}
}
