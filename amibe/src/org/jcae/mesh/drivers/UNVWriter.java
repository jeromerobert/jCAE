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
	Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA */

package org.jcae.mesh.drivers;

/**
 * @author Marie-Helene Garat
 */
import org.jcae.mesh.sd.*;
import java.io.*;
import java.util.*;
import org.apache.log4j.Logger;

public class UNVWriter extends MeshWriter
{
	private static Logger logger=Logger.getLogger(UNVWriter.class);
	public UNVWriter(OutputStream out, MeshMesh mesh)
	{
		super(out,mesh);
	}
	
	public UNVWriter(OutputStream out)
	{
		super(out);
	}
	
	/** */
	private MeshEdge getNextEdge(MeshFace face, MeshEdge currentEdge, MeshNode commonNode)
	{
		Iterator it=face.getEdgesIterator();
		while(it.hasNext())
		{
			MeshEdge e=(MeshEdge)it.next();
			if(e!=currentEdge)
			{
				if((e.getNodes1()==commonNode)||(e.getNodes2()==commonNode))
					return e;
			}
		}
		return currentEdge;
	}
	
	/** Return nodes of a face in the order needed by the UNV format */
	private Collection getNodesOfFace(MeshFace face)
	{
		ArrayList ns=new ArrayList();
		Iterator it=face.getEdgesIterator();
		MeshEdge firstEdge, edge;
		MeshNode pnode,onode;
		
		firstEdge=(MeshEdge)it.next();
		pnode=firstEdge.getNodes1();
		onode=firstEdge.getNodes2();
		ns.add(pnode);
		if(firstEdge.getMidNode()!=null) ns.add(firstEdge.getMidNode());
		
		edge=getNextEdge(face, firstEdge, onode);
		while(edge!=firstEdge)
		{			
			pnode=onode;
			
			if(pnode==edge.getNodes2()) onode=edge.getNodes1();
			else onode=edge.getNodes2();
			
			ns.add(pnode);
			if(edge.getMidNode()!=null) ns.add(edge.getMidNode());
			
			edge=getNextEdge(face, edge, onode);
		}

		return ns;
	}
		
	private void writeFaces(PrintWriter out)
	{
		logger.info("Writing faces");
		//bloc de faces
		out.println("    -1");
		out.println("2412");
		Iterator itface = mesh.getFacesIterator();
		int i = 1;
		while (itface.hasNext())
		{
			MeshFace f=(MeshFace)itface.next();
			f.setID(i);
			Collection ns = getNodesOfFace(f);			
			out.print(spaces(""+i)+"        ");
			switch(ns.size())
			{
				case 3: out.print("91"); break;
				case 6: out.print("92"); break;
				default: continue;
			}
			out.println("         1         1         1         "+ns.size());			

			Iterator itn=ns.iterator();
			while (itn.hasNext())
			{
				out.print(spaces(""+((MeshNode)itn.next()).getID()));
			}
			
			out.println();
			i++;
		}
		out.println("    -1");
	}
	
	public void writeMesh()	
	{
		//mesh.setArbitraryMidPoints();
		PrintWriter out=new PrintWriter(this);
		logger.info("Writing nodes");
		//bloc de points
		out.println("    -1");
		out.println("2411");
		
		Iterator itnode = mesh.getNodesIterator();
		int i = 1;
		while (itnode.hasNext())
		{			
			MeshNode p = (MeshNode) itnode.next();
			// Test to avoid to record useless node
			if (p.getElements().size()>0)
			{			
				out.println(i+"         1         1         1");
				p.setID(i);
				out.println(p.getX() + " " + p.getY() + " " + p.getZ());
				i++;
			}
			else p.setID(0);
		}
		out.println("    -1");		
		
		writeFaces(out);
		logger.info("Writing groups");

		//bloc de groupes
		Iterator itgroup = mesh.getGroupsIterator();
		if (itgroup.hasNext())
		{
			out.println("    -1");
			out.println("2430");
			i = 0;
			while (itgroup.hasNext())
			{
				MeshGroup g = (MeshGroup) itgroup.next();
				String line = "1      0         0         0         0         0         0      "+g.numberOfFaces();
				out.println(line);
				out.println(g.getName());
				Iterator itface = g.getFacesIterator();
				int counter=0;
				while (itface.hasNext())
				{
					MeshFace myf = (MeshFace) itface.next();						
					out.print("         8"+spaces(""+myf.getID()));
					counter++;
					if((counter%4)==0) out.println();
				}
				if((counter%4)!=0) out.println();
			}
			out.println("    -1");	
		}

		out.close();			
		//mesh.removeMidPoints();
	}
	
	/** Add spaces before a string to make it 10 characters */
	private String spaces(String s)
	{
		int n=10-s.length();
		char[] c=new char[n];
		for(int i=0;i<n;i++) c[i]=' ';
		return (new String(c))+s;
	}	
}
