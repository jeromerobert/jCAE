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

package org.jcae.mesh.sd;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Set;
import java.util.Iterator;
import java.util.Collection;
import org.jcae.opencascade.jni.*;
import org.jcae.mesh.*;
import java.util.HashMap;
import org.jcae.mesh.util.*;
import org.apache.log4j.Logger;
/**
 *
 * @author  Jerome Robert
 */
public class MeshOfCAD extends MeshMesh
{
	private static Logger logger=Logger.getLogger(MeshOfCAD.class);
	private ArrayList subMeshes;
	private MeshOfCAD father;
	private  TopoDS_Shape shape;
	private boolean myIsCircular=false;
	/**
	 * Association Shape -  submesh. */
	protected HashMap mapOfSubMesh;
	
	/** Creates a new instance of MeshOfCAD */
	public MeshOfCAD(TopoDS_Shape shape)
	{
		this.shape=shape;
		this.father=this;
		mapOfSubMesh=new HashMap();
		initMesh();
	}
	
	/** Creates a new instance of MeshOfCAD */
	private MeshOfCAD(TopoDS_Shape shape, MeshOfCAD father)
	{		
		this.shape=shape;
		this.father=father;
		mapOfSubMesh=father.mapOfSubMesh;
		initMesh();
	}
	
	public TopoDS_Shape getGeometry()
	{
		return shape;
	}
	
	public boolean isCircular()
	{
		return myIsCircular;
	}
	
	/**
	 * Retrieves the mesh corresponding to the shape passed in argument from the list of sub meshes
	 * @param shape : a TopoDS_Shape instance, the shape related to the mesh to retrieve.
	 * @return MeshMesh - the mesh corresponding to the shape.
	 */
	public MeshOfCAD getMeshFromMapOfSubMesh(TopoDS_Shape shape)
	{
		if (mapOfSubMesh.containsKey(shape))
			return (MeshOfCAD)mapOfSubMesh.get(shape);
		else
			return null;
	}
	/**
	 * Sub-Mesh building.
	 * Exploration of mesh entities. Iterative method.\n
	 * The main idea is to sub-structure the mesh linked to the geometry into several sub-meshes. This init step is used
	 * to structure data linked with the targeted mesh process.\n
	 * This method explores the geometric entities of the main mesh and generates mesh elements (mesh structure).
	 * According to the topological shape decomposition, the main mesh references: a list of sub-mesh used to define
	 * a hierarchy between the mesh step. Each sub-mesh is linked to a list of geometric entities (vertices, edges, faces). \n
	 * Each face is saved in a sub mesh of the main mesh\n
	 * Each wire is explored twice to treat circular edges differently \n
	 * Each edge is saved in a sub mesh of the main mesh. \n
	 * Each vertex is saved in a sub mesh of the main mesh \n
	 * \n Remark about edges: As edges are oriented, while exploring the geometry, edges are generaly met twice (in forward
	 * and in backward orientation). In order to avoid to save edges twice, orientation is take into acount and just forward
	 * oriented edge are saved. If such an edge does not exist, then backward oriented edge is saved.
	 * \code
	 *  TopoDS_Edge EE=null;
	 *	if (E.orientation()==TopAbs_Orientation.FORWARD){
	 *		EE =(TopoDS_Edge) E.oriented(TopAbs_Orientation.REVERSED);
	 *	}
	 *	else EE =(TopoDS_Edge) E.oriented(TopAbs_Orientation.FORWARD);
	 *
	 *	MeshMesh edge = father.getMeshFromMapOfSubMesh(E);
	 *	if (edge == null){
	 *		edge = father.getMeshFromMapOfSubMesh(EE);
	 *		if (edge==null) edge = new MeshMesh(E);
	 * 	}
	 *	this.addSubMeshInSet(edge);
	 * \endcode
	 * @param father : a MeshMesh instance, the main mesh to explore and decompose
	 * @see addSubMeshInSet
	 **/
	private void initMesh()
	{	
		subMeshes=new ArrayList();
		assert(null != shape);
		mapOfSubMesh.put(shape, this);
		switch(shape.shapeType())
		{
			case TopAbs_ShapeEnum.COMPOUND:
			case TopAbs_ShapeEnum.COMPSOLID:
			case TopAbs_ShapeEnum.SHELL:
			case TopAbs_ShapeEnum.SOLID:
				/* explore the shape for each face */
				TopExp_Explorer expF = new TopExp_Explorer(shape, TopAbs_ShapeEnum.FACE);
				while (expF.more())
				{
					TopoDS_Face F = (TopoDS_Face) expF.current();
					addChildMesh(F);					
					expF.next();
				}
				break;
			case TopAbs_ShapeEnum.FACE:
				initMeshWire((TopoDS_Face)shape);
				break;
			case TopAbs_ShapeEnum.EDGE: 
				TopoDS_Vertex[] v = TopExp.vertices((TopoDS_Edge)shape);
				if (v[0].isSame(v[1]))
					myIsCircular=true;
				
				for (int i=0; i<2; i++)
				{
					addChildMesh(v[i]);
				}
				break;
			default:
		}
	}

