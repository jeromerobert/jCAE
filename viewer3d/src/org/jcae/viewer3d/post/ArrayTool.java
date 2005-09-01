package org.jcae.viewer3d.post;

import java.io.PrintStream;

public class ArrayTool
{
	private float min;
	private float max;
	private boolean haveNegativeInfinity;
	private float[] array;
	private boolean boudariesOK=false;
	
	public ArrayTool(float[] array)
	{
		this.array=array;
	}
	
	private void computeBounds()
	{
		for(int i=0; i<array.length; i++)
		{
			if(array[i]>max)
			{
				max=array[i];
			}
			else if(array[i]==Float.NEGATIVE_INFINITY)
			{
				haveNegativeInfinity=true;
			}
			else if(array[i]<min)
				min=array[i];		
		}
		boudariesOK=true;
	}
	
	public boolean haveNegativeInfinity()
	{
		if(!boudariesOK)
			computeBounds();
		return haveNegativeInfinity;
	}
	public float getMax()
	{
		if(!boudariesOK)
			computeBounds();

		return max;
	}
	public float getMin()
	{
		if(!boudariesOK)
			computeBounds();

		return min;
	}
	
	public void logarithm()
	{
		for(int i=0; i<array.length; i++)
		{
			array[i]=(float) (Math.log(array[i])/Math.log(10));
		}
		boudariesOK=false;
	}

	public float[] getValues()
	{
		return array;
	}
	
	public void print(PrintStream out)
	{
		for(int i=0; i<array.length; i++)
		{
			out.println(i+": "+array[i]);
		}
	}
}
