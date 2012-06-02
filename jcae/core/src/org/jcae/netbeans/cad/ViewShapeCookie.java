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
 * (C) Copyright 2008, by EADS France
 */
package org.jcae.netbeans.cad;

import java.lang.ref.WeakReference;
import org.jcae.netbeans.viewer3d.SelectionManager;
import org.jcae.netbeans.viewer3d.ViewManager;
import org.jcae.vtk.View;
import org.jcae.vtk.ViewableCAD;
import org.openide.cookies.ViewCookie;
import org.openide.nodes.Node;
import org.openide.util.Lookup.Result;

public class ViewShapeCookie implements ViewCookie
{
	private Node node;
	WeakReference<ViewableCAD> viewableRef = new WeakReference<ViewableCAD>(null);

	public ViewShapeCookie() {
	}

	public ViewShapeCookie(Node node) {
		this.node = node;
	}

	public void setNode(Node node) {
		this.node = node;
	}

	public void view()
	{
		Node realNode = getCurrentBRepNode();
		NbShape nbShape = GeomUtils.getShape(node);
		View v = ViewManager.getDefault().getCurrentView();
		// TODO PLACE IT
		NbShape root = GeomUtils.getShape(node).getRootShape();
		ViewableCAD viewable = viewableRef.get();		
		if(viewable == null)
		{
			viewable = new NViewableCAD(nbShape, realNode);
			viewableRef = new WeakReference<ViewableCAD>(viewable);
			viewable.setName(node.getName());
			
			SelectionManager.getDefault().addInteractor(viewable, root);
		}
		else
			viewable.refresh();
		v.add(viewable);
	}

	private Node getCurrentBRepNode()
	{
		//To be able to highlight the node in the tree after a picking we need
		//the wrapped node (FilterNode)
		Result<Node> lr = org.openide.util.Utilities.actionsGlobalContext()
			.lookupResult(Node.class);
		for(Node n:lr.allInstances())
		{
			Node cn = n;
			NbShape s = cn.getLookup().lookup(NbShape.class);
			BrepDataObject sf = cn.getLookup().lookup(BrepDataObject.class);
			while(sf == null && s != null)
			{
				cn = cn.getParentNode();
				s = cn.getLookup().lookup(NbShape.class);
				sf = cn.getLookup().lookup(BrepDataObject.class);
			}
			if(sf != null)
				return cn;
		}
		throw new IllegalStateException("Cannot find the any BRepNode associated to "+
			lr.allInstances()+" in a cookies of "+node);
	}
}
