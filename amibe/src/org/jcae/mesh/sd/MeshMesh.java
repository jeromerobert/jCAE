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

package org.jcae.mesh.sd;

import org.jcae.opencascade.jni.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.HashMap;
import java.util.Stack;
import java.util.Arrays;
import org.jcae.mesh.util.Pair;
import org.jcae.mesh.util.HashSet;
import org.jcae.mesh.util.Calculs;
import org.apache.log4j.*;
import org.jcae.mesh.*;
import org.jcae.mesh.drivers.*;
import java.io.*;
import org.jcae.mesh.algos.*;


/**
 * MeshMesh: a class to store the mesh and federates all the different kinds of meshes.
 * \n
 * Mesh generation processes deals with various level of mesh entities:
 * - meshes built from vertex fields which are meshes defined only from geometric vertex and thus which only use node elements,
 * - meshes built from line fields which are meshes defined only from geometric line and thus which only use node and edge elements.
 * Such meshes are used for example to define structures built only from beams,
 * - meshes built from freeform surfaces which are meshes defined from geometric surface and thus which use node, edge and surface elements,
 * - meshes built using specific rules ...\n
 * In a global view, the mesh associated to a case of study can be decomposed of several specific meshes.
 * In order to manage this (1,n) relation we need to know for each mesh its specific structures. These structures
 * are defined using classes which bring together all data in relation with specific mesh definition.
 * \n These structures are : MeshNode class, MeshEdge class, MeshFace class and MeshVolume class, all linked with the MeshMesh class.
 * The main MeshMesh is explosed in sub meshes one for each geometrical entity
 * A MeshMesh contains 4 main lists: a volumes list, a faces list, a edges list and a nodes list used
 * during meshing.
 * @author Cyril BRANDY & Marie-Helene GARAT
 */
public class MeshMesh
{
	private static Logger logger=Logger.getLogger(MeshMesh.class);	
	public final static double epsilon = 0.001;

	/**
	 * Volumes list. */
	protected HashSet volumelist = new HashSet();
	
	/**
	 * Faces list. */
	protected HashSet facelist = new HashSet();
	
	/**
	 * Edges list. */
	protected HashSet edgelist = new HashSet();
	
	/**
	 * Nodes list. */
	protected HashSet nodelist = new HashSet();
		
	private HashMap groups=new HashMap();
	

	public MeshMesh()
	{
	}
	
	/** Insert a group from an other MeshMesh in the current MeshMesh.
	 MeshFace entities of the group are copied into the MeshMesh */
	public void insertGroup(MeshGroup group)
	{
		Iterator it=group.getFacesIterator();
		MeshGroup ng=new MeshGroup(this);
		while(it.hasNext())
		{
			ng.addFace(addExternalFace((MeshFace)it.next()));
		}
		addGroup(ng);
	}

	/** add a node 2d to the mesh.*/
	public MeshNode addNode(float x, float y, int id)
	{
		MeshNode2D n = new MeshNode2D(x,y);		
		n=(MeshNode2D)nodelist.addIfNotPresent(n);
		n.setID(id);
		return n;
	}
	
	/** Adds a node into the list.
	 * @param n  a MeshNode instance, the node to add
	 * @return MeshNode : the node added if not contained in the list, or the node already contained.
	 */
	public MeshNode addNode(MeshNode n)
	{
		MeshNode nn=(MeshNode)nodelist.addIfNotPresent(n);		
		return nn;
	}

