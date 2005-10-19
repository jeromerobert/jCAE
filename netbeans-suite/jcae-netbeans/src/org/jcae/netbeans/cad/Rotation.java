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

public class Rotation
{
	private double axisX1, axisY1, axisZ1, axisX2=1,
	axisY2, axisZ2, angle;

	public double getAngle()
	{
		return angle;
	}

	public void setAngle(double angle)
	{
		this.angle = angle;
	}

	public double getAxisX1()
	{
		return axisX1;
	}

	public void setAxisX1(double axisX1)
	{
		this.axisX1 = axisX1;
	}

	public double getAxisX2()
	{
		return axisX2;
	}

	public void setAxisX2(double axisX2)
	{
		this.axisX2 = axisX2;
	}

	public double getAxisY1()
	{
		return axisY1;
	}

	public void setAxisY1(double axisY1)
	{
		this.axisY1 = axisY1;
	}

	public double getAxisY2()
	{
		return axisY2;
	}

	public void setAxisY2(double axisY2)
	{
		this.axisY2 = axisY2;
	}

	public double getAxisZ1()
	{
		return axisZ1;
	}

	public void setAxisZ1(double axisZ1)
	{
		this.axisZ1 = axisZ1;
	}

	public double getAxisZ2()
	{
		return axisZ2;
	}

	public void setAxisZ2(double axisZ2)
	{
		this.axisZ2 = axisZ2;
	}
}
