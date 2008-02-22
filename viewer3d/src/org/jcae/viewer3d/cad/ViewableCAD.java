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
 * (C) Copyright 2007, by EADS France
 */

package org.jcae.viewer3d.cad;

import java.awt.Color;
import java.util.*;
import java.util.logging.Logger;
import javax.media.j3d.*;
import javax.vecmath.Color3f;
import org.jcae.viewer3d.*;
import org.jcae.viewer3d.cad.occ.OCCFaceDomain;

import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.geometry.Stripifier;

/**
 * A Viewable specilized to display CAD scenes.
 * The selected elements are highlighted.
 * @author Jerome Robert
 * @todo all methods must be implemented.
 */
public class ViewableCAD extends ViewableAdaptor 
{	
	private static final float zFactorAbs=Float.parseFloat(System.getProperty(
		"javax.media.j3d.zFactorAbs", "20.0f"));
	private static final float zFactorRel=Float.parseFloat(System.getProperty(
		"javax.media.j3d.zFactorRel", "2.0f"));
	private final static Color3f BACK_FACE_COLOR=new Color3f(0.5f,1f,0.5f);
	private final static Color3f FRONT_FACE_COLOR=new Color3f(0.5f,0.5f,1f);
	public final static PolygonAttributes polygonAttrFront =
		new PolygonAttributes(PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_FRONT,
				20.0f * zFactorAbs, false, zFactorRel);
	public final static PolygonAttributes polygonAttrBack =
		new PolygonAttributes(PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_BACK,
				20.0f * zFactorAbs, false, zFactorRel);
	public final static PolygonAttributes polygonAttrNone =
		new PolygonAttributes(PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE,
				20.0f * zFactorAbs, false, zFactorRel);
	
	static
	{
		polygonAttrFront.setCapability(PolygonAttributes.ALLOW_OFFSET_WRITE);
		polygonAttrBack.setCapability(PolygonAttributes.ALLOW_OFFSET_WRITE);
		polygonAttrNone.setCapability(PolygonAttributes.ALLOW_OFFSET_WRITE);
	}
	
	protected static abstract class CADPickingInfo{
		public int id;
	}
	
	protected static class FacePickingInfo extends CADPickingInfo
	{		
		Material[] materials;
		Color3f[] oldColor;
		/**
		 * @param id
		 * @param coloringAttributes
		 */
		public FacePickingInfo(int id, Material[] material){
			this(id,material,new Color3f(Color.WHITE));
		}

		public FacePickingInfo(int id, Material[] material,Color3f oldColor)
		{		
			this.materials = material;
			this.oldColor=new Color3f[]{oldColor};
		}

		public FacePickingInfo(int id, Material[] material,Color3f oldColor1, Color3f oldColor2)
		{
			this.id = id;
			this.materials = material;
			this.oldColor=new Color3f[]{oldColor1, oldColor2};
		}
	}  
	
