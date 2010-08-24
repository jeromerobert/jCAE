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

import org.jcae.opencascade.jni.BRepPrimAPI_MakeBox;
import org.jcae.opencascade.jni.TopoDS_Shape;

/**
 * @author Jerome Robert
 *
 */
public class Cube extends Primitive
{
	private double x1 = -1;
	private double x2 = 1;
	private double y1 = -1;
	private double y2 = 1;
	private double z1 = -1;
	private double z2 = 1;
    /**
	 * @param name
	 */
	public Cube()
	{
		setName("Cube");
	}

	public double getX1()
	{
		return x1;
	}
	public double getX2()
	{
		return x2;
	}
	public double getY1()
	{
		return y1;
	}
	public double getY2()
	{
		return y2;
	}
	public double getZ1()
	{
		return z1;
	}
	public double getZ2()
	{
		return z2;
	}
    
	/* (non-Javadoc)
	 * @see org.jcae.netbeans.cad.ModifiableShape#rebuild()
	 */
	public TopoDS_Shape rebuild()
	{
		double[] p1=new double[]{x1, y1, z1};
		double[] p2=new double[]{x2, y2, z2};
		return new BRepPrimAPI_MakeBox(p1, p2).shape();
	}
	
	public void setX1(double x1)
	{
		if(this.x1!=x1)
		{
			this.x1 = x1;
		}
	}
	
	public void setX2(double x2)
	{
		if(this.x2!=x2)
		{
			this.x2 = x2;		
		}
	}
	
	public void setY1(double y1)
	{
		if(this.y1!=y1)
		{
			this.y1 = y1;
		}
	}
	
	public void setY2(double y2)
	{
		if(this.y2!=y2)
		{
			this.y2 = y2;
		}		
	}
	
	public void setZ1(double z1)
	{
		if(this.z1!=z1)
		{
			this.z1 = z1;
		}		
	}
	
	public void setZ2(double z2)
	{
		if(this.z2!=z2)
		{
			this.z2 = z2;		
		}
	}
}