	private void initMeshWire(TopoDS_Face face)
	{
		HashSet localSetOfSubMesh = new HashSet();
		TopExp_Explorer expW = new TopExp_Explorer(face, TopAbs_ShapeEnum.WIRE);
		while (expW.more())
		{
			TopoDS_Wire W = (TopoDS_Wire) expW.current();
			BRepTools_WireExplorer wexp = new BRepTools_WireExplorer();
			for (wexp.init(W, face);wexp.more();wexp.next())
			{
				TopoDS_Edge E = wexp.current();
				MeshOfCAD edge = getMeshFromMapOfSubMesh(E);
				if (edge == null)
				{
					edge = getMeshFromMapOfSubMesh(E.reversed());
					/*if (edge == null || localSetOfSubMesh.contains(edge))
						edge=addChildMesh(E);
					localSetOfSubMesh.add(edge);*/
					if (edge == null) edge=addChildMesh(E);					
				}
			}
			expW.next();
		}
	}
	
	/**
	 * @return An iterator on all edges of the MeshMesh and its child MeshMesh
	 */
	public Iterator getEdgesIterator()
	{
		final Iterator it=super.getEdgesIterator();
		return new Iterator()
		{
			Iterator subMeshIterator=getAllSubMeshes().iterator();
			Iterator currentIterator=it;
		
			public boolean hasNext()
			{
				if(!currentIterator.hasNext())
				{
					if(subMeshIterator.hasNext())
					{
						MeshMesh m=(MeshMesh)subMeshIterator.next();
						currentIterator=m.edgelist.iterator();
						return hasNext();
					}
					else return false;
				}
				return true;
			}
			
			public Object next()
			{
				if(!currentIterator.hasNext())
				{
					if(subMeshIterator.hasNext())
					{
						MeshMesh m=(MeshMesh)subMeshIterator.next();
						currentIterator=m.edgelist.iterator();
					}
				}
				return currentIterator.next();
			}
			
			public void remove()
			{
				currentIterator.remove();
			}
		};
	}
	
	/**
	 * @return An iterator on all nodes of the MeshMesh and its child MeshMesh
	 */
	public Iterator getNodesIterator()
	{
		final Iterator it=super.getNodesIterator();
		return new Iterator()
		{
			Iterator subMeshIterator=getAllSubMeshes().iterator();
			Iterator currentIterator=it;
			public boolean hasNext()
			{
				if(!currentIterator.hasNext())
				{
					if(subMeshIterator.hasNext())
					{
						MeshMesh m=(MeshMesh)subMeshIterator.next();
						currentIterator=m.nodelist.iterator();
						return hasNext();
					}
					else return false;
				}
				return true;
			}
			
			public Object next()
			{				
				if(!currentIterator.hasNext())
				{
					if(subMeshIterator.hasNext())
					{
						MeshMesh m=(MeshMesh)subMeshIterator.next();						
						currentIterator=m.nodelist.iterator();
					}
				}
				return currentIterator.next();
			}
			
			public void remove()
			{
				currentIterator.remove();
			}
		};
	}
	
