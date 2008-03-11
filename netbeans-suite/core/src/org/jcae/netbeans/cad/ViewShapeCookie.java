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

import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Collection;
import javax.swing.SwingUtilities;
import org.jcae.netbeans.viewer3d.View3D;
import org.jcae.netbeans.viewer3d.View3DManager;
import org.jcae.opencascade.jni.*;
import org.jcae.viewer3d.cad.CADSelection;
import org.jcae.viewer3d.SelectionListener;
import org.jcae.viewer3d.cad.ViewableCAD;
import org.jcae.viewer3d.cad.occ.OCCProvider;
import org.openide.ErrorManager;
import org.openide.cookies.ViewCookie;
import org.openide.explorer.ExplorerManager;
import org.openide.nodes.Node;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

public class ViewShapeCookie implements ViewCookie
{	
	private class MySelectionListener implements SelectionListener
	{
		private final ViewableCAD viewable;
		private final NbShape shape;
		
		public MySelectionListener(ViewableCAD viewable, NbShape shape)
		{
			this.viewable=viewable;
			this.shape = shape.getRootShape();
		}
		
		public void selectionChanged()
		{
			final ArrayList<Node> nodes=new ArrayList<Node>();
			for(CADSelection cs:viewable.getSelection())
			{
				for(int i:cs.getFaceIDs())
				{
					NbShape s = shape.getShapeFromID(i+1, TopAbs_ShapeEnum.FACE);
					Node n = s.getNode();
					if(n!=null)
						nodes.add(n);
				}
			}

			SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					for(ExplorerManager exm:getExplorerManagers())
					{
						ArrayList<Node> nnodes=new ArrayList<Node>();
						for(Node n:nodes)
							for(Node mn:findModuleNodes(exm))
								nnodes.addAll(GeomUtils.findNode(mn, n));
						
						try
						{
							exm.setSelectedNodes(nnodes.toArray(
								new Node[nnodes.size()]));				
						}
						catch (PropertyVetoException e)
						{					
							ErrorManager.getDefault().notify(e);
						}
					}
				}
			});
		}
	}
	
	private final Node node;
	public ViewShapeCookie(Node node)
	{
		this.node=node;
	}
	
	public void view()
	{
		NbShape nbShape = GeomUtils.getShape(node);
		View3D v=View3DManager.getDefault().getView3D();
		ViewableCAD viewable = new ViewableCAD(new OCCProvider(nbShape.getImpl()));
		viewable.addSelectionListener(new MySelectionListener(viewable, nbShape));
		viewable.setName(node.getName());
		v.add(viewable);
		v.getView().fitAll();
	}
	
	private static ExplorerManager[] getExplorerManagers()
	{
		ArrayList<ExplorerManager> al=new ArrayList<ExplorerManager>();
		
		for(Mode m:WindowManager.getDefault().getModes())
		{
			for(TopComponent t:m.getTopComponents())
				if(t instanceof ExplorerManager.Provider)
					al.add(((ExplorerManager.Provider)t).getExplorerManager());
		}
		return al.toArray(new ExplorerManager[al.size()]);
	}

	/** Return all ModuleNode
	 * @param exm
	 * @return*/
	private static Collection<Node> findModuleNodes(ExplorerManager exm)
	{
		ArrayList<Node> toReturn = new ArrayList<Node>();
		for(Node n:exm.getRootContext().getChildren().getNodes())
		{
			for(Node nn:n.getChildren().getNodes())
			{
				ModuleNode mn = nn.getLookup().lookup(ModuleNode.class);
				if(mn != null)
				{
					toReturn.add(nn);
					break;
				}
			}
		}
		return toReturn;
	}
}
