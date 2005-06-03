/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.

    Copyright (C) 2005
                  Jerome Robert <jeromerobert@users.sourceforge.net>

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */

package org.jcae.mesh.oemm.indexed;

import org.jcae.mesh.oemm.raw.RawNode;
import org.jcae.mesh.oemm.raw.RawStorage;
import org.jcae.mesh.oemm.BinaryTree;
import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.DataOutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import org.apache.log4j.Logger;

public class IndexedStorage
{
	private static Logger logger=Logger.getLogger(IndexedStorage.class);	
	
	/**
	 */
	public static void indexInternalVertices(String inFile, String outFile)
	{
		//IndexedOEMM ret = new IndexedOEMM();
		try
		{
			FileInputStream fis = new FileInputStream(inFile);
			DataInputStream rawIn = new DataInputStream(new BufferedInputStream(fis));
			FileChannel fc = fis.getChannel();
			DataOutputStream indOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outFile)));
			long size = fc.size();
			while(fc.position() < size)
				indexInternalVertices(null, rawIn, indOut);
			rawIn.close();
			indOut.close();
		}
		catch (FileNotFoundException ex)
		{
			logger.error("File "+inFile+" not found");
		}
		catch (IOException ex)
		{
			logger.error("I/O error when reading inFile  "+inFile);
		}
		//return ret;
	}
	
	public static void indexInternalVertices(IndexedOEMM oemm, DataInputStream bufIn, DataOutputStream bufOut)
	{
		RawNode rawNode = RawStorage.readBlockHeader(bufIn);
		logger.debug("Node cell: "+rawNode);
		BinaryTree inner = new BinaryTree();
		BinaryTree outer = new BinaryTree();
		int nrExternal = 0;
		int nrDuplicates = 0;
		long index = 0L;
		try
		{
			int [] ijk = new int[3];
			for(int nr = 0; nr < rawNode.tn; nr ++)
			{
				for (int i = 0; i < 3; i++)
				{
					ijk[0] = bufIn.readInt();
					ijk[1] = bufIn.readInt();
					ijk[2] = bufIn.readInt();
					logger.debug("Coordinates "+Integer.toHexString(ijk[0])+" "+Integer.toHexString(ijk[1])+" "+Integer.toHexString(ijk[2]));
					if (ijk[0] < rawNode.i0 || ijk[0] >= rawNode.i0 + rawNode.size ||
					    ijk[1] < rawNode.j0 || ijk[1] >= rawNode.j0 + rawNode.size ||
					    ijk[2] < rawNode.k0 || ijk[2] >= rawNode.k0 + rawNode.size)
					{
						if (outer.insert(ijk, 0))
						{
							logger.debug("External node");
							nrExternal++;
						}
						else
						{
							logger.debug("External duplicate node");
							nrDuplicates++;
						}
						continue;
					}
					if (inner.insert(ijk, index))
					{
						logger.debug("Internal node");
						index++;
					}
					else
					{
						logger.debug("Internal duplicate node");
						nrDuplicates++;
					}
				}
			}
		}
		catch (IOException ex)
		{
			logger.error("I/O error when reading intermediate file");
		}
		logger.debug("number of internal vertices: "+index);
		logger.debug("number of external vertices: "+nrExternal);
		logger.debug("number of duplicated vertices: "+nrDuplicates);
	}
	
}