	/** Add a face from another MeshMesh. The face will be copied and inserted
	 in the current MeshMesh. Its edges and nodes will also be copied */
	public MeshFace addExternalFace(MeshFace face)
	{
		MeshFace toAdd=new MeshFace();
		Iterator it=face.getEdgesIterator();
		ArrayList myNodes=new ArrayList();
		while(it.hasNext())
		{
			MeshEdge e=(MeshEdge)it.next();
			MeshNode n1=e.getNodes1();
			MeshNode n2=e.getNodes2();
			MeshNode nn1=new MeshNode(n1.getX(), n1.getY(), n1.getZ());
			MeshNode nn2=new MeshNode(n2.getX(), n2.getY(), n2.getZ());
			nn1.setID(n1.getID());
			nn2.setID(n2.getID());
			nn1=addNode(nn1);
			nn2=addNode(nn2);
			myNodes.add(nn1);
			myNodes.add(nn2);
			e=new MeshEdge(nn1,nn2);
			e=addEdge(e);
			toAdd.addEdge(e);
		}
		
		for(int i=0;i<myNodes.size();i++)
			((MeshNode)myNodes.get(i)).link(toAdd);
		
		return addFace(toAdd);
	}

	/** Add an edge from an other MeshMesh. The edge will be copied and inserted
	 in the current MeshMesh. It's nodes will also be copied */
	public void addExternalEdge(MeshEdge edge)
	{
		/** @TODO */
	}
	
	/** Adds a node into the list. If the node is already present, it is replaced
	 * This method is faster than addNode but suppose there are no double nodes
	 * @param n  a MeshNode instance, the node to add
	 */
	public void addNodeFast(MeshNode n)
	{
		nodelist.addFast(n);
	}
	
	/** Adds a edge into the list.
	 * @param e : a MeshEdge instance, the edge to add
	 * @return MeshEdge : the edge added if not contained in the list, or the edge is already contained.
	 */
	public MeshEdge addEdge(MeshEdge e)
	{
		return (MeshEdge)edgelist.addIfNotPresent(e);
	}
	
	/** Adds a face into the list.
	 * @param f : a MeshFace instance, the face to add
	 * @return MeshFace : the face added if not contained in the list, or the face is already contained.
	 */
	public MeshFace addFace(MeshFace f)
	{
		return (MeshFace)facelist.addIfNotPresent(f);
	}
	
	/** Adds a volume into the list.
	 * @param v : a MeshVolume instance, the volume to add
	 * @return MeshVolume : the volume added if not contained in the list, or the volume is already contained.
	 */
	public MeshVolume addVolume(MeshVolume v)
	{
		return (MeshVolume)volumelist.addIfNotPresent(v);
	}
	
	public boolean isEmpty()
	{
		return volumelist.isEmpty() && facelist.isEmpty() && edgelist.isEmpty()
			&& nodelist.isEmpty();
	}
	
	/** Merges all the meshes of the list */
	public void mergeMesh()
	{
	}
	
	/** Deletes a node.
	 * @param node : a MeshNode instance, the node to delete
	 */
	public void rmNode(MeshNode node)
	{
		nodelist.remove(node);
	}
	
	/** Deletes an edge.
	 * @param edge : a MeshEdge instance, the edge to delete
	 */
	public void rmEdge(MeshEdge edge)
	{
		edge.unlink(edge);
		deleteInternalEdge(edge);		
	}
	
	/** Deletes a face.
	 * @param face : a MeshFace instance, the face to delete
	 * @see deleteInternalFace(MeshFace)
	 */
	private void deleteInternalEdge(MeshEdge e)
	{
		if (edgelist.remove(e))
		{
			MeshNode pt1 = e.getNodes1();
			MeshNode pt2 = e.getNodes2();
			if (pt1.canDestroy())
				nodelist.remove(pt1);
			if (pt2.canDestroy())
				nodelist.remove(pt2);
		}
	}

	public void rmFace(MeshFace face)
	{
		face.unlink(face);
		facelist.remove(face);
		for(Iterator it=face.getEdgesIterator(); it.hasNext();)
		{
			MeshEdge e = (MeshEdge) it.next();
			if (e.canDestroy())
				deleteInternalEdge(e);
		}
	}
	
	/** Deletes a volume.
	 * @param volume : a MeshVolume instance, the volume to delete
	 */
	public void rmVolume(MeshVolume volume)
	{
		volume.unlink(volume);
		volumelist.remove(volume);		
	}
	
