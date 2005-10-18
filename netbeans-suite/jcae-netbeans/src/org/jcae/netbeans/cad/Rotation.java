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
