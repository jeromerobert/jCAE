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
import org.apache.log4j.*;
import java.util.StringTokenizer;
import gnu.trove.THashMap;


/**
 * @author cb
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class UNVReader extends MeshReader {

	private static Logger logger=Logger.getLogger(UNVReader.class);

	private float unit = 1.0f;
	
	private THashMap nodesmap;
	private THashMap facesmap;
	
	/**
	 * Constructor for UNVReader.
	 * @param in
	 */
	public UNVReader(InputStream in)
	{
		super(in);
		nodesmap = new THashMap();
		facesmap = new THashMap();
	}

	/**
	 * Constructor for UNVReader.
	 * @param in
	 * @param mesh
	 */
	public UNVReader(InputStream in, MeshMesh mesh)
	{
		super(in, mesh);
		nodesmap = new THashMap();
		facesmap = new THashMap();
	}


	/**
	 * @see org.jcae.mesh.drivers.MeshReader#readMesh
	 */
	public void readMesh()
	{
		BufferedReader rd=new BufferedReader(this);	
		String line = new String();
		try
		{
			while ((line=rd.readLine())!=null)
			{
				if (line.trim().equals("-1"))
				{
					line = rd.readLine();
					if (line.trim().equals("2411"))
					{
						// read nodes
						readNodes(rd);
					}
					else if (line.trim().equals("2412"))
					{
						// read faces
						readFace(rd);
					}
					else if (line.trim().equals("164"))
					{
						// read unit
						readUnit(rd);
					}
					else if ( (line.trim().equals("2430")) || (line.trim().equals("2435")) )
					{
						// read groups
						readGroup(rd);
					}
					else if (line.trim().equals("2414"))
					{
						// read colors
					}
					else 
					{
					// default group
					// read end of group
						while (!(line=rd.readLine().trim()).equals("-1"))
						{
						
						}
					}	
				}	
			}
		}
		catch(Exception e)
		{
				e.printStackTrace();
		}		
	}

	private boolean readUnit(BufferedReader rd)
	{
		String line = new String();
		try 
		{
			//retrieve the second line
			line = rd.readLine();
			line = rd.readLine();
			
			// fisrt number : the unit
			StringTokenizer st = new StringTokenizer(line);
			String unite = st.nextToken();
			unite = unite.replace('D','E');
			unit = new Float(unite).floatValue();
			while(!(line=rd.readLine().trim()).equals("-1"))
			{
				// ???
			}					
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return true;
	}

	private boolean readNodes(BufferedReader rd)
	{
		boolean duplicateNodesFound=false;
		nodesmap.clear();
		float x,y,z;		
		String line = new String();
		try 
		{
			while(!(line=rd.readLine().trim()).equals("-1"))
			{
				//First number : the node's id
				StringTokenizer st = new StringTokenizer(line);
				Integer index = new Integer(st.nextToken());
				line = rd.readLine();
				
				//line contains coord x,y,z	
				st = new StringTokenizer(line);
				String x1 = st.nextToken();
				String y1 = st.nextToken();
				String z1;
				try
				{
					z1 = st.nextToken();
				}
				catch (java.util.NoSuchElementException ex)
				{
					z1="0.0";
				}
				
				x1 = x1.replace('D','E');
				y1 = y1.replace('D','E');
				z1 = z1.replace('D','E');
				x = new Float(x1).floatValue()/unit;
				y = new Float(y1).floatValue()/unit;
				z = new Float(z1).floatValue()/unit;
				MeshNode n = new MeshNode(x,y,z,new PST_3DPosition(x,y,z));
				n.setID(index.intValue());
				//mesh.getNodelist().add(n);				
				MeshNode n2=mesh.addNode(n);
				if(n2!=n) duplicateNodesFound=true;
				//recording the node in a map in order to retrieve it faster
				nodesmap.put((Object)index,(Object)n2);				
			}	
			if(duplicateNodesFound)
				logger.warn("The UNV file contains at least one duplicated node."+
				" jCAE merged theses nodes. It may have removed some free edges.");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return true;
	}

	private void readFace(BufferedReader rd)
	{
		int vertIndex, texIndex = 0, normIndex = 0;
		String line = new String();
	
		facesmap.clear();
		
		try
		{
			while (!(line=rd.readLine().trim()).equals("-1"))
			{
				// first line: type of object
				StringTokenizer st = new StringTokenizer(line);
				String index = st.nextToken();
				String type = st.nextToken();
				Integer ind = new Integer(index);
				Integer p1=null,p2=null,p3=null;
				if (type.equals("74") || type.equals("91"))
				{
					line=rd.readLine();
					// triangle
					st = new StringTokenizer(line);
					p1 = Integer.valueOf(st.nextToken());
					p2 = Integer.valueOf(st.nextToken());
					p3 = Integer.valueOf(st.nextToken());
				}
				if (type.equals("24"))
				{
					// line
					line = rd.readLine();
					line = rd.readLine();
					st = new StringTokenizer(line);
					p1 = Integer.valueOf(st.nextToken());
					p2 = Integer.valueOf(st.nextToken());
					p3 = Integer.valueOf(st.nextToken());
				}
				if (type.equals("92") || type.equals("72"))
				{
					line=rd.readLine();
					// triangle 2nd version
					st = new StringTokenizer(line);
					p1 = Integer.valueOf(st.nextToken());
					Integer p4 = Integer.valueOf(st.nextToken());
					p2 = Integer.valueOf(st.nextToken());
					Integer p5 = Integer.valueOf(st.nextToken());
					p3 = Integer.valueOf(st.nextToken());
					Integer p6 = Integer.valueOf(st.nextToken());
				}
				if (type.equals("94"))
				{
					// QUADRANGLE .................
				}
			
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
	
	private void readGroup(BufferedReader rd)
	{
		String line = new String();
		try
		{
			line = rd.readLine();
			while(!line.trim().equals("-1"))
			{
				// read the number of elements to read in the last number of the line
				StringTokenizer st = new StringTokenizer(line);
				String snb = new String();
				String noBloc = st.nextToken();
				while(st.hasMoreTokens())
				{
					snb = st.nextToken();	
				}
				int nbelem = Integer.valueOf(snb).intValue();
				// Read group name
				String title = rd.readLine().trim();
				MeshGroup g = new MeshGroup(mesh);
				g.setName(title);
				// read the group
				while ((line= rd.readLine().trim()).startsWith("8"))
				{
					st = new StringTokenizer(line);
					// read one element over two, the first one doesnt matter
					while(st.hasMoreTokens())
					{
						st.nextToken();
						String index = st.nextToken();
						Integer ind = new Integer(index);
						if (ind.intValue()!=0)
						{					
							MeshFace f1 = (MeshFace)facesmap.get(ind);
							g.addFace(f1);
						}
					}
				}
				mesh.addGroup(g);
				
			}
			facesmap.clear();	
		}	
		catch(Exception e)
		{
			e.printStackTrace();		
		}
	}
	
	
	
	
	
}	