	/** Get an edge defined by two nodes.
	 * \n This method identifies the MeshEdge entity which is bounding by the two nodes specified as input parameter.
	 * @param n1 : a MeshNode instance, one of both bounding nodes of the edge to find
	 * @param n2 : a MeshNode instance, the other node.
	 * @return MeshEdge : the edge defined by both nodes. Returns /c null if the edge has not been found.
	 */
	public MeshEdge getEdgeDefinedByNodes(MeshNode n1, MeshNode n2)
	{
		for(Iterator it=n1.getEdgesIterator(); it.hasNext();)
		{
			MeshEdge e=(MeshEdge)it.next();
			if(e.getNodes1().equals(n2)||e.getNodes2().equals(n2)) return e;
		}
		return null;
	}
	
	/**
	 * Get a face defined by a list of nodes.
	 * \n This method identify the MeshFace entity which is bounded by a given list of MeshNode entities.
	 * @param nodes   an array of nodes bounding the face
	 * @return MeshFace : the face defined by the list of nodes, or \c null if the face was not found.
	 */
	public MeshFace getFaceDefinedByNodes(MeshNode[] nodes)
	{
		Iterator fIt=nodes[0].getFacesIterator();
		HashSet s1=new HashSet(Arrays.asList(nodes));
		while(fIt.hasNext())
		{
			MeshFace f=(MeshFace)fIt.next();
			Iterator nIt=f.getNodesIterator();			
			HashSet s2=new HashSet();
			while(nIt.hasNext()) s2.add(nIt.next());
			if(s1.equals(s2)) return f;
		}
		return null;
	}
	
	/** Get a volume defined by a list of nodes.
	 * \n This method identify the MeshVolume entity which is bounded by a given list of MeshNode entities.
	 * @param nodeList : an ArrayList of nodes bounding the volume
	 * @return MeshVolume : the volume defined by the list of nodes
	 */
	public MeshVolume getVolumeDefinedByNodes(HashSet nodeList)
	{
		return null;
	}
	
	/** Get a volume defined by a faces list.
	 * @param faceList : a HashSet of faces bounding the volume to get
	 * @return MeshVolume : the volume bounded by the faces.
	 */
	public MeshVolume getVolumeDefinedByFaces(HashSet faceList)
	{
		return null;
		// Write your code here
	}
	
	
	/** Print mesh infos */
	public String toString()
	{
		String cr=System.getProperty("line.separator");
		String r="";
		for(Iterator it=getNodesIterator();it.hasNext();)
		{
			MeshNode node=(MeshNode)it.next();
			r+=node+cr;
		}
		for(Iterator it=getEdgesIterator();it.hasNext();)
		{
			MeshEdge edge=(MeshEdge)it.next();
			r+=edge+cr;
		}
		for(Iterator it=getFacesIterator();it.hasNext();)
		{
			MeshFace face=(MeshFace)it.next();
			r+=face+cr;
		}
		return r;
	}
		
	/**
	 * Method rmFaces.
	 * @param faces - a HashSet of faces to remove
	 */
	public void rmFaces(HashSet faces)
	{
		Iterator it = faces.iterator();
		while (it.hasNext())
		{
			MeshFace face = (MeshFace)it.next();
			rmFace(face);
			faces.remove(face);
			it = faces.iterator();
		}
	}
	
