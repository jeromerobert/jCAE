/* jCAE stand for Java Computer Aided Engineering. Features are : Small CAD
   modeler, Finit element mesher, Plugin architecture.
 
	Copyright (C) 2003 Jerome Robert <jeromerobert@users.sourceforge.net>
 
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
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jcae.mesh.drivers;

import java.io.*;
import org.jcae.mesh.sd.*;
import org.jcae.mesh.algos.MeshNode2D;
import org.apache.log4j.*;
import java.util.StringTokenizer;
import gnu.trove.THashMap;


public class MSHReader extends MeshReader {

	private static Logger logger=Logger.getLogger(MSHReader.class);

	private THashMap nodesmap;
	private THashMap facesmap;
	private int dimension = 0;
	
	/**
	 * Constructor for MSHReader.
	 * @param in
	 */
	public MSHReader(InputStream in)
	{
		super(in);
		nodesmap = new THashMap();
		facesmap = new THashMap();
	}

	/**
	 * Constructor for MSHReader.
	 * @param in
	 * @param mesh
	 */
	public MSHReader(InputStream in, MeshMesh mesh)
	{
		super(in, mesh);
		nodesmap = new THashMap();
		facesmap = new THashMap();
	}


	/**
	 * @see org.jcae.mesh.drivers.MeshReader#readMesh()
	 */
	public void readMesh()
	{
		BufferedReader rd=new BufferedReader(this);
		String line = new String();
		try
		{
			while ((line=rd.readLine())!=null)
			{
				if (line.trim().equals("Dimension"))
				{
					line = rd.readLine().trim();
					dimension = java.lang.Integer.parseInt(line);
				}
				else if (line.trim().equals("Vertices"))
				{
					// read nodes
					if (dimension==2)
						readNodes2D(rd);
					else
						readNodes3D(rd);
				}
				else if (line.trim().equals("Triangles"))
				{
					// read faces
					readFaces(rd);
				}
			}
		}
		catch(Exception e)
		{
				e.printStackTrace();
		}
	}

	private boolean readNodes2D(BufferedReader rd)
	{
		nodesmap.clear();
		float x,y;
		String line = new String();

		try
		{
			line=rd.readLine().trim();
			int nbNodes= java.lang.Integer.parseInt(line);
			for(int i=1; i<=nbNodes; i++)
			{
				Integer index = new Integer(i);
				String ind = index.toString();

				//line contains coord x,y,z
				line = rd.readLine().trim();
				StringTokenizer st = new StringTokenizer(line);
				
				String x1 = st.nextToken();
				String y1 = st.nextToken();
				
				x = new Float(x1).floatValue();
				y = new Float(y1).floatValue();

/*
				MeshNode n = new MeshNode(x,y,0,new PST_3DPosition(x,y,0));
				n = mesh.addNode(n);
*/				
				//recording the node in a map in order to retrieve it faster
				nodesmap.put((Object)index, mesh.addNode(x,y,i));
				
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return true;
	}

	private boolean readNodes3D(BufferedReader rd)
	{
		nodesmap.clear();
		float x,y,z;	
		String line = new String();

		try
		{
			line=rd.readLine().trim();
			int nbNodes= java.lang.Integer.parseInt(line);
			for(int i=1; i<=nbNodes; i++)
			{
				Integer index = new Integer(i);
				String ind = index.toString();

				//line contains coord x,y,z
				line = rd.readLine().trim();
				StringTokenizer st = new StringTokenizer(line);
				
				String x1 = st.nextToken();
				String y1 = st.nextToken();
				String z1 = st.nextToken();
				
				x = new Float(x1).floatValue();
				y = new Float(y1).floatValue();
				z = new Float(z1).floatValue();
				MeshNode n = new MeshNode(x,y,z,new PST_3DPosition(x,y,z));
				n = mesh.addNode(n);
				n.setID(index.intValue());
				//recording the node in a map in order to retrieve it faster
				nodesmap.put((Object)index,(Object)n);
				
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return true;
	}

	private void readFaces(BufferedReader rd)
	{
		int vertIndex, texIndex = 0, normIndex = 0;
		String line = new String();
		facesmap.clear();
		
		try
		{
			line=rd.readLine().trim();
			int nbFaces= java.lang.Integer.parseInt(line);
			for(int i=1; i<=nbFaces; i++)
			{
				line = rd.readLine();
				StringTokenizer st = new StringTokenizer(line);
				Integer ind = new Integer(i);
				String index = ind.toString();
				Integer p1 = Integer.valueOf(st.nextToken());
				Integer p2 = Integer.valueOf(st.nextToken());
				Integer p3 = Integer.valueOf(st.nextToken());
				
				// retrieve the MeshNode corresponding to node's id
				MeshNode n1 = (MeshNode)nodesmap.get(p1);			
				MeshNode n2 = (MeshNode)nodesmap.get(p2);			
				MeshNode n3 = (MeshNode)nodesmap.get(p3);			
				MeshFace f=mesh.addTriangle(n1,n2,n3);
				f.setID(ind.intValue());
				f.setFinitElement(true);

				// fill the map of faces
				facesmap.put((Object)ind,(Object)f);
			}
			// clear the nodesmap
			nodesmap.clear();
		}
		catch(Exception e)
		{
			//out.println(line);
			e.printStackTrace();
		}
	}
	
}