	/**
	 * @return An iterator on all faces of the MeshMesh and its child MeshMesh
	 */
	public Iterator getFacesIterator()
	{
		final Iterator it=super.getFacesIterator();
		
		return new Iterator()
		{
			Iterator subMeshIterator=getAllSubMeshes().iterator();
			Iterator currentIterator=it;
			public boolean hasNext()
			{
				if(!currentIterator.hasNext())
				{
					if(subMeshIterator.hasNext())
					{
						MeshMesh m=(MeshMesh)subMeshIterator.next();
						currentIterator=m.facelist.iterator();
						return hasNext();
					}
					else return false;
				}
				return true;
			}
			
			public Object next()
			{
				if(!currentIterator.hasNext())
				{
					if(subMeshIterator.hasNext())
					{
						MeshMesh m=(MeshMesh)subMeshIterator.next();
						currentIterator=m.facelist.iterator();
					}
				}
				return currentIterator.next();
			}
			
			public void remove()
			{
				currentIterator.remove();
			}
		};
	}
	
	/**
	 * @return An iterator on all volumes of the MeshMesh and its child MeshMesh
	 */
	public Iterator getVolumesIterator()
	{
		final Iterator it=super.getVolumesIterator();
		
		return new Iterator()
		{
			Iterator subMeshIterator=getAllSubMeshes().iterator();
			Iterator currentIterator=it;
			public boolean hasNext()
			{
				if(!currentIterator.hasNext())
				{
					if(subMeshIterator.hasNext())
					{
						MeshMesh m=(MeshMesh)subMeshIterator.next();
						currentIterator=m.volumelist.iterator();
						return hasNext();
					}
					else return false;
				}
				return true;
			}
			
			public Object next()
			{
				if(!currentIterator.hasNext())
				{
					if(subMeshIterator.hasNext())
					{
						MeshMesh m=(MeshMesh)subMeshIterator.next();
						currentIterator=m.volumelist.iterator();
					}
				}
				return currentIterator.next();
			}
			
			public void remove()
			{
				currentIterator.remove();
			}
		};
	}
	
	/** Return an iterator on all childs of this mesh*/
	private Collection getAllSubMeshes()
	{
		HashSet set=new HashSet(subMeshes);
		Iterator it=subMeshes.iterator();
		while(it.hasNext())
		{
			MeshOfCAD m=(MeshOfCAD)it.next();
			set.addAll(m.getAllSubMeshes());
		}
		return set;
	}
	
	/** Iterate on all geometries of the current mesh structure */
	public Iterator getGeometryIterator()
	{
		return mapOfSubMesh.keySet().iterator();
	}
	
	/*public void removeSubMesh(TopoDS_Shape shape)
	{
		mapOfSubMesh.remove(shape);
	}*/
	
	public void print()
	{
		privPrintMesh(0, this);
	}
	
	static private void privPrintMesh(int level, MeshOfCAD m)
	{
		int type = m.shape.shapeType();
		for (int i=0; i < level; i++)
			System.out.print("  ");
		System.out.print("Sub MeshMesh ");
		switch(type)
		{
			case TopAbs_ShapeEnum.COMPOUND:
				System.out.print("compound");
				break;
			case TopAbs_ShapeEnum.FACE:
				System.out.print("face");
				break;
			case TopAbs_ShapeEnum.EDGE:
				System.out.print("edge");
				break;
			case TopAbs_ShapeEnum.VERTEX:
				System.out.print("vertex");
				break;
			default:
				System.out.print("unknown");
		}
		int nf = 0, ne = 0, nv = 0;
		Iterator ite = m.getSubMeshIterator();
		while (ite.hasNext())
		{
			MeshOfCAD submesh = (MeshOfCAD)ite.next();
			if (submesh.getGeometry().shapeType() == TopAbs_ShapeEnum.FACE)
				nf++;
			else if (submesh.getGeometry().shapeType() == TopAbs_ShapeEnum.EDGE)
				ne++;
			else if (submesh.getGeometry().shapeType() == TopAbs_ShapeEnum.VERTEX)
				nv++;
		}
		/*if (nf>0)
			System.out.print(" Faces: "+nf);
		if (ne>0)
			System.out.print(" Edges: "+ne);
		if (nv>0)
			System.out.print(" Nodes: "+nv);*/
		System.out.print(" Faces: "+m.numberOfFaces());
		System.out.print(" Edges: "+m.numberOfEdges());
		System.out.print(" Nodes: "+m.numberOfNodes());
		System.out.println("");
		ite = m.getSubMeshIterator();
		while (ite.hasNext())
		{
			privPrintMesh(level+1, (MeshOfCAD) ite.next());
		}
	}
	
