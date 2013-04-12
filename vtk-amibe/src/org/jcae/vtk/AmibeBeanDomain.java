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

package org.jcae.vtk;

import gnu.trove.set.hash.TIntHashSet;
import gnu.trove.map.hash.TIntIntHashMap;
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;
import org.jcae.mesh.xmldata.IntFileReader;
import org.jcae.mesh.xmldata.PrimitiveFileReaderFactory;
import org.w3c.dom.Element;

public class AmibeBeanDomain
{
	private final static Logger LOGGER=Logger.getLogger(AmibeBeanDomain.class.getName());

	private final Color color;
	private final int[] beam2;
	private final float[] nodes;

	
	public AmibeBeanDomain(File directory, Element subMesh, Color color)
		throws IOException
	{
		this.color=color;
		beam2=readBeam2(subMesh, directory);
		int[] nodesID=makeNodeIDArray(beam2);
		nodes=readNodes(nodesID, directory, subMesh);
		LOGGER.finest( "number of nodes=" + nodes.length +", " +
			"number of beams="+beam2.length/2.0);
		renumberArray(beam2, nodesID);		
	}
	
	private void renumberArray(int[] arrayToRenumber, int[] newIndices)
	{
		TIntIntHashMap map=new TIntIntHashMap(newIndices.length);
		for(int i=0; i<newIndices.length; i++)
		{
			map.put(newIndices[i], i);
		}
		for(int i=0; i<arrayToRenumber.length; i++)
		{
			arrayToRenumber[i]=map.get(arrayToRenumber[i]);
		}
	}
	/**
	 * Create the list of needed nodes for a beam array
	 * @param beam the beam which require nodes
	 * @return the nodes id
	 */
	private int[] makeNodeIDArray(int[] beam)
	{		
		TIntHashSet set=new TIntHashSet(beam.length/2);
		for(int index : beam)
			set.add(index);
		return set.toArray();
	}
	
	private static int[] readBeam2(Element subMesh, File directory)
		throws IOException
	{
		Element beamE=(Element) subMesh.getElementsByTagName("beams").item(0);
		Element fileE=(Element) beamE.getElementsByTagName("file").item(0);
		File f=new File(directory, fileE.getAttribute("location"));
		IntFileReader irf = new PrimitiveFileReaderFactory().getIntReader(f);
		int[] toReturn=new int[(int) (f.length()/4)];
		irf.get(toReturn);
		irf.close();
		return toReturn;
	}

	public float[] getNodes()
	{
		return nodes;
	}
	
	public int[] getBeam2()
	{
		return beam2;
	}
	
	public Color getColor()
	{
		return color;
	}
	
	public int getNumberOfNodes()
	{
		return nodes.length/3;
	}
	
	public int getNumberOfBeam2()
	{
		return beam2.length/2;
	}
	
	private float[] readNodes(int[] nodesID, File directory, Element subMesh)
		throws IOException
	{
		File f=getNodeFile(directory, subMesh);
		// Open the file and then get a channel from the stream
		FileInputStream fis = new FileInputStream(f);
		FileChannel fc = fis.getChannel();

		// Get the file's size and then map it into memory
		float[] toReturn=new float[nodesID.length*3];
		double[] tmp=new double[3];
		ByteBuffer tmpbb = ByteBuffer.allocateDirect(3 * 8);		
		
		for(int i=0; i<nodesID.length; i++)
		{
			int pos = nodesID[i]*3*8;
			if(pos < 0)
				pos = -pos;
			fc.read(tmpbb, pos);
			tmpbb.rewind();
			tmpbb.asDoubleBuffer().get(tmp, 0, 3);
			toReturn[i*3]=(float) tmp[0];
			toReturn[i*3+1]=(float) tmp[1];
			toReturn[i*3+2]=(float) tmp[2];
		}
		
		fc.close();
		fis.close();				
		return toReturn;
	}

	private File getNodeFile(File directory, Element subMesh)
	{
		Element nodeE=(Element) subMesh.getElementsByTagName("nodes").item(0);
		Element fileE=(Element) nodeE.getElementsByTagName("file").item(0);
		return new File(directory, fileE.getAttribute("location"));
	}
}
