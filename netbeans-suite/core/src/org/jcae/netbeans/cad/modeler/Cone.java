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

import org.jcae.opencascade.jni.BRepPrimAPI_MakeCone;
import org.jcae.opencascade.jni.TopoDS_Shape;

/**
 * @author Jerome Robert
 *
 */
public class Cone extends Primitive
{
    private double centerX = 0;
    private double centerY = -1;
    private double centerZ = 0;
    private double axisX = 0;
    private double axisY = 1;
    private double axisZ = 0;
    private double radius1 = 1;
    private double radius2 = 0;
    private double height = 2;	

	public Cone()
	{
		setName("Cone");
	}
	
	/* (non-Javadoc)
	 * @see org.jcae.netbeans.cad.ModifiableShape#rebuild()
	 */
	public TopoDS_Shape rebuild()
	{
		double[] axis=new double[]{centerX, centerY, centerZ, axisX, axisY, axisZ};
		return new BRepPrimAPI_MakeCone(axis,radius1,radius2,height,2*Math.PI).shape();
	}
	
	public double getAxisX()
	{
		return axisX;
	}
	public void setAxisX(double axisX)
	{
		if(this.axisX!=axisX)
		{
			this.axisX = axisX;
		}		
	}
	public double getAxisY()
	{
		return axisY;
	}
	public void setAxisY(double axisY)
	{
		if(this.axisY!=axisY)
		{
			this.axisY = axisY;
		}		
	}
	public double getAxisZ()
	{
		return axisZ;
	}
	public void setAxisZ(double axisZ)
	{
		if(this.axisZ!=axisZ)
		{
			this.axisZ = axisZ;
		}
	}
	public double getCenterX()
	{
		return centerX;
	}
	public void setCenterX(double centerX)
	{
		if(this.centerX!=centerX)
		{
			this.centerX = centerX;		
		}
	}
	public double getCenterY()
	{
		return centerY;
	}
	public void setCenterY(double centerY)
	{
		if(this.centerY!=centerY)
		{
			this.centerY = centerY;
		}
	}
	public double getCenterZ()
	{
		return centerZ;
	}
	
	public void setCenterZ(double centerZ)
	{
		if(this.centerZ!=centerZ)
		{
			this.centerZ = centerZ;
		}
	}
	
	public double getHeight()
	{
		return height;
	}
	public void setHeight(double height)
	{
		if(this.height!=height)
		{
			this.height = height;
		}		
	}
	public double getRadius1()
	{
		return radius1;
	}
	public void setRadius1(double radius1)
	{
		if(this.radius1!=radius1)
		{
			this.radius1 = radius1;
		}
	}
	public double getRadius2()
	{
		return radius2;
	}
	public void setRadius2(double radius2)
	{
		if(this.radius2!=radius2)
		{
			this.radius2 = radius2;
		}			
	}
}
