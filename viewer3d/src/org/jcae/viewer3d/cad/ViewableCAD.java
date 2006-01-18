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
 */

package org.jcae.viewer3d.cad;

import java.awt.Color;
import java.util.*;
import java.util.logging.Logger;
import javax.media.j3d.*;
import javax.vecmath.Color3f;
import org.jcae.viewer3d.ColoredDomain;
import org.jcae.viewer3d.DomainProvider;
import org.jcae.viewer3d.SelectionListener;
import org.jcae.viewer3d.Viewable;
import org.jcae.viewer3d.cad.occ.OCCFaceDomain;
import org.jcae.viewer3d.fd.FDSelection;

import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.geometry.Stripifier;
import com.sun.j3d.utils.picking.PickResult;

/**
 * A Viewable specilized to display CAD scenes.
 * The selected elements are hightlighted.
 * @author Jerome Robert
 * @todo all methods must be implemented.
 */
public class ViewableCAD implements Viewable 
{
	private static interface CADPickingInfo{}
	
	private static class FacePickingInfo implements CADPickingInfo
	{
		int id;
		Material[] materials;
		Color3f oldColor;
		/**
		 * @param id
		 * @param coloringAttributes
		 */
		public FacePickingInfo(int id, Material[] material){
			this(id,material,new Color3f(Color.WHITE));
		}
		public FacePickingInfo(int id, Material[] material,Color3f oldColor)
		{
			super();
			this.id = id;
			this.materials = material;
			this.oldColor=oldColor;
		}
	}  
	
	private static class EdgePickingInfo implements CADPickingInfo
	{
		int id;
		Appearance appearance;
		ColoringAttributes coloringAttributes;
		/**
		 * 
		 * @param id
		 * @param appearance
		 * @param coloringAttributes
		 */
		public EdgePickingInfo(int id,Appearance appearance,ColoringAttributes coloringAttributes)
		{
			super();
			this.id = id;
			this.appearance = appearance;
			this.coloringAttributes = coloringAttributes;
		}
	}
	
	public final static short NONE_SELECTION=0;
	public final static short DOMAIN_SELECTION=1;
	public final static short FACE_SELECTION=2;
	public final static short EDGE_SELECTION=3;
	public final static short VERTEX_SELECTION=4;
	public final static short MULTI_SELECTION=5;
	
	private short selectionMode=FACE_SELECTION;
	private CADProvider provider;
	private BranchGroup branchGroup=new BranchGroup();
	private List selectionListeners=new ArrayList();
	private Map facesInfo;
	private Map edgesInfo;
	private Collection selectedFaces=new HashSet();
	private Collection selectedEdges=new HashSet();
	private String name;
	private LineAttributes lineAttributes=new LineAttributes();
	/**
	 * 
	 */
	public ViewableCAD(CADProvider provider)
	{
		this.provider=provider;
		branchGroup.setCapability(Group.ALLOW_CHILDREN_WRITE);
		branchGroup.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		branchGroup.setCapability(Group.ALLOW_CHILDREN_READ);
		domainsChanged(provider.getDomainIDs());
	}
	
