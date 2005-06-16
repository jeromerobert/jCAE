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

package org.jcae.viewer3d.fe;

import java.awt.Color;
import java.awt.Component;
import java.util.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import javax.media.j3d.*;
import javax.swing.JPanel;
import javax.vecmath.Color3f;
import org.jcae.viewer3d.DomainProvider;
import org.jcae.viewer3d.SelectionListener;
import org.jcae.viewer3d.Viewable;
import com.sun.j3d.utils.picking.PickResult;

/**
 * @author Jerome Robert
 * @todo implements all methods
 */
public class ViewableFE implements Viewable
{
	private FEProvider provider;
	private Map visibleDomain;
	private List selectionListeners=new ArrayList();
	private BranchGroup branchGroup;
	private Map domainIDToBranchGroup=new HashMap();	
	private Collection selectedDomains=new HashSet();
	private String name;
	/**
	 * 
	 */
	public ViewableFE(FEProvider provider)
	{
		this.provider=provider;
		visibleDomain=new HashMap();
		int[] ids=provider.getDomainIDs();
		for(int i=0; i<ids.length; i++)
		{
			visibleDomain.put(new Integer(ids[i]), Boolean.TRUE);
		}
	}
	
	/* (non-Javadoc)
	 * @see jcae.viewer3d.mesh.ViewableMesh#getDomainProvider()
	 */
	public DomainProvider getDomainProvider()
	{
		return provider;
	}		
	
