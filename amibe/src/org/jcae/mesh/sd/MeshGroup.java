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


import gnu.trove.*;
import java.util.*;

/**
 * This a group of MeshElements. A group may include a set of edges, faces and
 * volumes. It do not include any set of nodes. A MeshGroup is linked to a
 * MeshMesh and a MeshMesh may own a set of MeshGroup. A MeshGroup may own a set
 * of MeshGroup (groups of groups).
 * @author  Jerome Robert
 */
public class MeshGroup
{
	int id;
	Set nodes=new HashSet();
	Set edges=new HashSet();
	Set faces=new HashSet();
	Set volumes=new HashSet();
	Map groups=new HashMap();
	MeshMesh mesh;
	private String name;
	
	/** Creates a new instance of MeshGroup */
	public MeshGroup(MeshMesh mesh)
	{
		id=hashCode();
		this.mesh=mesh;
		name=String.valueOf(id);
	}
	
	/* Used to wrap a MeshMesh in a MeshGroup. Internat use only */
	public MeshGroup(MeshMesh mesh, Set nodes, Set edges,
		Set faces, Set volumes, Map groups)
	{
		this(mesh);
		this.nodes=nodes;
		this.edges=edges;
		this.faces=faces;
		this.volumes=volumes;
		this.groups=groups;
	}
	
	public int numberOfNodes()
	{
		return nodes.size();
	}
	
	public Iterator getNodesIterator()
	{
		return nodes.iterator();
	}
	
	public void addNode(MeshNode node)
	{
		nodes.add(node);
	}
	
	public void removeNode(MeshNode node)
	{
		nodes.remove(node);
	}
	
	public int numberOfEdges()
	{
		return edges.size();
	}
	
	public Iterator getEdgesIterator()
	{
		return edges.iterator();
	}
	
	public void addEdge(MeshEdge edge)
	{
		edges.add(edge);
	}
	
	public void removeEdge(MeshEdge edge)
	{
		edges.remove(edge);
	}

	public int numberOfFaces()
	{
		return faces.size();
	}
	
	public Iterator getFacesIterator()
	{
		return faces.iterator();
	}
	
	public void addFace(MeshFace face)
	{
		faces.add(face);
	}
	
	public void removeFace(MeshFace face)
	{
		faces.remove(face);
	}
	
	public Iterator getSubGroupsIterator()
	{
		return groups.values().iterator();
	}
	
	public void addSubGroup(MeshGroup group)
	{
		groups.put(new Integer(group.getID()), group);
	}
	
	public void removeSubGroup(MeshGroup group)
	{
		groups.remove(group);
	}
	
	public void setID(int id)
	{
		this.id=id;
	}
	
	public int getID()
	{
		return id;
	}
	
	/** Return edges which belong to more than 2 faces
	 * @return A collection of edges
	 */	
	public Collection getTEdges()
	{		
		final ArrayList toReturn=new ArrayList();
		THashSet edgesNCT = new THashSet();
		edgesNCT.ensureCapacity(numberOfFaces()*3/2);

		for(Iterator it=getFacesIterator();it.hasNext();)
		{
			MeshFace f=(MeshFace)it.next();
			Iterator itn=f.getEdgesIterator();
			while(itn.hasNext()) 
			{								
				MeshEdge e=(MeshEdge)itn.next();
				if(edgesNCT.add(e))
				{
					if(e.getFaces().size()>2) toReturn.add(e);
				}
			}
		}
		
		return toReturn;
	}
	
	/** Return edges which belong to less than one face
	 * @return A collection of edges
	 */	
	public Collection getFreeEdges()
	{
		final ArrayList toReturn=new ArrayList();
		TObjectIntHashMap edgesNCT = createDependantEdgesCollection();
		
		TObjectIntProcedure procedure=new TObjectIntProcedure()
		{
			public boolean execute(Object a, int b)
			{
				if(b==1)
				{
					if(((MeshEdge)a).getFaces().size()==1)
						toReturn.add(a);
				}
				return true;
			}
		};
		edgesNCT.forEachEntry(procedure);
		return toReturn;
	}
	
	private TObjectIntHashMap createDependantEdgesCollection()
	{
		TObjectIntHashMap set=new TObjectIntHashMap();
		
		// optimization
		set.ensureCapacity(numberOfFaces()*3/2);

		for(Iterator it=getFacesIterator();it.hasNext();)
		{
			MeshFace f=(MeshFace)it.next();
			Iterator itn=f.getEdgesIterator();
			while(itn.hasNext())
			{				
				Object e=itn.next();
				set.put(e, set.get(e)+1);
			}
		}
		return set;
	}
	
	public MeshMesh getMesh()
	{
		return mesh;
	}
	
	/**
	 * @return The array of nodes coordinates. The size of the array is 3 time
	 * the numberOfNodes
	 * @param indices This array will receive the indices of the faces. It must me allocated by the
	 * caller with a size of 3 three time the number of faces.
	 */	
	public float[] getRawMesh(int[] indices)
	{		
		TObjectIntHashMap nodeList=new TObjectIntHashMap();
		Iterator it;
		
		// optimization: aproximation of the number of nodes is numberOfFace/2.
		nodeList.ensureCapacity(numberOfFaces()/2);
		
		// find nodes needed to display the group
		for(it=getFacesIterator();it.hasNext();)
		{
			MeshFace f=(MeshFace)it.next();
			Iterator itn=f.getNodesIterator();
			while(itn.hasNext())
				nodeList.put(itn.next(),0);
		}
		
		// number each node from 0
		TObjectIntIterator itt=nodeList.iterator();
		float []  tabPts = new float[3*nodeList.size()];
		for(int i=0 ; i<nodeList.size() ; i++)
		{			
			itt.advance();
			itt.setValue(i);
			MeshNode p = (MeshNode)itt.key();
			tabPts[3*i]= (float)p.getX();
			tabPts[3*i+1] = (float)p.getY();
			tabPts[3*i+2] = (float)p.getZ();
		}
		
		Iterator itface = getFacesIterator();
		
		int i = 0;
		while (itface.hasNext())
		{
			MeshFace f = (MeshFace) itface.next();			
			Iterator itn = f.getNodesIterator();
			int j = 0;
			while (itn.hasNext())
			{
				indices[3*i+j] = nodeList.get(itn.next());
				j++;
			}
			i++;
		}
		return tabPts;
	}

	
	public void add(MeshGroup group)
	{
		nodes.addAll(group.nodes);
		edges.addAll(group.edges);
		faces.addAll(group.faces);
		volumes.addAll(group.volumes);
		groups.putAll(group.groups);
	}
	
	public void intersect(MeshGroup group)
	{
		nodes.retainAll(group.nodes);
		edges.retainAll(group.edges);
		faces.retainAll(group.faces);
		volumes.retainAll(group.volumes);
		groups.entrySet().retainAll(group.groups.entrySet());
	}
	
	public Object clone()
	{
		MeshGroup g=new MeshGroup(mesh,
			new HashSet(nodes),
			new HashSet(edges),
			new HashSet(faces),
			new HashSet(volumes),
			new HashMap(groups));
		mesh.addGroup(g);
		return g;
	}
	
	/** Return the triangle whose quality is lower than a given value */
	public Collection getWorstTriangle(float quality)
	{
		ArrayList toReturn=new ArrayList();
		for(Iterator it=faces.iterator();it.hasNext();)
		{
			MeshFace f=(MeshFace)it.next();
			if(f.qualite()<=quality) toReturn.add(f);
		}
		return toReturn;
	}
	
	public void setName(String name)
	{
		this.name=name;
	}
	
	public String getName()
	{
		return this.name;
	}
}