	protected static class EdgePickingInfo extends CADPickingInfo
	{
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
			this.appearance = appearance;
			this.coloringAttributes = coloringAttributes;
		}
	}
		
	protected static class VertexPickingInfo extends CADPickingInfo
	{
		int id;
		Appearance appearance;
		Shape3D shape3D;
		/**
		 * 
		 * @param id
		 * @param appearance
		 * @param coloringAttributes
		 */
		public VertexPickingInfo(int id, Appearance appearance, Shape3D shape3D)
		{
			this.id = id;
			this.appearance = appearance;
			this.shape3D = shape3D;
		}
	}
	
	public final static short NONE_SELECTION=0;
	public final static short DOMAIN_SELECTION=1;
	public final static short FACE_SELECTION=2;
	public final static short EDGE_SELECTION=3;
	public final static short VERTEX_SELECTION=4;
	public final static short MULTI_SELECTION=5;
	
	public final static int SELECTION_INVERT=0;
	public final static int SELECTION_ADD=1;
	public final static int SELECTION_REMOVE=2;
	
	private short selectionMode=FACE_SELECTION;
	private int selectionAction;
	private CADProvider provider;
	private BranchGroup branchGroup=new BranchGroup();
	private List selectionListeners=new ArrayList();
	private Map<Integer, FacePickingInfo> facesInfo;
	private Map<Integer, EdgePickingInfo> edgesInfo;
	private Map<Integer, VertexPickingInfo> verticesInfo;
	private Collection<Integer> selectedFaces=new HashSet<Integer>();
	private Collection<Integer> selectedEdges=new HashSet<Integer>();
	private Collection<Integer> selectedVertices=new HashSet<Integer>();
	private String name;
	private LineAttributes lineAttributes=new LineAttributes();
	private Node edgesNode;
	private Node facesNode;
	private Node verticesNode;
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
	@Override
	public void domainsChangedPerform(int[] domainId)
	{
		if(domainId==null)
			domainId=new int[]{0,1,2};
		if(branchGroup.numChildren()>0)
			branchGroup.removeAllChildren();
		for(int i=0;i<domainId.length;i++){
		
			switch(domainId[i]){
			case 0 : //Edges
				edgesNode=createEdgesNode((CADDomain) provider.getDomain(0));
				branchGroup.addChild(edgesNode);
				break;
			case 1 : //Faces
				facesNode=createFacesNode((CADDomain) provider.getDomain(1));
				branchGroup.addChild(facesNode);
				break;
			case 2 : //Vertices
				verticesNode=createVerticesNode((CADDomain) provider.getDomain(2));
				branchGroup.addChild(verticesNode);
				break;
			}
		}
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.Viewable#getDomainProvider()
	 */
	@Override
	public DomainProvider getDomainProvider()
	{
		return provider;
	}

	/* (non-Javadoc)
	 * @see jcae.viewer3d.Viewable#setDomainVisible(java.util.Map)
	 */
	@Override
	public void setDomainVisible(Map<Integer, Boolean> map)
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
		setPickable(verticesNode, mode==VERTEX_SELECTION);
		setPickable(facesNode, mode==FACE_SELECTION);
		setPickable(edgesNode, mode==EDGE_SELECTION);
	}
		
	/** Set sub shape3d pickable status */
	private void setPickable(Node node, boolean b)
	{
		if(node instanceof Group)
		{
			Group g=(Group)node;
			for(int i=0; i<g.numChildren(); i++)
			{
				setPickable(g.getChild(i), b);
			}
		}
		else if(node instanceof Shape3D)
		{
			node.setPickable(b);
		}
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
	@Override
	public void pick(PickViewable result)
	{
		Logger.getLogger("global").finest(
			"result.getGeometryArray().getUserData()=" +
			result.getGeometryArray().getUserData());
		if(pick(result.getGeometryArray()))
			fireSelectionChanged();
	}
	
	private boolean pick(Geometry geom)
	{
		if(geom == null)
			return false;
		
		Object o=geom.getUserData();
		if((o instanceof FacePickingInfo)&(selectionMode==FACE_SELECTION))
		{
			FacePickingInfo fpi=(FacePickingInfo) o;
			setFaceSelected(fpi, checkToSelect(selectedFaces, fpi));
			return true;
		}
		else if((o instanceof EdgePickingInfo)&(selectionMode==EDGE_SELECTION))
		{
			EdgePickingInfo epi=(EdgePickingInfo) o;			
			setEdgeSelected(epi, checkToSelect(selectedEdges, epi));
			return true;
		}
		else if((o instanceof VertexPickingInfo)&(selectionMode==VERTEX_SELECTION))
		{
			VertexPickingInfo epi=(VertexPickingInfo) o;
			setVertexSelected(epi, checkToSelect(selectedVertices, epi));
			return true;
		}
		return false;
	}
	
	private boolean checkToSelect(Collection selected, CADPickingInfo info)
	{
		boolean toReturn;
		if(selectionAction==SELECTION_INVERT)
			toReturn = !selected.contains(Integer.valueOf(info.id));
		else
			toReturn = selectionAction == SELECTION_ADD;
		return toReturn;
	}
	
	@Override
	public void pickArea(PickInfo[] result, Bounds bound) 
	{
		HashSet<Geometry> as = new HashSet();
		for(PickInfo pickInfo: result)
		{
			Node n = pickInfo.getNode();
			if(n instanceof Shape3D)
				as.add(((Shape3D)n).getGeometry());
		}

		boolean b = false;
		for(Geometry a: as)
			b = b || pick(a);
		if(b)
			fireSelectionChanged();
	}	

	static private int[] integerCollectionToArray(Collection<Integer> collection)
	{
		int[] toReturn=new int[collection.size()];
		Iterator<Integer> it=collection.iterator();
		int i=0;
		while(it.hasNext())
		{
			Integer n=it.next();
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
	public void highlightFace(int faceID, boolean status)
	{
		highlightFace(faceID, status,true);
	}
	/**
	 * 
	 * @param faceID
	 * @param status
	 * @param fireListeners
	 */
	public void highlightFace(int faceID, boolean status,boolean fireListeners)
	{
		
		FacePickingInfo fpi=facesInfo.get(new Integer(faceID));
		setFaceSelected(fpi, status);
		if(fireListeners) fireSelectionChanged();
		
	}
	
	/**
	 * 
	 * @param edgeID
	 * @param status
	 */
	public void highlightEdge(int edgeID, boolean status)
	{
		highlightEdge(edgeID, status, true);
	}
	/**
	 * 
	 * @param edgeID
	 * @param status
	 * @param fireListeners
	 */
	public void highlightEdge(int edgeID, boolean status,boolean fireListeners)
	{
			EdgePickingInfo epi=edgesInfo.get(new Integer(edgeID));
			setEdgeSelected(epi, status);
			if(fireListeners) fireSelectionChanged();
	}
	
	public void highlightVertex(int vertexID, boolean status)
	{
		highlightVertex(vertexID, status, true);
	}

	public void highlightVertex(int vertexID, boolean status,boolean fireListeners)
	{
		VertexPickingInfo epi=verticesInfo.get(new Integer(vertexID));
		setVertexSelected(epi, status);
		if(fireListeners) fireSelectionChanged();
	}
	
	private void setFaceSelected(FacePickingInfo fpi, boolean selected)
	{
		Color3f[] colorToSet=new Color3f[fpi.materials.length];
		if(selected)
		{
			Arrays.fill(colorToSet, new Color3f(Color.RED));
			selectedFaces.add(Integer.valueOf(fpi.id));
		}
		else
		{
			colorToSet=fpi.oldColor;
			selectedFaces.remove(Integer.valueOf(fpi.id));
		}
		
		for(int i=0; i<fpi.materials.length; i++)
		{
			fpi.materials[i].setDiffuseColor(colorToSet[i]);
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
	
	private void setVertexSelected(VertexPickingInfo epi, boolean selected)
	{
		if(selected)
		{
			ColoringAttributes ca=new ColoringAttributes();
			ca.setColor(new Color3f(Color.YELLOW));
			PointAttributes pa=new PointAttributes(5f, false);
			Appearance a=new Appearance();
			a.setPointAttributes(pa);
			a.setColoringAttributes(ca);
			epi.shape3D.setAppearance(a);
			selectedVertices.add(new Integer(epi.id));
		}
		else
		{
			epi.shape3D.setAppearance(epi.appearance);
			selectedVertices.remove(new Integer(epi.id));
		}
		
	}
	
	@Override
	protected void fireSelectionChanged()
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
				integerCollectionToArray(selectedFaces),integerCollectionToArray(selectedEdges)
				, integerCollectionToArray(selectedVertices))};
	}
	
	private Node createEdgesNode(CADDomain domain)
	{
		Iterator<float[]> it=domain.getEdgeIterator();
		BranchGroup toReturn=new BranchGroup();
		toReturn.setCapability(BranchGroup.ALLOW_DETACH);
		org.jcae.viewer3d.MarkUtils.setPickable(toReturn,true);
		edgesInfo=new HashMap<Integer, EdgePickingInfo>();
		int n=0;
		
		while(it.hasNext())
		{
			float[] coordinates=it.next();			
			LineStripArray lsa=new LineStripArray(coordinates.length/3,
				GeometryArray.COORDINATES,
				new int[]{coordinates.length/3});
			
			lsa.setCapability(GeometryArray.ALLOW_COLOR_READ);
			lsa.setCapability(GeometryArray.ALLOW_COLOR_WRITE);
			lsa.setCapability(GeometryArray.ALLOW_COORDINATE_READ);
			lsa.setCapability(GeometryArray.ALLOW_COUNT_READ);
			lsa.setCapability(GeometryArray.ALLOW_FORMAT_READ);
			
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
			shape3d.setCapability(Node.ALLOW_PICKABLE_WRITE);
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
		Iterator<FaceMesh> it=domain.getFaceIterator();
		BranchGroup toReturn=new BranchGroup();
		toReturn.setCapability(BranchGroup.ALLOW_DETACH);
		int n=0;
		facesInfo=new HashMap<Integer, FacePickingInfo>();
		
		Vector<Material> materials=new Vector<Material>();//Vector to save Face Materials
		
		while(it.hasNext())
		{			
			materials.clear();
			
			FaceMesh fm=it.next();
			
			//Case of an unmeshed face
			if(fm.getNodes().length==0){
				FacePickingInfo fpi = 
					new FacePickingInfo(n,new Material[0]);
				facesInfo.put(Integer.valueOf(n), fpi);
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
			g.setCapability(GeometryArray.ALLOW_COUNT_READ);
			g.setCapability(GeometryArray.ALLOW_FORMAT_READ);
			g.setCapability(GeometryArray.ALLOW_COORDINATE_READ);
			g.setCapability(IndexedGeometryArray.ALLOW_COORDINATE_INDEX_READ);	
			
			Color faceColor=null;
			if(domain instanceof OCCFaceDomain)
				faceColor=((OCCFaceDomain)domain).getFaceColor(n);
			
			if(faceColor==null) {
				Shape3D shape3d=new Shape3D(g);
				Appearance a=new Appearance();
				shape3d.setAppearance(a);
				a.setPolygonAttributes(polygonAttrFront);
				Material m1=new Material();
				m1.setDiffuseColor(FRONT_FACE_COLOR);
				m1.setSpecularColor(new Color3f(Color.BLACK));
				m1.setAmbientColor(FRONT_FACE_COLOR);
				m1.setCapability(Material.ALLOW_COMPONENT_WRITE);
				a.setMaterial(m1);			
				shape3d.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
				shape3d.setCapability(Node.ALLOW_PICKABLE_WRITE);
				toReturn.addChild(shape3d);
				materials.add(m1);
				
				shape3d=new Shape3D(g);
				a=new Appearance();
				shape3d.setAppearance(a);
				a.setPolygonAttributes(polygonAttrBack);
				Material m2=new Material();
				m2.setDiffuseColor(BACK_FACE_COLOR);
				m2.setSpecularColor(new Color3f(Color.BLACK));
				m2.setAmbientColor(BACK_FACE_COLOR);
				m2.setCapability(Material.ALLOW_COMPONENT_WRITE);			
				a.setMaterial(m2);			
				shape3d.setCapability(Shape3D.ALLOW_GEOMETRY_READ);	
				shape3d.setCapability(Node.ALLOW_PICKABLE_WRITE);
				toReturn.addChild(shape3d);	
				materials.add(m2);
				
				//Build Picking Data
				FacePickingInfo fpi =  new FacePickingInfo(n,
					materials.toArray(new Material[materials.size()]),
					FRONT_FACE_COLOR, BACK_FACE_COLOR);
				facesInfo.put(Integer.valueOf(n), fpi); 
				g.setUserData(fpi);
			}
			else {
				Shape3D shape3d=new Shape3D(g);
				Appearance a=new Appearance();
				shape3d.setAppearance(a);
				a.setPolygonAttributes(polygonAttrNone);
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
				shape3d.setCapability(Node.ALLOW_PICKABLE_WRITE);
				toReturn.addChild(shape3d);
				materials.add(m1);
				
				//Build Picking Data
				FacePickingInfo fpi = 
					new FacePickingInfo(n,materials.toArray(new Material[materials.size()])
							,color);
				facesInfo.put(Integer.valueOf(n), fpi); 
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
		int n=0;
		verticesInfo=new HashMap<Integer, VertexPickingInfo>();
		PointAttributes pat=new PointAttributes(pointSize, false);
		for(int i=0; i<points.length/3; i++)
		{
			PointArray pa=new PointArray(1, GeometryArray.COORDINATES);
			pa.setCoordinates(0, new float[]{points[3*i], points[3*i+1], points[3*i+2]});
			Appearance a=new Appearance();			
			a.setPointAttributes(pat);
			ColoringAttributes ca=new ColoringAttributes(
				new Color3f(Color.RED),
				ColoringAttributes.FASTEST);
			a.setColoringAttributes(ca);
			
			Shape3D s3d = new Shape3D(pa, a);
			s3d.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
			s3d.setCapability(Node.ALLOW_PICKABLE_WRITE);
			//return a BranchGroup which is the only detachable node.			
			toReturn.addChild(s3d);
			
			//Build Picking Data
			VertexPickingInfo epi=new VertexPickingInfo(n,a, s3d);
			verticesInfo.put(new Integer(n), epi); 
			pa.setUserData(epi);
			n++;			
		}
		
		return toReturn;
	}
	
	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.Viewable#getBranchGroup(org.jcae.viewer3d.View)
	 */
	@Override
	public Node getJ3DNode()
	{		
		return branchGroup;
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.Viewable#unselectAll()
	 */
	@Override
	public void unselectAll()
	{
		int[] ids=integerCollectionToArray(selectedFaces);
		for(int i=0; i<ids.length; i++)
		{
			FacePickingInfo fpi=facesInfo.get(new Integer(ids[i]));
			setFaceSelected(fpi, false);
		}
		
		ids=integerCollectionToArray(selectedEdges);
		for(int i=0; i<ids.length; i++)
		{
			EdgePickingInfo epi=edgesInfo.get(new Integer(ids[i]));
			setEdgeSelected(epi, false);
		}
		
		ids=integerCollectionToArray(selectedVertices);
		for(int i=0; i<ids.length; i++)
		{
			VertexPickingInfo epi=verticesInfo.get(new Integer(ids[i]));
			setVertexSelected(epi, false);
		}
		fireSelectionChanged();
		
	}
	
	public void setName(String name)
	{
		this.name=name;
	}
	
	@Override
	public String toString()
	{
		return name;
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.Viewable#addSelectionListener(org.jcae.viewer3d.SelectionListener)
	 */
	@Override
	public void addSelectionListener(SelectionListener listener)
	{
		selectionListeners.add(listener);
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.Viewable#removeSelectionListener(org.jcae.viewer3d.SelectionListener)
	 */
	@Override
	public void removeSelectionListener(SelectionListener listener)
	{
		selectionListeners.remove(listener);
	}

	public void setLineWidth(float lineWidth)
	{
		lineAttributes.setLineWidth(lineWidth);
	}	
	
	/** @param s SELECTION_INVERT, SELECTION_ADD, SELECTION_REMOVE */
	public void setSelectionAction(int s)
	{
		selectionAction = s;
	}
	
	public int getSelectionAction()
	{
		return selectionAction;
	}
}
