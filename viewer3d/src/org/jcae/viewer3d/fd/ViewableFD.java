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

package org.jcae.viewer3d.fd;

import java.awt.Color;
import java.util.*;
import java.util.logging.Logger;
import javax.media.j3d.*;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import org.jcae.viewer3d.*;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.picking.PickIntersection;


/**
 * The J3D node of this viewable has the following structure
 *<pre>
 *  parentBranchGroup
 * |
 * +-domainBg
 * |
 * +-wire Shape3D (also in wires Collection)
 * |
 * +-slots Shape3D (also in slots Collection)
 * |
 * +-plate Shape3d (also in plates Collection)
 * | 
 * +-plate wireframe Shape3d
 *</pre>
 * @author Jerome Robert
 *
 */
public class ViewableFD extends ViewableAdaptor
{
	public final static byte SELECT_PLATE=1;
	
	private final static PolygonAttributes PLATE_POLYGON_ATTRIBUTE=new PolygonAttributes(
		PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE,
		3.0f * Float.parseFloat(System.getProperty(
			"javax.media.j3d.zFactorAbs", "20.0f")), false, 2*Float
			.parseFloat(System.getProperty("javax.media.j3d.zFactorRel",
				"2.0f")));

	private final static PolygonAttributes BORDER_POLYGON_ATTRIBUTE=new PolygonAttributes(
		PolygonAttributes.POLYGON_LINE, PolygonAttributes.CULL_NONE,
		2.0f * Float.parseFloat(System.getProperty(
			"javax.media.j3d.zFactorAbs", "20.0f")), false, Float
			.parseFloat(System.getProperty("javax.media.j3d.zFactorRel",
				"2.0f")));

	private final static PolygonAttributes SELECT_POLYGON_ATTRIBUTE=new PolygonAttributes(
		PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE,
		1.0f * Float.parseFloat(System.getProperty(
			"javax.media.j3d.zFactorAbs", "20.0f")), false, Float
			.parseFloat(System.getProperty("javax.media.j3d.zFactorRel",
				"2.0f")));

	private final static LineAttributes SLOT_ATTRIBUTE=new LineAttributes();
	
	/**
	 * dummy object to make difference between plate Shape3D and wireframe
	 * plate Shape3D
	 */
	private final static Object PLATE_IDENTIFIER=new Object();

	/**
	 * dummy object to make difference between solid Shape3D and wireframe
	 * solid Shape3D
	 */
	private final static Object SOLID_IDENTIFIER=new Object();
	
	static
	{
		SLOT_ATTRIBUTE.setLinePattern(LineAttributes.PATTERN_USER_DEFINED);
		SLOT_ATTRIBUTE.setPatternMask(0x5555);
		SLOT_ATTRIBUTE.setLineWidth(2.0f);
	}
	/**
	 * Used to set a typed integer in the UserData field of a J3D Node
	 * Cannot use java.lang.Integer because it is final
	 */
	private static abstract class IntegerUserData
	{
		private int value;

		public IntegerUserData(int value)
		{
			this.value=value;
		}
		
		public int getValue()
		{
			return value;
		}
	}
	
	private static class WireDomainID extends IntegerUserData
	{		
		public WireDomainID(int domainID)
		{
			super(domainID);
		}
	}
	
	private static class SlotDomainID extends IntegerUserData
	{		
		public SlotDomainID(int domainID)
		{
			super(domainID);
		}
	}
	
	private static class XLineID extends IntegerUserData
	{
		public XLineID(int id)
		{
			super(id);
		}		
	}
	
	private static class SlotID extends IntegerUserData
	{
		private byte type;
		
		public SlotID(int id, byte type)
		{
			super(id);
			this.type=type;
		}
		
		public byte getType()
		{
			return type;
		}
	}

	private static class YLineID extends IntegerUserData
	{
		public YLineID(int id)
		{
			super(id);
		}		
	}	
	
	private static class ZLineID extends IntegerUserData
	{
		public ZLineID(int id)
		{
			super(id);
		}		
	}
	
	protected FDProvider provider;
	protected BranchGroup parentBranchGroup=new BranchGroup();
	private Shape3D selectedPlates;
	private Shape3D selectedWires, selectedJunctions;
	private SelectionManager selectionManager;
	private Map domainToBranchGroup=new HashMap();
	private Collection plates=new ArrayList();
	private Collection wires=new ArrayList();
	private Collection slots=new ArrayList();
	private Collection solids=new ArrayList();
	private Collection marks=new ArrayList();
	
	private boolean platePickable;
	private boolean wirePickable;
	private boolean slotPickable;
	private boolean markPickable;

	private boolean cellPicking;
	private CellManager cellManager;
	
