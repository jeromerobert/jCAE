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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Collection;
import javax.swing.SwingUtilities;
import org.jcae.netbeans.Utilities;
import org.jcae.netbeans.viewer3d.View3D;
import org.jcae.netbeans.viewer3d.View3DManager;
import org.jcae.opencascade.jni.TopAbs_ShapeEnum;
import org.jcae.viewer3d.cad.CADSelection;
import org.jcae.viewer3d.SelectionListener;
import org.jcae.viewer3d.cad.ViewableCAD;
import org.openide.ErrorManager;
import org.openide.cookies.ViewCookie;
import org.openide.explorer.ExplorerManager;
import org.openide.nodes.Node;

public class ViewShapeCookie implements ViewCookie
{	
	public static class Viewable extends ViewableCAD
		implements PropertyChangeListener, SelectionListener
	{
		private final NbShape shape;
		private boolean selectionLock;
		protected Viewable(NbShape shape)
		{
			super(shape.getOCCProvider());
			this.shape = shape;
			addSelectionListener(this);
		}
		
		public NbShape getShape()
		{
			return shape;
		}

		public void propertyChange(PropertyChangeEvent evt)
		{
			if(evt.getPropertyName().equals(ExplorerManager.PROP_SELECTED_NODES)
				&& evt.getNewValue() instanceof Node[] && !selectionLock)
			{
				selectionLock = true;
				unselectAll();
				Node[] nodes = (Node[]) evt.getNewValue();
				for(Node n:nodes)
				{
					NbShape s = GeomUtils.getShape(n);
					if(s != null && s.getType()==TopAbs_ShapeEnum.FACE)
					{
						int id = s.getID(shape);
						if(id>0)
							highlightFace(id-1, true);
					}
				}
				selectionLock = false;
			}
		}

		public void selectionChanged()
		{
			if(selectionLock)
				return;
			selectionLock = true;
			final ArrayList<Node> nodes=new ArrayList<Node>();
			for(CADSelection cs:getSelection())
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
					for(ExplorerManager exm:Utilities.getExplorerManagers())
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
			selectionLock = false;
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
		ViewableCAD viewable = new Viewable(nbShape);	
		viewable.setName(node.getName());
		v.add(viewable);
		v.getView().fitAll();
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
