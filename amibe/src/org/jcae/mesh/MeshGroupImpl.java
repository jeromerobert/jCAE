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

package org.jcae.mesh;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import org.apache.log4j.Logger;
import org.omg.CORBA.portable.ObjectImpl;

import org.jcae.mesh.drivers.UNVWriter;
import org.jcae.mesh.drivers.XMLWriter;
import org.jcae.mesh.sd.MeshEdge;
import org.jcae.mesh.sd.MeshNode;
import org.jcae.mesh.sd.MeshFace;

/** This is the CORBA interface for {@link org.jcae.mesh.sd.MeshGroup}
 * @author  Jerome Robert
 */
public class MeshGroupImpl implements MeshGroupOperations
{
	private static Logger logger=Logger.getLogger(org.jcae.mesh.drivers.SFMReader.class);
	private org.jcae.mesh.sd.MeshGroup group;
	private MeshModuleImpl module;
	/** Creates a new instance of MeshGroupImpl
	 * @param group The low level implementing object
	 * @param module
	 */
	public MeshGroupImpl(org.jcae.mesh.sd.MeshGroup group, MeshModuleImpl module)
	{
		this.module=module;
		this.group=group;
	}
	
	/** Used to display the mesh
	 * @param coordinates The coordinates (x,y,z) of the nodes of the mesh. The size of the array will be
	 * 3*number_of_nodes.
	 * @param indices The indices of the triangles of the mesh. The size of the array will be
	 * 3*number_of_triangles.
	 */	
	public void getRawMesh(floatsHolder coordinates, longsHolder indices)
	{
		int numberOfFaces=group.numberOfFaces();
		if(numberOfFaces==0)
		{
			indices.value=new int[0];
			coordinates.value=new float[0];
			logger.warn("There is an empty group in your mesh. It may be a bug");
		}
		else
		{
			indices.value=new int[numberOfFaces*3];
			coordinates.value=group.getRawMesh(indices.value);
		}
		logger.debug("getRawMesh : coordinates.length="+coordinates.value.length
			+" indices.length="+indices.value.length);
	}
	
	/** Used to display free edges
	 * @param coordinates The array will receive the coordinates of the nodes. It's size will be
	 * 6*number_of_edges.
	 */	
	public void getRawFreeEdges(floatsHolder coordinates)
	{
		coordinates.value=edgesCollectionToFloatArray(group.getFreeEdges());
	}
	
	/** Used to display multiple edges
	 * @param coordinates The array will receive the coordinates of the nodes. It's size will be
	 * 6*number_of_edges.
	 */	
	public void getRawTEdges(floatsHolder coordinates)
	{
		coordinates.value=edgesCollectionToFloatArray(group.getTEdges());
	}
	
	private float[] edgesCollectionToFloatArray(Collection edges)
	{
		float[] coords=new float[edges.size()*6];
		Iterator it=edges.iterator();
		for(int i=0;it.hasNext();i+=6)
		{
			MeshEdge edge=(MeshEdge)it.next();
			MeshNode n=edge.getNodes1();
			coords[i  ]=(float)n.getX();
			coords[i+1]=(float)n.getY();
			coords[i+2]=(float)n.getZ();
			n=edge.getNodes2();
			coords[i+3]=(float)n.getX();
			coords[i+4]=(float)n.getY();
			coords[i+5]=(float)n.getZ();			
		}
		return coords;
	}
	
	private float[] facesCollectionToFloatArray(Collection faces)
	{
		float[] coords=new float[faces.size()*9];
		Iterator it=faces.iterator();
		for(int i=0;it.hasNext();)
		{
			MeshFace face=(MeshFace)it.next();
			Iterator itn=face.getNodesIterator();
			for(;itn.hasNext();i+=3)
			{
				MeshNode n=(MeshNode)itn.next();
				coords[i  ]=(float)n.getX();
				coords[i+1]=(float)n.getY();
				coords[i+2]=(float)n.getZ();
			}
		}
		return coords;	
	}
	
	/** Get the ID of the group
	 * @return The ID of the group
	 */	
	public int getID()
	{
		return group.getID();
	}
	
	/** Change the id of the group
	 * @param id The new ID.
	 */	
	public void setID(int id)
	{
		group.setID(id);
	}
		
	/** Do a copy of the current MeshGroup object. This methode create a new CORBA
	 * object.
	 * @return a copy of the current object
	 */	
	public org.jcae.mesh.MeshGroup _clone()
	{		
		MeshGroupImpl impl=new MeshGroupImpl(
			(org.jcae.mesh.sd.MeshGroup)group.clone(),module);
		String thisName=module.getPath(this);
		String newName=module.renameInPath(thisName,impl.name());
		return (org.jcae.mesh.MeshGroup)module.activateObject(impl,newName);
	}
	
	/** Do intersection of 2 groups
	 * @param meshGroup The group to be intersected with the current one
	 */	
	public void intersect(org.jcae.mesh.MeshGroup meshGroup)
	{		
		group.intersect(getImplGroup(meshGroup));
	}
	
	/** Add all elements of a MeshGroup to the current MeshGroup.
	 * @param meshGroup The MeshGroup to be added
	 */	
	public void add(org.jcae.mesh.MeshGroup meshGroup)
	{
		group.add(getImplGroup(meshGroup));
	}
	
	private static org.jcae.mesh.sd.MeshGroup getImplGroup(org.jcae.mesh.MeshGroup group)
	{
		ObjectImpl oi=(ObjectImpl)group;
		org.omg.CORBA.portable.ServantObject _so = oi._servant_preinvoke( "",Object.class);
		MeshGroupPOATie localServantTie = (MeshGroupPOATie)_so.servant;
		MeshGroupImpl impl=(MeshGroupImpl)localServantTie._delegate();
		return impl.group;	
	}
	
	public void getRawBadTriangle(org.jcae.mesh.floatsHolder coordinates,
		float quality)
	{
		coordinates.value=facesCollectionToFloatArray(
			group.getWorstTriangle(quality));
	}
	
	public String name()
	{
		return group.getName();
	}
	
	public void name(String arg)
	{
		group.setName(arg);
		String path=module.getPath(this);
		module.rename(path,arg);
	}
	
	public String toString()
	{
		return name()+"."+MeshModuleImpl.dotEscapedNoImplClassName(getClass());
	}
	
	public void addGroup(org.jcae.mesh.MeshGroup aGroup)
	{
		group.addSubGroup(getImplGroup(aGroup));
	}
	
	public org.jcae.mesh.sd.MeshGroup getImplementation()
	{
		return group;
	}
}
