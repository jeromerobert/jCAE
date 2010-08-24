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

package org.jcae.netbeans.cad.modeler;

import org.jcae.opencascade.jni.BRepPrimAPI_MakeTorus;
import org.jcae.opencascade.jni.TopoDS_Shape;

/**
 * @author Jerome Robert
 *
 */
public class Torus extends Primitive
{
	private double centerX = 0;
    private double centerY = -1;
    private double centerZ = 0;
    private double directionX = 0;
    private double directionY = 1;
    private double directionZ = 0;
    private double radius1 = 1;
    private double radius2 = 0.1;
    /**
	 * @param name
	 */
	public Torus()
	{
		setName("Torus");
	}
	/**
	 * @return Returns the centerX.
	 */
	public double getCenterX()
	{
		return centerX;
	}
	/**
	 * @return Returns the centerY.
	 */
	public double getCenterY()
	{
		return centerY;
	}
	/**
	 * @return Returns the centerZ.
	 */
	public double getCenterZ()
	{
		return centerZ;
	}
	/**
	 * @return Returns the directionX.
	 */
	public double getDirectionX()
	{
		return directionX;
	}
	/**
	 * @return Returns the directionY.
	 */
	public double getDirectionY()
	{
		return directionY;
	}
	/**
	 * @return Returns the directionZ.
	 */
	public double getDirectionZ()
	{
		return directionZ;
	}
	/**
	 * @return Returns the radius1.
	 */
	public double getRadius1()
	{
		return radius1;
	}
	/**
	 * @return Returns the radius2.
	 */
	public double getRadius2()
	{
		return radius2;
	}
	/* (non-Javadoc)
	 * @see org.jcae.netbeans.cad.ModifiableShape#rebuild()
	 */
	public TopoDS_Shape rebuild()
	{
		double[] axis=new double[]{centerX, centerY, centerZ, directionX, directionY, directionZ};
		return new BRepPrimAPI_MakeTorus(axis, radius1, radius2).shape();
	}
	/**
	 * @param centerX The centerX to set.
	 */
	public void setCenterX(double centerX)
	{
		if(this.centerX!=centerX)
		{
			this.centerX = centerX;
		}

	}
	/**
	 * @param centerY The centerY to set.
	 */
	public void setCenterY(double centerY)
	{
		if(this.centerY!=centerY)
		{
			this.centerY = centerY;
		}
	}
	/**
	 * @param centerZ The centerZ to set.
	 */
	public void setCenterZ(double centerZ)
	{
		if(this.centerZ!=centerZ)
		{
			this.centerZ = centerZ;
		}
	}
	/**
	 * @param directionX The directionX to set.
	 */
	public void setDirectionX(double directionX)
	{
		if(this.directionX!=directionX)
		{
			this.directionX = directionX;
		}
	}
	/**
	 * @param directionY The directionY to set.
	 */
	public void setDirectionY(double directionY)
	{
		if(this.directionY!=directionY)
		{
			this.directionY = directionY;
		}
	}
	/**
	 * @param directionZ The directionZ to set.
	 */
	public void setDirectionZ(double directionZ)
	{
		if(this.directionZ!=directionZ)
		{
			this.directionZ = directionZ;
		}
	}
	/**
	 * @param radius1 The radius1 to set.
	 */
	public void setRadius1(double radius1)
	{
		if(this.radius1!=radius1)
		{
			this.radius1 = radius1;
		}
	}
	/**
	 * @param radius2 The radius2 to set.
	 */
	public void setRadius2(double radius2)
	{
		if(this.radius2!=radius2)
		{
			this.radius2 = radius2;
		}	
	}
}
