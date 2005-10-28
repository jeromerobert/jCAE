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
import java.util.Map.Entry;
import java.util.logging.Logger;
import javax.media.j3d.*;
import javax.swing.JPanel;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import org.jcae.viewer3d.DomainProvider;
import org.jcae.viewer3d.SelectionListener;
import org.jcae.viewer3d.Viewable;
import org.jcae.viewer3d.fd.Utils;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.picking.PickIntersection;
import com.sun.j3d.utils.picking.PickResult;

/**
 * @author Jerome Robert
 * @todo implements all methods
 */
public class ViewableFE implements Viewable
{
	public static final byte PICK_DOMAIN = 2;
	public static final byte PICK_NODE = 1;
	private FEProvider provider;
	private Map visibleDomain;
	private BranchGroup branchGroup;
	private Shape3D nodeSelectionShape=new Shape3D();
	private Map nodeSelections=new HashMap();
	private Map domainIDToBranchGroup=new HashMap();	
	private Collection selectedDomains=new HashSet();
	private String name;
	private Collection listeners=Collections.synchronizedCollection(new ArrayList());
	private short pickingMode=PICK_DOMAIN;
	private boolean showShapeLine=true;
	private static final float zFactorAbs=Float.parseFloat(System.getProperty(
		"javax.media.j3d.zFactorAbs", "20.0f"));
	private static final float zFactorRel=Float.parseFloat(System.getProperty(
		"javax.media.j3d.zFactorRel", "2.0f"));

