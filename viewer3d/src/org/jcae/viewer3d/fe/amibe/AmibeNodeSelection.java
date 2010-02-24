package org.jcae.viewer3d.fe.amibe;

import java.io.*;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import org.jcae.viewer3d.fe.NodeSelection;
import org.w3c.dom.Element;


public class AmibeNodeSelection implements NodeSelection
{
	private int[] elementIDs;
	private byte[] nodeIDs;
	
	public AmibeNodeSelection(AmibeProvider provider, NodeSelection[] ns) throws IOException
	{
		int count=0;
		for(int i=0; i<ns.length; i++)
		{
			count+=ns[i].getCount();
		}
		
		elementIDs=new int[count];
		int k=0;
		for(int i=0; i<ns.length; i++)
		{
			Element groupElement=AmibeDomain.getXmlGroup(
				(Element) provider.getDocument().getElementsByTagName("groups").item(0),
				ns[i].getDomainID());
			Element numberNode=(Element)groupElement.getElementsByTagName("number").item(0);
			String v=numberNode.getChildNodes().item(0).getNodeValue();
			int number=Integer.parseInt(v);
			
			String groupFileN=((Element)groupElement.
				getElementsByTagName("file").item(0)).getAttribute("location");
			
			String os=((Element)groupElement.getElementsByTagName("file").item(0)).getAttribute("offset");
			if (os.isEmpty())
				os = "0";
			File groupFile=new File(provider.getDirectory(), groupFileN);		
			long offset=Long.parseLong(os);
			System.out.println(groupFile);
			FileInputStream fos=new FileInputStream(groupFile);
			FileChannel fc=fos.getChannel();
	        // Get the file's size and then map it into memory
	        MappedByteBuffer bbG = fc.map(FileChannel.MapMode.READ_ONLY, offset*4, number*4);		
			IntBuffer groups = bbG.asIntBuffer();
			for(int j=0; j<ns[i].getCount(); j++)
			{
				elementIDs[k++]=groups.get(ns[i].getElementID(j));
			}
			AmibeDomain.clean(bbG);
			fc.close();
			fos.close();
		}
		
		k=0;
		nodeIDs=new byte[count];
		for(int i=0; i<ns.length; i++)
		{
			for(int j=0; j<ns[i].getCount(); j++)
				nodeIDs[k++]=ns[i].getNodeID(j);
		}
	}

	public int getDomainID()
	{
		return -1;
	}

	public int getCount()
	{
		return elementIDs.length;
	}

	public int getElementID(int id)
	{
		return elementIDs[id];
	}

	public byte getNodeID(int id)
	{
		return nodeIDs[id];
	}

	@Override
	public Object clone()
	{
		try
		{
			return super.clone();
		} catch (CloneNotSupportedException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	@Override
	public String toString()
	{
		String toReturn="";
		for(int i=0; i<getCount(); i++)
		{
			toReturn+="E: "+getElementID(i)+" N: "+getNodeID(i);
		}		
		return toReturn;
	}
}
