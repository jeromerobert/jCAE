package org.jcae.viewer3d.post;

import java.util.Map;
import javax.media.j3d.*;
import org.jcae.viewer3d.DomainProvider;
import org.jcae.viewer3d.PickViewable;
import org.jcae.viewer3d.SelectionListener;
import org.jcae.viewer3d.ViewableAdaptor;
import org.jcae.viewer3d.post.ColorMapper;
import org.jcae.viewer3d.post.DefaultColorMapper;
import com.sun.j3d.utils.picking.PickTool;

public class QuadPostViewable extends ViewableAdaptor
{
	private final static Appearance APPEARANCE;
	static
	{
		APPEARANCE=new Appearance();
		PolygonAttributes pa=new PolygonAttributes(
			PolygonAttributes.POLYGON_FILL,
			PolygonAttributes.CULL_NONE,0);
		APPEARANCE.setPolygonAttributes(pa);
	}
	
	BranchGroup branchGroup;
	private ColorMapper colorMapper;
	private int numberOfVertices;
	private IndexedQuadArray quadArray;
	
	public QuadPostViewable(float[] coordinates, int[] coordinateIndices)
	{
		numberOfVertices=coordinates.length/3;
		System.out.println("coordinates.length="+coordinates.length);
		System.out.println("coordinateIndices.length="+coordinateIndices.length);
		
		branchGroup=new BranchGroup();
		quadArray = new IndexedQuadArray(
					coordinates.length/3,
					GeometryArray.COORDINATES|GeometryArray.COLOR_3,
					coordinateIndices.length);
		quadArray.setCoordinates(0, coordinates);
		quadArray.setCoordinateIndices(0, coordinateIndices);
		quadArray.setCapability(GeometryArray.ALLOW_COLOR_WRITE);
		quadArray.setCapability(IndexedGeometryArray.ALLOW_COLOR_INDEX_WRITE);
		Shape3D s=new Shape3D(quadArray, APPEARANCE);
		PickTool.setCapabilities(s, PickTool.INTERSECT_COORD);
		branchGroup.addChild(s);
	}

	public void setValues(float[] values)
	{
		if(colorMapper==null)
		{
			setColorMapper(new DefaultColorMapper(values, numberOfVertices));		
		}
				
		int[] colorIndices=new int[values.length*4];
		for(int i=0; i<values.length; i++)
		{
			int colorId=colorMapper.map(values[i]);
			int pid=i*4;
			colorIndices[pid++]=colorId;
			colorIndices[pid++]=colorId;
			colorIndices[pid++]=colorId;
			colorIndices[pid]=colorId;			
		}
		quadArray.setColorIndices(0, colorIndices);
	}

	public void setColorMapper(ColorMapper cm)
	{
		colorMapper=cm;
		quadArray.setColors(0, colorMapper.getPalette());
	}
	
	@Override
	public void domainsChangedPerform(int[] domainId)
	{
		//nothing
	}

	@Override
	public DomainProvider getDomainProvider()
	{
		return null;
	}

	@Override
	public void setDomainVisible(Map<Integer, Boolean> map)
	{
		//nothing
	}

	@Override
	public Node getJ3DNode()
	{
		System.out.println(branchGroup.getBounds());
		return branchGroup;
	}

	@Override
	public void pick(PickViewable result)
	{
		//nothing
	}

	@Override
	public void unselectAll()
	{
		//nothing
	}

	@Override
	public void addSelectionListener(SelectionListener listener)
	{
		//nothing
	}

	@Override
	public void removeSelectionListener(SelectionListener listener)
	{
		//nothing
	}
}
