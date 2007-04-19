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

package org.jcae.viewer3d.fe.unv;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import java.awt.Color;
import java.util.Iterator;
import org.jcae.viewer3d.fe.FEDomainAdapter;

public class UNVDomain extends FEDomainAdapter
{
	private Color color;
	private int id;
	private float[] nodes;
	private int[] tria3=new int[0];
	private int[] quad4=new int[0];
	private int[] beam2=new int[0];
	private int[] tria6=new int[0];
	
	public UNVDomain(UNVParser parser, int id, Color color)
	{
		this.id=id;
		this.color=color;
		if(id==UNVProvider.OTHERS_GROUP)
		{
			nodes=parser.getNodesCoordinates();
			beam2=parser.getBeam2Indices();
			tria6=parser.getTria6Indices();	
		}
		else if(id<parser.getTria3GroupNames().length)
		{
			tria3=readTria3(parser);
			int[] nodesID=makeNodeIDArray(tria3);
			nodes=readNodes(nodesID, parser.getNodesCoordinates());
			renumberArray(tria3, nodesID);
		}
		else
		{
			quad4=readQuad4(parser);
			int[] nodesID=makeNodeIDArray(quad4);
			nodes=readNodes(nodesID, parser.getNodesCoordinates());
			renumberArray(quad4, nodesID);			
		}		
	}
	
	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.fe.FEDomainAdapter#getColor()
	 */
	public Color getColor(){
		return color;
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.fe.FEDomainAdapter#getID()
	 */
	public int getID(){
		return id;
	}
	
	public float[] getNodes()
	{
		return nodes;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.jcae.viewer3d.fe.FEDomainAdapter#getNumberOfNodes()
	 */
	public int getNumberOfNodes(){
		return nodes.length/3;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.jcae.viewer3d.fe.FEDomainAdapter#getNumberOfTria3()
	 */
	public int getNumberOfTria3(){
		return tria3.length/3;
	}
			
	/*
	 * (non-Javadoc)
	 * @see org.jcae.viewer3d.fe.FEDomainAdapter#getNumberOfTria3()
	 */
	public int getNumberOfQuad4(){
		return quad4.length/3;
	}
	
	public int getNumberOfBeam2()
	{
		System.out.println("beam2: "+beam2.length);
		return beam2.length;
	}
	
	public int[] getBeam2Indices()
	{
		return beam2;
	}
	
	public int getNumberOfTria6()
	{
		System.out.println("tria6: "+tria6.length);
		return tria6.length;
	}
	
	public int[] getTria6()
	{
		return tria6;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.jcae.viewer3d.fe.FEDomainAdapter#getTria3Iterator()
	 */
	public Iterator getTria3Iterator()
	{
		return new Iterator()
		{
			private int index=0;
			public boolean hasNext()
			{
				return index<tria3.length;
			}

			public Object next()
			{
				int[] toReturn=new int[3];
				System.arraycopy(tria3, index, toReturn, 0, 3);
				index+=3;
				return toReturn;
			}

			public void remove()
			{
				// TODO Auto-generated method stub
				throw new UnsupportedOperationException();
			}
		};
	}
	
	public int[] getQuad4()
	{
		return quad4;
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
	
	private float[] readNodes(int[] nodesID, float[] allNodes)
	{
		float[] toReturn=new float[nodesID.length*3];		
		
		for(int i=0; i<nodesID.length; i++)
		{
			int ii=i*3;
			int iid=nodesID[i]*3;
			toReturn[ii]=allNodes[iid];
			toReturn[ii+1]=allNodes[iid+1];
			toReturn[ii+2]=allNodes[iid+2];
		}
		
		return toReturn;
	}

	private int[] readQuad4(UNVParser parser)
	{
		int[] elids=parser.getQuad4Groups()[id-parser.getTria3GroupNames().length];
		int[] toReturn=new int[elids.length*4];
		int[] tri=parser.getQuad4Indices();		
		for(int i=0; i<elids.length; i++)
		{
			int ii=i*4;
			int iid=elids[i]*4;
			toReturn[ii++]=tri[iid++];
			toReturn[ii++]=tri[iid++];
			toReturn[ii++]=tri[iid++];
			toReturn[ii++]=tri[iid++];
		}
		
		return toReturn;
	}
	
	private int[] readTria3(UNVParser parser)
	{
		int[] elids=parser.getTria3Groups()[id];
		int[] toReturn=new int[elids.length*3];
		int[] tri=parser.getTria3Indices();
		for(int i=0; i<elids.length; i++)
		{
			int ii=i*3;
			int iid=elids[i]*3;
			toReturn[ii++]=tri[iid++];
			toReturn[ii++]=tri[iid++];
			toReturn[ii++]=tri[iid++];
		}
		
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

}

