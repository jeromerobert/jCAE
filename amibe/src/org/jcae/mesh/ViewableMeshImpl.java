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
import org.omg.PortableServer.POA;
import org.omg.CORBA.portable.ObjectImpl;
import org.jcae.view3d.*;
import java.awt.Color;
import org.apache.log4j.*;
import org.jcae.mesh.sd.*;
import org.jcae.mesh.algos.MiscAlgos;
import org.jcae.mesh.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;
import org.jcae.opencascade.jni.*;
//import org.jcae.mesh.drivers.*;
//import java.io.*;


/** This class encapsulate the {@link MeshObject} class to make it visible
 * @author Jerome Robert
 */
public class ViewableMeshImpl extends Object3DImpl implements ViewableMeshOperations
{
	private static Logger logger=Logger.getLogger(Object3DImpl.class);	
	static float[] FREE_EDGE_COLOR=Color.RED.getComponents(null);
	static float[] T_EDGE_COLOR=Color.MAGENTA.getComponents(null);
	static float[] BAD_TRIANGLE_COLOR=Color.YELLOW.getComponents(null);
	private boolean showTEdges, showFreeEdges, showMesh;
	private float badTriangleQuality=Float.MAX_VALUE;
	private Geometry emptyGeometry;
	
	MeshGroup meshObject;
	
	/** Holds value of property color. */
	private float[] color;
	
	/**
	 * @param o The CORBA Mesh object
	 * @param poa
	 */
	public ViewableMeshImpl(MeshGroup o,POA poa)
	{
		super(poa);
		meshObject=o;
		showTEdges=false;
		showFreeEdges=false;
		showMesh=true;
		emptyGeometry=createEmptyGeometry();
	}
	
	public int numGeometries()
	{
		return 4;
	}
	
	/** Get a geometry to be displayed.
	 * @param i The geometry to be returned :
	 * 0 - The main mesh
	 * 1 - Free edges
	 * 2 - Multiple edges
	 * 3 - Low quality triangles
	 * @return The geometry
	 */
	public Geometry getGeometry(int i)
	{
		Geometry toReturn=emptyGeometry;
		//logger.debug("Begin getGeometry("+i+")");
		switch(i)
		{
			case GEOM_MESH: if(showMesh) toReturn=getRawGeometry(); break;
			case GEOM_FREE_EDGE: if(showFreeEdges) toReturn=getFreeEdgesGeometry(); break;
			case GEOM_MULTI_EDGE: if(showTEdges) toReturn=getTEdgesGeometry(); break;
			case GEOM_BAD_TRIANGLE: if(badTriangleQuality<=Math.PI/3) toReturn=getBadTriangleGeometry(); break;
			default : toReturn=null;
		}
		//logger.debug("End of getGeometry("+i+")");
		return toReturn;
	}
	
	private Geometry getBadTriangleGeometry()
	{
		Geometry geom=createEmptyGeometry();
		geom.isSelectable(false);
		geom.appearance(Geometry.APPEARANCE_FLAT);
		geom.type(Geometry.TYPE_TRIANGLE);
		geom.isIndexed(false);
		geom.defaultColor(BAD_TRIANGLE_COLOR);
		floatsHolder coordinates=new floatsHolder();
		meshObject.getRawBadTriangle(coordinates, badTriangleQuality);
		geom.coordinates(coordinates.value);		
		return geom;		
	}
	
	private Geometry getFreeEdgesGeometry()
	{
		Geometry geom=createEmptyGeometry();
		geom.isSelectable(false);
		geom.appearance(Geometry.APPEARANCE_WIREFRAME);
		geom.type(Geometry.TYPE_LINE);
		geom.isIndexed(false);
		geom.defaultColor(FREE_EDGE_COLOR);
		floatsHolder coordinates=new floatsHolder();
		meshObject.getRawFreeEdges(coordinates);
		geom.coordinates(coordinates.value);		
		return geom;
	}
	
	private Geometry getTEdgesGeometry()
	{
		Geometry geom=createEmptyGeometry();
		geom.isSelectable(false);
		geom.appearance(Geometry.APPEARANCE_WIREFRAME);
		geom.type(Geometry.TYPE_LINE);
		geom.isIndexed(false);
		geom.defaultColor(T_EDGE_COLOR);
		floatsHolder coordinates=new floatsHolder();
		meshObject.getRawTEdges(coordinates);
		geom.coordinates(coordinates.value);		
		return geom;
	}
	
	private Geometry getRawGeometry()
	{
		Geometry geom=createEmptyGeometry();
		longsHolder indices=new longsHolder();
		floatsHolder coordinates=new floatsHolder();
		
		geom.isSelectable(true);
		geom.appearance(Geometry.APPEARANCE_WIREFRAME);
		//geom.appearance(Geometry.APPEARANCE_SHADED);
		geom.type(Geometry.TYPE_TRIANGLE);
		geom.isIndexed(true);
		geom.defaultColor(color);
		meshObject.getRawMesh(coordinates,indices);		
		geom.indices(indices.value);
		geom.coordinates(coordinates.value);	
		return geom;
	}
		
