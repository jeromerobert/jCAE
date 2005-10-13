package org.jcae.viewer3d.post;

import java.awt.*;
import java.text.DecimalFormat;
import javax.swing.JPanel;

public class Legend extends JPanel
{
	private boolean haveInfinity;
	private float min=0, max=1;
	private float minInf;
	private ColorMapper colorMapper=new DefaultColorMapper();
	
	static class DoubleFormater
	{
		static DecimalFormat f1=new DecimalFormat("0.##E0");
		static DecimalFormat f2=new DecimalFormat("##.##");
		static String format(double d)
		{
			String s1=f1.format(d);
			String s2=f2.format(d);
			if(s1.length()>=s2.length() && !s2.equals("0") && !s2.equals("-0"))
				return s2; else return s1;
		}
	}
	
	/**
	 * 
	 */
	public Legend()
	{
		setBackground(Color.BLACK);		
	}
		/* (non-Javadoc)
	 * @see java.awt.Component#getPreferredSize()
	 */
	public Dimension getPreferredSize()
	{
		return new Dimension(100, 0);
	}
	
	public void setMin(float min)
	{
		this.min=min;
	}

	public void setMax(float max)
	{
		this.max=max;
	}
	
	public void setNegativeInfinity(boolean b)
	{
		haveInfinity=b;
		if(b)
		{
			minInf=min-(max-min)/10;
		}
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.JComponent#paint(java.awt.Graphics)
	 */
	public void paint(Graphics g)
	{
		super.paint(g);
		if(min>max)
			return;
		
		Rectangle bnd = getBounds();
		int margin=g.getFontMetrics().getHeight();		
		double d;		
		
		for(int y=margin/2; y<bnd.height-margin/2; y++)
		{
			double f=(double)(y-margin/2)/(double)(bnd.height-margin);
			if(haveInfinity)
			{
				d=f*(minInf-max)+max;
				if(d<min)
					d=Float.NEGATIVE_INFINITY;
			}
			else
			{
				d=f*(min-max)+max;
			}

			g.setColor(colorMapper.mapColor((float) d));
			g.drawLine(0, y, 25, y);

			if((y-margin/2)%(margin*2)==0)
			{
				String s;
				if(haveInfinity)
				{					
					if(d<min)
						s="-\u221E";
					else
					{						
						s=DoubleFormater.format(d);
					}
				}
				else
				{				
					s=DoubleFormater.format(d);
				}
				
				g.drawString(s, 35, y+margin/2);
			}
		}
	}
	
	public void setColorMapper(ColorMapper colorMapper)
	{
		this.colorMapper = colorMapper;
	}
	public boolean isHaveInfinity()
	{
		return haveInfinity;
	}
	public float getMax()
	{
		return max;
	}
	public float getMin()
	{
		return min;
	}
}
