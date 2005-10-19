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

import org.jcae.opencascade.jni.BRepPrimAPI_MakeCylinder;
import org.jcae.opencascade.jni.TopoDS_Shape;

/**
 * @author Jerome Robert
 *
 */
public class Cylinder extends Primitive
{
    private double directionX = 1;
    private double directionY = 1;
    private double directionZ = 1;
    private double height = 2;    
	private double originX = -1;
    private double originY = -1;
    private double originZ = -1;
    private double radius = 1;
    /**
	 * @param name
	 */
	public Cylinder()
	{
		setName("Cylinder");
	}
	public double getDirectionX()
	{
		return directionX;
	}
	public double getDirectionY()
	{
		return directionY;
	}
	public double getDirectionZ()
	{
		return directionZ;
	}
	public double getHeight()
	{
		return height;
	}
	public double getOriginX()
	{
		return originX;
	}
	public double getOriginY()
	{
		return originY;
	}
	public double getOriginZ()
	{
		return originZ;
	}
	public double getRadius()
	{
		return radius;
	}
	/* (non-Javadoc)
	 * @see org.jcae.netbeans.cad.ModifiableShape#rebuild()
	 */
	public TopoDS_Shape rebuild()
	{
		double[] axis=new double[]{originX, originY, originZ, directionX, directionY, directionZ};
		return new BRepPrimAPI_MakeCylinder(axis, radius, height, 2*Math.PI).shape();
	}
	
	public void setDirectionX(double directionX)
	{
		if(this.directionX!=directionX)
		{
			this.directionX = directionX;
		}		
	}
	
	public void setDirectionY(double directionY)
	{
		if(this.directionY!=directionY)
		{
			this.directionY = directionY;
		}		

	}
	
	public void setDirectionZ(double directionZ)
	{
		if(this.directionZ!=directionZ)
		{
			this.directionZ = directionZ;
		}		

	}
	
	public void setHeight(double height)
	{
		if(this.height!=height)
		{
			this.height = height;
		}
	}
	
	public void setOriginX(double originX)
	{
		if(this.originX!=originX)
		{
			this.originX = originX;
		}
	}
	
	public void setOriginY(double originY)
	{
		if(this.originY!=originY)
		{
			this.originY = originY;
		}
	}
	
	public void setOriginZ(double originZ)
	{
		if(this.originZ!=originZ)
		{
			this.originZ = originZ;
		}
	}
	
	public void setRadius(double radius)
	{

		if(this.radius!=radius)
		{
			this.radius = radius;
		}
	}
}
