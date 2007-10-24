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
 * (C) Copyright 2007, by EADS France
 */

package org.jcae.viewer3d.bg;

import java.awt.Component;
import java.util.Enumeration;
import java.util.Map;
import java.util.logging.Logger;
import javax.media.j3d.*;
import javax.swing.JPanel;
import org.jcae.viewer3d.DomainProvider;
import org.jcae.viewer3d.PickViewable;
import org.jcae.viewer3d.SelectionListener;
import org.jcae.viewer3d.ViewableAdaptor;
import com.sun.j3d.utils.picking.PickIntersection;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TIntHashSet;

/**
 * @author Jerome Robert
 * @todo implements all methods
 */
public class ViewableBG extends ViewableAdaptor
{
	private BranchGroup branchGroup;
	private TIntHashSet leaves = new TIntHashSet();
	private TIntObjectHashMap<QuadArray> leavesGeometry = new TIntObjectHashMap<QuadArray>();
	private Shape3D selectedLeaves = new Shape3D();
	private String name;
	
	private final static PolygonAttributes SELECT_POLYGON_ATTRIBUTE=new PolygonAttributes(
		PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE,
		1.0f * Float.parseFloat(System.getProperty(
			"javax.media.j3d.zFactorAbs", "20.0f")), false, Float
			.parseFloat(System.getProperty("javax.media.j3d.zFactorRel",
				"2.0f")));

	public ViewableBG(BranchGroup bg)
	{
		branchGroup = bg;
		
		branchGroup.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		branchGroup.setCapability(Group.ALLOW_CHILDREN_READ);
		branchGroup.setCapability(Group.ALLOW_CHILDREN_WRITE);
		
		selectedLeaves.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
		selectedLeaves.setCapability(Shape3D.ALLOW_GEOMETRY_READ);
		Appearance a = new Appearance();
		a.setTransparencyAttributes(new TransparencyAttributes(TransparencyAttributes.FASTEST, 0.5f));
		a.setPolygonAttributes(SELECT_POLYGON_ATTRIBUTE);
		selectedLeaves.setPickable(false);
		selectedLeaves.setAppearance(a);
		branchGroup.addChild(selectedLeaves);
	}
	
	@Override
	public DomainProvider getDomainProvider()
	{
		return null;
	}		
	
	/*
	public void setSelectionFilter(Collection types)
	{
	}
	*/

	@Override
	public void domainsChangedPerform(int[] ids)
	{
	}

	@Override
	public void setDomainVisible(Map<Integer, Boolean> map)
	{
	}
	
	public void setFreeEdgesVisible(boolean visible)
	{
	}
	
	public void setMultiEdgesVisible(boolean visible)
	{
	}
	
	public Component getResultPalette()
	{
		//TODO
		return new JPanel();
	}

	public TIntHashSet getResultSet()
	{
		return leaves;
	}

	@Override
	public void pick(PickViewable result)
	{		
		if(result != null && result.getIntersection() != null &&
			result.getIntersection().getGeometryArray() instanceof QuadArray)
		{						
			pickSolid(result, true);
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
		selected = !leaves.contains(solidID);
		if(selected)
		{
			Logger.getLogger("global").finest("cellid= "+solidID);
			highlightNode(solidID, pi.getGeometryArray());
		}
		else
		{
			selectedLeaves.removeGeometry(leavesGeometry.get(solidID));
			leavesGeometry.remove(solidID);
			leaves.remove(solidID);
		}
	}

	private void highlightNode(int solidID, Object obj)
	{
		if (obj instanceof QuadArray) {
			QuadArray geom = (QuadArray) obj;
			
			QuadArray qa=new QuadArray(24, GeometryArray.COORDINATES);
			float [] coord = new float[72];
			geom.getCoordinates(24*solidID, coord);
			qa.setCapability(GeometryArray.ALLOW_COORDINATE_READ);
			qa.setCoordinates(0, coord);
/*
			QuadArray qa=new QuadArray(24, QuadArray.COORDINATES);
*/
			selectedLeaves.addGeometry(qa);
			leavesGeometry.put(solidID, qa);
			leaves.add(solidID);
		}
	}	

	@SuppressWarnings("unchecked")
	public void highlight(int domainID, boolean selected)
	{
		for (Enumeration enumer = branchGroup.getAllChildren(); enumer.hasMoreElements(); )
		{
			Shape3D shape3D = (Shape3D) enumer.nextElement();
			for (Enumeration enumer2 = shape3D.getAllGeometries(); enumer2.hasMoreElements(); )
			{
				Object obj = enumer2.nextElement();
				try {
					highlightNode(domainID, obj);
				} catch (ArrayIndexOutOfBoundsException e) {
					//ignore
				}
			}
		}
	}
		
	@Override
	public Node getJ3DNode()
	{
		return branchGroup;
	}

	@Override
	public void unselectAll()
	{
		selectedLeaves.removeAllGeometries();
		leaves.clear();
		leavesGeometry.clear();
	}
	
	public void setName(String name)
	{
		this.name=name;
	}
	
	@Override
	public String toString()
	{
		if(name==null)
			return super.toString();
		return name;
	}
	
	@Override
	public void addSelectionListener(SelectionListener listener)
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeSelectionListener(SelectionListener listener)
	{
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}
}