	/** Show the free edges. This methodes do not update the scene and will not
	 * display the changes. It's intended for internal use. Use
	 * MeshScene.showFreeEdges instead of.
	 * @param param if true, free edges are shown.
	 */	
	public void showFreeEdges(boolean param)
	{
		showFreeEdges=param;		
	}
	
	/** Show multiple edges. This methodes do not update the scene and will not
	 * display the changes. It's intended for internal use. Use
	 * MeshScene.showTEdges instead of.
	 * @param param
	 */	
	public void showTEdges(boolean param)
	{
		showTEdges=param;
	}
	
	/** Show or hide the mesh. This methodes do not update the scene and will
	 * not display the changes. It's intended for internal use. Use
	 * {@link MeshSceneImpl#showMesh} instead of.
	 * @param param
	 */	
	public void showMesh(boolean param)
	{
		showMesh=param;
	}

	public void showWorstTriangle(float quality)
	{
		badTriangleQuality=quality;
	}	
	
	public org.jcae.mesh.MeshGroup getMeshGroup()
	{
		return meshObject;
	}	
	
	/** Setter for property color.
	 * @param color New value of property color.
	 *
	 */
	public void setColor(float[] color)
	{
		this.color = color;
	}
	
	public void pick(float[] point, float[] closestVertex)
	{
		pickContour(point, closestVertex);
	}
	
	private void pickContour(float[] point, float[] closestVertex)
	{
		if (true) return ;
		logger.debug("Enter ViewvableMeshImpl.pick");

		// get local implementation of group
		ObjectImpl oi=(ObjectImpl)meshObject;
		org.omg.CORBA.portable.ServantObject _so = oi._servant_preinvoke( "",Object.class);
		MeshGroupPOATie localServantTie = (MeshGroupPOATie)_so.servant;
		MeshGroupImpl groupImpl=(MeshGroupImpl)localServantTie._delegate();		

		// retrieve the mesh
		MeshMesh mesh = groupImpl.getImplementation().getMesh();

		
		// cas pick node:
		
		// retrieve the corresponding MeshNode instance in the data structure
		MeshNode node = mesh.getNode(closestVertex);
		if (node==null)
		{
			logger.info("Picked point not found in mesh data structure");
			return;
		}
		// case pick node ---> 		return node;

		
		// case pick edge:

		// find one free edge linked to the node
		MeshEdge freeEdge = null;
		HashSet freeEdges = node.getFreeEdges();;
		logger.debug("nb free edges linked to node: "+freeEdges.size());
		//return;
		
		// retrieve the closest edge from node
		// first find the MeshFace containing the picked point
		/*HashSet*/ freeEdges = new HashSet();
		MeshEdge edgeSelected = null;
		if (freeEdges.isEmpty())
		{
			MeshNode picked = new MeshNode (point[0], point[1], point[2]);
			// then find the closest edge from the picked node
			edgeSelected = node.getClosestEdge(picked);
			logger.debug("edge selectionnee: "+edgeSelected);
			//return edgeSelected;
		}
		else 
			edgeSelected = (MeshEdge)freeEdges.iterator().next();
		// case pick edge ---> return edgeSelected;
		

		// case fill holl
		HashSet contour = MiscAlgos.getFreeContour(edgeSelected);
		if (contour.isEmpty()) 
		{
			logger.info("contour not found");
			return;
		}
		
		// fill contour
		MeshMesh sm = null;
		ArrayList alist = new ArrayList();
		alist.addAll(contour);
		// retreive the TopoDS_Shape .........
		MeshFace face = (MeshFace)edgeSelected.getFaces().iterator().next();
		TopoDS_Edge tedge = new MiscAlgos((MeshOfCAD)mesh).getTopoEdge(edgeSelected.getNodes1(),edgeSelected.getNodes2());
		if (tedge!=null)
		{		
			ArrayList topofaces = new MiscAlgos((MeshOfCAD)mesh).getTopoFaces(tedge);
			if (topofaces.size() == 1)
				new MiscAlgos((MeshOfCAD)mesh).fillContour(alist);
			else 
			{
				logger.info("Topology not found");
				return;
			}
		}
		else 
			new MiscAlgos((MeshOfCAD)mesh).fillContour(alist);
			
		// test refine submesh
		
		HashSet set=new HashSet();
		Iterator it=sm.getEdgesIterator();
		while(it.hasNext()) set.add(it.next());
		new MiscAlgos((MeshOfCAD)sm).refineSubMesh(set);
		// set isWire flag to false before adding subMesh to main mesh
		Iterator ite = contour.iterator();
		while (ite.hasNext())
		{
			MeshEdge e = (MeshEdge)ite.next();
			if (e.isWire()) e.setWire(false);
			if (e.isFrozen()) e.setFrozen(false);
		}

		groupImpl.getImplementation().add(mesh.getAsGroup());
		this.getRawGeometry();

		logger.debug("Fin de viewvableMeshImpl.pick");
		return;

	}	

}
