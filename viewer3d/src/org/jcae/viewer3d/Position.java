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

/**
 * This class describe where the user is in the 3D scene and in which direction
 * he is watching.
 * Note:
 * Zooming is not the same thing than be closer from an object. By being closer
 * from an object a user can go throught an object, but by zooming this must
 * not happen.
 * @author Jerome Robert
 */
public class Position
{
	private float x, y, z, theta, phi, zoom;
		/**
	 * @param x
	 * @param y
	 * @param z
	 * @param theta
	 * @param phi
	 * @param zoom
	 */
	public Position(float x, float y, float z, float theta, float phi,
		float zoom)
	{
		this.x = x;
		this.y = y;
		this.z = z;
		this.theta = theta;
		this.phi = phi;
		this.zoom = zoom;
	}
	
	public float getPhi()
	{
		return phi;
	}
	public void setPhi(float phi)
	{
		this.phi = phi;
	}
	public float getTheta()
	{
		return theta;
	}
	public void setTheta(float theta)
	{
		this.theta = theta;
	}
	public float getX()
	{
		return x;
	}
	public void setX(float x)
	{
		this.x = x;
	}
	public float getY()
	{
		return y;
	}
	public void setY(float y)
	{
		this.y = y;
	}
	public float getZ()
	{
		return z;
	}
	public void setZ(float z)
	{
		this.z = z;
	}
	public float getZoom()
	{
		return zoom;
	}
	public void setZoom(float zoom)
	{
		this.zoom = zoom;
	}
}