	/**
	 * 
	 */
	public ViewableFD(FDProvider provider)
	{
		this.provider=provider;
		selectionManager=new SelectionManager(provider);
		cellManager=new CellManager(provider);
		parentBranchGroup.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		parentBranchGroup.setCapability(Group.ALLOW_CHILDREN_READ);
		parentBranchGroup.setCapability(Group.ALLOW_CHILDREN_WRITE);
		
		selectedPlates=new Shape3D();
		selectedPlates.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
		selectedPlates.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		Appearance a=new Appearance();
		a.setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.FASTEST, 0.5f));
		a.setPolygonAttributes(SELECT_POLYGON_ATTRIBUTE);
		selectedPlates.setPickable(false);
		selectedPlates.setAppearance(a);
		parentBranchGroup.addChild(selectedPlates);
		
		selectedWires=new Shape3D();
		selectedWires.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
		selectedWires.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		a=new Appearance();
		a.setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.FASTEST, 0.5f));
		a.setLineAttributes(new LineAttributes(3, LineAttributes.PATTERN_SOLID, false));
		selectedWires.setPickable(false);
		selectedWires.setAppearance(a);
		parentBranchGroup.addChild(selectedWires);		

		selectedJunctions=new Shape3D();
		selectedJunctions.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
		selectedJunctions.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		a=new Appearance();		
		a.setPointAttributes(new PointAttributes(4, false));
		a.setColoringAttributes(new ColoringAttributes(
			new Color3f(Color.RED), ColoringAttributes.FASTEST));
		selectedJunctions.setPickable(false);
		selectedJunctions.setAppearance(a);
		parentBranchGroup.addChild(selectedJunctions);		

		
		domainsChanged(provider.getDomainIDs());
	}

	private void createPlates(Integer iDomainId, BranchGroup platesBg, FDDomain domain)
	{
		float[] coordinates=createQuadArray(domain);
		if(coordinates.length<3)
			return;
		QuadArray qa=new QuadArray(coordinates.length/3, GeometryArray.COORDINATES);
		qa.setCoordinates(0, coordinates);			
		coordinates=null;
		GeometryInfo gi=new GeometryInfo(qa);
		//new NormalGenerator().generateNormals(gi);
		Geometry geom=gi.getIndexedGeometryArray();
		geom.setCapability(GeometryArray.ALLOW_COORDINATE_READ);
		geom.setCapability(GeometryArray.ALLOW_COUNT_READ);
		geom.setCapability(GeometryArray.ALLOW_FORMAT_READ);
		geom.setCapability(IndexedGeometryArray.ALLOW_COORDINATE_INDEX_READ);							
		geom.setCapabilityIsFrequent(GeometryArray.ALLOW_COORDINATE_READ);
		geom.setCapabilityIsFrequent(GeometryArray.ALLOW_COUNT_READ);
		geom.setCapabilityIsFrequent(GeometryArray.ALLOW_FORMAT_READ);
		geom.setCapabilityIsFrequent(IndexedGeometryArray.ALLOW_COORDINATE_INDEX_READ);					

		geom.setUserData(iDomainId);
		Appearance a=new Appearance();
		ColoringAttributes ca=new ColoringAttributes(new Color3f(domain.getColor()),
			ColoringAttributes.SHADE_FLAT);
		a.setColoringAttributes(ca);
		/*Material m=new Material();
		m.setAmbientColor(new Color3f(domain.getColor()));
		m.setDiffuseColor(new Color3f(domain.getColor()));
		m.setEmissiveColor(new Color3f(Color.WHITE));
		a.setMaterial(m);*/			
		a.setPolygonAttributes(PLATE_POLYGON_ATTRIBUTE);
		Shape3D s3d=new Shape3D(geom, a);
		s3d.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		s3d.setCapability(Node.ALLOW_PICKABLE_WRITE);
		s3d.setUserData(PLATE_IDENTIFIER);
		plates.add(s3d);
		platesBg.addChild(s3d);
		
		a=new Appearance();
		ca=new ColoringAttributes(new Color3f(domain.getColor().darker()),
			ColoringAttributes.SHADE_FLAT);
		a.setColoringAttributes(ca);
		a.setPolygonAttributes(BORDER_POLYGON_ATTRIBUTE);
		s3d=new Shape3D(geom, a);
		s3d.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		s3d.setPickable(false);
		Logger.global.finest("Plates bounds="+s3d.getBounds());
		platesBg.addChild(s3d);
	}
	
	private void createWires(Integer iDomainId, BranchGroup platesBg, FDDomain domain)
	{
		LineArray[] las=createLineArray(domain);
		if(las.length==0)
			return;
		Shape3D s3d=new Shape3D(las[0]);
		s3d.setBoundsAutoCompute(false); //Auto compute for LineArray seems buggy
		BoundingBox bb=computeBounds(las[0]);
		for(int i=1; i<las.length; i++)
		{
			s3d.addGeometry(las[i]);			
			bb.combine(computeBounds(las[i]));
		}
		s3d.setBounds(bb);
		Appearance a=new Appearance();
		ColoringAttributes ca=new ColoringAttributes(new Color3f(domain.getColor()),
			ColoringAttributes.SHADE_FLAT);
		a.setColoringAttributes(ca);
		s3d.setAppearance(a);
		s3d.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		s3d.setCapability(Node.ALLOW_PICKABLE_WRITE);
		s3d.setUserData(new WireDomainID(iDomainId.intValue()));
		Logger.global.finest("Wires bounds="+s3d.getBounds());
		platesBg.addChild(s3d);
		wires.add(s3d);
	}
	
	private void createSlots(Integer iDomainId, BranchGroup platesBg, FDDomain domain)
	{
		LineArray[] las=createSlotArray(domain);
		if(las.length==0)
			return;
		Shape3D s3d=new Shape3D(las[0]);
		s3d.setBoundsAutoCompute(false); //Auto compute for LineArray seems buggy
		BoundingBox bb=computeBounds(las[0]);
		for(int i=1; i<las.length; i++)
		{
			s3d.addGeometry(las[i]);			
			bb.combine(computeBounds(las[i]));
		}
		s3d.setBounds(bb);
		Appearance a=new Appearance();
		ColoringAttributes ca=new ColoringAttributes(new Color3f(domain.getColor()),
			ColoringAttributes.SHADE_FLAT);
		a.setColoringAttributes(ca);
		a.setLineAttributes(SLOT_ATTRIBUTE);
		s3d.setAppearance(a);
		s3d.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		s3d.setCapability(Node.ALLOW_PICKABLE_WRITE);
		s3d.setUserData(new SlotDomainID(iDomainId.intValue()));
		s3d.setCapability(Node.ALLOW_PICKABLE_READ);
		platesBg.addChild(s3d);
		slots.add(s3d);
	}
	
	/**
	 * 
	 * @param cells Input array: {i_min, j_min, k_min, i_max, j_max, k_max}
	 * @param index the index where to put the cube in the output coordinates arrays
	 * @param indexi the index where to put the cube in the output indices arrays
	 * @param indices indices output array
	 * @param coordinates coordinates output array
	 */
	private void createSolidCube(int[] cells, int indexi, int index, int[] indices, float[] coordinates)
	{
		//	    5/-----------/6 
		//	    /           /|
		//	   /           / |              z
		//	 0/-----------/  |            /
		//	  |          1|  |           /
		//	  |           |  |7         -----> x
		//	  |           |  /          |
		//	  |           | /           |
		//	  |           |/            y
		//	3 /-----------/ 2 
		
		int ic=index*3;
		int ii=indexi*4;
		coordinates[ic]=(float) provider.getXGrid(cells[0]); //X0
		coordinates[ic+1]=(float) provider.getYGrid(cells[1]); //Y0
		coordinates[ic+2]=(float) provider.getZGrid(cells[2]); //Z0
		
		coordinates[ic+3]=(float) provider.getXGrid(cells[0]+1); //X1
		coordinates[ic+4]=coordinates[ic+1]; //Y1
		coordinates[ic+5]=coordinates[ic+2]; //Z1

		coordinates[ic+6]=coordinates[ic+3]; //X2
		coordinates[ic+7]=(float) provider.getYGrid(cells[1]+1); //Y2
		coordinates[ic+8]=coordinates[ic+2]; //Z2

		coordinates[ic+9]=coordinates[ic]; //X3
		coordinates[ic+10]=coordinates[ic+7]; //Y3
		coordinates[ic+11]=coordinates[ic+2]; //Z3

		coordinates[ic+12]=coordinates[ic]; //X4
		coordinates[ic+13]=coordinates[ic+7]; //Y4
		coordinates[ic+14]=(float) provider.getZGrid(cells[2]+1); //Z4

		coordinates[ic+15]=coordinates[ic]; //X5
		coordinates[ic+16]=coordinates[ic+1]; //Y5
		coordinates[ic+17]=coordinates[ic+14]; //Z5
		
		coordinates[ic+18]=coordinates[ic+3]; //X6
		coordinates[ic+19]=coordinates[ic+1]; //Y6
		coordinates[ic+20]=coordinates[ic+14]; //Z6

		coordinates[ic+21]=coordinates[ic+3]; //X7
		coordinates[ic+22]=coordinates[ic+7]; //Y7
		coordinates[ic+23]=coordinates[ic+14]; //Z7
		
		//0123 face
		indices[ii]=index;
		indices[ii+1]=index+1;
		indices[ii+2]=index+2;
		indices[ii+3]=index+3;
		
		//0543
		indices[ii+4]=index;
		indices[ii+5]=index+5;
		indices[ii+6]=index+4;
		indices[ii+7]=index+3;
		
		//0561
		indices[ii+8]=index;
		indices[ii+9]=index+5;
		indices[ii+10]=index+6;
		indices[ii+11]=index+1;
		
		//3274
		indices[ii+12]=index+3;
		indices[ii+13]=index+2;
		indices[ii+14]=index+7;
		indices[ii+15]=index+4;		

		//2761
		indices[ii+16]=index+2;
		indices[ii+17]=index+7;
		indices[ii+18]=index+6;
		indices[ii+19]=index+1;		

		//4765
		indices[ii+20]=index+4;
		indices[ii+21]=index+7;
		indices[ii+22]=index+6;
		indices[ii+23]=index+5;		
	}

	private interface CoordinatesComputer
	{
		void compute(int[] indices, float[] out);
	}
	
	private class CoordinatesComputerXZ implements CoordinatesComputer
	{
		public void compute(int[] wire, float[] coordinates)
		{
			coordinates[0]=(float) provider.getXGrid(wire[0]);			
			coordinates[1]=(float) (provider.getYGrid(wire[2])+provider.getYGrid(wire[2]+1))/2;
			coordinates[2]=(float) provider.getZGrid(wire[3]);
			coordinates[3]=(float) provider.getXGrid(wire[1]);
			coordinates[4]=coordinates[1];
			coordinates[5]=coordinates[2];
		}
	}
	
	private class CoordinatesComputerXY implements CoordinatesComputer
	{
		public void compute(int[] wire, float[] coordinates)
		{
			coordinates[0]=(float) provider.getXGrid(wire[0]);
			coordinates[1]=(float) provider.getYGrid(wire[2]);
			coordinates[2]=(float) (provider.getZGrid(wire[3])+provider.getZGrid(wire[3]+1))/2;			
			coordinates[3]=(float) provider.getXGrid(wire[1]);
			coordinates[4]=coordinates[1];
			coordinates[5]=coordinates[2];
		}
	}
	
	private class CoordinatesComputerYZ implements CoordinatesComputer
	{
		public void compute(int[] wire, float[] coordinates)
		{
			coordinates[0]=(float) (provider.getXGrid(wire[2])+provider.getXGrid(wire[2]+1))/2;			
			coordinates[1]=(float) provider.getYGrid(wire[0]);
			coordinates[2]=(float) provider.getZGrid(wire[3]);
			coordinates[3]=coordinates[0];
			coordinates[4]=(float) provider.getYGrid(wire[1]);
			coordinates[5]=coordinates[2];
		}
	}
	
	private class CoordinatesComputerYX implements CoordinatesComputer
	{
		public void compute(int[] wire, float[] coordinates)
		{
			coordinates[0]=(float) provider.getXGrid(wire[2]);
			coordinates[1]=(float) provider.getYGrid(wire[0]);
			coordinates[2]=(float) (provider.getZGrid(wire[3])+provider.getZGrid(wire[3]+1))/2;			
			coordinates[3]=coordinates[0];
			coordinates[4]=(float) provider.getYGrid(wire[1]);
			coordinates[5]=coordinates[2];
		}
	}
	
	private class CoordinatesComputerZY implements CoordinatesComputer
	{
		public void compute(int[] wire, float[] coordinates)
		{
			coordinates[0]=(float) (provider.getXGrid(wire[2])+provider.getXGrid(wire[2]+1))/2;			
			coordinates[1]=(float) provider.getYGrid(wire[3]);
			coordinates[2]=(float) provider.getZGrid(wire[0]);
			coordinates[3]=coordinates[0];
			coordinates[4]=coordinates[1];
			coordinates[5]=(float) provider.getZGrid(wire[1]);
		}
	}
	
	private class CoordinatesComputerZX implements CoordinatesComputer
	{
		public void compute(int[] wire, float[] coordinates)
		{
			coordinates[0]=(float) provider.getXGrid(wire[2]);
			coordinates[1]=(float) (provider.getYGrid(wire[3])+provider.getYGrid(wire[3]+1))/2;
			coordinates[2]=(float) provider.getZGrid(wire[0]);
			coordinates[3]=coordinates[0];
			coordinates[4]=coordinates[1];
			coordinates[5]=(float) provider.getZGrid(wire[1]);
		}
	}	

	/** 
	 * @param domain
	 * @param toReturn a collection to add LineArray
	 * @param type FDDomain.XZ_SLOT
	 * @param coordinatesComputer
	 */
	private void createSlotArray(
		FDDomain domain,
		Collection toReturn,
		byte type,
		CoordinatesComputer coordinatesComputer)
	{
		int k=0;
		Iterator it=domain.getSlotIterator(type);
		while(it.hasNext())
		{
			Object o=it.next();
			float[] coordinates;
			if(o instanceof int[])
			{
				int[] wire=(int[])o;
				coordinates=new float[6];
				coordinatesComputer.compute(wire, coordinates);
			}
			else
			{
				coordinates=(float[]) o;
			}
			//printFloatDebug(coordinates);
			LineArray la=new LineArray(6, GeometryArray.COORDINATES);
			setGeometryCapabilities(la);
			la.setCoordinates(0, coordinates);
			la.setUserData(new SlotID(k, type));
			toReturn.add(la);
			k++;
		}
	}
	
	/**
	 * @param domain
	 * @return
	 */
	private LineArray[] createSlotArray(FDDomain domain)
	{		
		Collection toReturn=new ArrayList();
		createSlotArray(domain, toReturn, FDDomain.XZ_SLOT, new CoordinatesComputerXZ());
		createSlotArray(domain, toReturn, FDDomain.XY_SLOT, new CoordinatesComputerXY());
		createSlotArray(domain, toReturn, FDDomain.YZ_SLOT, new CoordinatesComputerYZ());
		createSlotArray(domain, toReturn, FDDomain.YX_SLOT, new CoordinatesComputerYX());
		createSlotArray(domain, toReturn, FDDomain.ZY_SLOT, new CoordinatesComputerZY());
		createSlotArray(domain, toReturn, FDDomain.ZX_SLOT, new CoordinatesComputerZX());		
		return (LineArray[]) toReturn.toArray(new LineArray[toReturn.size()]);
	}

	private static BoundingBox computeBounds(GeometryArray array)
	{
		float[] coords=new float[array.getVertexCount()];
		array.getCoordinates(0, coords);
		float xmin=Float.POSITIVE_INFINITY;
		float ymin=Float.POSITIVE_INFINITY;
		float zmin=Float.POSITIVE_INFINITY;
		float xmax=Float.NEGATIVE_INFINITY;
		float ymax=Float.NEGATIVE_INFINITY;
		float zmax=Float.NEGATIVE_INFINITY;
		for(int i=0; i<coords.length;)
		{
			if(coords[i]<xmin)
				xmin=coords[i];
			if(coords[i]>xmax)
				xmax=coords[i];

			i++;
			if(coords[i]<ymin)
				ymin=coords[i];
			if(coords[i]>ymax)
				ymax=coords[i];

			i++;
			if(coords[i]<zmin)
				zmin=coords[i];
			if(coords[i]>zmax)
				zmax=coords[i];

			i++;
		}
		BoundingBox toReturn=new BoundingBox(
			new Point3d(xmin, ymin, zmin),
			new Point3d(xmax, ymax, zmax));

		return toReturn;
	}
	/* (non-Javadoc)
	 * @see jcae.viewer3d.Viewable#domainsChanged(java.util.Collection)
	 */
	synchronized public void domainsChangedPerform(int[] domainId)
	{			
		Set ids=new HashSet(Utils.intArrayToCollection(provider.getDomainIDs()));
		for(int d=0; d<domainId.length; d++)
		{	
			Integer iDomainId=new Integer(domainId[d]);
			
			Logger.global.finest("Create BranchGroup for domain n°"+
				Integer.toHexString(domainId[d])+ "provider="+provider+" ids="+ids);
			
			BranchGroup old=(BranchGroup)domainToBranchGroup.get(iDomainId);
			if(old!=null)
			{
				old.detach();
				domainToBranchGroup.remove(iDomainId);
				plates.removeAll(Collections.list(old.getAllChildren()));
			}
			if(!ids.contains(iDomainId))
				continue;
			BranchGroup domainBg=new BranchGroup();
			domainBg.setCapability(BranchGroup.ALLOW_DETACH);
			domainBg.setCapability(Group.ALLOW_CHILDREN_READ);
			FDDomain domain=(FDDomain)provider.getDomain(domainId[d]);
			createPlates(iDomainId, domainBg, domain);
			createWires(iDomainId, domainBg, domain);
			createSlots(iDomainId, domainBg, domain);
			createSolids(iDomainId, domainBg, domain);
			Node m=MarkUtils.createMarkNode(domain, iDomainId.intValue());
			marks.add(m);
			domainBg.addChild(m);				
				
			parentBranchGroup.addChild(domainBg);
			domainToBranchGroup.put(iDomainId, domainBg);
		}		
		unselectAll();
		setPlatePicking(platePickable);
		setWirePicking(wirePickable);
		setSlotPicking(slotPickable);
		setMarkPicking(markPickable);
	}

	/**
	 * @param domainId
	 * @param domainBg
	 * @param domain
	 */
	private void createSolids(Integer domainId, BranchGroup domainBg, FDDomain domain)
	{
		Iterator it=domain.getSolidIterator();
		int[] indices=new int[domain.getNumberOfSolid()*24];
		if(indices.length==0)
			return;
		float[] coordinates=new float[domain.getNumberOfSolid()*24];
		int index=0, indexi=0;
		while(it.hasNext())
		{
			int[] array=(int[])it.next();
			createSolidCube(array, indexi, index, indices, coordinates);			
			index+=8; //8 nodes in a cube
			indexi+=6; //6 faces in a cube
		}
			
		GeometryInfo gi=new GeometryInfo(GeometryInfo.QUAD_ARRAY);		
		gi.setCoordinateIndices(indices);
		gi.setCoordinates(coordinates);
		//gi.recomputeIndices();
		
		//new NormalGenerator().generateNormals(gi);
		Geometry geom=gi.getIndexedGeometryArray();
		geom.setCapability(GeometryArray.ALLOW_COORDINATE_READ);
		geom.setCapability(GeometryArray.ALLOW_COUNT_READ);
		geom.setCapability(GeometryArray.ALLOW_FORMAT_READ);
		geom.setCapability(IndexedGeometryArray.ALLOW_COORDINATE_INDEX_READ);							
		geom.setCapabilityIsFrequent(GeometryArray.ALLOW_COORDINATE_READ);
		geom.setCapabilityIsFrequent(GeometryArray.ALLOW_COUNT_READ);
		geom.setCapabilityIsFrequent(GeometryArray.ALLOW_FORMAT_READ);
		geom.setCapabilityIsFrequent(IndexedGeometryArray.ALLOW_COORDINATE_INDEX_READ);					

		geom.setUserData(domainId);
		Appearance a=new Appearance();
		ColoringAttributes ca=new ColoringAttributes(new Color3f(domain.getColor()),
			ColoringAttributes.SHADE_FLAT);
		a.setColoringAttributes(ca);
		/*Material m=new Material();
		m.setAmbientColor(new Color3f(domain.getColor()));
		m.setDiffuseColor(new Color3f(domain.getColor()));
		m.setEmissiveColor(new Color3f(Color.WHITE));
		a.setMaterial(m);*/			
		a.setPolygonAttributes(PLATE_POLYGON_ATTRIBUTE);
		Shape3D s3d=new Shape3D(geom, a);
		s3d.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		s3d.setCapability(Node.ALLOW_PICKABLE_WRITE);
		s3d.setUserData(SOLID_IDENTIFIER);
		solids.add(s3d);
		domainBg.addChild(s3d);
		
		a=new Appearance();
		ca=new ColoringAttributes(new Color3f(domain.getColor().darker()),
			ColoringAttributes.SHADE_FLAT);
		a.setColoringAttributes(ca);
		a.setPolygonAttributes(BORDER_POLYGON_ATTRIBUTE);
		s3d=new Shape3D(geom, a);
		s3d.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		s3d.setPickable(false);
		domainBg.addChild(s3d);		
	}

	/**
	 * @param markPickable2
	 */
	public void setMarkPicking(boolean enable)
	{
		Logger.global.finest("enable="+enable);
		markPickable=enable;
		Iterator it=marks.iterator();
		while(it.hasNext())
		{
			Node n=(Node) it.next();
			MarkUtils.setPickable(n, enable);			
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
	
	public void setDomainPickable(int domainID, boolean enable)
	{
		Logger.global.finest("domainID="+Integer.toHexString(domainID)
			+" enable="+enable);
		BranchGroup bg=(BranchGroup) domainToBranchGroup.get(new Integer(domainID));
		if(bg==null)
		{
			Logger.global.info("domain 0x"+Integer.toHexString(domainID)+
				" is empty. Cannot change pickable status");
			return;
		}
			
		Enumeration e=bg.getAllChildren();
		while(e.hasMoreElements())
		{
			Node n=(Node) e.nextElement();			
			Logger.global.finest(""+n.getUserData());
			if(n.getUserData()==MarkUtils.MARK_IDENTIFIER)
			{
				Logger.global.finest("MarkUtils.setPickable("+n+","+enable);
				MarkUtils.setPickable(n, enable);
			}
			else if(n.getUserData()!=null)
			{
				Logger.global.finest("setPickable("+n+","+enable);
				n.setPickable(enable);
			}				
		}
	}
	
	public void setPlatePicking(boolean enable)
	{
		Logger.global.finest("enable="+enable);
		platePickable=enable;
		Iterator it=plates.iterator();
		while(it.hasNext())
		{
			Node n=(Node) it.next();
			n.setPickable(enable);
		}
	}

	public void setWirePicking(boolean enable)
	{
		Logger.global.finest("enable="+enable);
		wirePickable=enable;
		Iterator it=wires.iterator();
		while(it.hasNext())
		{
			Node n=(Node) it.next();
			n.setPickable(enable);
		}
	}
	
	public void setSlotPicking(boolean enable)
	{
		Logger.global.finest("enable="+enable);
		slotPickable=enable;
		Iterator it=slots.iterator();
		while(it.hasNext())
		{
			Node n=(Node) it.next();			
			n.setPickable(enable);
		}
	}

	
	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.Viewable#pick(com.sun.j3d.utils.picking.PickResult)
	 */
	public void pick(PickViewable result, boolean selected)
	{				
		if(result==null)
			return;
		
		if(result.getObject().getUserData()==PLATE_IDENTIFIER)
		{
			pickPlate(result, selected);
		}
		if(result.getObject().getUserData()==SOLID_IDENTIFIER)
		{
			pickSolid(result, selected);
		}
		else if(result.getObject().getUserData() instanceof WireDomainID)
		{
			if(cellPicking)
				pickWireCell(result, selected);
			else
				pickWire(result, selected);
		}
		else if(result.getObject().getUserData() instanceof SlotDomainID)
		{
			if(cellPicking)
				pickSlotCell(result, selected);
			else
				pickSlot(result, selected);
		}
		else if(result.getObject().getUserData() instanceof MarkUtils.MarkID)
		{
			pickJunction(result, selected);
		}
		fireSelectionChanged();
	}

	private void pickSlotCell(PickViewable result, boolean selected)
	{
		int domainId=((SlotDomainID)result.getObject().getUserData()).getValue();
		PickIntersection pi = result.getIntersection();
		SlotID o=(SlotID)pi.getGeometryArray().getUserData();
		
		Point3d[] cds=pi.getPrimitiveCoordinates();
		Point3d point=pi.getPointCoordinates();
		LineArray la=null;
		if(selected)
		{						
			la=cellManager.selectSlot(o.getType(), domainId, o.getValue(), cds, point);
			selectedWires.addGeometry(la);
		}
		else
		{
			la=cellManager.unselectSlot(o.getType(), domainId, o.getValue(), cds, point);
			selectedWires.removeGeometry(la);
		}
	}

	private void pickWireCell(PickViewable result, boolean selected)
	{
		int domainId=((WireDomainID)result.getObject().getUserData()).getValue();
		PickIntersection pi = result.getIntersection();
		IntegerUserData o=(IntegerUserData)pi.getGeometryArray().getUserData();

		Point3d[] cds=pi.getPrimitiveCoordinates();
		Point3d point=pi.getPointCoordinates();
		Geometry la=null;
		
		if(selected)
		{			
			if(o instanceof XLineID)
			{				
				la=cellManager.selectXWire(domainId, o.getValue(), cds, point);
			}
			else if(o instanceof YLineID)
			{
				la=cellManager.selectYWire(domainId, o.getValue(), cds, point);
			}
			else if(o instanceof ZLineID)
			{
				la=cellManager.selectZWire(domainId, o.getValue(), cds, point);
			}			
			selectedWires.addGeometry(la);

		}
		else
		{
			if(o instanceof XLineID)
			{
				la=cellManager.unselectXWire(domainId, o.getValue(), cds, point);
			}
			else if(o instanceof YLineID)
			{
				la=cellManager.unselectYWire(domainId, o.getValue(), cds, point);
			}
			else if(o instanceof ZLineID)
			{
				la=cellManager.unselectZWire(domainId, o.getValue(), cds, point);
			}
			selectedWires.removeGeometry(la);
		}
	}

	private void pickJunction(PickViewable result, boolean selected)
	{
		int domainId=((MarkUtils.MarkID)result.getObject().getUserData()).getDomainID();
		PickIntersection pi = result.getIntersection();
		int typeId=((MarkUtils.MarkID)result.getObject().getUserData()).getTypeID();

		int[] idx = pi.getPrimitiveVertexIndices();
		int markID=idx[0];		
		
		if(selected)
		{
			Point3d[] cds=pi.getPrimitiveCoordinates();
			PointArray pa=new PointArray(cds.length, GeometryArray.COORDINATES);
			pa.setCoordinates(0, cds);
			selectedJunctions.addGeometry(pa);
			selectionManager.selectMark(domainId, typeId, markID, pa);
		}
		else
		{
			selectedJunctions.removeGeometry(
				selectionManager.getGeometryForMark(domainId, typeId, markID));
			selectionManager.unselect(domainId, typeId, markID);
		}
	}
	
	private void pickWire(PickViewable result, boolean selected)
	{
		int domainId=((WireDomainID)result.getObject().getUserData()).getValue();
		PickIntersection pi = result.getIntersection();
		IntegerUserData o=(IntegerUserData)pi.getGeometryArray().getUserData();
		if(selected)
		{
			Point3d[] cds=pi.getPrimitiveCoordinates();
			LineArray la=new LineArray(cds.length, GeometryArray.COORDINATES);
			la.setCoordinates(0, cds);
			selectedWires.addGeometry(la);
			if(o instanceof XLineID)
			{				
				selectionManager.selectXWire(domainId, o.getValue(), la);
			}
			else if(o instanceof YLineID)
			{
				selectionManager.selectYWire(domainId, o.getValue(), la);
			}
			else if(o instanceof ZLineID)
			{
				selectionManager.selectZWire(domainId, o.getValue(), la);
			}
		}
		else
		{
			Geometry toRemove=null;
			if(o instanceof XLineID)
			{
				toRemove=selectionManager.getGeometryForXWire(
					domainId, o.getValue());
				selectionManager.unselectXWire(domainId, o.getValue());
			}
			else if(o instanceof YLineID)
			{
				toRemove=selectionManager.getGeometryForYWire(
					domainId, o.getValue());
				selectionManager.unselectYWire(domainId, o.getValue());
			}
			else if(o instanceof ZLineID)
			{
				toRemove=selectionManager.getGeometryForZWire(
					domainId, o.getValue());
				selectionManager.unselectZWire(domainId, o.getValue());
			}
			selectedWires.removeGeometry(toRemove);
		}
	}
	
	private void pickSlot(PickViewable result, boolean selected)
	{		
		int domainId=((SlotDomainID)result.getObject().getUserData()).getValue();
		PickIntersection pi = result.getIntersection();
		SlotID o=(SlotID)pi.getGeometryArray().getUserData();
		if(selected)
		{
			Point3d[] cds=pi.getPrimitiveCoordinates();
			LineArray la=new LineArray(cds.length, GeometryArray.COORDINATES);
			la.setCoordinates(0, cds);
			selectedWires.addGeometry(la);
			selectionManager.selectSlot(o.getType(), domainId, o.getValue(), la);
		}
		else
		{
			Geometry toRemove=selectionManager.getGeometryForSlot(
				o.getType(), domainId, o.getValue());
			selectionManager.unselectSlot(o.getType(), domainId, o.getValue());
			selectedWires.removeGeometry(toRemove);
		}
	}
	
	private void pickPlate(PickViewable result, boolean selected)
	{		
		PickIntersection pi = result.getIntersection();

		// indices of the picked quad
		// Indices are set to vertex indices, as this is not an Index
		// Geometry object
		// => easy to find the plate index from this
		int[] idx = pi.getPrimitiveVertexIndices();
		int plateID=idx[0]/4;
		int domainID=((Integer)pi.getGeometryArray().getUserData()).intValue();

		if(selected)
		{
			Point3d[] pc=pi.getPrimitiveCoordinates();
			Logger.global.finest("domainID: "+domainID);		
			Logger.global.finest("coordinates: "+Arrays.asList(pc));
			
			QuadArray qa=new QuadArray(4, GeometryArray.COORDINATES);
			qa.setCoordinates(0, pi.getPrimitiveCoordinates());
			selectedPlates.addGeometry(qa);
			selectionManager.selectPlate(plateID, domainID, qa);
		}
		else
		{
			selectedPlates.removeGeometry(
				selectionManager.getGeometryForPlate(domainID, plateID));
			selectionManager.unselectPlate(plateID, domainID);
		}
	}
	
	private void pickSolid(PickViewable result, boolean selected)
	{		
		PickIntersection pi = result.getIntersection();

		// indices of the picked quad
		// Indices are set to vertex indices, as this is not an Index
		// Geometry object
		// => easy to find the plate index from this
		int[] idx = pi.getPrimitiveVertexIndices();
		int solidID=idx[0]/24;
		int domainID=((Integer)pi.getGeometryArray().getUserData()).intValue();
		if(selected)
		{
			Logger.global.finest("cellid= "+idx[0]/24);
			Logger.global.finest("domainID: "+domainID);					
			IndexedGeometryArray iga=(IndexedGeometryArray)pi.getGeometryArray();
			int[] indices=new int[24];
			iga.getCoordinateIndices(solidID*24, indices);
			float[] coordinates=new float[24*3];
			Point3f p=new Point3f();
			for(int i=0, j=0; i<indices.length; i++)
			{
				iga.getCoordinate(indices[i], p);
				coordinates[j++]=p.x;
				coordinates[j++]=p.y;
				coordinates[j++]=p.z;
			}
			QuadArray qa=new QuadArray(24, GeometryArray.COORDINATES);
			qa.setCoordinates(0, coordinates);
			selectedPlates.addGeometry(qa);
			selectionManager.selectSolid(solidID, domainID, qa);
		}
		else
		{
			selectedPlates.removeGeometry(
				selectionManager.getGeometryForSolid(domainID, solidID));
			selectionManager.unselectSolid(solidID, domainID);
		}
	}	

	
	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.Viewable#getBranchGroup(org.jcae.viewer3d.View)
	 */
	public Node getJ3DNode()
	{		
		return parentBranchGroup;
	}

	/* (non-Javadoc)
	 * @see org.jcae.viewer3d.Viewable#unselectAll()
	 */
	public void unselectAll()
	{
		selectionManager.unselectAll();
		selectedPlates.removeAllGeometries();
		selectedWires.removeAllGeometries();
		selectedJunctions.removeAllGeometries();
		cellManager.unselectAll();
		fireSelectionChanged();
	}
		
	private LineArray[] createLineArray(FDDomain domain)
	{
		Iterator it=domain.getXWireIterator();
		Collection toReturn=new ArrayList();
		int k=0;
		while(it.hasNext())
		{
			int[] wire=(int[])it.next();
			float[] coordinates=new float[6];
			coordinates[0]=(float) provider.getXGrid(wire[0]);
			coordinates[1]=(float) provider.getYGrid(wire[1]);
			coordinates[2]=(float) provider.getZGrid(wire[2]);
			coordinates[3]=(float) provider.getXGrid(wire[3]);
			coordinates[4]=coordinates[1];
			coordinates[5]=coordinates[2];
			//printFloatDebug(coordinates);
			LineArray la=new LineArray(6, GeometryArray.COORDINATES);
			setGeometryCapabilities(la);
			la.setCoordinates(0, coordinates);
			la.setUserData(new XLineID(k));
			toReturn.add(la);
			k++;
		}
		
		k=0;
		it=domain.getYWireIterator();		
		while(it.hasNext())
		{			
			int[] wire=(int[])it.next();
			float[] coordinates=new float[6];
			coordinates[0]=(float) provider.getXGrid(wire[0]);
			coordinates[1]=(float) provider.getYGrid(wire[1]);
			coordinates[2]=(float) provider.getZGrid(wire[2]);
			coordinates[3]=coordinates[0];
			coordinates[4]=(float) provider.getYGrid(wire[3]);
			coordinates[5]=coordinates[2];
			//printFloatDebug(coordinates);
			LineArray la=new LineArray(6, GeometryArray.COORDINATES);
			setGeometryCapabilities(la);
			la.setCoordinates(0, coordinates);
			la.setUserData(new YLineID(k));
			toReturn.add(la);
			k++;
		}
		
		k=0;
		it=domain.getZWireIterator();		
		while(it.hasNext())
		{
			int[] wire=(int[])it.next();
			float[] coordinates=new float[6];
			coordinates[0]=(float) provider.getXGrid(wire[0]);
			coordinates[1]=(float) provider.getYGrid(wire[1]);
			coordinates[2]=(float) provider.getZGrid(wire[2]);
			coordinates[3]=coordinates[0];
			coordinates[4]=coordinates[1];
			coordinates[5]=(float) provider.getZGrid(wire[3]);
			//printFloatDebug(coordinates);
			LineArray la=new LineArray(6, GeometryArray.COORDINATES);
			setGeometryCapabilities(la);
			la.setCoordinates(0, coordinates);
			la.setUserData(new ZLineID(k));
			toReturn.add(la);
			k++;
		}
		
		return (LineArray[]) toReturn.toArray(new LineArray[toReturn.size()]);
	}

	private static void printFloatDebug(float[] f)
	{
		for(int i=0; i<f.length; i++)
		{
			System.out.print(f[i]+", ");
		}
		System.out.println();
	}
	
	private static void setGeometryCapabilities(Geometry geom)
	{
		geom.setCapability(GeometryArray.ALLOW_COORDINATE_READ);
		geom.setCapability(GeometryArray.ALLOW_COUNT_READ);
		geom.setCapability(GeometryArray.ALLOW_FORMAT_READ);
		geom.setCapability(IndexedGeometryArray.ALLOW_COORDINATE_INDEX_READ);							
		geom.setCapabilityIsFrequent(GeometryArray.ALLOW_COORDINATE_READ);
		geom.setCapabilityIsFrequent(GeometryArray.ALLOW_COUNT_READ);
		geom.setCapabilityIsFrequent(GeometryArray.ALLOW_FORMAT_READ);
		geom.setCapabilityIsFrequent(IndexedGeometryArray.ALLOW_COORDINATE_INDEX_READ);	
	}
	
	private float[] createQuadArray(FDDomain domain)
	{
		int numberOfPlates=domain.getNumberOfXPlate()+domain.getNumberOfYPlate()+domain.getNumberOfZPlate();
		float[] coordinates=new float[numberOfPlates*12];
		Iterator it=domain.getXPlateIterator();
		int k=0;
		while(it.hasNext())
		{
			int[] plate=(int[])it.next();
			float x=(float)provider.getXGrid(plate[0]);
			float y1=(float)provider.getYGrid(plate[1]);
			float z1=(float)provider.getZGrid(plate[2]);
			float y2=(float)provider.getYGrid(plate[3]);
			float z2=(float)provider.getZGrid(plate[4]);

			coordinates[k++]=x;
			coordinates[k++]=y1;
			coordinates[k++]=z1;

			coordinates[k++]=x;
			coordinates[k++]=y2;
			coordinates[k++]=z1;

			coordinates[k++]=x;
			coordinates[k++]=y2;
			coordinates[k++]=z2;			
			
			coordinates[k++]=x;
			coordinates[k++]=y1;
			coordinates[k++]=z2;
		}
		
		it=domain.getYPlateIterator();
		while(it.hasNext())
		{
			int[] plate=(int[])it.next();
			float y=(float)provider.getYGrid(plate[0]);
			float x1=(float)provider.getXGrid(plate[1]);
			float z1=(float)provider.getZGrid(plate[2]);
			float x2=(float)provider.getXGrid(plate[3]);
			float z2=(float)provider.getZGrid(plate[4]);
			coordinates[k++]=x1;
			coordinates[k++]=y;
			coordinates[k++]=z1;

			coordinates[k++]=x1;
			coordinates[k++]=y;
			coordinates[k++]=z2;
			
			coordinates[k++]=x2;
			coordinates[k++]=y;
			coordinates[k++]=z2;

			coordinates[k++]=x2;
			coordinates[k++]=y;
			coordinates[k++]=z1;			
		}

		it=domain.getZPlateIterator();
		while(it.hasNext())
		{
			int[] plate=(int[])it.next();
			float z=(float)provider.getZGrid(plate[0]);
			float x1=(float)provider.getXGrid(plate[1]);
			float y1=(float)provider.getYGrid(plate[2]);
			float x2=(float)provider.getXGrid(plate[3]);
			float y2=(float)provider.getYGrid(plate[4]);
			coordinates[k++]=x1;
			coordinates[k++]=y1;
			coordinates[k++]=z;
			
			coordinates[k++]=x1;
			coordinates[k++]=y2;
			coordinates[k++]=z;
			
			coordinates[k++]=x2;
			coordinates[k++]=y2;
			coordinates[k++]=z;
			
			coordinates[k++]=x2;
			coordinates[k++]=y1;
			coordinates[k++]=z;			
		}
		return coordinates;
	}
	
	/** Return the current selection */
	public FDSelection[] getSelection()
	{
		if(cellPicking)
			return cellManager.getSelection();
		else
			return selectionManager.getSelection();
	}

	/**
	 * Tell the viewer to select wire and slot by elementary element
	 */
	public void setCellPicking(boolean b)
	{
		this.cellPicking=b;
	}
}