	final private static PolygonAttributes FILL_POLYGON_ATTR=new PolygonAttributes(
		PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE,
		2.0f * zFactorAbs, true, zFactorRel);
	
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
		branchGroup=new BranchGroup();
		branchGroup.setCapability(Group.ALLOW_CHILDREN_WRITE);
		branchGroup.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		branchGroup.setCapability(Group.ALLOW_CHILDREN_READ);
		Appearance app=new Appearance();
		PointAttributes pa=new PointAttributes(4f, false);
		app.setPointAttributes(pa);
		nodeSelectionShape.setAppearance(app);
	}
	
	/* (non-Javadoc)
	 * @see jcae.viewer3d.mesh.ViewableMesh#getDomainProvider()
	 */
	public DomainProvider getDomainProvider()
	{
		return provider;
	}		

	/* (non-Javadoc)
	 * @see jcae.viewer3d.Viewable#domainsChanged(java.util.Collection)
	 */
	public void domainsChanged(int[] ids)
	{
		if(ids!=null)
		{
			for(int i=0; i<ids.length; i++)
			{
				
				//If the domain already exists remove it before readding it
				BranchGroup dbg=(BranchGroup) domainIDToBranchGroup.get(new Integer(ids[i]));
				if(dbg!=null)
				{
					branchGroup.removeChild(dbg);
					domainIDToBranchGroup.remove(new Integer(ids[i]));
				}
				
				//If the domain is not visible do not readd it
				Boolean b=(Boolean) visibleDomain.get(new Integer(ids[i]));
				if(b.booleanValue())
				{
					Logger.global.finest("<Loading domain "+ids[i]+">");
					createBranchGroup((FEDomain)provider.getDomain(ids[i]));
					Logger.global.finest("</Loading domain "+ids[i]+">");
				}
			}
		}
		else //for a null parameter remove all domains
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
					createBranchGroup((FEDomain)provider.getDomain(ids[i]));
					Logger.global.finest("</Loading domain "+ids[i]+">");
				}				
			}
		}
	}
	
	private void createBranchGroup(FEDomain d)
	{
		if(d.getNumberOfTria3()>0)
			branchGroup.addChild(createTriaBranchGroup(getGeomForTrianglesGroup(d), d));
		else if(d.getNumberOfBeam2()>0)
			branchGroup.addChild(createBeamBranchGroup(d));		
	}
	
	private Node createBeamBranchGroup(FEDomain d)
	{
		BranchGroup bg=new BranchGroup();
		IndexedLineArray ila=new IndexedLineArray(
			d.getNumberOfNodes(),
			IndexedGeometryArray.COORDINATES,
			d.getNumberOfBeam2()*2);
		ila.setCoordinates(0, d.getNodes());
		ila.setCoordinateIndices(0, d.getBeam2Indices());
		Appearance app=new Appearance();
		LineAttributes la=new LineAttributes(3f, LineAttributes.PATTERN_SOLID, false);
		
		ColoringAttributes ca=new ColoringAttributes(new Color3f(d.getColor()),
			ColoringAttributes.FASTEST);
		
		app.setLineAttributes(la);
		app.setColoringAttributes(ca);
		Shape3D s3d=new Shape3D(ila, app);
		bg.addChild(s3d);
		domainIDToBranchGroup.put(new Integer(d.getID()), bg);	
		return bg;
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
					branchGroup.addChild(createTriaBranchGroup(getGeomForTrianglesGroup(d), d));
				}
			}
			visibleDomain.put(entry.getKey(), newStatus);
		}
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

	public void setPickingMode(byte mode)
	{
		pickingMode=mode;
	}
	
	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.Viewable#pick(com.sun.j3d.utils.picking.PickResult)
	 */
	public void pick(PickResult result, boolean selected)
	{		
		Logger.global.finest("result="+result);
		Logger.global.finest("result.getGeometryArray().getUserData()="+result.getGeometryArray().getUserData());
		Integer o=(Integer)result.getGeometryArray().getUserData();		
		
		switch(pickingMode)
		{
			case PICK_NODE:
				PickIntersection pi=result.getIntersection(0);
				
				int[] ids=pi.getPrimitiveVertexIndices();
				setSelectedNode(
					ids[0]/ids.length,
					(byte) pi.getClosestVertexIndex(),
					pi.getClosestVertexCoordinates(),
					o.intValue(),
					selected);
			break;
			case PICK_DOMAIN:
				setSelectedDomain(o.intValue(), selected);
				fireSelectionChanged();
			break;
		}		
	}

	private void setSelectedNode(int triaID, byte nodeID, Point3d point3d,
		int domainID, boolean selected)
	{
		if(selected)
		{
			NodeSelectionImpl ns=(NodeSelectionImpl)
				nodeSelections.get(new Integer(domainID));
			if(ns==null)
			{
				ns=new NodeSelectionImpl(domainID);
				nodeSelections.put(new Integer(domainID), ns);
			}
			ns.addNode(triaID, nodeID);
			PointArray pa=new PointArray(1, PointArray.COORDINATES);
			nodeSelectionShape.addGeometry(pa);
			fireSelectionChanged();
		}
		else
		{
			//TODO Implement unselect
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
		((Shape3D)bg.getChild(bg.numChildren()-1)).getAppearance().
			getColoringAttributes().setColor(new Color3f(colorToSet));		
	}
	
	private IndexedTriangleArray getGeomForTrianglesGroup(FEDomain domain)
	{
		if(domain.getNumberOfNodes()==0 || domain.getNumberOfTria3()==0)
			return null;
		int[] tria3=new int[domain.getNumberOfTria3()*3];
		float[] nodes=domain.getNodes();
		if(nodes==null)
			nodes=iteratorToArray(domain.getNodesIterator(), domain.getNumberOfNodes());
		
		int i=0;
		Iterator it=domain.getTria3Iterator();
		while(it.hasNext())
		{
			int[] f=(int[]) it.next();
			System.arraycopy(f, 0, tria3, i, 3);
			i+=3;
		}
		
		IndexedTriangleArray geom;
		if(showShapeLine)
		{
			geom = new IndexedTriangleArray(nodes.length / 3,
				GeometryArray.COORDINATES, tria3.length);
			geom.setCoordinateIndices(0, tria3);
			geom.setCoordinates(0, nodes);
		}
		else
		{
			GeometryInfo gi=new GeometryInfo(GeometryInfo.TRIANGLE_ARRAY);
			gi.setCoordinates(nodes);
			gi.setCoordinateIndices(tria3);
			NormalGenerator ng=new NormalGenerator(0);
			ng.generateNormals(gi);
			geom=(IndexedTriangleArray) gi.getIndexedGeometryArray();
		}				
		
		geom.setCapability(GeometryArray.ALLOW_COUNT_READ);
		geom.setCapability(GeometryArray.ALLOW_FORMAT_READ);
		geom.setCapability(GeometryArray.ALLOW_COORDINATE_READ);
		geom.setCapability(IndexedGeometryArray.ALLOW_COORDINATE_INDEX_READ);
		
		geom.setUserData(new Integer(domain.getID()));
		return geom;
	}

	private float[] iteratorToArray(Iterator it, int numberOfNodes)
	{
		float[] toReturn = new float[numberOfNodes*3]; 
		int i=0;
		while(it.hasNext())
		{
			float[] f=(float[]) it.next();
			System.arraycopy(f, 0, toReturn, i, 3);
			i+=3;
		}
		return toReturn;
	}

	/**
	 * Creates a Java3D BranchGroup whcih represents a group.
	 * It creates two Java3D Shapes3D : one for polygons, one for edges.
	 * @param the Java3D geometry of a Group.
	 */
	private BranchGroup createTriaBranchGroup(IndexedTriangleArray geom, FEDomain domain)
	{
		BranchGroup branchGroup = new BranchGroup();
		if(geom==null)
		{
			return branchGroup;
		}
		Appearance shapeFillAppearance = new Appearance();
		shapeFillAppearance.setPolygonAttributes(FILL_POLYGON_ATTR);
		Shape3D shapeFill = new Shape3D(geom, shapeFillAppearance);
		
		if(showShapeLine)
		{
			shapeFillAppearance.setColoringAttributes(new ColoringAttributes(
				new Color3f(domain.getColor().darker()), ColoringAttributes.FASTEST));
		}
		else
		{
			Material m=new Material();
			m.setAmbientColor(new Color3f(domain.getColor()));
			shapeFillAppearance.setMaterial(m);
			shapeFill.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
			shapeFillAppearance.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_READ);
		}
						
		shapeFill.setCapability(Shape3D.ALLOW_GEOMETRY_READ);		
		branchGroup.addChild(shapeFill);
		
		if(showShapeLine)
		{
			Appearance shapeLineAppearance = new Appearance();
			shapeLineAppearance.setPolygonAttributes(new PolygonAttributes(
				PolygonAttributes.POLYGON_LINE, PolygonAttributes.CULL_NONE, Float
					.parseFloat(System.getProperty("javax.media.j3d.zFactorAbs",
						"20.0f"))));
			ColoringAttributes ca = new ColoringAttributes(new Color3f(Color.WHITE), ColoringAttributes.FASTEST);
			ca.setCapability(ColoringAttributes.ALLOW_COLOR_WRITE);
			shapeLineAppearance.setColoringAttributes(ca);
			Shape3D shapeLine = new Shape3D(geom, shapeLineAppearance);
			shapeLine.setPickable(false);
			shapeLine.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
			shapeLineAppearance.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_READ);		
			branchGroup.addChild(shapeLine);
		}
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
		if(branchGroup.numChildren()==0)
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
		nodeSelections.clear();
		nodeSelectionShape.removeAllGeometries();
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
		listeners.add(listener);
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.Viewable#removeSelectionListener(org.jcae.viewer3d.SelectionListener)
	 */
	public void removeSelectionListener(SelectionListener listener)
	{
		listeners.add(listener);
	}
	
	protected void fireSelectionChanged()
	{
		Iterator it = listeners.iterator();
		while (it.hasNext())
		{
			SelectionListener s = (SelectionListener) it.next();
			s.selectionChanged();
		}
	}
	
	public int[] getSelectedDomains()
	{
		return integerCollectionToArray(selectedDomains);
	}
	
	public NodeSelection[] getSelectedNodes()
	{
		NodeSelection[] toReturn=new NodeSelection[nodeSelections.size()];
		Iterator it=nodeSelections.values().iterator();
		int i=0;
		while(it.hasNext())
		{
			toReturn[i++]=(NodeSelection) ((NodeSelectionImpl)it.next()).clone();
		}
		return toReturn;
	}

	public void setShowShapeLine(boolean showShapeLine)
	{
		this.showShapeLine = showShapeLine;
	}
}
