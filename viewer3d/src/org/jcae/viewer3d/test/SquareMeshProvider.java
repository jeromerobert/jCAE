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
 * (C) Copyright 2007, by EADS France
 */

package org.jcae.viewer3d.test;

import java.awt.Color;
import java.util.Iterator;
import javax.vecmath.Point3f;
import org.jcae.viewer3d.Domain;
import org.jcae.viewer3d.Palette;
import org.jcae.viewer3d.fe.FEDomainAdaptor;
import org.jcae.viewer3d.fe.FEProvider;
import com.sun.j3d.utils.geometry.GeometryInfo;

/** A simple mesh which ca easily grow to load the graphic card */
public class SquareMeshProvider implements FEProvider
{
	private int PLATE_NX=100;
	private int PLATE_NY=100;
	private final static int PLATE_SX=1;
	private final static int PLATE_SY=1;
	private int cellNumber;
	
	public SquareMeshProvider(int n)
	{
		setCellNumber(n);
	}
	
	public int getCellNumber()
	{
		return cellNumber; 
	}
	
	public void setCellNumber(int n)
	{
		PLATE_NX=(int) Math.sqrt(n);
		PLATE_NY=(int) Math.sqrt(n);
		cellNumber=PLATE_NX*PLATE_NY*2;
	}
		
	private float[] createOnePlate(float x, float y)
	{
		float[] toReturn=new float[PLATE_NX*PLATE_NY*6*3];
		int k=0;
		for(int i=0; i<PLATE_NX; i++)
			for(int j=0; j<PLATE_NY; j++)
			{
				toReturn[k++]=x+i*PLATE_SX;
				toReturn[k++]=y+j*PLATE_SY;
				k++; //z
				toReturn[k++]=x+(i+1)*PLATE_SX;
				toReturn[k++]=y+j*PLATE_SY;
				k++; //z
				toReturn[k++]=x+i*PLATE_SX;
				toReturn[k++]=y+(j+1)*PLATE_SY;
				k++; //z
				toReturn[k++]=x+i*PLATE_SX;
				toReturn[k++]=y+(j+1)*PLATE_SY;
				k++; //z				
				toReturn[k++]=x+(i+1)*PLATE_SX;
				toReturn[k++]=y+(j+1)*PLATE_SY;
				k++; //z				
				toReturn[k++]=x+(i+1)*PLATE_SX;
				toReturn[k++]=y+j*PLATE_SY;
				k++; //z				
			}
		return toReturn;
	}

	private float[] pointToFloat(Point3f[] p)
	{
		float[] toReturn=new float[p.length*3];
		for(int i=0; i<p.length; i++)
		{
			toReturn[3*i]=p[i].getX();
			toReturn[3*i+1]=p[i].getY();
			toReturn[3*i+2]=p[i].getZ();
		}
		return toReturn;
	}
	
	public Domain getDomain(final int id)
	{
		GeometryInfo gi=new GeometryInfo(GeometryInfo.TRIANGLE_ARRAY);
		gi.setCoordinates(createOnePlate(0, 0));
		gi.convertToIndexedTriangles();
		final int[] indices=gi.getCoordinateIndices();
		final float[] coordsn=pointToFloat(gi.getCoordinates());

		return new FEDomainAdaptor()
		{
			private Palette palette=new Palette(35);
			public float[] getNodes()
			{
				return coordsn;
			}
			public int getNumberOfNodes()
			{
				return coordsn.length;
			}
			
			public int getNumberOfTria3()
			{
				return indices.length/3;
			}
			
			public Iterator getTria3Iterator()
			{
				return new Iterator()
				{
					private int i=0;
					public boolean hasNext()
					{
						return i<indices.length;
					}

					public Object next()
					{																		
						int[] toreturn=new int[]{indices[i], indices[i+1], indices[i+2]};
						i+=3;
						return toreturn;
					}
					
					public void remove()
					{				
						throw new UnsupportedOperationException();
					}
				};
			}
			
			public Color getColor()
			{
				return palette.getColor(id);				
			}
		};
	}

	public int[] getDomainIDs()
	{
		return new int[]{0};
	}
}