	public int numberOfNodes()
	{
		int n=super.numberOfNodes();
		Iterator it=subMeshes.iterator();
		while(it.hasNext())
		{
			n+=((MeshOfCAD)it.next()).numberOfNodes();
		}
		return n;
	}

	public int numberOfEdges()
	{
		int n=super.numberOfEdges();
		Iterator it=subMeshes.iterator();
		while(it.hasNext())
		{
			n+=((MeshOfCAD)it.next()).numberOfEdges();
		}
		return n;
	}
	
	public int numberOfFaces()
	{
		int n=super.numberOfFaces();
		Iterator it=subMeshes.iterator();
		while(it.hasNext())
		{
			n+=((MeshOfCAD)it.next()).numberOfFaces();
		}
		return n;
	}		
	
	private MeshOfCAD addChildMesh(TopoDS_Shape aShape)
	{		
		MeshOfCAD aMesh=getMeshFromMapOfSubMesh(aShape);
		if (null == aMesh)
		{
			aMesh=new MeshOfCAD(aShape,this);
			mapOfSubMesh.put(aShape, aMesh);						
		}	
		subMeshes.add(aMesh);
		return aMesh;
	}
	
	public void rmNode(MeshNode node)
	{
		if (node.getPosition().getType() == PST_Position.VERTEX)
		{
			logger.info("The node is classified on a vertex: cannot remove");
			return;
		}
		super.rmNode(node);
	}
		
	public MeshOfCAD getFather()
	{
		return father;
	}
	
	public Iterator getSubMeshIterator()
	{
		return subMeshes.iterator();
	}
	/**
	 * Run all checks
	 */
	public void checkAll()
	{
		checkUnlinkedNodes();
		checkLinkedFaces();
		//checkValidity();
	}

	/**
	 * Check the mesh validity.
	 * This is performed by writing into a UNV file and reading it
	 */
/*
	public void checkValidity()
	{
		logger.info("Running checkValidity");
		String tmpfile = "@.unv";
		try
		{
			UNVWriter sav = new UNVWriter(new FileOutputStream(tmpfile), this);
			sav.writeMesh();
			UNVReader unv2 = new UNVReader(new FileInputStream(tmpfile), new MeshMesh(null));
			unv2.readMesh();
			File f = new File(tmpfile);
			//f.delete();
		}
		catch(Exception e)
		{
			logger.fatal(e.toString());
			e.printStackTrace();
		}
	}
*/

	/**
	 * Check unlibked nodss.
	 */
	public void checkUnlinkedNodes()
	{
		logger.info("Running checkUnlinkedNodes");
		Iterator itn = getNodesIterator();
		while (itn.hasNext())
		{
			MeshNode node = (MeshNode)itn.next();
			if (node.getElements().isEmpty()) {
				logger.warn("found an empty element list: "+node);
				logger.warn(""+node.getPosition()+" "+((PST_LinePosition)node.getPosition()).getParam());
			}
		}
	}

	/**
	 * Check that face nodes are linked to this face
	 */
	public void checkLinkedFaces()
	{
		logger.info("Running checkLinkedFaces");
		Iterator itf = getFacesIterator();
		while (itf.hasNext())
		{
			MeshFace face = (MeshFace)itf.next();
			Iterator itn = face.getNodesIterator();
			while (itn.hasNext())
			{
				MeshNode node = (MeshNode)itn.next();
				if (!node.getFaces().contains(face)) {
					logger.warn("found a wring linked face: "+face);
					break;
				}
			}
		}
	}	
}
