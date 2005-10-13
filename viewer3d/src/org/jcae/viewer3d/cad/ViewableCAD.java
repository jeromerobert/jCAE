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
	private static class FacePickingInfo
	{
		int id;
		Material[] materials;
		/**
		 * @param id
		 * @param coloringAttributes
		 */
		public FacePickingInfo(int id, Material[] material)
		{
			super();
			this.id = id;
			this.materials = material;
		}
	}
	
	public static short DOMAIN_SELECTION=0;
	public static short FACE_SELECTION=1;
	public static short EDGE_SELECTION=2;
	public static short VERTEX_SELECTION=3;
	public static short MULTI_SELECTION=4;
	
	private short selectionMode=FACE_SELECTION;
	private CADProvider provider;
	private BranchGroup branchGroup=new BranchGroup();
	private List selectionListeners=new ArrayList();
	private Map facesInfo;
	private Collection selectedFaces=new HashSet();
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
		domainsChanged(null);
	}
	
	/* (non-Javadoc)
	 * @see jcae.viewer3d.Viewable#domainsChanged(java.util.Collection)
	 */
	public void domainsChanged(int[] domainId)
	{
		if(branchGroup.numChildren()>0)
			branchGroup.removeAllChildren();
		branchGroup.addChild(createEdgesNode((CADDomain) provider.getDomain(0)));
		branchGroup.addChild(createFacesNode((CADDomain) provider.getDomain(1)));
		branchGroup.addChild(createVerticesNode((CADDomain) provider.getDomain(2)));
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
	 * <li>0, for domain selection (see CADProvider for how CADDomain are created)</li>
	 * <li>1, for face selection</li>
	 * <li>2, for edge selection</li>
	 * <li>3, for vertex selection</li>
	 * <li>4, for multimodal (face, edge, vertex) selection</li>
	 * </ul>
	 * @param mode
	 */
	public void setSelectionMode(short mode)
	{
		selectionMode=mode;
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
		Object o=result.getGeometryArray().getUserData();
		if(o instanceof FacePickingInfo)
		{
			FacePickingInfo fpi=(FacePickingInfo) o;
			setFaceSelected(fpi, selected);
			fireSelectionChanged();
		}
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
	
	public void hightLightFace(int faceID, boolean status)
	{
		FacePickingInfo fpi=(FacePickingInfo) facesInfo.get(new Integer(faceID));
		setFaceSelected(fpi, status);
		fireSelectionChanged();
	}
	
	private void setFaceSelected(FacePickingInfo fpi, boolean selected)
	{
		Color colorToSet;
		if(selected)
		{
			colorToSet=Color.RED;
			selectedFaces.add(new Integer(fpi.id));
		}
		else
		{
			colorToSet=Color.WHITE;
			selectedFaces.remove(new Integer(fpi.id));
		}
		
		for(int i=0; i<fpi.materials.length; i++)
		{
			fpi.materials[i].setDiffuseColor(new Color3f(colorToSet));
		}
	}
	
	private void fireSelectionChanged()
	{
		for(int i=0; i<selectionListeners.size(); i++)
		{
			CADSelectionListener listener=(CADSelectionListener) selectionListeners.get(i);
			CADSelection[] cs=new CADSelection[]{new CADSelection(0,
				integerCollectionToArray(selectedFaces), new int[0], new int[0])};
			listener.selectionChanged(cs);
		}					
	}
	
	private Node createEdgesNode(CADDomain domain)
	{
		Iterator it=domain.getEdgeIterator();
		BranchGroup toReturn=new BranchGroup();
		while(it.hasNext())
		{
			float[] coordinates=(float[])it.next();			
			LineStripArray lsa=new LineStripArray(coordinates.length/3,
				LineStripArray.COORDINATES,
				new int[]{coordinates.length/3});
			lsa.setCoordinates(0, coordinates);
			Shape3D shape3d=new Shape3D(lsa);
			Appearance a=new Appearance();
			ColoringAttributes ca=new ColoringAttributes();
			if(domain instanceof ColoredDomain)
			{
				ColoredDomain d=(ColoredDomain)domain;
				ca.setColor(new Color3f(d.getColor()));
			}
			a.setLineAttributes(lineAttributes);
			a.setColoringAttributes(ca);
			shape3d.setAppearance(a);
			toReturn.addChild(shape3d);
		}
		toReturn.setPickable(false);
		toReturn.setCapability(BranchGroup.ALLOW_DETACH);
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
		
		while(it.hasNext())
		{			
			FaceMesh fm=(FaceMesh) it.next();
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
			
			FacePickingInfo fpi = new FacePickingInfo(n, new Material[]{m1, m2});
			facesInfo.put(new Integer(n), fpi); 
			g.setUserData(fpi);
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
		// TODO Implement this method and remove the other addSelectionListener method
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.Viewable#removeSelectionListener(org.jcae.viewer3d.SelectionListener)
	 */
	public void removeSelectionListener(SelectionListener listener)
	{
		// TODO Implement this method and remove the other addSelectionListener method
		throw new UnsupportedOperationException();
	}

	public void setLineWidth(float lineWidth)
	{
		lineAttributes.setLineWidth(lineWidth);
	}
}