	/* (non-Javadoc)
	 * @see jcae.viewer3d.Viewable#domainsChanged(java.util.Collection)
	 */
	public void domainsChanged(int[] domainId)
	{
		if(branchGroup.numChildren()>0)
			branchGroup.removeAllChildren();
		for(int i=0;i<domainId.length;i++){
		
			switch(domainId[i]){
			case 0 : //Edges
				branchGroup.addChild(createEdgesNode((CADDomain) provider.getDomain(0)));
				break;
			case 1 : //Faces
				branchGroup.addChild(createFacesNode((CADDomain) provider.getDomain(1)));
				break;
			case 2 : //Vertices
				branchGroup.addChild(createVerticesNode((CADDomain) provider.getDomain(2)));
				break;
			}
		}
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.Viewable#getDomainProvider()
	 */
	public DomainProvider getDomainProvider()
	{
		return provider;
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.Viewable#setDomainVisible(java.util.Map)
	 */
	public void setDomainVisible(Map map)
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Set the picking mode of this viewable.
	 * Allowed values are:
	 * <ul>
	 * <li>0, for none selection</li>
	 * <li>1, for domain selection (see CADProvider for how CADDomain are created)</li>
	 * <li>2, for face selection</li>
	 * <li>3, for edge selection</li>
	 * <li>4, for vertex selection</li>
	 * <li>5, for multimodal (face, edge, vertex) selection</li>
	 * </ul>
	 * @param mode
	 */
	public void setSelectionMode(short mode)
	{
		selectionMode=mode;
	}
	
	public void setSelectionMode(short mode,boolean unselectAll){
		if(unselectAll) unselectAll();
		setSelectionMode(mode);
	}
	
	/**
	 * Add a selectionListener to this viewable
	 * As an optimisation, when there is no listener, the picking should be
	 * disabled on this viewable.
	 * 	 * @param listener
	 */
	public void addSelectionListener(CADSelectionListener listener)
	{
		selectionListeners.add(listener);
	}

	/**
	 * Remove a selectionListener from this viewable.
	 * As an optimisation, when there is no listener, the picking should be
	 * disabled on this viewable.
	 * @param listener
	 */
	public void removeSelectionListener(CADSelectionListener listener)
	{
		selectionListeners.remove(listener);
	}


	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.Viewable#pick(com.sun.j3d.utils.picking.PickResult)
	 */
	public void pick(PickResult result, boolean selected)
	{
		Logger.global.finest("result.getGeometryArray().getUserData()="+result.getGeometryArray().getUserData());
		Object o=getPickUserData(result);
		if((o instanceof FacePickingInfo)&(selectionMode==FACE_SELECTION))
		{
			FacePickingInfo fpi=(FacePickingInfo) o;
			setFaceSelected(fpi, selected);
			fireSelectionChanged();
		}
		else if((o instanceof EdgePickingInfo)&(selectionMode==EDGE_SELECTION))
		{
			EdgePickingInfo epi=(EdgePickingInfo) o;
			setEdgeSelected(epi, selected);
			fireSelectionChanged();
		}
	}
	
	
	private static Object getPickUserData(PickResult result){
		SceneGraphObject sgo;
		sgo=result.getGeometryArray();
		if(sgo!=null){
			Object o=sgo.getUserData();
			if(o instanceof CADPickingInfo)
				return o;
		}
		return null;
	}

	static private int[] integerCollectionToArray(Collection collection)
	{
		int[] toReturn=new int[collection.size()];
		Iterator it=collection.iterator();
		int i=0;
		while(it.hasNext())
		{
			Integer n=(Integer) it.next();
			toReturn[i]=n.intValue();
			i++;
		}
		return toReturn;
	}
	/**
	 * 
	 * @param faceID
	 * @param status
	 */
	public void hightLightFace(int faceID, boolean status){
		hightLightFace(faceID, status,true);
	}
	/**
	 * 
	 * @param faceID
	 * @param status
	 * @param fireListeners
	 */
	public void hightLightFace(int faceID, boolean status,boolean fireListeners)
	{
		
		FacePickingInfo fpi=(FacePickingInfo) facesInfo.get(new Integer(faceID));
		setFaceSelected(fpi, status);
		if(fireListeners) fireSelectionChanged();
		
	}
	
	/**
	 * 
	 * @param edgeID
	 * @param status
	 */
	public void hightLightEdge(int edgeID, boolean status){
		hightLightEdge(edgeID, status, true);
	}
	/**
	 * 
	 * @param edgeID
	 * @param status
	 * @param fireListeners
	 */
	public void hightLightEdge(int edgeID, boolean status,boolean fireListeners)
	{
		if(selectionMode==EDGE_SELECTION){
			EdgePickingInfo epi=(EdgePickingInfo) edgesInfo.get(new Integer(edgeID));
			setEdgeSelected(epi, status);
			if(fireListeners) fireSelectionChanged();
		}
	}
	
	private void setFaceSelected(FacePickingInfo fpi, boolean selected)
	{
		Color3f colorToSet;
		if(selected)
		{
			colorToSet=new Color3f(Color.RED);
			selectedFaces.add(new Integer(fpi.id));
		}
		else
		{
			colorToSet=fpi.oldColor;
			selectedFaces.remove(new Integer(fpi.id));
		}
		
		for(int i=0; i<fpi.materials.length; i++)
		{
			fpi.materials[i].setDiffuseColor(colorToSet);
		}
	}
	
	private void setEdgeSelected(EdgePickingInfo epi, boolean selected)
	{
		if(selected)
		{
			ColoringAttributes ca=new ColoringAttributes();
			ca.setColor(new Color3f(Color.RED));
			epi.appearance.setColoringAttributes(ca);
			selectedEdges.add(new Integer(epi.id));
		}
		else
		{
			epi.appearance.setColoringAttributes(epi.coloringAttributes);
			selectedEdges.remove(new Integer(epi.id));
		}
		
	}
	
	private void fireSelectionChanged()
	{
		
			for(int i=0; i<selectionListeners.size(); i++)
			{
				Object alistener=selectionListeners.get(i);
				if(alistener instanceof CADSelectionListener){
					CADSelectionListener cadlistener=(CADSelectionListener) alistener;
					CADSelection[] cs=new CADSelection[]{new CADSelection(0,
							integerCollectionToArray(selectedFaces), new int[0], new int[0])};
					cadlistener.selectionChanged(cs);
				}
				else if(alistener instanceof SelectionListener){
					((SelectionListener)alistener).selectionChanged();
				}
		}
	}
	
	public CADSelection[] getSelection(){
		return new CADSelection[]{new CADSelection(0,
				integerCollectionToArray(selectedFaces), new int[0], new int[0])};
	}
	
	private Node createEdgesNode(CADDomain domain)
	{
		Iterator it=domain.getEdgeIterator();
		BranchGroup toReturn=new BranchGroup();
		toReturn.setCapability(BranchGroup.ALLOW_DETACH);
		org.jcae.viewer3d.MarkUtils.setPickable(toReturn,true);
		edgesInfo=new HashMap();
		int n=0;
		
		while(it.hasNext())
		{
			float[] coordinates=(float[])it.next();			
			LineStripArray lsa=new LineStripArray(coordinates.length/3,
				LineStripArray.COORDINATES,
				new int[]{coordinates.length/3});
			
			lsa.setCapability(LineStripArray.ALLOW_COLOR_READ);
			lsa.setCapability(LineStripArray.ALLOW_COLOR_WRITE);
			lsa.setCapability(LineStripArray.ALLOW_COORDINATE_READ);
			lsa.setCapability(LineStripArray.ALLOW_COUNT_READ);
			lsa.setCapability(LineStripArray.ALLOW_FORMAT_READ);
			
			lsa.setCoordinates(0, coordinates);
			Shape3D shape3d=new Shape3D(lsa);
			Appearance a=new Appearance();
			a.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE);
			ColoringAttributes ca=new ColoringAttributes();
			if(domain instanceof ColoredDomain)
			{
				ColoredDomain d=(ColoredDomain)domain;
				ca.setColor(new Color3f(d.getColor()));
			}
			a.setLineAttributes(lineAttributes);
			a.setColoringAttributes(ca);
			shape3d.setAppearance(a);
			shape3d.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
			toReturn.addChild(shape3d);
			
			//Build Picking Data
			EdgePickingInfo epi=new EdgePickingInfo(n,a,ca);
			edgesInfo.put(new Integer(n), epi); 
			lsa.setUserData(epi);
			n++;
		}
		return toReturn;
	}
	
	private Node createFacesNode(CADDomain domain)
	{
		Iterator it=domain.getFaceIterator();
		BranchGroup toReturn=new BranchGroup();
		toReturn.setCapability(BranchGroup.ALLOW_DETACH);
		int n=0;
		facesInfo=new HashMap();
		
		float factorAbs=20.0f * Float.parseFloat(System.getProperty(
			"javax.media.j3d.zFactorAbs", "20.0f"));
		float factorRel=Float.parseFloat(System.getProperty("javax.media.j3d.zFactorRel",
			"2.0f"));
		
		PolygonAttributes paFront=new PolygonAttributes(
			PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_FRONT,
			factorAbs, false, factorRel);	

		PolygonAttributes paBack=new PolygonAttributes(
			PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_BACK,
			factorAbs, false, factorRel);	

		PolygonAttributes paNone=new PolygonAttributes(
			PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE,
			factorAbs, false, factorRel);	
		
		Vector materials=new Vector();//Vector to save Face Materials
		
		while(it.hasNext())
		{			
			materials.clear();
			
			FaceMesh fm=(FaceMesh) it.next();
			
			//Case of an unmeshed face
			if(fm.getNodes().length==0){
				FacePickingInfo fpi = 
					new FacePickingInfo(n,new Material[0]);
				facesInfo.put(new Integer(n), fpi);
				n++;
				continue;
			}
			GeometryInfo gi=new GeometryInfo(GeometryInfo.TRIANGLE_ARRAY);
			gi.setCoordinates(fm.getNodes());
			gi.setCoordinateIndices(fm.getMesh());			
			NormalGenerator ng = new NormalGenerator();
	        ng.generateNormals(gi);	        
	        Stripifier st = new Stripifier();
			st.stripify(gi);
	        
			GeometryArray g=gi.getGeometryArray();
			g.setCapability(IndexedTriangleArray.ALLOW_COUNT_READ);
			g.setCapability(IndexedTriangleArray.ALLOW_FORMAT_READ);
			g.setCapability(IndexedTriangleArray.ALLOW_COORDINATE_READ);
			g.setCapability(IndexedTriangleArray.ALLOW_COORDINATE_INDEX_READ);	
			
			Color faceColor=null;
			if(domain instanceof OCCFaceDomain)
				faceColor=((OCCFaceDomain)domain).getFaceColor(n);
			
			if(faceColor==null) {
				Shape3D shape3d=new Shape3D(g);
				Appearance a=new Appearance();
				shape3d.setAppearance(a);
				a.setPolygonAttributes(paFront);
				Material m1=new Material();
				m1.setAmbientColor(new Color3f(Color.BLUE));
				m1.setCapability(Material.ALLOW_COMPONENT_WRITE);			
				a.setMaterial(m1);			
				shape3d.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
				toReturn.addChild(shape3d);
				materials.add(m1);
				
				shape3d=new Shape3D(g);
				a=new Appearance();
				shape3d.setAppearance(a);
				a.setPolygonAttributes(paBack);
				Material m2=new Material();
				m2.setAmbientColor(new Color3f(Color.GREEN));
				m2.setCapability(Material.ALLOW_COMPONENT_WRITE);			
				a.setMaterial(m2);			
				shape3d.setCapability(Shape3D.ALLOW_GEOMETRY_READ);						
				toReturn.addChild(shape3d);	
				materials.add(m2);
				
				//Build Picking Data
				FacePickingInfo fpi = 
					new FacePickingInfo(n,(Material[])materials.toArray(new Material[materials.size()]));
				facesInfo.put(new Integer(n), fpi); 
				g.setUserData(fpi);
			}
			else {
				Shape3D shape3d=new Shape3D(g);
				Appearance a=new Appearance();
				shape3d.setAppearance(a);
				a.setPolygonAttributes(paNone);
				Material m1=new Material();
				Color3f color=new Color3f(
						((float)faceColor.getRed())/255
						,((float)faceColor.getGreen())/255
						,((float)faceColor.getBlue())/255);
				
				m1.setAmbientColor(color);
				m1.setDiffuseColor(color);
				m1.setSpecularColor(color);
				
				m1.setCapability(Material.ALLOW_COMPONENT_WRITE);			
				a.setMaterial(m1);			
				shape3d.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
				toReturn.addChild(shape3d);
				materials.add(m1);
				
				//Build Picking Data
				FacePickingInfo fpi = 
					new FacePickingInfo(n,(Material[])materials.toArray(new Material[materials.size()])
							,color);
				facesInfo.put(new Integer(n), fpi); 
				g.setUserData(fpi);
			}			
			n++;
		}
		return toReturn;
	}
		
	private Node createVerticesNode(CADDomain domain)
	{
		float pointSize=((Float)domain.getMarksTypes()[0]).floatValue();
		float[] points=domain.getMarks(null);
		BranchGroup toReturn=new BranchGroup();
		toReturn.setCapability(BranchGroup.ALLOW_DETACH);

		if(points.length>0)
		{
			PointArray pa=new PointArray(points.length/3, PointArray.COORDINATES);
			pa.setCoordinates(0, points);
			Appearance a=new Appearance();
			PointAttributes pat=new PointAttributes(pointSize, false);
			a.setPointAttributes(pat);
			ColoringAttributes ca=new ColoringAttributes(new Color3f(Color.RED), ColoringAttributes.FASTEST);
			a.setColoringAttributes(ca);
			Shape3D s3d = new Shape3D(pa, a);
			s3d.setPickable(false);
			//return a BranchGroup which is the only detachable node.			
			toReturn.addChild(s3d);
		}
		return toReturn;
	}
	
	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.Viewable#getBranchGroup(org.jcae.viewer3d.View)
	 */
	public Node getJ3DNode()
	{		
		return branchGroup;
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.Viewable#unselectAll()
	 */
	public void unselectAll()
	{
		int[] ids=integerCollectionToArray(selectedFaces);
		for(int i=0; i<ids.length; i++)
		{
			FacePickingInfo fpi=(FacePickingInfo) facesInfo.get(new Integer(ids[i]));
			setFaceSelected(fpi, false);
		}
		
		ids=integerCollectionToArray(selectedEdges);
		for(int i=0; i<ids.length; i++)
		{
			EdgePickingInfo epi=(EdgePickingInfo) edgesInfo.get(new Integer(ids[i]));
			setEdgeSelected(epi, false);
		}
		fireSelectionChanged();
	}
	
	public void setName(String name)
	{
		this.name=name;
	}
	
	public String toString()
	{
		return name;
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.Viewable#addSelectionListener(org.jcae.viewer3d.SelectionListener)
	 */
	public void addSelectionListener(SelectionListener listener)
	{
		selectionListeners.add(listener);
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.Viewable#removeSelectionListener(org.jcae.viewer3d.SelectionListener)
	 */
	public void removeSelectionListener(SelectionListener listener)
	{
		selectionListeners.remove(listener);
	}

	public void setLineWidth(float lineWidth)
	{
		lineAttributes.setLineWidth(lineWidth);
	}
}