	/**
	 * @param types collection of allowed elements type (collection of Short)
	 * @see FESelectionListener for type list
	 */
	public void setSelectionFilter(Collection types)
	{
		//TODO
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.Viewable#domainsChanged(java.util.Collection)
	 */
	public void domainsChanged(int[] ids)
	{
		if(branchGroup==null)
		{
			branchGroup=new BranchGroup();
			branchGroup.setCapability(Group.ALLOW_CHILDREN_WRITE);
			branchGroup.setCapability(Group.ALLOW_CHILDREN_EXTEND);
			branchGroup.setCapability(Group.ALLOW_CHILDREN_READ);
		}
		if(ids!=null)
		{
			for(int i=0; i<ids.length; i++)
			{
				
				BranchGroup dbg=(BranchGroup) domainIDToBranchGroup.get(new Integer(ids[i]));
				if(dbg!=null)
				{
					branchGroup.removeChild(dbg);
					domainIDToBranchGroup.remove(new Integer(ids[i]));
				}
				
				Boolean b=(Boolean) visibleDomain.get(new Integer(ids[i]));
				if(b.booleanValue())
				{
					Logger.global.finest("<Loading domain "+ids[i]+">");
					FEDomain d=(FEDomain) provider.getDomain(ids[i]);
					branchGroup.addChild(createBranchGroup(getGeomForTrianglesGroup(d), d));
					Logger.global.finest("</Loading domain "+ids[i]+">");
				}
			}
		}
		else
		{
			domainIDToBranchGroup.clear();
			branchGroup.removeAllChildren();
			ids=getDomainProvider().getDomainIDs();
			for(int i=0; i<ids.length; i++)
			{
				Boolean b=(Boolean) visibleDomain.get(new Integer(ids[i]));
				if(b.booleanValue())
				{
					Logger.global.finest("<Loading domain "+ids[i]+">");
					FEDomain d=(FEDomain) provider.getDomain(ids[i]);
					branchGroup.addChild(createBranchGroup(getGeomForTrianglesGroup(d), d));
					Logger.global.finest("</Loading domain "+ids[i]+">");
				}				
			}
		}
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.Viewable#setDomainVisible(java.util.Map)
	 */
	public void setDomainVisible(Map map)
	{
		Iterator it=map.entrySet().iterator();
		while(it.hasNext())
		{			
			Map.Entry entry= (Entry) it.next();
			Boolean newStatus=(Boolean) entry.getValue();
			Boolean oldStatus=(Boolean) visibleDomain.get(entry.getKey());
			
			if(oldStatus==null)
				oldStatus=Boolean.FALSE;
			
			if(!newStatus.booleanValue() && oldStatus.booleanValue())
			{
				BranchGroup dbg=(BranchGroup) domainIDToBranchGroup.get(entry.getKey());
				if(dbg!=null)
				{
					branchGroup.removeChild(dbg);					
				}
			}
			else if(newStatus.booleanValue() && !oldStatus.booleanValue())
			{
				BranchGroup dbg=(BranchGroup) domainIDToBranchGroup.get(entry.getKey());
				if(dbg!=null)
				{
					branchGroup.addChild(dbg);
				}
				else
				{
					FEDomain d=(FEDomain) provider.getDomain(((Integer)entry.getKey()).intValue());
					branchGroup.addChild(createBranchGroup(getGeomForTrianglesGroup(d), d));
				}
			}
			visibleDomain.put(entry.getKey(), newStatus);
		}
	}
	
	/**
	 * Emphasize edges owned by only one face
	 * @param visible
	 */
	public void setFreeEdgesVisible(boolean visible)
	{
		
	}
	
	/**
	 * Emphasize edges owned by more than 2 faces
	 * @param visible
	 */
	public void setMultiEdgesVisible(boolean visible)
	{
		
	}
	
	/**
	 * Add a new listener to the current Viewable.
	 * @param listener
	 */
	public void addSelectionListener(FESelectionListener listener)
	{
		selectionListeners.add(listener);
	}

	public void removeSelectionListener(FESelectionListener listener)
	{
		selectionListeners.remove(listener);
	}
	
	/**
	 * Return a color palette showing the color used to represent the result
	 * in this viewable.
	 * The palette include numerical graduation.
	 * The caller may resize the returned component to get an horizontal or a
	 * vertical palette.
	 * @return
	 */
	public Component getResultPalette()
	{
		//TODO
		return new JPanel();
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.Viewable#pick(com.sun.j3d.utils.picking.PickResult)
	 */
	public void pick(PickResult result, boolean selected)
	{		
		Logger.global.finest("result="+result);
		Logger.global.finest("result.getGeometryArray().getUserData()="+result.getGeometryArray().getUserData());
		Object o=result.getGeometryArray().getUserData();
		if(o instanceof FEDomain)
		{
			FEDomain domain=(FEDomain) o;
			setSelectedDomain(domain.getID(), selected);
			fireSelectionChanged();
		}		
	}

	public void hightLight(int domainID, boolean selected)
	{
		setSelectedDomain(domainID, selected);
		fireSelectionChanged();
	}
		
	private void setSelectedDomain(int domainID, boolean selected)
	{
		BranchGroup bg=(BranchGroup) domainIDToBranchGroup.get(new Integer(domainID));
		
		if(bg==null) //test for empty groups
			return;
		
		Logger.global.finest("Changing color of domain n°"+domainID+" to red. bg="+bg);
		Color colorToSet;
		if(selected)
		{
			colorToSet=Color.RED;
			selectedDomains.add(new Integer(domainID));
		}
		else
		{
			colorToSet=Color.WHITE;
			selectedDomains.remove(new Integer(domainID));
		}
		((Shape3D)bg.getChild(1)).getAppearance().getColoringAttributes().setColor(new Color3f(colorToSet));		
	}
	
	private void fireSelectionChanged()
	{
		Map map=new HashMap();		
		Iterator it=selectedDomains.iterator();
		while(it.hasNext())
		{
			map.put(it.next(), null);
		}
		Logger.global.finest("selected domains are:"+map.keySet());
		for(int i=0; i<selectionListeners.size(); i++)
		{
			FESelectionListener listener=(FESelectionListener) selectionListeners.get(i);
			listener.elementsSelected(map);
		}
	}
	
	private IndexedTriangleArray getGeomForTrianglesGroup(FEDomain domain)
	{
		if(domain.getNumberOfNodes()==0)
			return null;
		int[] tria3=new int[domain.getNumberOfTria3()*3];
		float[] nodes=new float[domain.getNumberOfNodes()*3]; 
		Iterator it=domain.getNodesIterator();
		int i=0;
		while(it.hasNext())
		{
			float[] f=(float[]) it.next();
			System.arraycopy(f, 0, nodes, i, 3);
			i+=3;
		}
		
		i=0;
		it=domain.getTria3Iterator();
		while(it.hasNext())
		{
			int[] f=(int[]) it.next();
			System.arraycopy(f, 0, tria3, i, 3);
			i+=3;
		}
			
		IndexedTriangleArray geom = new IndexedTriangleArray(nodes.length / 3,
			TriangleArray.COORDINATES, tria3.length);
		geom.setCoordinateIndices(0, tria3);
		geom.setCapability(IndexedTriangleArray.ALLOW_COUNT_READ);
		geom.setCapability(IndexedTriangleArray.ALLOW_FORMAT_READ);
		geom.setCapability(IndexedTriangleArray.ALLOW_COORDINATE_READ);
		geom.setCapability(IndexedTriangleArray.ALLOW_COORDINATE_INDEX_READ);
		geom.setCoordinates(0, nodes);
		geom.setUserData(domain);
		return geom;
	}

	/**
	 * Creates a Java3D BranchGroup whcih represents a group.
	 * It creates two Java3D Shapes3D : one for polygons, one for edges.
	 * @param the Java3D geometry of a Group.
	 */
	private BranchGroup createBranchGroup(IndexedTriangleArray geom, FEDomain domain)
	{
		BranchGroup branchGroup = new BranchGroup();
		if(geom==null)
		{
			return branchGroup;
		}
		Appearance shapeFillAppearance = new Appearance();
		shapeFillAppearance.setPolygonAttributes(new PolygonAttributes(
			PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE,
			2.0f * Float.parseFloat(System.getProperty(
				"javax.media.j3d.zFactorAbs", "20.0f")), false, Float
				.parseFloat(System.getProperty("javax.media.j3d.zFactorRel",
					"2.0f"))));
		shapeFillAppearance.setColoringAttributes(new ColoringAttributes(
			new Color3f(domain.getColor().darker()), ColoringAttributes.SHADE_FLAT));
		Shape3D shapeFill = new Shape3D(geom, shapeFillAppearance);		
		shapeFill.setCapability(Shape3D.ALLOW_GEOMETRY_READ);		
		branchGroup.addChild(shapeFill);
		Appearance shapeLineAppearance = new Appearance();
		shapeLineAppearance.setPolygonAttributes(new PolygonAttributes(
			PolygonAttributes.POLYGON_LINE, PolygonAttributes.CULL_NONE, Float
				.parseFloat(System.getProperty("javax.media.j3d.zFactorAbs",
					"20.0f"))));
		ColoringAttributes ca = new ColoringAttributes(new Color3f(Color.WHITE), ColoringAttributes.SHADE_GOURAUD);
		ca.setCapability(ColoringAttributes.ALLOW_COLOR_WRITE);
		shapeLineAppearance.setColoringAttributes(ca);
		Shape3D shapeLine = new Shape3D(geom, shapeLineAppearance);
		shapeLine.setPickable(false);
		shapeLine.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
		shapeLineAppearance.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_READ);		
		branchGroup.addChild(shapeLine);
		branchGroup.setCapability(Group.ALLOW_CHILDREN_READ);
		branchGroup.setCapability(BranchGroup.ALLOW_DETACH);
		domainIDToBranchGroup.put(new Integer(domain.getID()), branchGroup);
		return branchGroup;
	}	
	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.Viewable#getBranchGroup()
	 */
	public Node getJ3DNode()
	{
		if(branchGroup==null)
			domainsChanged(getDomainProvider().getDomainIDs());
		return branchGroup;
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
	
	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.Viewable#unselectAll()
	 */
	public void unselectAll()
	{
		int[] ids=integerCollectionToArray(selectedDomains);
		for(int i=0; i<ids.length; i++)
		{
			setSelectedDomain(ids[i], false);
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
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.Viewable#removeSelectionListener(org.jcae.viewer3d.SelectionListener)
	 */
	public void removeSelectionListener(SelectionListener listener)
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
}
