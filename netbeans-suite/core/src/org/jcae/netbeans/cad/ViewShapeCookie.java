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

public class ViewShapeCookie implements ViewCookie
{

	private final Node node;
	WeakReference<ViewableCAD> viewableRef = new WeakReference<ViewableCAD>(null);

	public ViewShapeCookie(Node node)
	{
		this.node = node;
	}

	public void view()
	{
		NbShape nbShape = GeomUtils.getShape(node);
		View v = ViewManager.getDefault().getCurrentView();

		// TODO PLACE IT
		NbShape root = GeomUtils.getShape(node).getRootShape();

		// Force garbage collection to be sure the weak reference is cleared if needed
		System.gc();
		ViewableCAD viewable = viewableRef.get();		
		if(viewable == null)
		{
			viewable = new ViewableCAD(nbShape.getImpl());
			viewableRef = new WeakReference<ViewableCAD>(viewable);
			viewable.setName(node.getName());
			
			SelectionManager.getDefault().addInteractor(viewable, root);

			// Create a CAOSelection ?
			/*if (SelectionManager.getDefault().getEntitySelection(root) == null)
			{
				CADSelection caoSelection = new CADSelection(root);
				SelectionManager.getDefault().addEntitySelection(
						root, caoSelection);
			}*/
			
			v.add(viewable);
		}
	}
}
