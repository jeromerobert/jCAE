package org.jcae.viewer3d.fe;

import gnu.trove.TIntArrayList;

public class NodeSelectionImpl implements NodeSelection
{
	private TIntArrayList elementIDs=new TIntArrayList();
	private TIntArrayList nodeIDs=new TIntArrayList();
	private int domainID;
	
	public NodeSelectionImpl(int domainID)
	{
		this.domainID=domainID;
	}
	
	public int getCount()
	{
		return elementIDs.size();
	}

	public int getElementID(int id)
	{
		return elementIDs.get(id);
	}

	public byte getNodeID(int id)
	{
		return (byte) nodeIDs.get(id);
	}
	
	void addNode(int elementID, byte nodeID)
	{
		elementIDs.add(elementID);
		nodeIDs.add(nodeID);
	}
	
	public Object clone()
	{
		try
		{
			return super.clone();
		}
		catch (CloneNotSupportedException e)
		{		
			e.printStackTrace();
			return null;
		}
	}

	public void clear()
	{
		elementIDs.clear();
		nodeIDs.clear();
	}
	
	public String toString()
	{
		return "Elements: "+elementIDs+"\n Nodes: "+nodeIDs;
	}

	public int getDomainID()
	{
		return domainID;
	}
}
