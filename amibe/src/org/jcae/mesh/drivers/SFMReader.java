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

/**
 * @author  Jerome Robert
 */

import java.io.*;
import java.util.*;
import org.jcae.util.StreamTokenizerExt;
import org.jcae.mesh.sd.*;
import org.apache.log4j.*;

public class SFMReader extends MeshReader
{	
	private static Logger logger=Logger.getLogger(SFMReader.class);
	
	public SFMReader(InputStream in, MeshMesh mesh)
	{
		super(in,mesh);
	}
	
	public SFMReader(InputStream in)
	{
		super(in);
	}
	
	/** read the mesh.
	 * TODO : The IDs of the nodes are currently ignored. A good id
	 * manager need to be implemented */

	public void readMesh()
	{
		int i=0;
		try
		{
			TreeMap nodeMap=new TreeMap();
			StreamTokenizerExt intk=new StreamTokenizerExt(this);						
			int nbFaces=intk.readInteger();
			int nbNodes=intk.readInteger();
			int nbEdges=intk.readInteger();
			float x,y,z;
			x=0;
			y=0;
			z=0;
			logger.info("Reading "+nbNodes+" nodes and "+nbFaces+" faces.");
			mesh.ensureNodeCapacity(mesh.numberOfNodes()+nbNodes);
			mesh.ensureFaceCapacity(mesh.numberOfFaces()+nbFaces);
			
			for(i=1;i<=nbNodes;i++)
			{
				if(i%50000==0)
				{
					//Runtime.getRuntime().gc();
					logger.debug(""+i+" nodes read. Used memory : "+Runtime.getRuntime().totalMemory()/1E6);
				}
				intk.readInteger(); //skip node ID.
				x=intk.readFloat();
				y=intk.readFloat();
				z=intk.readFloat();
				MeshNode n=new MeshNode(x,y,z,new PST_3DPosition(x,y,z));
				n.setID(i);
				mesh.addNodeFast(n);
				nodeMap.put(new Integer(i), n);
			}
			
			int currentGroupID;
			MeshGroup currentGroup=null;
			for(i=0;i<nbFaces;i++)
			{
				if(i%50000==0) 
				{
					//Runtime.getRuntime().gc();
					logger.debug(""+i+" faces read. Used memory : "+Runtime.getRuntime().totalMemory()/1E6);
				}
				int id=intk.readInteger();
				MeshNode n1,n2,n3;
				n1=(MeshNode)nodeMap.get(new Integer(intk.readInteger()));
				n2=(MeshNode)nodeMap.get(new Integer(intk.readInteger()));
				n3=(MeshNode)nodeMap.get(new Integer(intk.readInteger()));
				MeshFace f=mesh.addTriangle(n1,n2,n3);
				f.setID(id);
				currentGroupID=intk.readInteger(); // skip group id
				if((currentGroup==null)||(currentGroupID!=currentGroup.getID()))
				{
					currentGroup=new MeshGroup(mesh);
					currentGroup.setID(currentGroupID);
					currentGroup.setName(String.valueOf(currentGroupID));
					mesh.addGroup(currentGroup);
				}
				currentGroup.addFace(f);
			}
			logger.debug("Mesh now have "+mesh.numberOfNodes()+" nodes and "+
				mesh.numberOfFaces()+" faces");
		} catch(Exception ex)
		{
			ex.printStackTrace();
			logger.fatal("error at iteration i="+i);
		}
	}
	
	/** A main method to test the class */
	public static void main(String[] args)
	{
		try
		{
			SFMReader reader=new SFMReader(new FileInputStream(args[0]));
			reader.readMesh();
		} catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}
}
