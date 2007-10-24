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

package org.jcae.viewer3d.fe.amibe;

import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import java.awt.Color;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.logging.Logger;
import org.jcae.viewer3d.fe.FEDomainAdapter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A FEDomain which get data from the XML/binaries file of the amibe mesher
 * @author Jerome Robert
 * @todo implement
 */
public class AmibeDomain extends FEDomainAdapter
{
	private File directory;
	private Document document;
	private int id;
	private float[] nodes;
	private int[] tria3;
	private Color color;

	/**
	 * @param document
	 * @param id
	 * @param color This parameter is only used to compute the colors of groups
	 * @throws IOException
	 */
	public AmibeDomain(File directory, Document document, int id, Color color) throws IOException
	{
		this.directory=directory;
		this.document=document;
		this.id=id;
		this.color=color;
		tria3=readTria3();
		int[] nodesID=makeNodeIDArray(tria3);
		nodes=readNodes(nodesID);
		Logger.getLogger("global").finest("number of nodes="+nodes.length+", number of tria3="+tria3.length/3.0);
		renumberArray(tria3, nodesID);		
	}
	
	private File getNodeFile()
	{
		Element xmlNodes = (Element) document.getElementsByTagName(
			"nodes").item(0);
		String a=((Element)xmlNodes.getElementsByTagName("file").item(0)).getAttribute("location");
		return new File(directory, a);
	}
	