	/**
	 * Method rmEdges.
	 * @param edges - a HashSet of edges to remove
	 */
	public void rmEdges(HashSet edges)
	{
		Iterator it = edges.iterator();
		while (it.hasNext())
		{
			MeshEdge edge = (MeshEdge)it.next();
			rmEdge(edge);
			edges.remove(edge);
			it = edges.iterator();
		}
	}
	
	
	/**
	 * Method to add a sub mesh in the current mesh.
	 * Used when a hole was fill to update the main mesh
	 * @param submesh - a MeshMesh instance treated as a submesh of a meshmesh
	 * @return MeshMesh : the current mesh where the submesh has been added to.
	 * @see getFillContour()
	 * @see fillContour()
	 */
	//public void addSubMesh(MeshMesh submesh )
	//{
		/** @TODO */
		/*HashSet tempedge=new HashSet();
		// For each face
		Iterator it_face = submesh.facelist.iterator();
		while (it_face.hasNext())
		{
			MeshFace face = (MeshFace) it_face.next();
			HashSet edges = father.getEdgesOfSubMesh(new HashSet());
			// for each edge
			Iterator it_edge = face.getEdges().iterator();
			while (it_edge.hasNext())
			{
				MeshEdge edge = (MeshEdge) (it_edge.next());
				MeshNode n1 = edge.getNodes1();
				MeshNode n2 = edge.getNodes2();

				n1=(MeshNode)father.getNodesOfSubMesh(new HashSet()).addIfNotPresent(n1);
				n2=(MeshNode)father.getNodesOfSubMesh(new HashSet()).addIfNotPresent(n2);

				//HashSet edges = father.getEdgesOfSubMesh(new HashSet());
				//MeshEdge ae = null;
				if (edges.contains( edge) )
					edge = (MeshEdge)edges.addIfNotPresent(edge);
				else
					edge=addEdge(edge);

				//ae.setWire(edge.isWire);
				//ae.setFrozen(edge.isFrozen);
				tempedge.add(edge);
				
			}
			face.clearList();
			face.addAllEdges(tempedge);
			face=addFace(face);
			addElement(face);
			tempedge.clear();
		}*/
	//}


    /**
	 * Method retrieves the MeshNode giving 3D coordinates
	 * @param coord  the 3D coordinates of a point
	 * @return the MeshNode instance corresponding to coordinates passed in parameter
	 */
	public MeshNode getNode(float[] coord)
	{
		PST_3DPosition pos = new PST_3DPosition(coord[0],coord[1],coord[2]);
		MeshNode nodeToFind = new MeshNode(coord[0],coord[1],coord[2],pos);
		if ( ! nodelist.contains(nodeToFind) )
		{
			// happens when the mesh is computed and not read from a file (pst_pos are not the same type)
			boolean trouve=false;
			Iterator it = this.getNodesIterator();
			while(it.hasNext())
			{
				MeshNode n = (MeshNode)it.next();
				// construction de n3D afin d'eviter les erreurs d'arondis
				MeshNode n3D = new MeshNode(n.getX(),n.getY(),n.getZ(),new PST_3DPosition(n.getX(),n.getY(),n.getZ()));
				if (n3D.equals(nodeToFind)==true)
				{
					nodeToFind = n;
					trouve = true;
					break;
				}
			}
			if (!trouve)
			{
				logger.debug("Node not found");
				return null;
			}
		}

		MeshNode node = (MeshNode)nodelist.addIfNotPresent(nodeToFind);
		return node;
	}	
	
	/** Create a triangle defined by 3 nodes and add it to the mesh. This
	 * methods create missing edges and add them to the mesh. The nodes must
	 * already be in the MeshMesh.
	 * The created MeshFace is tagged as a finit element.
	 */
	public MeshFace addTriangle(MeshNode n1, MeshNode n2, MeshNode n3)
	{
		MeshEdge e1=getEdgeDefinedByNodes(n1, n2);		
		MeshEdge e2=getEdgeDefinedByNodes(n2, n3);
		MeshEdge e3=getEdgeDefinedByNodes(n3, n1);		
		
		if(e1==null) e1=addEdge(new MeshEdge(n1,n2));
		if(e2==null) e2=addEdge(new MeshEdge(n2,n3));
		if(e3==null) e3=addEdge(new MeshEdge(n3,n1));
		MeshFace f=addFace(new MeshFace(e1,e2,e3));
		n1.link(f);
		n2.link(f);
		n3.link(f);
		return f;
	}

