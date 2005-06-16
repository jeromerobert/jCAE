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

package org.jcae.viewer3d.fd.sd;
import java.io.*;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
/**
 *
 * @author  Jerome Robert
 */
public class Jqf02File
{	
	private static Logger logger=Logger.getLogger(Jqf02File.class);
	private File file;
	private float version;
	private float[] iterations;
	private int nfx,nfy,nfz;
	private long headerSize;
	private long iterationBlockSize;
	private float minValue=Float.MAX_VALUE;
	private float maxValue=-Float.MAX_VALUE;
	public float[] getIterations()
	{
		return iterations;
	}

	/** return the minimum value read by the last readValue() call */
	public float getMinValue()
	{
		return minValue;
	}

	/** return the maximum value read by the last readValue() call */
	public float getMaxValue()
	{
		return maxValue;
	}

	public void readValue(int iteration, List plates, int valueType) throws IOException
	{
		long offset=headerSize+iteration*iterationBlockSize+12;
		DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
		in.skip(offset);
		minValue=Float.MAX_VALUE;
		maxValue=-Float.MAX_VALUE;
		logger.debug("offset is "+offset);
		for(Iterator it=plates.iterator();it.hasNext();)
		{
			Plate p=(Plate)it.next();
			int nbcells=p.numberOfCells();
			p.values=new float[nbcells];
			//logger.debug(p);
			for(int ip=0;ip<nbcells;ip++)
			{
				in.readInt(); offset+=4;
				for(int j=0;j<valueType;j++)
				{
					in.readFloat();
					offset+=4;
				}
				p.values[ip]=in.readFloat(); offset+=4;				
				//logger.debug("readValue "+p.values[ip]+" at "+(offset-4));
				if(p.values[ip]>maxValue) maxValue=p.values[ip];
				if(p.values[ip]<minValue) minValue=p.values[ip];
				for(int j=valueType+1;j<3;j++)
				{
					in.readFloat();
					offset+=4;
				}
				in.readInt(); offset+=4;
				/*in.skip(16); offset+=16;*/
			}
		}
		logger.info("maximum value= "+maxValue);
		logger.info("minimum value= "+minValue);
	}
	
	/** Creates a new instance of Jqf02File */
	public Jqf02File()
	{
	}

	/** Creates a new instance of Jqf02File */
	/*public Jqf02File(File file)
	{
		init(file);
	}*/


	public void init(File file) throws IOException
	{		
		this.file=file;
		DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
		
		// solver version number
		in.readInt();
		version=in.readFloat();
		logger.info("Version "+version);
		in.readInt();
		
		// number of iterations
		in.readInt();		
		iterations=new float[in.readInt()];
		in.readInt();
		
		// the iterations
		in.readInt();
		for(int i=0;i<iterations.length;i++) iterations[i]=in.readFloat();
		in.readInt();
		
		in.readInt();
		nfx=in.readInt();
		nfy=in.readInt();
		nfz=in.readInt();
		logger.info("Surface cell x,y,z : "+nfx+" "+nfy+" "+nfz);
		in.readInt();

		headerSize=(3+3+iterations.length+2+5)*4;
		//iterationBlockSize=4*(32+1+3*(nfx+nfy+nfz));
		iterationBlockSize=4*(3+5*(nfx+nfy+nfz));
	}
}
