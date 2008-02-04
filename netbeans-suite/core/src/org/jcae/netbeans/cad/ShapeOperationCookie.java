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

public class ShapeOperationCookie implements ViewCookie
{	
	private class MySelectionListener implements SelectionListener
	{
		private ViewableCAD viewable;
		
		public MySelectionListener(ViewableCAD viewable)
		{
			this.viewable=viewable;
		}
		
		public void selectionChanged()
		{
			ShapePool p=getPool();
			final ArrayList nodes=new ArrayList();
			CADSelection[] selection=viewable.getSelection();
			for(int j=0; j<selection.length; j++)
			{
				int[] ids=selection[0].getFaceIDs();
				for(int i=0; i<ids.length; i++)
				{				
					Node n=p.getNode(getSubFace(ids[i]));
					if(n!=null)
						nodes.add(n);
				}
			}

			Runnable r=new Runnable()
			{
				public void run()
				{
					ExplorerManager[] exs=getExplorerManagers();
					for(int i=0; i<exs.length; i++)
					{
						ArrayList nnodes=new ArrayList();
						for(int j=0; j<nodes.size(); j++)
						{
							Node n=(Node) nodes.get(j);
							Collection nn=GeomUtils.findNode(exs[i].getRootContext(), n);
							nnodes.addAll(nn);
						}
						
						try
						{
							exs[i].setSelectedNodes((Node[]) nnodes.toArray(new Node[nnodes.size()]));						
						}
						catch (PropertyVetoException e)
						{					
							ErrorManager.getDefault().notify(e);
						}
					}
				}
			};
			SwingUtilities.invokeLater(r);		
		}
	}
	
	private Node node;
	public ShapeOperationCookie(Node node)
	{
		this.node=node;
	}
	
	private TopoDS_Shape getShape()
	{
		ShapeCookie sc=(ShapeCookie) node.getCookie(ShapeCookie.class);
		return sc.getShape();
	}
	
	private ShapePool getPool()
	{
		return (ShapePool) node.getCookie(ShapePool.class);
	}
	
	public void explode(int type)
	{
		TopoDS_Shape shape = getShape();
		if(shape==null)
			return;
		TopExp_Explorer explorer = new TopExp_Explorer();
		TopLoc_Location loc = new TopLoc_Location();					
		Collection l=new ArrayList();
		ShapePool shapePool=getPool();
		for (explorer.init(shape, type); explorer.more(); explorer.next())
		{
			TopoDS_Shape s=explorer.current();
			String label=shapePool.getName(s);
			if(label==null)
			{
				shapePool.putName(s);
			}
			l.add(s);
		}
		
		// ensure there are no doublons
		ShapeChildren mc=(ShapeChildren) node.getChildren();
		mc.addShapes(l);
	}
	
	public void view()
	{
		TopoDS_Shape shape = getShape();
		View3D v=View3DManager.getDefault().getView3D();
		ViewableCAD viewable = new ViewableCAD(new OCCProvider(shape));
		viewable.addSelectionListener(new MySelectionListener(viewable));
		viewable.setName(node.getName());
		v.add(viewable);
		v.getView().fitAll();
	}

	private TopoDS_Face getSubFace(int id)
	{
		TopExp_Explorer exp=new TopExp_Explorer(getShape(), TopAbs_ShapeEnum.FACE);
		for(int i=0; i<id; i++)
			exp.next();
		return (TopoDS_Face) exp.current();
	}
	
	private static ExplorerManager[] getExplorerManagers()
	{
		ArrayList al=new ArrayList();
		
		Mode[] ms=(Mode[]) WindowManager.getDefault().getModes()
			.toArray(new Mode[0]);
		
		for(int i=0; i<ms.length; i++)
		{
			TopComponent[] ts = ms[i].getTopComponents();
			for(int j=0; j<ts.length; j++)
			{
				if(ts[j] instanceof ExplorerManager.Provider)
					al.add(((ExplorerManager.Provider)ts[j]).getExplorerManager());
			}			
		}
		return (ExplorerManager[]) al.toArray(new ExplorerManager[al.size()]);
	}


}
