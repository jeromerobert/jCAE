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
import org.jcae.netbeans.viewer3d.SelectionManager;
import org.jcae.netbeans.viewer3d.ViewManager;
import org.jcae.opencascade.jni.BRep_Builder;
import org.jcae.opencascade.jni.ShapeAnalysis_FreeBounds;
import org.jcae.opencascade.jni.TopoDS_Compound;
import org.jcae.opencascade.jni.TopoDS_Shape;
import org.jcae.vtk.AbstractNode.MapperCustomiser;
import org.jcae.vtk.View;
import org.jcae.vtk.ViewableCAD;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.actions.CookieAction;
import vtk.vtkMapper;

public class FreeBoundsAction extends CookieAction
{
	protected int mode()
	{
		return CookieAction.MODE_ALL;
	}

	protected Class[] cookieClasses()
	{
		return new Class[]{NbShape.class};
	}

	protected void performAction(Node[] arg0)
	{	
		View v=ViewManager.getDefault().getCurrentView();
		for(Node node : arg0) {
			ViewableCAD viewable = new ViewableCAD(GeomUtils.getShape(node).getImpl(),true);
			viewable.setName(node.getName() + " free edges");
			viewable.setEdgeSize(viewable.getEdgeSize() * 2);
			
			v.add(viewable);
		}
		/*View v=ViewManager.getDefault().getCurrentView();
		OCCProvider occp=new OCCProvider(tc);
		occp.setEdgeColor(Color.GREEN);
		
		 TODO : J3D Version
		ViewableCAD viewable = new ViewableCAD(occp);
		viewable.setName(arg0[0].getName()+" free edges");
		viewable.setLineWidth(3f);
		v.add(viewable);
		 */
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