	/** Create a triangle defined by 3 edges and add it to the mesh. The edges
	 * must already be in the MeshMesh.
	 * The created MeshFace is tagged as a finit element.
	 */
	public MeshFace addTriangle(MeshEdge e1, MeshEdge e2, MeshEdge e3)
	{
		assert edgelist.contains(e1);
		assert edgelist.contains(e2);
		assert edgelist.contains(e3);
		
		MeshFace f=addFace(new MeshFace(e1,e2,e3));
		f.link(f);
		return f;
	}
	
	public int numberOfGroups()
	{
		return groups.size();
	}
	
	public Iterator getGroupsIterator()
	{
		return groups.values().iterator();
	}
	
	public void addGroup(MeshGroup group)
	{
		groups.put(new Integer(group.getID()),group);
	}
	
	public void removeGroup(MeshGroup group)
	{
		if(groups.remove(new Integer(group.getID()))==null)
			logger.debug("removeGroup : "+group+" not found");
	}
	
	public MeshGroup getGroup(int id)
	{
		return (MeshGroup)groups.get(new Integer(id));
	}
	
	/* Wrap the MeshMesh in a MeshGroup. Internat datas are shared (Adding a
	 * face in the MeshMesh will add on in the MeshGroup).
	 */
	public MeshGroup getAsGroup()
	{
		int count;
		Iterator it;

		count = 1;
		for(it = getNodesIterator();it.hasNext();it.next())
			count++;
		HashSet nodeset = new HashSet(count);
		it = getNodesIterator();
		while(it.hasNext())
			nodeset.addFast(it.next());

		count = 1;
		for(it = getEdgesIterator();it.hasNext();it.next())
			count++;
		HashSet edgeset = new HashSet(count);
		it = getEdgesIterator();
		while(it.hasNext())
			edgeset.addFast(it.next());

		count = 1;
		for(it = getFacesIterator();it.hasNext();it.next())
			count++;
		HashSet faceset = new HashSet(count);
		it = getFacesIterator();
		while(it.hasNext())
			faceset.addFast(it.next());

		count = 1;
		for(it = getVolumesIterator();it.hasNext();it.next())
			count++;
		HashSet volumeset = new HashSet(count);
		it = getVolumesIterator();
		while(it.hasNext())
			volumeset.addFast(it.next());

		MeshGroup group=new MeshGroup(this, nodeset, edgeset, faceset, volumeset, groups);
		logger.info("Create group with "+nodeset.size()+" nodes and "+
			faceset.size()+" faces");
		return group;
	}
	
	public MeshGroup[] getGroupsFromGeomFaces()
	{
		/** @TODO */
		return null;
	}

	public Iterator getNodesIterator()
	{
		return nodelist.iterator();
	}
	
	public Iterator getEdgesIterator()
	{
		return edgelist.iterator();
	}
	
	public Iterator getFacesIterator()
	{
		return facelist.iterator();
	}

	public Iterator getVolumesIterator()
	{
		return volumelist.iterator();
	}
	
	public int numberOfNodes()
	{
		return nodelist.size();
	}

	public int numberOfEdges()
	{
		return edgelist.size();
	}
	
	public int numberOfFaces()
	{
		return facelist.size();
	}
	
	/** To optimize memory allocation when the node number is known before the
	 built of the mesh */
	public void ensureNodeCapacity(int capacity)
	{
		nodelist.ensureCapacity(capacity);
	}
	
	/** To optimize memory allocation when the face number is known before the
	 built of the mesh */
	public void ensureFaceCapacity(int capacity)
	{
		facelist.ensureCapacity(capacity);
	}	

	/**
	 * Check the mesh validity.
	 * This is performed by writing into a UNV file and reading it
	 */
	public void checkValidity()
	{
		String tmpfile = "@.unv";
		try
		{
			UNVWriter sav = new UNVWriter(new FileOutputStream(tmpfile), this);
			sav.writeMesh();
			UNVReader unv2 = new UNVReader(new FileInputStream(tmpfile), new MeshMesh());
			unv2.readMesh();
			File f = new File(tmpfile);
			f.delete();
		}
		catch(Exception e)
		{
			logger.fatal(e.toString());
			e.printStackTrace();
		}
	}
}
