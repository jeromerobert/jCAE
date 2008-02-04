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

package org.jcae.netbeans.cad;

import java.awt.Color;
import org.jcae.netbeans.viewer3d.View3D;
import org.jcae.netbeans.viewer3d.View3DManager;
import org.jcae.opencascade.jni.BRep_Builder;
import org.jcae.opencascade.jni.ShapeAnalysis_FreeBounds;
import org.jcae.opencascade.jni.TopoDS_Compound;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.jcae.viewer3d.cad.ViewableCAD;
import org.jcae.viewer3d.cad.occ.OCCProvider;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;

public class FreeBoundsAction extends CookieAction
{
	protected int mode()
	{
		return CookieAction.MODE_ALL;
	}

	protected Class[] cookieClasses()
	{
		return new Class[]{ShapeCookie.class};
	}

	protected void performAction(Node[] arg0)
	{
		BRep_Builder bb = new BRep_Builder();
		TopoDS_Compound tc=new TopoDS_Compound();
		bb.makeCompound(tc);		
		for(int i=0; i<arg0.length; i++)
		{
			TopoDS_Shape shape = GeomUtils.getShape(arg0[i]);
			ShapeAnalysis_FreeBounds safb=new ShapeAnalysis_FreeBounds(shape);
			TopoDS_Compound closedWires=safb.getClosedWires();
			TopoDS_Compound openWires=safb.getOpenWires();
			
			if(closedWires!=null)
				bb.add(tc, closedWires);
			
			if(openWires!=null)
				bb.add(tc, openWires);
		}
		View3D v=View3DManager.getDefault().getView3D();
		OCCProvider occp=new OCCProvider(tc);
		occp.setEdgeColor(Color.GREEN);
		ViewableCAD viewable = new ViewableCAD(occp);
		viewable.setName(arg0[0].getName()+" free edges");
		viewable.setLineWidth(3f);
		v.add(viewable);
	}

	public String getName()
	{
		return "Free edges";
	}

	public HelpCtx getHelpCtx()
	{
		return null;
	}
	
	protected boolean asynchronous()
	{
		return false;
	}
}