	@Override
	public float[] getNodes()
	{
		return nodes;
	}
	/*
	 * (non-Javadoc)
	 * @see org.jcae.viewer3d.fe.FEDomainAdapter#getNumberOfNodes()
	 */
	@Override
	public int getNumberOfNodes()
	{
		return nodes.length/3;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.jcae.viewer3d.fe.FEDomainAdapter#getNumberOfTria3()
	 */
	@Override
	public int getNumberOfTria3()
	{
		return tria3.length/3;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.jcae.viewer3d.fe.FEDomainAdapter#getTria3Iterator()
	 */
	@Override
	public Iterator<int[]> getTria3Iterator()
	{
		return new Iterator<int[]>()
		{
			private int index=0;
			public void remove()
			{
				// TODO Auto-generated method stub
				throw new UnsupportedOperationException();
			}

			public boolean hasNext()
			{
				return index<tria3.length;
			}

			public int[] next()
			{
				int[] toReturn=new int[3];
				System.arraycopy(tria3, index, toReturn, 0, 3);
				index+=3;
				return toReturn;
			}
		};
	}
	
	private File getTriaFile()
	{
		Element xmlNodes = (Element) document.getElementsByTagName(
			"triangles").item(0);
		Node fn = xmlNodes.getElementsByTagName("file").item(0);
		String a=((Element)fn).getAttribute("location");
		return new File(directory, a);
	}
	
	/**
	 * @param the xml element of DOM tree corresponding to the tag "groups".
	 * @param a group.
	 * @return the xml element of DOM tree corresponding to the group.
	 */
	public static Element getXmlGroup(Element xmlGroups, int groupID)
	{
		NodeList list = xmlGroups.getElementsByTagName("group");
		int length=list.getLength();
		Element elt = null;
		int i = 0;
		boolean found = false;
		while (!found && i < length)
		{
			elt = (Element) list.item(i);
			int aId = -1;
			try
			{
				aId = Integer.parseInt(elt.getAttribute("id"));
			} catch (Exception e)
			{
				e.printStackTrace(System.out);
			}
			if (groupID == aId)
			{
				found = true;
			} else
			{
				i++;
			}
		}
		if (found)
		{
			return elt;
		} else
		{
			return null;
		}
	}
	/**
	 * Create the list of needed nodes for a triangle array
	 * @param trias the triangles which require nodes
	 * @return the nodes id
	 */
	private int[] makeNodeIDArray(int[] trias)
	{		
		TIntHashSet set=new TIntHashSet(trias.length/2);
		for(int i=0; i<trias.length; i++)
		{
			set.add(trias[i]);
		}
		return set.toArray();
	}
	
	private float[] readNodes(int[] nodesID) throws IOException
	{
		File f=getNodeFile();
		// Open the file and then get a channel from the stream
        FileInputStream fis = new FileInputStream(f);
        FileChannel fc = fis.getChannel();
 
        // Get the file's size and then map it into memory
        MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, f.length());
        DoubleBuffer nodesBuffer=bb.asDoubleBuffer();        
		float[] toReturn=new float[nodesID.length*3];		
		
		for(int i=0; i<nodesID.length; i++)
		{
			int ii=i*3;
			int iid=nodesID[i]*3;
			toReturn[ii]=(float) nodesBuffer.get(iid);
			toReturn[ii+1]=(float) nodesBuffer.get(iid+1);
			toReturn[ii+2]=(float) nodesBuffer.get(iid+2);
		}
		
		fc.close();
		fis.close();
		clean(bb);		
		return toReturn;
	}
	
	private int[] readTria3() throws IOException
	{
		Element e=getXmlGroup((Element) document.getElementsByTagName("groups").item(0), id);
		Element numberNode=(Element)e.getElementsByTagName("number").item(0);
		String v=numberNode.getChildNodes().item(0).getNodeValue();
		int number=Integer.parseInt(v);
		if(number==0)
			return new int[0];
		String groupFileN=((Element)e.getElementsByTagName("file").item(0)).getAttribute("location");
		String os=((Element)e.getElementsByTagName("file").item(0)).getAttribute("offset");
		File groupFile=new File(directory, groupFileN);		
		long offset=Long.parseLong(os);
		
		// Open the file and then get a channel from the stream
        FileInputStream fisG = new FileInputStream(groupFile);
        FileChannel fcG = fisG.getChannel();
 
        // Get the file's size and then map it into memory
        MappedByteBuffer bbG = fcG.map(FileChannel.MapMode.READ_ONLY, offset*4, number*4);		
		IntBuffer groups = bbG.asIntBuffer();
		
		
		File f=getTriaFile();
		// Open the file and then get a channel from the stream
        FileInputStream fis = new FileInputStream(f);
        FileChannel fc = fis.getChannel();
 
        // Get the file's size and then map it into memory
        MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, f.length());
        IntBuffer trias=bb.asIntBuffer();        
		int[] toReturn=new int[number*3];		
		
		for(int i=0; i<number; i++)
		{
			trias.position(groups.get(i)*3);
			trias.get(toReturn, i*3, 3);
		}
		
		fc.close();
		fis.close();
		fcG.close();
		fisG.close();
		clean(bbG);
		clean(bb);
		return toReturn;
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
	 * Workaround for Bug ID4724038.
	 * see http://bugs.sun.com/bugdatabase/view_bug.do;:YfiG?bug_id=4724038
	 */
	public static void clean(final MappedByteBuffer buffer)
	{
		try
		{
			Class cleanerClass=Class.forName("sun.misc.Cleaner");
			final Method cleanMethod=cleanerClass.getMethod("clean", null);
			AccessController.doPrivileged(new PrivilegedAction()
			{
				public Object run()
				{
					try
					{
						Method getCleanerMethod = buffer.getClass().getMethod(
							"cleaner", new Class[0]);
						
						getCleanerMethod.setAccessible(true);
						Object cleaner = getCleanerMethod.invoke(buffer,new Object[0]);
						if(cleaner!=null)
						{
							cleanMethod.invoke(cleaner, null);
						}
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
					return null;
				}
			});
		}		
		catch(ClassNotFoundException ex)
		{
			//Not a Sun JVM so we exit.
		}
		catch (SecurityException e)
		{
			e.printStackTrace();
		}
		catch (NoSuchMethodException e)
		{
			e.printStackTrace();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.fe.FEDomainAdapter#getColor()
	 */
	public Color getColor()
	{
		return color;
	}
	
	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.fe.FEDomainAdapter#getID()
	 */
	public int getID()
	{
		return id;
	}
}
