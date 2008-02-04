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
package org.jcae.netbeans.cad;

import org.jcae.opencascade.jni.BRepPrimAPI_MakeSphere;
import org.jcae.opencascade.jni.TopoDS_Shape;

/**
 * @author Jerome Robert
 *
 */
public class Sphere extends Primitive
{
    private double radius = 1;
	private double x = 0;
    private double y = 0;
    private double z = 0;
    /**
	 * @param name
	 */
	public Sphere()
	{
		setName("sphere");
	}
	/**
	 * @return Returns the radius.
	 */
	public double getRadius()
	{
		return radius;
	}
	/**
	 * @return Returns the x.
	 */
	public double getX()
	{
		return x;
	}
	/**
	 * @return Returns the y.
	 */
	public double getY()
	{
		return y;
	}
	/**
	 * @return Returns the z.
	 */
	public double getZ()
	{
		return z;
	}
    
	/* (non-Javadoc)
	 * @see org.jcae.netbeans.cad.ModifiableShape#rebuild()
	 */
	public TopoDS_Shape rebuild()
	{
		return new BRepPrimAPI_MakeSphere(new double[]{x, y, z}, radius).shape();
	}
	/**
	 * @param radius The radius to set.
	 */
	public void setRadius(double radius)
	{
		if(this.radius!=radius)
		{
			this.radius = radius;
		}		
	}
	/**
	 * @param x The x to set.
	 */
	public void setX(double x)
	{
		if(this.x!=x)
		{
			this.x = x;
		}		
	}
	
	/**
	 * @param y The y to set.
	 */
	public void setY(double y)
	{
		if(this.y!=y)
		{
			this.y = y;
		}	
	}
	/**
	 * @param z The z to set.
	 */
	public void setZ(double z)
	{
		if(this.z!=z)
		{
			this.z = z;
		}	
	}
}
