package org.jcae.viewer3d.fd;

import java.util.Map;
import javax.media.j3d.*;
import org.jcae.viewer3d.DomainProvider;
import org.jcae.viewer3d.PickViewable;
import org.jcae.viewer3d.SelectionListener;
import org.jcae.viewer3d.ViewableAdaptor;


public class SimplePlateViewable extends ViewableAdaptor
{
	private final static Appearance APPEARANCE;
	static
	{
		APPEARANCE=new Appearance();
		PolygonAttributes pa=new PolygonAttributes(
			PolygonAttributes.POLYGON_LINE,
			PolygonAttributes.CULL_NONE,0);
		APPEARANCE.setPolygonAttributes(pa);
	}
	
	BranchGroup branchGroup;
	private IndexedQuadArray quadArray;
	
	public SimplePlateViewable(float[] coordinates, int[] coordinateIndices)
	{
		branchGroup=new BranchGroup();
		quadArray = new IndexedQuadArray(
					coordinates.length/3,
					GeometryArray.COORDINATES,
					coordinateIndices.length);
		quadArray.setCoordinates(0, coordinates);
		quadArray.setCoordinateIndices(0, coordinateIndices);
		Shape3D s=new Shape3D(quadArray, APPEARANCE);
		s.setPickable(false);
		branchGroup.addChild(s);
	}
	
	public void domainsChangedPerform(int[] domainId)
	{
		//nothing
	}

	public DomainProvider getDomainProvider()
	{
		return null;
	}

	public void setDomainVisible(Map map)
	{
		//nothing
	}

	public Node getJ3DNode()
	{
		System.out.println(branchGroup.getBounds());
		return branchGroup;
	}

	public void pick(PickViewable result, boolean selected)
	{
		//nothing
	}

	public void unselectAll()
	{
		//nothing
	}

	public void addSelectionListener(SelectionListener listener)
	{
		//nothing
	}

	public void removeSelectionListener(SelectionListener listener)
	{
		//nothing
	}
}
