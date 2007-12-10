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
 * (C) Copyright 2007, by EADS France
 */

package org.jcae.viewer3d.fe.unv;
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIntHashMap;
import java.awt.Color;
import org.jcae.viewer3d.fe.FEDomainAdaptor;

public class UNVDomain extends FEDomainAdaptor
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
		TIntHashSet nodeset=new TIntHashSet();
		tria3=parser.getTria3FromGroup(id);
		if (tria3.length > 0)
		{
			nodeset.ensureCapacity(tria3.length);
			nodeset.addAll(tria3);
		}
		quad4=parser.getQuad4FromGroup(id);
		if (quad4.length > 0)
		{
			nodeset.ensureCapacity(nodeset.size() + quad4.length);
			nodeset.addAll(quad4);
		}
		beam2=parser.getBeam2FromGroup(id);
		if (beam2.length > 0)
		{
			nodeset.ensureCapacity(nodeset.size() + beam2.length);
			nodeset.addAll(beam2);
		}
		tria6=parser.getTria6FromGroup(id);
		if (tria6.length > 0)
		{
			nodeset.ensureCapacity(nodeset.size() + tria6.length);
			nodeset.addAll(tria6);
		}

		int[] nodesID=nodeset.toArray();
		nodes=readNodes(nodesID, parser.getNodesCoordinates());

		// Compute inverse relation
		TIntIntHashMap map=new TIntIntHashMap(nodesID.length);
		for(int i=0; i<nodesID.length; i++)
			map.put(nodesID[i], i);

		for (int i = 0; i < tria3.length; i++)
			tria3[i] = map.get(tria3[i]);
		for (int i = 0; i < quad4.length; i++)
			quad4[i] = map.get(quad4[i]);
		for (int i = 0; i < beam2.length; i++)
			beam2[i] = map.get(beam2[i]);
		for (int i = 0; i < tria6.length; i++)
			tria6[i] = map.get(tria6[i]);
	}
	
	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.fe.FEDomainAdaptor#getColor()
	 */
	@Override
	public Color getColor(){
		return color;
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.fe.FEDomainAdaptor#getID()
	 */
	@Override
	public int getID(){
		return id;
	}
	
	@Override
	public float[] getNodes()
	{
		return nodes;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.jcae.viewer3dgetQuad4().fe.FEDomainAdapter#getNumberOfNodes()
	 */
	@Override
	public int getNumberOfNodes(){
		return nodes.length/3;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.jcae.viewer3d.fe.FEDomainAdaptor#getNumberOfTria3()
	 */
	@Override
	public int getNumberOfTria3(){
		//System.out.println("tria3: "+(tria3.length/3));
		return tria3.length/3;
	}
			
	/*
	 * (non-Javadoc)
	 * @see org.jcae.viewer3d.fe.FEDomainAdaptor#getNumberOfQuad4()
	 */
	@Override
	public int getNumberOfQuad4(){
		//System.out.println("quad4: "+(quad4.length/4));
		return quad4.length/4;
	}
	
	@Override
	public int getNumberOfBeam2()
	{
		//System.out.println("beam2: "+(beam2.length/2));
		return beam2.length;
	}
	
	@Override
	public int getNumberOfTria6()
	{
		//System.out.println("tria6: "+tria6.length);
		return tria6.length;
	}
	
	@Override
	public int[] getBeam2()
	{
		return beam2;
	}
	
	@Override
	public int[] getTria3()
	{
		return tria3;
	}
	
	@Override
	public int[] getTria6()
	{
		return tria6;
	}
	
	@Override
	public int[] getQuad4()
	{
		return quad4;
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

}

